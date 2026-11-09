package com.livebroadcast.broadcast.out.persistence;

import java.time.Instant;

import com.livebroadcast.broadcast.domain.BroadcastStatus;
import com.livebroadcast.broadcast.domain.SourceType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "broadcasts")
public class BroadcastJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "owner_nickname", nullable = false, length = 20)
    private String ownerNickname;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private BroadcastStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", length = 20)
    private SourceType sourceType;

    @Column(name = "stream_online", nullable = false)
    private boolean streamOnline;

    @Column(name = "publish_token", length = 64)
    private String publishToken;

    @Column(name = "stream_offline_since")
    private Instant streamOfflineSince;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected BroadcastJpaEntity() {
    }

    BroadcastJpaEntity(Long id, Long ownerId, String ownerNickname, String title, String description,
                       BroadcastStatus status, SourceType sourceType, boolean streamOnline, String publishToken,
                       Instant streamOfflineSince, Instant startedAt, Instant endedAt,
                       Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.ownerId = ownerId;
        this.ownerNickname = ownerNickname;
        this.title = title;
        this.description = description;
        this.status = status;
        this.sourceType = sourceType;
        this.streamOnline = streamOnline;
        this.publishToken = publishToken;
        this.streamOfflineSince = streamOfflineSince;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    Long getId() { return id; }
    Long getOwnerId() { return ownerId; }
    String getOwnerNickname() { return ownerNickname; }
    String getTitle() { return title; }
    String getDescription() { return description; }
    BroadcastStatus getStatus() { return status; }
    SourceType getSourceType() { return sourceType; }
    boolean isStreamOnline() { return streamOnline; }
    String getPublishToken() { return publishToken; }
    Instant getStreamOfflineSince() { return streamOfflineSince; }
    Instant getStartedAt() { return startedAt; }
    Instant getEndedAt() { return endedAt; }
    Instant getCreatedAt() { return createdAt; }
    Instant getUpdatedAt() { return updatedAt; }
}
