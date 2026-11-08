package com.livebroadcast.broadcast.domain;

public enum BroadcastStatus {
    /** 방송 정보만 등록된 상태 */
    READY,
    /** 방송 중 (송출 권한이 발급된 상태) */
    LIVE,
    /** 방송 종료 */
    ENDED
}
