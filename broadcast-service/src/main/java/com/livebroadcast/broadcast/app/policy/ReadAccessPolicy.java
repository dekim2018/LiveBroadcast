package com.livebroadcast.broadcast.app.policy;

import org.springframework.stereotype.Component;

import com.livebroadcast.broadcast.app.port.in.MediaEventUseCase.MediaAuthCommand;
import com.livebroadcast.broadcast.app.port.out.BroadcastRepositoryPort;
import com.livebroadcast.broadcast.domain.Broadcast;

/** 시청: 방송 중인 방송이면 비로그인 사용자도 허용 */
@Component
public class ReadAccessPolicy implements MediaAccessPolicy {

    private final BroadcastRepositoryPort broadcastRepository;

    public ReadAccessPolicy(BroadcastRepositoryPort broadcastRepository) {
        this.broadcastRepository = broadcastRepository;
    }

    @Override
    public String action() {
        return "read";
    }

    @Override
    public boolean allows(MediaAuthCommand command) {
        return Broadcast.parseMediaPath(command.path())
                .flatMap(broadcastRepository::findById)
                .map(Broadcast::isWatchable)
                .orElse(false);
    }
}
