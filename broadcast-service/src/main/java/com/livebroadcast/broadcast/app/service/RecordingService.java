package com.livebroadcast.broadcast.app.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.livebroadcast.broadcast.app.MediaProperties;
import com.livebroadcast.broadcast.app.PageResult;
import com.livebroadcast.broadcast.app.port.in.RecordingUseCase;
import com.livebroadcast.broadcast.app.port.out.MediaServerPort;
import com.livebroadcast.broadcast.app.port.out.RecordingRepositoryPort;
import com.livebroadcast.broadcast.domain.BroadcastErrors;
import com.livebroadcast.broadcast.domain.RecordedSegment;
import com.livebroadcast.broadcast.domain.Recording;

@Service
public class RecordingService implements RecordingUseCase {

    /** 녹화 시작 시각과 실제 파일 시작 시각 사이의 오차를 흡수하는 여유 시간 */
    private static final Duration MARGIN = Duration.ofSeconds(10);

    private final RecordingRepositoryPort recordingRepository;
    private final MediaServerPort mediaServer;
    private final MediaProperties mediaProperties;

    public RecordingService(RecordingRepositoryPort recordingRepository, MediaServerPort mediaServer,
                            MediaProperties mediaProperties) {
        this.recordingRepository = recordingRepository;
        this.mediaServer = mediaServer;
        this.mediaProperties = mediaProperties;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Recording> list(Long broadcastId, int page, int size) {
        int safeSize = size <= 0 ? 20 : Math.min(size, 50);
        return recordingRepository.findVisible(broadcastId, Math.max(page, 0), safeSize);
    }

    @Override
    @Transactional(readOnly = true)
    public RecordingDetail get(Long recordingId) {
        Recording recording = recordingRepository.findById(recordingId)
                .orElseThrow(BroadcastErrors::recordingNotFound);
        if (!recording.getStatus().isPlayable()) {
            return new RecordingDetail(recording, List.of());
        }
        SegmentWindow window = windowOf(recording);
        List<PlaybackSegment> segments = mediaServer.listRecordedSegments(recording.getMediaPath()).stream()
                .filter(s -> window.contains(s.start()))
                .map(s -> toPlayback(recording.getMediaPath(), s))
                .toList();
        return new RecordingDetail(recording, segments);
    }

    @Override
    @Transactional
    public void delete(Long recordingId, Long requesterId) {
        Recording recording = recordingRepository.findById(recordingId)
                .orElseThrow(BroadcastErrors::recordingNotFound);
        recording.ensureOwner(requesterId);
        if (recording.getStatus().isInProgress()) {
            throw BroadcastErrors.recordingInProgress();
        }
        // 파일을 먼저 지운다. 행만 지우면 같은 경로의 다른 녹화가 남아 있는 한 공개 재생 API 로 계속 볼 수 있다.
        SegmentWindow window = windowOf(recording);
        mediaServer.deleteRecordedSegments(recording.getMediaPath(), window.from(), window.to());
        recordingRepository.delete(recordingId);
    }

    /**
     * 이 녹화에 속한 녹화 파일은 시작 시각으로 고른다.
     * 시작 쪽만 여유를 두고, 직전 녹화가 끝난 시각 이전으로는 넓히지 않는다.
     * 끝 쪽은 녹화 종료 시각까지만 본다. 끊겼다가 바로 다시 송출해도 다음 녹화의 파일이 섞이지 않는다.
     */
    private SegmentWindow windowOf(Recording recording) {
        Instant from = recording.getStartedAt().minus(MARGIN);
        Instant previousEnd = recordingRepository.findPrevious(recording.getMediaPath(), recording.getStartedAt())
                .map(Recording::getEndedAt)
                .orElse(null);
        if (previousEnd != null && previousEnd.isAfter(from)) {
            from = previousEnd;
        }
        return new SegmentWindow(from, recording.getEndedAt());
    }

    private record SegmentWindow(Instant from, Instant to) {
        boolean contains(Instant start) {
            return !start.isBefore(from) && start.isBefore(to);
        }
    }

    private PlaybackSegment toPlayback(String mediaPath, RecordedSegment segment) {
        String start = segment.start().toString();
        String url = mediaProperties.publicPlaybackBase() + "/get"
                + "?path=" + URLEncoder.encode(mediaPath, StandardCharsets.UTF_8)
                + "&start=" + URLEncoder.encode(start, StandardCharsets.UTF_8)
                + "&duration=" + segment.durationSeconds();
        return new PlaybackSegment(url, start, segment.durationSeconds());
    }
}
