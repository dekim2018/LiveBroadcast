package com.livebroadcast.broadcast.app.service;

import java.time.Clock;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.livebroadcast.broadcast.app.MediaProperties;
import com.livebroadcast.broadcast.app.PageResult;
import com.livebroadcast.broadcast.app.port.in.BroadcastCommandUseCase;
import com.livebroadcast.broadcast.app.port.in.BroadcastQueryUseCase;
import com.livebroadcast.broadcast.app.port.in.LiveStreamUseCase;
import com.livebroadcast.broadcast.app.port.out.BroadcastRepositoryPort;
import com.livebroadcast.broadcast.app.port.out.MediaServerPort;
import com.livebroadcast.broadcast.app.port.out.RecordingRepositoryPort;
import com.livebroadcast.broadcast.domain.Broadcast;
import com.livebroadcast.broadcast.domain.BroadcastErrors;
import com.livebroadcast.broadcast.domain.BroadcastStatus;
import com.livebroadcast.broadcast.domain.SourceType;

@Service
public class BroadcastService implements BroadcastCommandUseCase, BroadcastQueryUseCase, LiveStreamUseCase {

    private static final int MAX_PAGE_SIZE = 50;

    private final BroadcastRepositoryPort broadcastRepository;
    private final RecordingRepositoryPort recordingRepository;
    private final MediaServerPort mediaServer;
    private final PublishTokenGenerator tokenGenerator;
    private final MediaProperties mediaProperties;
    private final Clock clock;

    public BroadcastService(BroadcastRepositoryPort broadcastRepository, RecordingRepositoryPort recordingRepository,
                            MediaServerPort mediaServer, PublishTokenGenerator tokenGenerator,
                            MediaProperties mediaProperties, Clock clock) {
        this.broadcastRepository = broadcastRepository;
        this.recordingRepository = recordingRepository;
        this.mediaServer = mediaServer;
        this.tokenGenerator = tokenGenerator;
        this.mediaProperties = mediaProperties;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Broadcast create(CreateCommand command) {
        Broadcast broadcast = Broadcast.create(
                command.ownerId(), command.ownerNickname(), command.title(), command.description(), clock.instant());
        return broadcastRepository.save(broadcast);
    }

    @Override
    @Transactional
    public Broadcast update(UpdateCommand command) {
        Broadcast broadcast = lock(command.broadcastId());
        broadcast.updateInfo(command.requesterId(), command.title(), command.description(), clock.instant());
        return broadcastRepository.save(broadcast);
    }

    @Override
    @Transactional
    public void delete(Long broadcastId, Long requesterId) {
        Broadcast broadcast = lock(broadcastId);
        broadcast.ensureDeletable(requesterId);
        broadcastRepository.delete(broadcastId);
    }

    @Override
    @Transactional(readOnly = true)
    public Broadcast get(Long broadcastId) {
        return broadcastRepository.findById(broadcastId).orElseThrow(BroadcastErrors::notFound);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Broadcast> list(BroadcastStatus status, int page, int size) {
        return broadcastRepository.findAll(status, Math.max(page, 0), clampSize(size));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Broadcast> listByOwner(Long ownerId, int page, int size) {
        return broadcastRepository.findByOwner(ownerId, Math.max(page, 0), clampSize(size));
    }

    @Override
    @Transactional
    public LiveSession start(Long broadcastId, Long requesterId, SourceType sourceType) {
        Broadcast broadcast = lock(broadcastId);
        broadcast.startLive(requesterId, sourceType, tokenGenerator.generate(), clock.instant());
        Broadcast saved = broadcastRepository.save(broadcast);
        return toSession(saved);
    }

    @Override
    @Transactional
    public Broadcast end(Long broadcastId, Long requesterId) {
        Broadcast broadcast = lock(broadcastId);
        broadcast.endLive(requesterId, clock.instant());
        Broadcast saved = broadcastRepository.save(broadcast);
        recordingRepository.findActiveByMediaPath(saved.mediaPath()).ifPresent(recording -> {
            recording.finish(clock.instant());
            recordingRepository.save(recording);
        });
        // 송출 토큰은 이미 무효화됐고, 남아 있는 송출 연결은 커밋 후 미디어 서버에서 강제로 끊는다.
        String mediaPath = saved.mediaPath();
        AfterCommit.run(() -> mediaServer.kickPublisher(mediaPath));
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public LiveSession getSession(Long broadcastId, Long requesterId) {
        Broadcast broadcast = get(broadcastId);
        broadcast.ensureOwner(requesterId);
        if (broadcast.getStatus() != BroadcastStatus.LIVE) {
            throw BroadcastErrors.notLive();
        }
        return toSession(broadcast);
    }

    private LiveSession toSession(Broadcast broadcast) {
        return new LiveSession(broadcast, mediaProperties.whipUrl(broadcast.mediaPath()), broadcast.getPublishToken());
    }

    private Broadcast lock(Long broadcastId) {
        return broadcastRepository.findByIdForUpdate(broadcastId).orElseThrow(BroadcastErrors::notFound);
    }

    private int clampSize(int size) {
        if (size <= 0) {
            return 20;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }
}
