package com.livebroadcast.interaction.domain.broadcast;

/**
 * broadcast-service 에서 조회한 방송 정보(읽기 전용 스냅샷).
 */
public record BroadcastInfo(Long id, Long ownerId, String ownerNickname, String title, String status) {
}
