package com.livebroadcast.interaction.out.event;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.livebroadcast.interaction.app.port.out.LiveEventPublisherPort;

/**
 * 방송별 시청자에게 댓글/후원 이벤트를 Server-Sent Events 로 밀어준다.
 * 단일 인스턴스 기준 인메모리 구현이며, 여러 인스턴스로 늘릴 때는 Redis Pub/Sub 어댑터로 교체하면 된다.
 */
@Component
public class SseEventHub implements LiveEventPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(SseEventHub.class);
    private static final long TIMEOUT_MS = 30 * 60 * 1000L;
    private static final int MAX_SUBSCRIBERS_PER_BROADCAST = 2000;

    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long broadcastId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
        // 추가와 제거를 같은 키에 대한 compute 안에서 해서, 마지막 시청자가 나가며 목록을 지우는 순간에
        // 새 구독자가 이미 지워진 목록에 들어가 이벤트를 못 받는 경우를 막는다.
        boolean[] added = {false};
        emitters.compute(broadcastId, (id, list) -> {
            List<SseEmitter> current = list == null ? new CopyOnWriteArrayList<>() : list;
            if (current.size() < MAX_SUBSCRIBERS_PER_BROADCAST) {
                current.add(emitter);
                added[0] = true;
            }
            return current.isEmpty() ? null : current;
        });
        if (!added[0]) {
            emitter.complete();
            return emitter;
        }
        Runnable remove = () -> removeEmitter(broadcastId, emitter);
        emitter.onCompletion(remove);
        emitter.onTimeout(remove);
        emitter.onError(e -> remove.run());
        try {
            emitter.send(SseEmitter.event().name("connected").data(Map.of("broadcastId", broadcastId)));
        } catch (IOException e) {
            remove.run();
        }
        return emitter;
    }

    @Override
    public void publish(Long broadcastId, String eventName, Object payload) {
        List<SseEmitter> list = emitters.get(broadcastId);
        if (list == null || list.isEmpty()) {
            return;
        }
        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(payload, MediaType.APPLICATION_JSON));
            } catch (IOException | IllegalStateException e) {
                removeEmitter(broadcastId, emitter);
            }
        }
    }

    /** 프록시(Caddy, Gateway)가 유휴 연결을 끊지 않도록 주기적으로 주석 이벤트를 보낸다. */
    @Scheduled(fixedRate = 20_000)
    public void heartbeat() {
        emitters.forEach((broadcastId, list) -> {
            for (SseEmitter emitter : list) {
                try {
                    emitter.send(SseEmitter.event().comment("ping"));
                } catch (IOException | IllegalStateException e) {
                    removeEmitter(broadcastId, emitter);
                }
            }
        });
    }

    private void removeEmitter(Long broadcastId, SseEmitter emitter) {
        emitters.computeIfPresent(broadcastId, (id, list) -> {
            list.remove(emitter);
            return list.isEmpty() ? null : list;
        });
        log.debug("SSE 구독 해제 broadcastId={}", broadcastId);
    }
}
