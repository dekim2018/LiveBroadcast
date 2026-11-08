package com.livebroadcast.broadcast.in.web.dto;

import com.livebroadcast.broadcast.app.MediaProperties;
import com.livebroadcast.broadcast.app.port.in.LiveStreamUseCase.LiveSession;

/**
 * 방송자에게만 내려주는 송출 정보. publishToken 은 이번 방송에서만 유효하다.
 */
public record LiveSessionResponse(BroadcastResponse broadcast, String publishUrl, String publishToken) {

    public static LiveSessionResponse of(LiveSession session, MediaProperties media) {
        return new LiveSessionResponse(BroadcastResponse.of(session.broadcast(), media),
                session.publishUrl(), session.publishToken());
    }
}
