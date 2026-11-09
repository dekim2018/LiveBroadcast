package com.livebroadcast.broadcast.domain.event;

import java.time.Instant;

/** 미디어 서버에서 해당 경로의 송출이 끊김. 방송 상태와 관계없이 미디어 서버가 알려온 사실이다. */
public record MediaStreamStopped(String mediaPath, Instant stoppedAt) {
}
