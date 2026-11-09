package com.livebroadcast.broadcast.in.internal;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.livebroadcast.broadcast.app.port.in.BroadcastQueryUseCase;
import com.livebroadcast.broadcast.in.internal.dto.InternalBroadcastResponse;

/**
 * 다른 서비스(interaction-service)가 방송 존재 여부와 방송자(후원 대상)를 확인할 때 사용한다.
 */
@RestController
@RequestMapping("/internal/broadcasts")
public class InternalBroadcastController {

    private final BroadcastQueryUseCase queryUseCase;

    public InternalBroadcastController(BroadcastQueryUseCase queryUseCase) {
        this.queryUseCase = queryUseCase;
    }

    @GetMapping("/{broadcastId}")
    public InternalBroadcastResponse get(@PathVariable Long broadcastId) {
        return InternalBroadcastResponse.from(queryUseCase.get(broadcastId));
    }
}
