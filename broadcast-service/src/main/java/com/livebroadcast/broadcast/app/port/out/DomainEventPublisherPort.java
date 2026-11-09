package com.livebroadcast.broadcast.app.port.out;

/**
 * 도메인 이벤트 발행. 후속 처리(녹화 마무리, 송출 연결 끊기 등)는 이벤트 리스너가 맡는다.
 * 새 후속 처리가 필요하면 서비스를 고치지 않고 리스너를 추가한다.
 */
public interface DomainEventPublisherPort {

    void publish(Object event);
}
