package com.livebroadcast.broadcast.app.port.out;

import java.util.Optional;

import com.livebroadcast.broadcast.app.PageResult;
import com.livebroadcast.broadcast.domain.Recording;

public interface RecordingRepositoryPort {

    Recording save(Recording recording);

    Optional<Recording> findById(Long id);

    Optional<Recording> findActiveByMediaPath(String mediaPath);

    PageResult<Recording> findVisible(Long broadcastId, int page, int size);

    boolean existsReadyByMediaPath(String mediaPath);

    void delete(Long id);
}
