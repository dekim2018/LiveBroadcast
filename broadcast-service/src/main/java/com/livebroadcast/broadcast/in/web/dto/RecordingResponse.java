package com.livebroadcast.broadcast.in.web.dto;

import java.time.Instant;
import java.util.List;

import com.livebroadcast.broadcast.app.port.in.RecordingUseCase.PlaybackSegment;
import com.livebroadcast.broadcast.domain.Recording;
import com.livebroadcast.broadcast.domain.RecordingStatus;

public record RecordingResponse(
        Long id,
        Long broadcastId,
        Long ownerId,
        String ownerNickname,
        String title,
        RecordingStatus status,
        String mediaPath,
        String storageLocation,
        Instant startedAt,
        Instant endedAt,
        long durationSeconds,
        int segmentCount,
        List<PlaybackSegment> segments
) {
    public static RecordingResponse of(Recording r, List<PlaybackSegment> segments) {
        return new RecordingResponse(r.getId(), r.getBroadcastId(), r.getOwnerId(), r.getOwnerNickname(),
                r.getTitle(), r.getStatus(), r.getMediaPath(), r.getStorageLocation(), r.getStartedAt(),
                r.getEndedAt(), r.getDurationSeconds(), r.getSegmentCount(), segments);
    }
}
