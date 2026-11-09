package com.livebroadcast.broadcast.app.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.livebroadcast.broadcast.app.MediaProperties;
import com.livebroadcast.broadcast.app.policy.MediaAccessPolicy;
import com.livebroadcast.broadcast.app.port.in.MediaEventUseCase;
import com.livebroadcast.broadcast.app.port.out.BroadcastRepositoryPort;
import com.livebroadcast.broadcast.app.port.out.DomainEventPublisherPort;
import com.livebroadcast.broadcast.app.port.out.RecordingRepositoryPort;
import com.livebroadcast.broadcast.domain.Broadcast;
import com.livebroadcast.broadcast.domain.event.BroadcastEnded;
import com.livebroadcast.broadcast.domain.event.BroadcastStreamStarted;
import com.livebroadcast.broadcast.domain.event.MediaStreamStopped;

@Service
public class MediaEventService implements MediaEventUseCase {

    private static final Logger log = LoggerFactory.getLogger(MediaEventService.class);

    private final BroadcastRepositoryPort broadcastRepository;
    private final RecordingRepositoryPort recordingRepository;
    private final DomainEventPublisherPort events;
    private final Map<String, MediaAccessPolicy> accessPolicies;
    private final MediaProperties mediaProperties;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    public MediaEventService(BroadcastRepositoryPort broadcastRepository, RecordingRepositoryPort recordingRepository,
                             DomainEventPublisherPort events, List<MediaAccessPolicy> accessPolicies,
                             MediaProperties mediaProperties, PlatformTransactionManager transactionManager,
                             Clock clock) {
        this.broadcastRepository = broadcastRepository;
        this.recordingRepository = recordingRepository;
        this.events = events;
        // 같은 action 을 맡는 정책이 둘이면 기동 시점에 실패시킨다.
        this.accessPolicies = accessPolicies.stream()
                .collect(Collectors.toUnmodifiableMap(MediaAccessPolicy::action, Function.identity()));
        this.mediaProperties = mediaProperties;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.clock = clock;
    }

    /**
     * MediaMTX 가 송출(publish), 시청(read), 녹화 재생(playback) 요청마다 호출한다.
     * 실제 판단은 action 별 {@link MediaAccessPolicy} 가 하고, 담당 정책이 없는 action 은 거절한다.
     */
    @Override
    @Transactional(readOnly = true)
    public boolean authorize(MediaAuthCommand command) {
        MediaAccessPolicy policy = command.action() == null ? null : accessPolicies.get(command.action());
        return policy != null && policy.allows(command);
    }

    @Override
    @Transactional
    public void onStreamReady(String mediaPath) {
        Optional<Long> id = Broadcast.parseMediaPath(mediaPath);
        if (id.isEmpty()) {
            return;
        }
        broadcastRepository.findByIdForUpdate(id.get()).ifPresent(broadcast -> {
            Instant now = clock.instant();
            broadcast.markStreamOnline(now);
            broadcastRepository.save(broadcast);
            if (broadcast.isWatchable()) {
                events.publish(new BroadcastStreamStarted(broadcast, now));
            }
            log.info("송출 시작 감지 broadcastId={}", broadcast.getId());
        });
    }

    @Override
    @Transactional
    public void onStreamNotReady(String mediaPath) {
        Optional<Long> id = Broadcast.parseMediaPath(mediaPath);
        if (id.isEmpty()) {
            return;
        }
        Instant now = clock.instant();
        broadcastRepository.findByIdForUpdate(id.get()).ifPresent(broadcast -> {
            broadcast.markStreamOffline(now);
            broadcastRepository.save(broadcast);
            log.info("송출 끊김 감지 broadcastId={}", broadcast.getId());
        });
        events.publish(new MediaStreamStopped(mediaPath, now));
    }

    @Override
    @Transactional
    public void onRecordSegmentComplete(String mediaPath, String segmentPath) {
        recordingRepository.findActiveByMediaPath(mediaPath).ifPresent(recording -> {
            recording.addSegment(segmentPath);
            recordingRepository.save(recording);
        });
    }

    /**
     * 브라우저를 그냥 닫는 등 종료 버튼 없이 송출이 끊긴 방송을 정리한다.
     * 각 방송을 별도 트랜잭션으로 처리해서 하나가 실패해도 나머지는 정리된다.
     */
    @Override
    public int endStaleBroadcasts() {
        List<Long> candidates = broadcastRepository.findLiveOfflineIds();
        int ended = 0;
        for (Long id : candidates) {
            try {
                Boolean result = transactionTemplate.execute(status -> endIfStale(id));
                if (Boolean.TRUE.equals(result)) {
                    ended++;
                }
            } catch (RuntimeException e) {
                log.warn("자동 종료 실패 broadcastId={}", id, e);
            }
        }
        return ended;
    }

    private boolean endIfStale(Long id) {
        Broadcast broadcast = broadcastRepository.findByIdForUpdate(id).orElse(null);
        Instant now = clock.instant();
        if (broadcast == null || !broadcast.isStale(now, mediaProperties.staleGrace())) {
            return false;
        }
        broadcast.forceEnd(now);
        broadcastRepository.save(broadcast);
        events.publish(new BroadcastEnded(broadcast.getId(), broadcast.mediaPath(), now));
        log.info("송출이 끊긴 방송 자동 종료 broadcastId={}", id);
        return true;
    }
}
