package com.livebroadcast.broadcast.out.persistence;

import java.time.Instant;

import com.livebroadcast.broadcast.domain.RecordingStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "recordings")
public class RecordingJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "broadcast_id", nullable = false)
    private Long broadcastId;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "owner_nickname", nullable = false, length = 20)
    private String ownerNickname;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(name = "media_path", nullable = false, length = 50)
    private String mediaPath;

    @Column(name = "storage_location", nullable = false, length = 255)
    private String storageLocation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private RecordingStatus status;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "duration_seconds", nullable = false)
    private long durationSeconds;

    @Column(name = "segment_count", nullable = false)
    private int segmentCount;

    @Column(name = "last_segment_path", length = 255)
    private String lastSegmentPath;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected RecordingJpaEntity() {
    }

    RecordingJpaEntity(Long id, Long broadcastId, Long ownerId, String ownerNickname, String title, String mediaPath,
                       String storageLocation, RecordingStatus status, Instant startedAt, Instant endedAt,
                       long durationSeconds, int segmentCount, String lastSegmentPath, Instant createdAt) {
        this.id = id;
        this.broadcastId = broadcastId;
        this.ownerId = ownerId;
        this.ownerNickname = ownerNickname;
        this.title = title;
        this.mediaPath = mediaPath;
        this.storageLocation = storageLocation;
        this.status = status;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.durationSeconds = durationSeconds;
        this.segmentCount = segmentCount;
        this.lastSegmentPath = lastSegmentPath;
        this.createdAt = createdAt;
    }

    Long getId() { return id; }
    Long getBroadcastId() { return broadcastId; }
    Long getOwnerId() { return ownerId; }
    String getOwnerNickname() { return ownerNickname; }
    String getTitle() { return title; }
    String getMediaPath() { return mediaPath; }
    String getStorageLocation() { return storageLocation; }
    RecordingStatus getStatus() { return status; }
    Instant getStartedAt() { return startedAt; }
    Instant getEndedAt() { return endedAt; }
    long getDurationSeconds() { return durationSeconds; }
    int getSegmentCount() { return segmentCount; }
    String getLastSegmentPath() { return lastSegmentPath; }
    Instant getCreatedAt() { return createdAt; }
}
