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
import com.livebroadcast.broadcast.domain.RecordingStatus;

@Service
public class RecordingService implements RecordingUseCase {

    /** 녹화 시작/종료 시각과 실제 파일 시각 사이의 오차를 흡수하는 여유 시간 */
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
        if (recording.getStatus() != RecordingStatus.READY) {
            return new RecordingDetail(recording, List.of());
        }
        Instant from = recording.getStartedAt().minus(MARGIN);
        Instant to = recording.getEndedAt().plus(MARGIN);
        List<PlaybackSegment> segments = mediaServer.listRecordedSegments(recording.getMediaPath()).stream()
                .filter(s -> overlaps(s, from, to))
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
        recordingRepository.delete(recordingId);
    }

    private boolean overlaps(RecordedSegment segment, Instant from, Instant to) {
        Instant segmentEnd = segment.start().plusMillis((long) (segment.durationSeconds() * 1000));
        return segment.start().isBefore(to) && segmentEnd.isAfter(from);
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
