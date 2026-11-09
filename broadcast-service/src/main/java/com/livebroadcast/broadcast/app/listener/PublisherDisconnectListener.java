package com.livebroadcast.broadcast.app.listener;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.livebroadcast.broadcast.app.port.out.MediaServerPort;
import com.livebroadcast.broadcast.domain.event.BroadcastEnded;

/**
 * 방송이 끝나면 미디어 서버에 남아 있는 송출 연결을 끊는다.
 * 송출 토큰은 이미 무효화됐으므로, 종료가 커밋된 뒤에만 외부 호출을 한다.
 */
@Component
public class PublisherDisconnectListener {

    private final MediaServerPort mediaServer;

    public PublisherDisconnectListener(MediaServerPort mediaServer) {
        this.mediaServer = mediaServer;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(BroadcastEnded event) {
        mediaServer.kickPublisher(event.mediaPath());
    }
}
