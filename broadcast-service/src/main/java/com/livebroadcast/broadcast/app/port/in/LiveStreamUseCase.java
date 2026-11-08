package com.livebroadcast.broadcast.app.port.in;

import com.livebroadcast.broadcast.domain.Broadcast;
import com.livebroadcast.broadcast.domain.SourceType;

public interface LiveStreamUseCase {

    LiveSession start(Long broadcastId, Long requesterId, SourceType sourceType);

    Broadcast end(Long broadcastId, Long requesterId);

    /** 송출 정보 재발급용(방송자 화면 새로고침 등). 소유자만 조회할 수 있다. */
    LiveSession getSession(Long broadcastId, Long requesterId);

    record LiveSession(Broadcast broadcast, String publishUrl, String publishToken) {
    }
}
