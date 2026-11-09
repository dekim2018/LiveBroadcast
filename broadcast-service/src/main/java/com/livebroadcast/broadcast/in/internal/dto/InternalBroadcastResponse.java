package com.livebroadcast.broadcast.in.internal.dto;

import com.livebroadcast.broadcast.domain.Broadcast;
import com.livebroadcast.broadcast.domain.BroadcastStatus;

public record InternalBroadcastResponse(Long id, Long ownerId, String ownerNickname, String title,
                                        BroadcastStatus status) {

    public static InternalBroadcastResponse from(Broadcast b) {
        return new InternalBroadcastResponse(b.getId(), b.getOwnerId(), b.getOwnerNickname(), b.getTitle(),
                b.getStatus());
    }
}
