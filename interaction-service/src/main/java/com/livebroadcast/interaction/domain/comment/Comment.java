package com.livebroadcast.interaction.domain.comment;

import java.time.Instant;

import com.livebroadcast.common.error.DomainException;

public class Comment {

    public static final int MAX_LENGTH = 300;

    private final Long id;
    private final Long broadcastId;
    private final Long authorId;
    private final String authorNickname;
    private String content;
    private final Instant createdAt;
    private Instant updatedAt;

    public Comment(Long id, Long broadcastId, Long authorId, String authorNickname, String content,
                   Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.broadcastId = broadcastId;
        this.authorId = authorId;
        this.authorNickname = authorNickname;
        this.content = content;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Comment write(Long broadcastId, Long authorId, String authorNickname, String content, Instant now) {
        return new Comment(null, broadcastId, authorId, authorNickname, validContent(content), now, now);
    }

    public void edit(Long requesterId, String content, Instant now) {
        ensureAuthor(requesterId);
        this.content = validContent(content);
        this.updatedAt = now;
    }

    public void ensureAuthor(Long requesterId) {
        if (requesterId == null || !authorId.equals(requesterId)) {
            throw DomainException.forbidden("NOT_COMMENT_AUTHOR", "댓글 작성자만 수정하거나 삭제할 수 있습니다.");
        }
    }

    public boolean isEdited() {
        return updatedAt != null && updatedAt.isAfter(createdAt);
    }

    private static String validContent(String content) {
        if (content == null || content.isBlank()) {
            throw DomainException.badRequest("INVALID_COMMENT", "댓글 내용을 입력해 주세요.");
        }
        String stripped = content.strip();
        if (stripped.length() > MAX_LENGTH) {
            throw DomainException.badRequest("INVALID_COMMENT", "댓글은 " + MAX_LENGTH + "자 이하여야 합니다.");
        }
        return stripped;
    }

    public Long getId() { return id; }
    public Long getBroadcastId() { return broadcastId; }
    public Long getAuthorId() { return authorId; }
    public String getAuthorNickname() { return authorNickname; }
    public String getContent() { return content; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
