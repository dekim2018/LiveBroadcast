package com.livebroadcast.broadcast.app.service;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.livebroadcast.broadcast.app.MediaProperties;
import com.livebroadcast.broadcast.app.port.in.MediaEventUseCase;
import com.livebroadcast.broadcast.app.port.out.BroadcastRepositoryPort;
import com.livebroadcast.broadcast.app.port.out.MediaServerPort;
import com.livebroadcast.broadcast.app.port.out.RecordingRepositoryPort;
import com.livebroadcast.broadcast.domain.Broadcast;
import com.livebroadcast.broadcast.domain.Recording;

@Service
public class MediaEventService implements MediaEventUseCase {

    private static final Logger log = LoggerFactory.getLogger(MediaEventService.class);

    private final BroadcastRepositoryPort broadcastRepository;
    private final RecordingRepositoryPort recordingRepository;
    private final MediaServerPort mediaServer;
    private final MediaProperties mediaProperties;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    public MediaEventService(BroadcastRepositoryPort broadcastRepository, RecordingRepositoryPort recordingRepository,
                             MediaServerPort mediaServer, MediaProperties mediaProperties,
                             PlatformTransactionManager transactionManager, Clock clock) {
        this.broadcastRepository = broadcastRepository;
        this.recordingRepository = recordingRepository;
        this.mediaServer = mediaServer;
        this.mediaProperties = mediaProperties;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.clock = clock;
    }

    /**
     * MediaMTX 가 송출(publish), 시청(read), 녹화 재생(playback) 요청마다 호출한다.
     * <ul>
     *     <li>publish : 방송 중이고, 이번 방송에 발급된 송출 토큰이 일치할 때만 허용</li>
     *     <li>read    : 방송 중인 방송이면 비로그인 사용자도 허용</li>
     *     <li>playback: 다시보기 가능한 녹화가 있는 경로만 허용</li>
     * </ul>
     */
    @Override
    @Transactional(readOnly = true)
    public boolean authorize(MediaAuthCommand command) {
        String action = command.action() == null ? "" : command.action();
        return switch (action) {
            case "publish" -> findBroadcast(command.path())
                    .map(b -> b.canPublishWith(extractToken(command)))
                    .orElse(false);
            case "read" -> findBroadcast(command.path())
                    .map(Broadcast::isWatchable)
                    .orElse(false);
            case "playback" -> command.path() != null && recordingRepository.existsReadyByMediaPath(command.path());
            default -> false;
        };
    }

    @Override
    @Transactional
    public void onStreamReady(String mediaPath) {
        Optional<Long> id = Broadcast.parseMediaPath(mediaPath);
        if (id.isEmpty()) {
            return;
        }
        broadcastRepository.findByIdForUpdate(id.get()).ifPresent(broadcast -> {
            broadcast.markStreamOnline(clock.instant());
            broadcastRepository.save(broadcast);
            if (broadcast.isWatchable() && recordingRepository.findActiveByMediaPath(mediaPath).isEmpty()) {
                recordingRepository.save(Recording.start(broadcast, mediaProperties.recordingRoot(), clock.instant()));
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
        broadcastRepository.findByIdForUpdate(id.get()).ifPresent(broadcast -> {
            broadcast.markStreamOffline(clock.instant());
            broadcastRepository.save(broadcast);
            log.info("송출 끊김 감지 broadcastId={}", broadcast.getId());
        });
        recordingRepository.findActiveByMediaPath(mediaPath).ifPresent(recording -> {
            recording.finish(clock.instant());
            recordingRepository.save(recording);
        });
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
        if (broadcast == null || !broadcast.isStale(clock.instant(), mediaProperties.staleGrace())) {
            return false;
        }
        broadcast.forceEnd(clock.instant());
        broadcastRepository.save(broadcast);
        recordingRepository.findActiveByMediaPath(broadcast.mediaPath()).ifPresent(r -> {
            r.finish(clock.instant());
            recordingRepository.save(r);
        });
        String mediaPath = broadcast.mediaPath();
        AfterCommit.run(() -> mediaServer.kickPublisher(mediaPath));
        log.info("송출이 끊긴 방송 자동 종료 broadcastId={}", id);
        return true;
    }

    private Optional<Broadcast> findBroadcast(String mediaPath) {
        return Broadcast.parseMediaPath(mediaPath).flatMap(broadcastRepository::findById);
    }

    /** WHIP 요청의 Authorization: Bearer, 쿼리스트링 token, 비밀번호 필드 순서로 송출 토큰을 찾는다. */
    static String extractToken(MediaAuthCommand command) {
        if (command.token() != null && !command.token().isBlank()) {
            return command.token();
        }
        if (command.query() != null && !command.query().isBlank()) {
            for (String pair : command.query().split("&")) {
                int idx = pair.indexOf('=');
                if (idx > 0 && "token".equals(pair.substring(0, idx))) {
                    return URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
                }
            }
        }
        if (command.password() != null && !command.password().isBlank()) {
            return command.password();
        }
        return null;
    }
}
