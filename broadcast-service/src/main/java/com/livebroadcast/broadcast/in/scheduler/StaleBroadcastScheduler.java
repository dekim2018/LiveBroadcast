package com.livebroadcast.broadcast.in.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.livebroadcast.broadcast.app.port.in.MediaEventUseCase;

/**
 * 종료 버튼 없이 송출이 끊긴(브라우저 종료, 네트워크 단절) 방송을 주기적으로 종료 처리한다.
 */
@Component
public class StaleBroadcastScheduler {

    private static final Logger log = LoggerFactory.getLogger(StaleBroadcastScheduler.class);

    private final MediaEventUseCase mediaEventUseCase;

    public StaleBroadcastScheduler(MediaEventUseCase mediaEventUseCase) {
        this.mediaEventUseCase = mediaEventUseCase;
    }

    @Scheduled(fixedDelayString = "${app.media.stale-check-interval:60s}", initialDelayString = "30s")
    public void endStaleBroadcasts() {
        int ended = mediaEventUseCase.endStaleBroadcasts();
        if (ended > 0) {
            log.info("자동 종료된 방송 수: {}", ended);
        }
    }
}
