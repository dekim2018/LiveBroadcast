package com.livebroadcast.broadcast.in.web.dto;

import java.time.Instant;

import com.livebroadcast.broadcast.app.MediaProperties;
import com.livebroadcast.broadcast.domain.Broadcast;
import com.livebroadcast.broadcast.domain.BroadcastStatus;
import com.livebroadcast.broadcast.domain.SourceType;

public record BroadcastResponse(
        Long id,
        Long ownerId,
        String ownerNickname,
        String title,
        String description,
        BroadcastStatus status,
        SourceType sourceType,
        boolean streamOnline,
        String watchUrl,
        Instant startedAt,
        Instant endedAt,
        Instant createdAt,
        Instant updatedAt
) {
    public static BroadcastResponse of(Broadcast b, MediaProperties media) {
        return new BroadcastResponse(b.getId(), b.getOwnerId(), b.getOwnerNickname(), b.getTitle(),
                b.getDescription(), b.getStatus(), b.getSourceType(), b.isStreamOnline(),
                media.whepUrl(b.mediaPath()), b.getStartedAt(), b.getEndedAt(), b.getCreatedAt(), b.getUpdatedAt());
    }
}
