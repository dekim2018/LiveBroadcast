package com.livebroadcast.interaction.in.web.dto;

import java.time.Instant;

import com.livebroadcast.interaction.domain.comment.Comment;

public record CommentResponse(Long id, Long broadcastId, Long authorId, String authorNickname, String content,
                              Instant createdAt, Instant updatedAt, boolean edited) {

    public static CommentResponse from(Comment c) {
        return new CommentResponse(c.getId(), c.getBroadcastId(), c.getAuthorId(), c.getAuthorNickname(),
                c.getContent(), c.getCreatedAt(), c.getUpdatedAt(), c.isEdited());
    }
}
