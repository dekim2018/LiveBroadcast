package com.livebroadcast.broadcast.support;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.livebroadcast.broadcast.app.PageResult;
import com.livebroadcast.broadcast.app.port.out.RecordingRepositoryPort;
import com.livebroadcast.broadcast.domain.Recording;
import com.livebroadcast.broadcast.domain.RecordingStatus;

/** 메모리에 녹화를 저장하는 테스트용 저장소 */
public class FakeRecordingRepository implements RecordingRepositoryPort {
    public final Map<Long, Recording> byId = new HashMap<>();
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
