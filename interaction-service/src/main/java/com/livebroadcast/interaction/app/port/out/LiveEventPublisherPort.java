package com.livebroadcast.interaction.app.port.out;

public interface LiveEventPublisherPort {

    void publish(Long broadcastId, String eventName, Object payload);
}
