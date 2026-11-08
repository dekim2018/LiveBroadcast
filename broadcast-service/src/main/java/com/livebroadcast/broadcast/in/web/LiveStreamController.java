package com.livebroadcast.broadcast.in.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.livebroadcast.broadcast.app.MediaProperties;
import com.livebroadcast.broadcast.app.port.in.LiveStreamUseCase;
import com.livebroadcast.broadcast.in.web.dto.BroadcastResponse;
import com.livebroadcast.broadcast.in.web.dto.LiveSessionResponse;
import com.livebroadcast.broadcast.in.web.dto.StartLiveRequest;
import com.livebroadcast.common.security.AuthUser;
import com.livebroadcast.common.security.LoginUser;

import jakarta.validation.Valid;

/**
 * 송출 제어. 모든 요청에서 로그인 여부와 방송 소유자 여부를 서버에서 검증한다.
 */
@RestController
@RequestMapping("/api/broadcasts/{broadcastId}/live")
public class LiveStreamController {

    private final LiveStreamUseCase liveStreamUseCase;
    private final MediaProperties media;

    public LiveStreamController(LiveStreamUseCase liveStreamUseCase, MediaProperties media) {
        this.liveStreamUseCase = liveStreamUseCase;
        this.media = media;
    }

    @PostMapping("/start")
    public LiveSessionResponse start(@LoginUser AuthUser user, @PathVariable Long broadcastId,
                                     @Valid @RequestBody StartLiveRequest request) {
        return LiveSessionResponse.of(liveStreamUseCase.start(broadcastId, user.id(), request.sourceType()), media);
    }

    @PostMapping("/end")
    public BroadcastResponse end(@LoginUser AuthUser user, @PathVariable Long broadcastId) {
        return BroadcastResponse.of(liveStreamUseCase.end(broadcastId, user.id()), media);
    }

    @GetMapping("/session")
    public LiveSessionResponse session(@LoginUser AuthUser user, @PathVariable Long broadcastId) {
        return LiveSessionResponse.of(liveStreamUseCase.getSession(broadcastId, user.id()), media);
    }
}
