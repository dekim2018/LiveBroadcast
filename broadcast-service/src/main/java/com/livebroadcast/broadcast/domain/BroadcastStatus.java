package com.livebroadcast.broadcast.domain;

/**
 * 방송 상태와 상태별 규칙. 상태를 추가하거나 전이 규칙을 바꿀 때는 이 enum 만 고친다.
 *
 * <pre>
 * READY -> LIVE -> ENDED -> LIVE ...
 * </pre>
 */
public enum BroadcastStatus {
    /** 방송 정보만 등록된 상태 */
    READY,
    /** 방송 중 (송출 권한이 발급된 상태) */
    LIVE,
    /** 방송 종료 */
    ENDED;

    /** 이 상태에서 next 로 바뀔 수 있으면 true */
    public boolean canTransitionTo(BroadcastStatus next) {
        return switch (this) {
            case READY, ENDED -> next == LIVE;
            case LIVE -> next == ENDED;
        };
    }

    /** 송출과 시청이 허용되는 상태 */
    public boolean isOnAir() {
        return this == LIVE;
    }

    /** 방송 정보를 삭제할 수 있는 상태 */
    public boolean isDeletable() {
        return !isOnAir();
    }
}
