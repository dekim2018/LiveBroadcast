package com.livebroadcast.interaction.app.service;

import java.time.Clock;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.livebroadcast.common.error.DomainException;
import com.livebroadcast.interaction.app.PageResult;
import com.livebroadcast.interaction.app.port.in.CommentUseCase;
import com.livebroadcast.interaction.app.port.out.BroadcastQueryPort;
import com.livebroadcast.interaction.app.port.out.CommentRepositoryPort;
import com.livebroadcast.interaction.app.port.out.DomainEventPublisherPort;
import com.livebroadcast.interaction.domain.comment.Comment;
import com.livebroadcast.interaction.domain.comment.CommentDeleted;
import com.livebroadcast.interaction.domain.comment.CommentEdited;
import com.livebroadcast.interaction.domain.comment.CommentWritten;

@Service
public class CommentService implements CommentUseCase {

    private final CommentRepositoryPort commentRepository;
    private final BroadcastQueryPort broadcastQuery;
    private final DomainEventPublisherPort events;
    private final Clock clock;

    public CommentService(CommentRepositoryPort commentRepository, BroadcastQueryPort broadcastQuery,
                          DomainEventPublisherPort events, Clock clock) {
        this.commentRepository = commentRepository;
        this.broadcastQuery = broadcastQuery;
        this.events = events;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Comment write(Long broadcastId, Long authorId, String authorNickname, String content) {
        broadcastQuery.findBroadcast(broadcastId).orElseThrow(CommentService::broadcastNotFound);
        Comment saved = commentRepository.save(
                Comment.write(broadcastId, authorId, authorNickname, content, clock.instant()));
        events.publish(new CommentWritten(saved));
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
        events.publish(new CommentEdited(saved));
        return saved;
    }

    @Override
    @Transactional
    public void delete(Long commentId, Long requesterId) {
        Comment comment = commentRepository.findById(commentId).orElseThrow(CommentService::commentNotFound);
        comment.ensureAuthor(requesterId);
        commentRepository.delete(commentId);
        events.publish(new CommentDeleted(commentId, comment.getBroadcastId()));
    }

    private static DomainException broadcastNotFound() {
        return DomainException.notFound("BROADCAST_NOT_FOUND", "방송을 찾을 수 없습니다.");
    }

    private static DomainException commentNotFound() {
        return DomainException.notFound("COMMENT_NOT_FOUND", "댓글을 찾을 수 없습니다.");
    }
}
