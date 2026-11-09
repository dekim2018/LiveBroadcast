package com.livebroadcast.broadcast.app.port.out;

import java.time.Instant;
import java.util.Optional;

import com.livebroadcast.broadcast.app.PageResult;
import com.livebroadcast.broadcast.domain.Recording;

public interface RecordingRepositoryPort {

    Recording save(Recording recording);

    Optional<Recording> findById(Long id);

    Optional<Recording> findActiveByMediaPath(String mediaPath);

    /** 같은 경로에서 startedAt 직전에 시작한 녹화 */
    Optional<Recording> findPrevious(String mediaPath, Instant startedAt);

    PageResult<Recording> findVisible(Long broadcastId, int page, int size);

    boolean existsReadyByMediaPath(String mediaPath);

    void delete(Long id);
}
