package com.livebroadcast.broadcast.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.livebroadcast.common.error.DomainException;
import com.livebroadcast.common.error.ErrorType;

class BroadcastTest {

    private static final Long OWNER = 1L;
    private static final Long OTHER = 2L;
    private final Instant now = Instant.parse("2026-11-10T00:00:00Z");

    private Broadcast saved() {
        Broadcast b = Broadcast.create(OWNER, "방송자", "첫 방송", "설명", now);
        return new Broadcast(10L, b.getOwnerId(), b.getOwnerNickname(), b.getTitle(), b.getDescription(),
                b.getStatus(), null, false, null, null, null, null, now, now);
    }

    @Test
    void 생성하면_READY_상태다() {
        assertThat(saved().getStatus()).isEqualTo(BroadcastStatus.READY);
    }

    @Test
    void 제목이_비어있으면_예외() {
        assertThatThrownBy(() -> Broadcast.create(OWNER, "n", " ", null, now))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void 소유자가_시작하면_LIVE_가_되고_토큰으로만_송출할_수_있다() {
        Broadcast b = saved();
        b.startLive(OWNER, SourceType.PC_SCREEN, "token-1", now);

        assertThat(b.getStatus()).isEqualTo(BroadcastStatus.LIVE);
        assertThat(b.canPublishWith("token-1")).isTrue();
        assertThat(b.canPublishWith("wrong")).isFalse();
        assertThat(b.canPublishWith(null)).isFalse();
    }

    @Test
    void 다른_사용자는_시작_수정_종료_삭제할_수_없다() {
        Broadcast b = saved();

        assertForbidden(() -> b.startLive(OTHER, SourceType.MOBILE_CAMERA, "t", now));
        assertForbidden(() -> b.updateInfo(OTHER, "새 제목", "", now));
        assertForbidden(() -> b.ensureDeletable(OTHER));

        b.startLive(OWNER, SourceType.MOBILE_CAMERA, "t", now);
        assertForbidden(() -> b.endLive(OTHER, now));
    }

    @Test
    void 종료하면_토큰이_폐기되어_더이상_송출할_수_없다() {
        Broadcast b = saved();
        b.startLive(OWNER, SourceType.PC_SCREEN, "token-1", now);
        b.endLive(OWNER, now.plusSeconds(60));

        assertThat(b.getStatus()).isEqualTo(BroadcastStatus.ENDED);
        assertThat(b.canPublishWith("token-1")).isFalse();
        assertThat(b.isWatchable()).isFalse();
    }

    @Test
    void 이미_방송중이면_다시_시작할_수_없다() {
        Broadcast b = saved();
        b.startLive(OWNER, SourceType.PC_SCREEN, "t", now);

        assertThatThrownBy(() -> b.startLive(OWNER, SourceType.PC_SCREEN, "t2", now))
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).getType())
                .isEqualTo(ErrorType.CONFLICT);
    }

    @Test
    void 종료된_방송은_다시_시작할_수_있다() {
        Broadcast b = saved();
        b.startLive(OWNER, SourceType.PC_SCREEN, "t1", now);
        b.endLive(OWNER, now);
        b.startLive(OWNER, SourceType.MOBILE_CAMERA, "t2", now);

        assertThat(b.getStatus()).isEqualTo(BroadcastStatus.LIVE);
        assertThat(b.canPublishWith("t1")).isFalse();
        assertThat(b.canPublishWith("t2")).isTrue();
    }

    @Test
    void 방송중에는_삭제할_수_없다() {
        Broadcast b = saved();
        b.startLive(OWNER, SourceType.PC_SCREEN, "t", now);

        assertThatThrownBy(() -> b.ensureDeletable(OWNER)).isInstanceOf(DomainException.class);
    }

    @Test
    void 송출이_유예시간_이상_끊기면_stale_로_판단한다() {
        Broadcast b = saved();
        b.startLive(OWNER, SourceType.PC_SCREEN, "t", now);
        b.markStreamOnline(now.plusSeconds(5));
        assertThat(b.isStale(now.plusSeconds(600), Duration.ofMinutes(3))).isFalse();

        b.markStreamOffline(now.plusSeconds(10));
        assertThat(b.isStale(now.plusSeconds(60), Duration.ofMinutes(3))).isFalse();
        assertThat(b.isStale(now.plusSeconds(400), Duration.ofMinutes(3))).isTrue();
    }

    @Test
    void 미디어_경로를_파싱한다() {
        assertThat(Broadcast.parseMediaPath("live/42")).contains(42L);
        assertThat(Broadcast.parseMediaPath("live/abc")).isEmpty();
        assertThat(Broadcast.parseMediaPath("other/1")).isEmpty();
    }

    private void assertForbidden(Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).getType())
                .isEqualTo(ErrorType.FORBIDDEN);
    }
}
