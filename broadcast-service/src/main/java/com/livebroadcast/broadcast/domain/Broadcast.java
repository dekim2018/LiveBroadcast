package com.livebroadcast.broadcast.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 방송 애그리거트. 방송 상태 전이(READY -> LIVE -> ENDED -> LIVE ...)와 소유자 검증 규칙을 가진다.
 *
 * <pre>
 * status     : 방송자가 시작/종료 버튼으로 바꾸는 "방송 상태"
 * streamOnline: 미디어 서버에 실제 영상이 들어오고 있는지 나타내는 "송출 상태" (MediaMTX 훅으로 갱신)
 * </pre>
 */
public class Broadcast {

    private static final Pattern MEDIA_PATH = Pattern.compile("^live/(\\d+)$");
    private static final int MAX_TITLE = 100;
    private static final int MAX_DESCRIPTION = 1000;

    private final Long id;
    private final Long ownerId;
    private final String ownerNickname;
    private String title;
    private String description;
    private BroadcastStatus status;
    private SourceType sourceType;
    private boolean streamOnline;
    private String publishToken;
    private Instant streamOfflineSince;
    private Instant startedAt;
    private Instant endedAt;
    private final Instant createdAt;
    private Instant updatedAt;

    public Broadcast(Long id, Long ownerId, String ownerNickname, String title, String description,
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

    public static Broadcast create(Long ownerId, String ownerNickname, String title, String description, Instant now) {
        return new Broadcast(null, ownerId, ownerNickname, validTitle(title), validDescription(description),
                BroadcastStatus.READY, null, false, null, null, null, null, now, now);
    }

    public static Optional<Long> parseMediaPath(String path) {
        if (path == null) {
            return Optional.empty();
        }
        Matcher m = MEDIA_PATH.matcher(path);
        return m.matches() ? Optional.of(Long.valueOf(m.group(1))) : Optional.empty();
    }

    public static String mediaPathOf(Long broadcastId) {
        return "live/" + broadcastId;
    }

    public void updateInfo(Long requesterId, String title, String description, Instant now) {
        ensureOwner(requesterId);
        this.title = validTitle(title);
        this.description = validDescription(description);
        this.updatedAt = now;
    }

    /**
     * 방송을 시작하고 이번 방송에서만 쓸 수 있는 송출 토큰을 발급한다.
     * 종료된 방송도 다시 시작할 수 있다.
     */
    public void startLive(Long requesterId, SourceType sourceType, String newPublishToken, Instant now) {
        ensureOwner(requesterId);
        if (!status.canTransitionTo(BroadcastStatus.LIVE)) {
            throw BroadcastErrors.alreadyLive();
        }
        this.status = BroadcastStatus.LIVE;
        this.sourceType = Objects.requireNonNull(sourceType, "sourceType");
        this.publishToken = Objects.requireNonNull(newPublishToken, "publishToken");
        this.streamOnline = false;
        // 시작 버튼만 누르고 송출이 끝내 연결되지 않는 경우도 자동 종료 대상이 되도록 기준 시각을 둔다.
        this.streamOfflineSince = now;
        this.startedAt = now;
        this.endedAt = null;
        this.updatedAt = now;
    }

    public void endLive(Long requesterId, Instant now) {
        ensureOwner(requesterId);
        if (!status.canTransitionTo(BroadcastStatus.ENDED)) {
            throw BroadcastErrors.notLive();
        }
        end(now);
    }

    /** 시스템에 의한 종료(송출 끊김 장기화 등). 방송 중이 아니면 아무것도 하지 않는다. */
    public boolean forceEnd(Instant now) {
        if (!status.canTransitionTo(BroadcastStatus.ENDED)) {
            return false;
        }
        end(now);
        return true;
    }

    private void end(Instant now) {
        this.status = BroadcastStatus.ENDED;
        // 토큰을 지우면 이후 같은 토큰으로 재접속(재송출)하려 해도 미디어 서버 인증에서 거절된다.
        this.publishToken = null;
        this.streamOnline = false;
        this.streamOfflineSince = null;
        this.endedAt = now;
        this.updatedAt = now;
    }

    public void ensureDeletable(Long requesterId) {
        ensureOwner(requesterId);
        if (!status.isDeletable()) {
            throw BroadcastErrors.deleteWhileLive();
        }
    }

    /** 송출 정보 재조회처럼 방송 중에만 할 수 있는 작업 전에 확인한다. */
    public void ensureOnAir() {
        if (!status.isOnAir()) {
            throw BroadcastErrors.notLive();
        }
    }

    public boolean canPublishWith(String token) {
        if (!status.isOnAir() || publishToken == null || token == null) {
            return false;
        }
        return MessageDigest.isEqual(
                publishToken.getBytes(StandardCharsets.UTF_8),
                token.getBytes(StandardCharsets.UTF_8));
    }

    public boolean isWatchable() {
        return status.isOnAir();
    }

    public void markStreamOnline(Instant now) {
        if (!status.isOnAir()) {
            return;
        }
        this.streamOnline = true;
        this.streamOfflineSince = null;
        this.updatedAt = now;
    }

    public void markStreamOffline(Instant now) {
        this.streamOnline = false;
        if (status.isOnAir()) {
            this.streamOfflineSince = now;
        }
        this.updatedAt = now;
    }

    /** 방송 중인데 송출이 grace 이상 끊겨 있으면 true */
    public boolean isStale(Instant now, Duration grace) {
        return status.isOnAir()
                && !streamOnline
                && streamOfflineSince != null
                && streamOfflineSince.plus(grace).isBefore(now);
    }

    public boolean isOwnedBy(Long userId) {
        return ownerId.equals(userId);
    }

    public void ensureOwner(Long requesterId) {
        if (requesterId == null || !isOwnedBy(requesterId)) {
            throw BroadcastErrors.notOwner();
        }
    }

    public String mediaPath() {
        return mediaPathOf(id);
    }

    private static String validTitle(String title) {
        if (title == null || title.isBlank() || title.strip().length() > MAX_TITLE) {
            throw BroadcastErrors.invalidTitle();
        }
        return title.strip();
    }

    private static String validDescription(String description) {
        if (description == null) {
            return "";
        }
        if (description.length() > MAX_DESCRIPTION) {
            throw BroadcastErrors.invalidDescription();
        }
        return description.strip();
    }

    public Long getId() { return id; }
    public Long getOwnerId() { return ownerId; }
    public String getOwnerNickname() { return ownerNickname; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public BroadcastStatus getStatus() { return status; }
    public SourceType getSourceType() { return sourceType; }
    public boolean isStreamOnline() { return streamOnline; }
    public String getPublishToken() { return publishToken; }
    public Instant getStreamOfflineSince() { return streamOfflineSince; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getEndedAt() { return endedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
