package com.livebroadcast.interaction.in.web;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.livebroadcast.interaction.out.event.SseEventHub;

/**
 * 시청 화면이 구독하는 실시간 이벤트 스트림(댓글, 후원). 비로그인 시청자도 구독할 수 있다.
 */
@RestController
public class LiveEventController {

    private final SseEventHub eventHub;

    public LiveEventController(SseEventHub eventHub) {
        this.eventHub = eventHub;
    }

    @GetMapping(value = "/api/broadcasts/{broadcastId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@PathVariable Long broadcastId) {
        return eventHub.subscribe(broadcastId);
    }
}
