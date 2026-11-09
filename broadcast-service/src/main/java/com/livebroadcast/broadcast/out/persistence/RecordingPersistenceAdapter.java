package com.livebroadcast.broadcast.out.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import com.livebroadcast.broadcast.app.PageResult;
import com.livebroadcast.broadcast.app.port.out.RecordingRepositoryPort;
import com.livebroadcast.broadcast.domain.Recording;
import com.livebroadcast.broadcast.domain.RecordingStatus;

@Component
public class RecordingPersistenceAdapter implements RecordingRepositoryPort {

    /** 목록에는 녹화 중인 것과 재생 가능한 것만 보여준다. */
    private static final List<RecordingStatus> VISIBLE = List.of(RecordingStatus.RECORDING, RecordingStatus.READY);

    private final RecordingJpaRepository repository;

    public RecordingPersistenceAdapter(RecordingJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Recording save(Recording r) {
        return toDomain(repository.save(new RecordingJpaEntity(r.getId(), r.getBroadcastId(), r.getOwnerId(),
                r.getOwnerNickname(), r.getTitle(), r.getMediaPath(), r.getStorageLocation(), r.getStatus(),
                r.getStartedAt(), r.getEndedAt(), r.getDurationSeconds(), r.getSegmentCount(),
                r.getLastSegmentPath(), r.getCreatedAt())));
    }

    @Override
    public Optional<Recording> findById(Long id) {
        return repository.findById(id).map(RecordingPersistenceAdapter::toDomain);
    }

    @Override
    public Optional<Recording> findActiveByMediaPath(String mediaPath) {
        return repository.findFirstByMediaPathAndStatusOrderByIdDesc(mediaPath, RecordingStatus.RECORDING)
                .map(RecordingPersistenceAdapter::toDomain);
    }

    @Override
    public Optional<Recording> findPrevious(String mediaPath, Instant startedAt) {
        return repository.findFirstByMediaPathAndStartedAtBeforeOrderByStartedAtDesc(mediaPath, startedAt)
                .map(RecordingPersistenceAdapter::toDomain);
    }

    @Override
    public PageResult<Recording> findVisible(Long broadcastId, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "startedAt"));
        Page<RecordingJpaEntity> result = broadcastId == null
                ? repository.findByStatusIn(VISIBLE, pageable)
                : repository.findByBroadcastIdAndStatusIn(broadcastId, VISIBLE, pageable);
        return new PageResult<>(result.getContent().stream().map(RecordingPersistenceAdapter::toDomain).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Override
    public boolean existsReadyByMediaPath(String mediaPath) {
        return repository.existsByMediaPathAndStatus(mediaPath, RecordingStatus.READY);
    }

    @Override
    public void delete(Long id) {
        repository.deleteById(id);
    }

    static Recording toDomain(RecordingJpaEntity e) {
        return new Recording(e.getId(), e.getBroadcastId(), e.getOwnerId(), e.getOwnerNickname(), e.getTitle(),
                e.getMediaPath(), e.getStorageLocation(), e.getStatus(), e.getStartedAt(), e.getEndedAt(),
                e.getDurationSeconds(), e.getSegmentCount(), e.getLastSegmentPath(), e.getCreatedAt());
    }
}
