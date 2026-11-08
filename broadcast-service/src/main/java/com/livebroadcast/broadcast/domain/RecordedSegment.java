package com.livebroadcast.broadcast.domain;

import java.time.Instant;

/** 미디어 서버에 실제로 저장된 연속 녹화 구간 */
public record RecordedSegment(Instant start, double durationSeconds) {
}
