package com.livebroadcast.broadcast.domain.event;

import java.time.Instant;

/** 방송이 종료됨(방송자가 종료했거나, 송출이 오래 끊겨 시스템이 종료함) */
public record BroadcastEnded(Long broadcastId, String mediaPath, Instant endedAt) {
}
