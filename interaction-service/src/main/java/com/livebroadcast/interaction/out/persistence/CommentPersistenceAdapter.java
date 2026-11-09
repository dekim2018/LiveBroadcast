package com.livebroadcast.interaction.out.persistence;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import com.livebroadcast.interaction.app.PageResult;
import com.livebroadcast.interaction.app.port.out.CommentRepositoryPort;
import com.livebroadcast.interaction.domain.comment.Comment;

@Component
public class CommentPersistenceAdapter implements CommentRepositoryPort {

    private final CommentJpaRepository repository;

    public CommentPersistenceAdapter(CommentJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Comment save(Comment c) {
        return toDomain(repository.save(new CommentJpaEntity(c.getId(), c.getBroadcastId(), c.getAuthorId(),
                c.getAuthorNickname(), c.getContent(), c.getCreatedAt(), c.getUpdatedAt())));
    }

    @Override
    public Optional<Comment> findById(Long id) {
        return repository.findById(id).map(CommentPersistenceAdapter::toDomain);
    }

    @Override
    public PageResult<Comment> findLatestByBroadcast(Long broadcastId, int page, int size) {
        Page<CommentJpaEntity> result = repository.findByBroadcastId(broadcastId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id")));
        return new PageResult<>(result.getContent().stream().map(CommentPersistenceAdapter::toDomain).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Override
    public void delete(Long id) {
        repository.deleteById(id);
    }

    private static Comment toDomain(CommentJpaEntity e) {
        return new Comment(e.getId(), e.getBroadcastId(), e.getAuthorId(), e.getAuthorNickname(), e.getContent(),
                e.getCreatedAt(), e.getUpdatedAt());
    }
}
