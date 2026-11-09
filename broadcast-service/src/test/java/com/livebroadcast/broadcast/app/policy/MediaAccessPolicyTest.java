package com.livebroadcast.broadcast.app.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.livebroadcast.broadcast.app.MediaProperties;
import com.livebroadcast.broadcast.app.port.in.MediaEventUseCase.MediaAuthCommand;
import com.livebroadcast.broadcast.app.service.MediaEventService;

/** MediaEventService 가 action 에 맞는 정책을 골라 판단을 맡기는지 검증한다. */
class MediaAccessPolicyTest {

    private static MediaAccessPolicy fixed(String action, boolean allowed) {
        return new MediaAccessPolicy() {
            @Override
            public String action() {
                return action;
            }

            @Override
            public boolean allows(MediaAuthCommand command) {
                return allowed;
            }
        };
    }

    private static MediaEventService service(List<MediaAccessPolicy> policies) {
        return new MediaEventService(null, null, event -> { }, policies,
                new MediaProperties(null, null, null, null, null, null), null, Clock.systemUTC());
    }

    private static MediaAuthCommand command(String action) {
        return new MediaAuthCommand(action, "live/1", null, null, null, "1.1.1.1");
    }

    @Test
    void action_에_맞는_정책이_판단한다() {
        MediaEventService service = service(List.of(fixed("read", true), fixed("publish", false)));

        assertThat(service.authorize(command("read"))).isTrue();
        assertThat(service.authorize(command("publish"))).isFalse();
    }

    @Test
    void 담당_정책이_없는_action_은_거절한다() {
        MediaEventService service = service(List.of(fixed("read", true)));

        assertThat(service.authorize(command("api"))).isFalse();
        assertThat(service.authorize(command(null))).isFalse();
    }

    @Test
    void 같은_action_을_맡는_정책이_둘이면_생성할_수_없다() {
        assertThatThrownBy(() -> service(List.of(fixed("read", true), fixed("read", false))))
                .isInstanceOf(IllegalStateException.class);
    }
}
