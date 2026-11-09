package com.livebroadcast.broadcast.domain;

import java.time.Duration;
import java.time.Instant;

/**
 * 한 번의 송출 구간(스트림 시작 ~ 끊김)을 하나의 녹화 영상으로 본다.
 * 실제 파일은 MediaMTX 가 storageLocation 아래에 세그먼트 단위로 저장하고,
 * 재생은 MediaMTX playback 서버가 시간 구간(start, duration)으로 이어 붙여서 제공한다.
 */
public class Recording {

    private static final Duration MIN_DURATION = Duration.ofSeconds(2);

    private final Long id;
    private final Long broadcastId;
    private final Long ownerId;
    private final String ownerNickname;
    private final String title;
    private final String mediaPath;
    private final String storageLocation;
    private RecordingStatus status;
    private final Instant startedAt;
    private Instant endedAt;
    private long durationSeconds;
    private int segmentCount;
    private String lastSegmentPath;
    private final Instant createdAt;

    public Recording(Long id, Long broadcastId, Long ownerId, String ownerNickname, String title, String mediaPath,
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

    public static Recording start(Broadcast broadcast, String recordingRoot, Instant now) {
        String path = broadcast.mediaPath();
        return new Recording(null, broadcast.getId(), broadcast.getOwnerId(), broadcast.getOwnerNickname(),
                broadcast.getTitle(), path, recordingRoot + "/" + path, RecordingStatus.RECORDING,
                now, null, 0, 0, null, now);
    }

    public void addSegment(String segmentPath) {
        this.segmentCount++;
        this.lastSegmentPath = segmentPath;
    }

    public void finish(Instant now) {
        if (!status.isInProgress()) {
            return;
        }
        this.endedAt = now;
        Duration duration = Duration.between(startedAt, now);
        this.durationSeconds = duration.toSeconds();
        this.status = duration.compareTo(MIN_DURATION) >= 0 ? RecordingStatus.READY : RecordingStatus.FAILED;
    }

    public void ensureOwner(Long requesterId) {
        if (requesterId == null || !ownerId.equals(requesterId)) {
            throw BroadcastErrors.notRecordingOwner();
        }
    }

    public Long getId() { return id; }
    public Long getBroadcastId() { return broadcastId; }
    public Long getOwnerId() { return ownerId; }
    public String getOwnerNickname() { return ownerNickname; }
    public String getTitle() { return title; }
    public String getMediaPath() { return mediaPath; }
    public String getStorageLocation() { return storageLocation; }
    public RecordingStatus getStatus() { return status; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getEndedAt() { return endedAt; }
    public long getDurationSeconds() { return durationSeconds; }
    public int getSegmentCount() { return segmentCount; }
    public String getLastSegmentPath() { return lastSegmentPath; }
    public Instant getCreatedAt() { return createdAt; }
}
