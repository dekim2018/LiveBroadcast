package com.livebroadcast.interaction.app.service;

import java.time.Clock;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.livebroadcast.common.error.DomainException;
import com.livebroadcast.interaction.app.AfterCommit;
import com.livebroadcast.interaction.app.PageResult;
import com.livebroadcast.interaction.app.port.in.CommentUseCase;
import com.livebroadcast.interaction.app.port.out.BroadcastQueryPort;
import com.livebroadcast.interaction.app.port.out.CommentRepositoryPort;
import com.livebroadcast.interaction.app.port.out.LiveEventPublisherPort;
import com.livebroadcast.interaction.domain.comment.Comment;

@Service
public class CommentService implements CommentUseCase {

    private final CommentRepositoryPort commentRepository;
    private final BroadcastQueryPort broadcastQuery;
    private final LiveEventPublisherPort eventPublisher;
    private final Clock clock;

    public CommentService(CommentRepositoryPort commentRepository, BroadcastQueryPort broadcastQuery,
                          LiveEventPublisherPort eventPublisher, Clock clock) {
        this.commentRepository = commentRepository;
        this.broadcastQuery = broadcastQuery;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Comment write(Long broadcastId, Long authorId, String authorNickname, String content) {
        broadcastQuery.findBroadcast(broadcastId).orElseThrow(CommentService::broadcastNotFound);
        Comment saved = commentRepository.save(
                Comment.write(broadcastId, authorId, authorNickname, content, clock.instant()));
        AfterCommit.run(() -> eventPublisher.publish(broadcastId, "comment.created", CommentEvent.from(saved)));
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Comment> list(Long broadcastId, int page, int size) {
        int safeSize = size <= 0 ? 50 : Math.min(size, 100);
        return commentRepository.findLatestByBroadcast(broadcastId, Math.max(page, 0), safeSize);
    }

    @Override
    @Transactional
    public Comment edit(Long commentId, Long requesterId, String content) {
        Comment comment = commentRepository.findById(commentId).orElseThrow(CommentService::commentNotFound);
        comment.edit(requesterId, content, clock.instant());
        Comment saved = commentRepository.save(comment);
        AfterCommit.run(() -> eventPublisher.publish(saved.getBroadcastId(), "comment.updated", CommentEvent.from(saved)));
        return saved;
    }

    @Override
    @Transactional
    public void delete(Long commentId, Long requesterId) {
        Comment comment = commentRepository.findById(commentId).orElseThrow(CommentService::commentNotFound);
        comment.ensureAuthor(requesterId);
        commentRepository.delete(commentId);
        Long broadcastId = comment.getBroadcastId();
        AfterCommit.run(() -> eventPublisher.publish(broadcastId, "comment.deleted", Map.of("id", commentId)));
    }

    private static DomainException broadcastNotFound() {
        return DomainException.notFound("BROADCAST_NOT_FOUND", "방송을 찾을 수 없습니다.");
    }

    private static DomainException commentNotFound() {
        return DomainException.notFound("COMMENT_NOT_FOUND", "댓글을 찾을 수 없습니다.");
    }

    public record CommentEvent(Long id, Long broadcastId, Long authorId, String authorNickname, String content,
                               String createdAt, boolean edited) {
        static CommentEvent from(Comment c) {
            return new CommentEvent(c.getId(), c.getBroadcastId(), c.getAuthorId(), c.getAuthorNickname(),
                    c.getContent(), c.getCreatedAt().toString(), c.isEdited());
        }
    }
}
