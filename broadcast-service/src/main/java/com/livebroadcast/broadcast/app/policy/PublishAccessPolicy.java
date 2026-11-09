package com.livebroadcast.broadcast.app.policy;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Component;

import com.livebroadcast.broadcast.app.port.in.MediaEventUseCase.MediaAuthCommand;
import com.livebroadcast.broadcast.app.port.out.BroadcastRepositoryPort;
import com.livebroadcast.broadcast.domain.Broadcast;

/** 송출: 방송 중이고, 이번 방송에 발급된 송출 토큰이 일치할 때만 허용 */
@Component
public class PublishAccessPolicy implements MediaAccessPolicy {

    private final BroadcastRepositoryPort broadcastRepository;

    public PublishAccessPolicy(BroadcastRepositoryPort broadcastRepository) {
        this.broadcastRepository = broadcastRepository;
    }

    @Override
    public String action() {
        return "publish";
    }

    @Override
    public boolean allows(MediaAuthCommand command) {
        return Broadcast.parseMediaPath(command.path())
                .flatMap(broadcastRepository::findById)
                .map(b -> b.canPublishWith(extractToken(command)))
                .orElse(false);
    }

    /** WHIP 요청의 Authorization: Bearer, 쿼리스트링 token, 비밀번호 필드 순서로 송출 토큰을 찾는다. */
    static String extractToken(MediaAuthCommand command) {
        if (command.token() != null && !command.token().isBlank()) {
            return command.token();
        }
        if (command.query() != null && !command.query().isBlank()) {
            for (String pair : command.query().split("&")) {
                int idx = pair.indexOf('=');
                if (idx > 0 && "token".equals(pair.substring(0, idx))) {
                    return URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
                }
            }
        }
        if (command.password() != null && !command.password().isBlank()) {
            return command.password();
        }
        return null;
    }
}
