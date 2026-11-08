package com.livebroadcast.interaction.app.port.out;

import java.util.Optional;

import com.livebroadcast.interaction.domain.broadcast.BroadcastInfo;

public interface BroadcastQueryPort {

    Optional<BroadcastInfo> findBroadcast(Long broadcastId);
}
