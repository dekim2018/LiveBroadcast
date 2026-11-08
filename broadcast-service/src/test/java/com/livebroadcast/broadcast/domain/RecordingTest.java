package com.livebroadcast.broadcast.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class RecordingTest {

    private final Instant now = Instant.parse("2026-11-10T00:00:00Z");

    private Broadcast broadcast() {
        return new Broadcast(7L, 1L, "방송자", "제목", "", BroadcastStatus.LIVE, SourceType.PC_SCREEN,
                true, "t", null, now, null, now, now);
    }

    @Test
    void 충분히_녹화되면_READY_가_된다() {
        Recording r = Recording.start(broadcast(), "/recordings", now);
        r.finish(now.plusSeconds(30));

        assertThat(r.getStatus()).isEqualTo(RecordingStatus.READY);
        assertThat(r.getDurationSeconds()).isEqualTo(30);
        assertThat(r.getStorageLocation()).isEqualTo("/recordings/live/7");
    }

    @Test
    void 너무_짧으면_FAILED_가_된다() {
        Recording r = Recording.start(broadcast(), "/recordings", now);
        r.finish(now.plusMillis(500));

        assertThat(r.getStatus()).isEqualTo(RecordingStatus.FAILED);
    }

    @Test
    void 이미_끝난_녹화는_다시_끝내도_변하지_않는다() {
        Recording r = Recording.start(broadcast(), "/recordings", now);
        r.finish(now.plusSeconds(10));
        r.finish(now.plusSeconds(100));

        assertThat(r.getDurationSeconds()).isEqualTo(10);
    }
}
