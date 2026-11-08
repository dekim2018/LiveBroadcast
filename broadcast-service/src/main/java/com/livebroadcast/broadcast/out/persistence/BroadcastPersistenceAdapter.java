package com.livebroadcast.broadcast.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import com.livebroadcast.broadcast.app.PageResult;
import com.livebroadcast.broadcast.app.port.out.BroadcastRepositoryPort;
import com.livebroadcast.broadcast.domain.Broadcast;
import com.livebroadcast.broadcast.domain.BroadcastStatus;

@Component
public class BroadcastPersistenceAdapter implements BroadcastRepositoryPort {

    private static final Sort LATEST = Sort.by(Sort.Direction.DESC, "createdAt");

    private final BroadcastJpaRepository repository;

    public BroadcastPersistenceAdapter(BroadcastJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Broadcast save(Broadcast b) {
        return toDomain(repository.save(toEntity(b)));
    }

    @Override
    public Optional<Broadcast> findById(Long id) {
        return repository.findById(id).map(BroadcastPersistenceAdapter::toDomain);
    }

    @Override
    public Optional<Broadcast> findByIdForUpdate(Long id) {
        return repository.findByIdForUpdate(id).map(BroadcastPersistenceAdapter::toDomain);
    }

    @Override
    public PageResult<Broadcast> findAll(BroadcastStatus status, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, LATEST);
        Page<BroadcastJpaEntity> result = status == null
                ? repository.findAll(pageable)
                : repository.findByStatus(status, pageable);
        return toPage(result);
    }

    @Override
    public PageResult<Broadcast> findByOwner(Long ownerId, int page, int size) {
        return toPage(repository.findByOwnerId(ownerId, PageRequest.of(page, size, LATEST)));
    }

    @Override
    public List<Long> findLiveOfflineIds() {
        return repository.findIdsByStatusAndOffline(BroadcastStatus.LIVE);
    }

    @Override
    public void delete(Long id) {
        repository.deleteById(id);
    }

    private PageResult<Broadcast> toPage(Page<BroadcastJpaEntity> page) {
        return new PageResult<>(
                page.getContent().stream().map(BroadcastPersistenceAdapter::toDomain).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    static BroadcastJpaEntity toEntity(Broadcast b) {
        return new BroadcastJpaEntity(b.getId(), b.getOwnerId(), b.getOwnerNickname(), b.getTitle(),
                b.getDescription(), b.getStatus(), b.getSourceType(), b.isStreamOnline(), b.getPublishToken(),
                b.getStreamOfflineSince(), b.getStartedAt(), b.getEndedAt(), b.getCreatedAt(), b.getUpdatedAt());
    }

    static Broadcast toDomain(BroadcastJpaEntity e) {
        return new Broadcast(e.getId(), e.getOwnerId(), e.getOwnerNickname(), e.getTitle(), e.getDescription(),
                e.getStatus(), e.getSourceType(), e.isStreamOnline(), e.getPublishToken(),
                e.getStreamOfflineSince(), e.getStartedAt(), e.getEndedAt(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
