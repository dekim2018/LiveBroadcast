package com.livebroadcast.interaction.out.persistence;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "comments")
public class CommentJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "broadcast_id", nullable = false)
    private Long broadcastId;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(name = "author_nickname", nullable = false, length = 20)
    private String authorNickname;

    @Column(nullable = false, length = 300)
    private String content;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CommentJpaEntity() {
    }

    CommentJpaEntity(Long id, Long broadcastId, Long authorId, String authorNickname, String content,
                     Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.broadcastId = broadcastId;
        this.authorId = authorId;
        this.authorNickname = authorNickname;
        this.content = content;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    Long getId() { return id; }
    Long getBroadcastId() { return broadcastId; }
    Long getAuthorId() { return authorId; }
    String getAuthorNickname() { return authorNickname; }
    String getContent() { return content; }
    Instant getCreatedAt() { return createdAt; }
    Instant getUpdatedAt() { return updatedAt; }
}
