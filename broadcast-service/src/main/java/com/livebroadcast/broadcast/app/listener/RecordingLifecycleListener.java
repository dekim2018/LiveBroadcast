package com.livebroadcast.broadcast.app.listener;

import java.time.Instant;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.livebroadcast.broadcast.app.MediaProperties;
import com.livebroadcast.broadcast.app.port.out.RecordingRepositoryPort;
import com.livebroadcast.broadcast.domain.Recording;
import com.livebroadcast.broadcast.domain.event.BroadcastEnded;
import com.livebroadcast.broadcast.domain.event.BroadcastStreamStarted;
import com.livebroadcast.broadcast.domain.event.MediaStreamStopped;

/**
 * 송출 시작/끊김/방송 종료에 맞춰 녹화를 시작하고 마무리한다.
 * 이벤트를 발행한 트랜잭션 안에서 바로 처리해서, 방송 상태와 녹화 상태가 함께 커밋되거나 함께 롤백된다.
 */
@Component
public class RecordingLifecycleListener {

    private final RecordingRepositoryPort recordingRepository;
    private final MediaProperties mediaProperties;

    public RecordingLifecycleListener(RecordingRepositoryPort recordingRepository, MediaProperties mediaProperties) {
        this.recordingRepository = recordingRepository;
        this.mediaProperties = mediaProperties;
    }

    @EventListener
    public void on(BroadcastStreamStarted event) {
        String mediaPath = event.broadcast().mediaPath();
        if (recordingRepository.findActiveByMediaPath(mediaPath).isEmpty()) {
            recordingRepository.save(
                    Recording.start(event.broadcast(), mediaProperties.recordingRoot(), event.startedAt()));
        }
    }

    @EventListener
    public void on(MediaStreamStopped event) {
        finishActive(event.mediaPath(), event.stoppedAt());
    }

    @EventListener
    public void on(BroadcastEnded event) {
        finishActive(event.mediaPath(), event.endedAt());
    }

    private void finishActive(String mediaPath, Instant at) {
        recordingRepository.findActiveByMediaPath(mediaPath).ifPresent(recording -> {
            recording.finish(at);
            recordingRepository.save(recording);
        });
    }
}
