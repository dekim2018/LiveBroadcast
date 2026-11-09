package com.livebroadcast.broadcast.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BroadcastStatusTest {

    @Test
    void 허용된_전이만_가능하다() {
        assertThat(BroadcastStatus.READY.canTransitionTo(BroadcastStatus.LIVE)).isTrue();
        assertThat(BroadcastStatus.LIVE.canTransitionTo(BroadcastStatus.ENDED)).isTrue();
        assertThat(BroadcastStatus.ENDED.canTransitionTo(BroadcastStatus.LIVE)).isTrue();

        assertThat(BroadcastStatus.READY.canTransitionTo(BroadcastStatus.ENDED)).isFalse();
        assertThat(BroadcastStatus.LIVE.canTransitionTo(BroadcastStatus.LIVE)).isFalse();
        assertThat(BroadcastStatus.ENDED.canTransitionTo(BroadcastStatus.READY)).isFalse();
    }

    @Test
    void 방송_중일_때만_송출과_시청이_가능하고_삭제는_불가하다() {
        assertThat(BroadcastStatus.LIVE.isOnAir()).isTrue();
        assertThat(BroadcastStatus.LIVE.isDeletable()).isFalse();
        assertThat(BroadcastStatus.READY.isOnAir()).isFalse();
        assertThat(BroadcastStatus.ENDED.isDeletable()).isTrue();
    }
}
