package com.livebroadcast.interaction.app.port.out;

/**
 * 도메인 이벤트 발행. 후속 처리(실시간 전송 등)는 이벤트 리스너가 맡는다.
 * 새 후속 처리(후원 랭킹, 알림 등)가 필요하면 서비스를 고치지 않고 리스너를 추가한다.
 */
public interface DomainEventPublisherPort {

    void publish(Object event);
}
