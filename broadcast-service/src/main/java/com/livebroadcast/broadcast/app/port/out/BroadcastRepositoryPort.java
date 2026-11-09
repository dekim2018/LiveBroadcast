package com.livebroadcast.broadcast.app.port.out;

import java.util.List;
import java.util.Optional;

import com.livebroadcast.broadcast.app.PageResult;
import com.livebroadcast.broadcast.domain.Broadcast;
import com.livebroadcast.broadcast.domain.BroadcastStatus;

public interface BroadcastRepositoryPort {

    Broadcast save(Broadcast broadcast);

    Optional<Broadcast> findById(Long id);

    /** 상태 변경 시 동시 요청(사용자 버튼 + 미디어 서버 훅)이 섞이지 않도록 행 잠금으로 조회한다. */
    Optional<Broadcast> findByIdForUpdate(Long id);

    PageResult<Broadcast> findAll(BroadcastStatus status, int page, int size);

    PageResult<Broadcast> findByOwner(Long ownerId, int page, int size);

    List<Long> findLiveOfflineIds();

    void delete(Long id);
}
