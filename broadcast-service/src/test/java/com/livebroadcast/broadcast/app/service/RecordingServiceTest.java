package com.livebroadcast.broadcast.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.livebroadcast.broadcast.app.MediaProperties;
import com.livebroadcast.broadcast.app.PageResult;
import com.livebroadcast.broadcast.app.port.in.RecordingUseCase.PlaybackSegment;
import com.livebroadcast.broadcast.app.port.out.MediaServerPort;
import com.livebroadcast.broadcast.app.port.out.RecordingRepositoryPort;
import com.livebroadcast.broadcast.domain.RecordedSegment;
import com.livebroadcast.broadcast.domain.Recording;
import com.livebroadcast.broadcast.domain.RecordingStatus;

/**
 * 같은 방송에서 끊겼다가 바로 다시 송출해 녹화가 둘로 나뉘었을 때, 각 녹화가 자기 파일만 재생/삭제하는지 검증한다.
 */
class RecordingServiceTest {

    private static final String PATH = "live/1";
    private static final Long OWNER = 7L;
    private static final Instant T0 = Instant.parse("2026-11-10T00:00:00Z");

    private final FakeRecordingRepository repository = new FakeRecordingRepository();
    private final FakeMediaServer mediaServer = new FakeMediaServer();
    private final RecordingService service = new RecordingService(repository, mediaServer,
            new MediaProperties("http://api", "http://playback", null, null, null, null));

    // 첫 송출 60초, 5초 끊김 후 다시 60초
    private final Recording first = repository.save(recording(T0, T0.plusSeconds(60)));
    private final Recording second = repository.save(recording(T0.plusSeconds(65), T0.plusSeconds(125)));

    {
        mediaServer.segments.add(new RecordedSegment(T0.plusMillis(300), 59.5));
        mediaServer.segments.add(new RecordedSegment(T0.plusSeconds(65).plusMillis(300), 59.5));
    }

    @Test
    void 바로_이어진_다음_녹화의_파일은_재생_목록에_섞이지_않는다() {
        List<PlaybackSegment> firstSegments = service.get(first.getId()).segments();
        List<PlaybackSegment> secondSegments = service.get(second.getId()).segments();

        assertThat(firstSegments).extracting(PlaybackSegment::start).containsExactly("2026-11-10T00:00:00.300Z");
        assertThat(secondSegments).extracting(PlaybackSegment::start).containsExactly("2026-11-10T00:01:05.300Z");
    }

    @Test
    void 녹화를_지우면_그_녹화의_파일만_미디어_서버에서_지운다() {
        service.delete(first.getId(), OWNER);

        assertThat(mediaServer.segments).extracting(RecordedSegment::start)
                .containsExactly(T0.plusSeconds(65).plusMillis(300));
        assertThat(repository.findById(first.getId())).isEmpty();
    }

    @Test
    void 녹화_중인_영상은_지울_수_없다() {
        Recording active = repository.save(new Recording(null, 1L, OWNER, "방송자", "제목", PATH, "/recordings/" + PATH,
                RecordingStatus.RECORDING, T0.plusSeconds(200), null, 0, 0, null, T0));

        assertThatThrownBy(() -> service.delete(active.getId(), OWNER)).hasMessageContaining("녹화 중");
        assertThat(mediaServer.segments).hasSize(2);
    }

    private static Recording recording(Instant start, Instant end) {
        return new Recording(null, 1L, OWNER, "방송자", "제목", PATH, "/recordings/" + PATH, RecordingStatus.READY,
                start, end, end.getEpochSecond() - start.getEpochSecond(), 1, null, start);
    }

    static class FakeMediaServer implements MediaServerPort {
        final List<RecordedSegment> segments = new ArrayList<>();

        @Override
        public void kickPublisher(String mediaPath) {
        }

        @Override
        public List<RecordedSegment> listRecordedSegments(String mediaPath) {
            return List.copyOf(segments);
        }

        @Override
        public void deleteRecordedSegments(String mediaPath, Instant from, Instant to) {
            segments.removeIf(s -> !s.start().isBefore(from) && s.start().isBefore(to));
        }
    }

    static class FakeRecordingRepository implements RecordingRepositoryPort {
        final Map<Long, Recording> byId = new HashMap<>();
        long seq;

        @Override
        public Recording save(Recording r) {
            Recording saved = new Recording(r.getId() == null ? ++seq : r.getId(), r.getBroadcastId(), r.getOwnerId(),
                    r.getOwnerNickname(), r.getTitle(), r.getMediaPath(), r.getStorageLocation(), r.getStatus(),
                    r.getStartedAt(), r.getEndedAt(), r.getDurationSeconds(), r.getSegmentCount(),
                    r.getLastSegmentPath(), r.getCreatedAt());
            byId.put(saved.getId(), saved);
            return saved;
        }

        @Override
        public Optional<Recording> findById(Long id) {
            return Optional.ofNullable(byId.get(id));
        }

        @Override
        public Optional<Recording> findActiveByMediaPath(String mediaPath) {
            return byId.values().stream()
                    .filter(r -> r.getMediaPath().equals(mediaPath) && r.getStatus() == RecordingStatus.RECORDING)
                    .findFirst();
        }

        @Override
        public Optional<Recording> findPrevious(String mediaPath, Instant startedAt) {
            return byId.values().stream()
                    .filter(r -> r.getMediaPath().equals(mediaPath) && r.getStartedAt().isBefore(startedAt))
                    .max(Comparator.comparing(Recording::getStartedAt));
        }

        @Override
        public PageResult<Recording> findVisible(Long broadcastId, int page, int size) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean existsReadyByMediaPath(String mediaPath) {
            return byId.values().stream()
                    .anyMatch(r -> r.getMediaPath().equals(mediaPath) && r.getStatus() == RecordingStatus.READY);
        }

        @Override
        public void delete(Long id) {
            byId.remove(id);
        }
    }
}
