package com.livebroadcast.broadcast.app.listener;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.livebroadcast.broadcast.app.MediaProperties;
import com.livebroadcast.broadcast.domain.Broadcast;
import com.livebroadcast.broadcast.domain.BroadcastStatus;
import com.livebroadcast.broadcast.domain.Recording;
import com.livebroadcast.broadcast.domain.RecordingStatus;
import com.livebroadcast.broadcast.domain.SourceType;
import com.livebroadcast.broadcast.domain.event.BroadcastEnded;
import com.livebroadcast.broadcast.domain.event.BroadcastStreamStarted;
import com.livebroadcast.broadcast.domain.event.MediaStreamStopped;
import com.livebroadcast.broadcast.support.FakeRecordingRepository;

class RecordingLifecycleListenerTest {

    private static final Instant T0 = Instant.parse("2026-11-10T00:00:00Z");

    private final FakeRecordingRepository repository = new FakeRecordingRepository();
    private final RecordingLifecycleListener listener = new RecordingLifecycleListener(repository,
            new MediaProperties(null, null, null, null, null, null));
    private final Broadcast broadcast = new Broadcast(1L, 7L, "방송자", "제목", "", BroadcastStatus.LIVE,
            SourceType.PC_SCREEN, true, "token", null, T0, null, T0, T0);

    @Test
    void 송출이_시작되면_녹화를_하나만_시작한다() {
        listener.on(new BroadcastStreamStarted(broadcast, T0));
        listener.on(new BroadcastStreamStarted(broadcast, T0.plusSeconds(1)));

        assertThat(repository.byId.values()).singleElement()
                .extracting(Recording::getStatus).isEqualTo(RecordingStatus.RECORDING);
    }

    @Test
    void 송출이_끊기면_진행_중인_녹화를_마무리한다() {
        listener.on(new BroadcastStreamStarted(broadcast, T0));

        listener.on(new MediaStreamStopped("live/1", T0.plusSeconds(30)));

        assertThat(repository.byId.values()).singleElement().satisfies(r -> {
            assertThat(r.getStatus()).isEqualTo(RecordingStatus.READY);
            assertThat(r.getEndedAt()).isEqualTo(T0.plusSeconds(30));
        });
    }

    @Test
    void 방송이_끝나면_진행_중인_녹화를_마무리한다() {
        listener.on(new BroadcastStreamStarted(broadcast, T0));

        listener.on(new BroadcastEnded(1L, "live/1", T0.plusSeconds(30)));

        assertThat(repository.findActiveByMediaPath("live/1")).isEmpty();
    }
}
