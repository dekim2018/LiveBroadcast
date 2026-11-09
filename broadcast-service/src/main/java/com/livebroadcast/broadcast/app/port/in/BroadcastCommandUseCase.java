package com.livebroadcast.broadcast.app.port.in;

import com.livebroadcast.broadcast.domain.Broadcast;

public interface BroadcastCommandUseCase {

    Broadcast create(CreateCommand command);

    Broadcast update(UpdateCommand command);

    void delete(Long broadcastId, Long requesterId);

    record CreateCommand(Long ownerId, String ownerNickname, String title, String description) {
    }

    record UpdateCommand(Long broadcastId, Long requesterId, String title, String description) {
    }
}
