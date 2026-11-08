package com.livebroadcast.broadcast.app.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.livebroadcast.broadcast.app.port.in.MediaEventUseCase.MediaAuthCommand;

class MediaEventServiceTokenTest {

    @Test
    void Bearer_토큰을_가장_먼저_사용한다() {
        var cmd = new MediaAuthCommand("publish", "live/1", "bearer", "pw", "token=query", "1.1.1.1");
        assertThat(MediaEventService.extractToken(cmd)).isEqualTo("bearer");
    }

    @Test
    void 쿼리스트링의_token_을_사용한다() {
        var cmd = new MediaAuthCommand("publish", "live/1", null, null, "a=1&token=abc%2Bd", "1.1.1.1");
        assertThat(MediaEventService.extractToken(cmd)).isEqualTo("abc+d");
    }

    @Test
    void 아무것도_없으면_null() {
        var cmd = new MediaAuthCommand("publish", "live/1", "", "", "", "1.1.1.1");
        assertThat(MediaEventService.extractToken(cmd)).isNull();
    }
}
