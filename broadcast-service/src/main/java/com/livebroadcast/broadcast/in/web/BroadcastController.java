package com.livebroadcast.broadcast.in.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.livebroadcast.broadcast.app.MediaProperties;
import com.livebroadcast.broadcast.app.port.in.BroadcastCommandUseCase;
import com.livebroadcast.broadcast.app.port.in.BroadcastCommandUseCase.CreateCommand;
import com.livebroadcast.broadcast.app.port.in.BroadcastCommandUseCase.UpdateCommand;
import com.livebroadcast.broadcast.app.port.in.BroadcastQueryUseCase;
import com.livebroadcast.broadcast.domain.BroadcastStatus;
import com.livebroadcast.broadcast.in.web.dto.BroadcastRequest;
import com.livebroadcast.broadcast.in.web.dto.BroadcastResponse;
import com.livebroadcast.broadcast.in.web.dto.PageResponse;
import com.livebroadcast.common.security.AuthUser;
import com.livebroadcast.common.security.LoginUser;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/broadcasts")
public class BroadcastController {

    private final BroadcastCommandUseCase commandUseCase;
    private final BroadcastQueryUseCase queryUseCase;
    private final MediaProperties media;

    public BroadcastController(BroadcastCommandUseCase commandUseCase, BroadcastQueryUseCase queryUseCase,
                               MediaProperties media) {
        this.commandUseCase = commandUseCase;
        this.queryUseCase = queryUseCase;
        this.media = media;
    }

    /** 방송 목록 (비로그인 허용). status 를 주지 않으면 전체 */
    @GetMapping
    public PageResponse<BroadcastResponse> list(@RequestParam(required = false) BroadcastStatus status,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "20") int size) {
        return PageResponse.from(queryUseCase.list(status, page, size).map(b -> BroadcastResponse.of(b, media)));
    }

    /** 내 방송 목록 */
    @GetMapping("/mine")
    public PageResponse<BroadcastResponse> mine(@LoginUser AuthUser user,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "20") int size) {
        return PageResponse.from(queryUseCase.listByOwner(user.id(), page, size)
                .map(b -> BroadcastResponse.of(b, media)));
    }

    /** 방송 상세 (비로그인 허용) */
    @GetMapping("/{broadcastId}")
    public BroadcastResponse get(@PathVariable Long broadcastId) {
        return BroadcastResponse.of(queryUseCase.get(broadcastId), media);
    }

    @PostMapping
    public ResponseEntity<BroadcastResponse> create(@LoginUser AuthUser user,
                                                    @Valid @RequestBody BroadcastRequest request) {
        var created = commandUseCase.create(
                new CreateCommand(user.id(), user.nickname(), request.title(), request.description()));
        return ResponseEntity.status(HttpStatus.CREATED).body(BroadcastResponse.of(created, media));
    }

    @PatchMapping("/{broadcastId}")
    public BroadcastResponse update(@LoginUser AuthUser user, @PathVariable Long broadcastId,
                                    @Valid @RequestBody BroadcastRequest request) {
        var updated = commandUseCase.update(
                new UpdateCommand(broadcastId, user.id(), request.title(), request.description()));
        return BroadcastResponse.of(updated, media);
    }

    @DeleteMapping("/{broadcastId}")
    public ResponseEntity<Void> delete(@LoginUser AuthUser user, @PathVariable Long broadcastId) {
        commandUseCase.delete(broadcastId, user.id());
        return ResponseEntity.noContent().build();
    }
}
