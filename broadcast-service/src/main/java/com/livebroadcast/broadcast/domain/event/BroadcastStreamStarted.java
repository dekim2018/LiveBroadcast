package com.livebroadcast.broadcast.domain.event;

import java.time.Instant;

import com.livebroadcast.broadcast.domain.Broadcast;

/**
 * 방송 중인 방송에 실제 영상이 들어오기 시작함.
 * 같은 트랜잭션 안에서 처리하는 리스너가 방송 정보를 그대로 쓰도록 방송 객체를 담는다.
 */
public record BroadcastStreamStarted(Broadcast broadcast, Instant startedAt) {
}
