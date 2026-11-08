package com.livebroadcast.broadcast.app.port.in;

import com.livebroadcast.broadcast.app.PageResult;
import com.livebroadcast.broadcast.domain.Broadcast;
import com.livebroadcast.broadcast.domain.BroadcastStatus;

public interface BroadcastQueryUseCase {

    Broadcast get(Long broadcastId);

    PageResult<Broadcast> list(BroadcastStatus status, int page, int size);

    PageResult<Broadcast> listByOwner(Long ownerId, int page, int size);
}
