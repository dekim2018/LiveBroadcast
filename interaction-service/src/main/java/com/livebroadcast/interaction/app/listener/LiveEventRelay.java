package com.livebroadcast.interaction.app.listener;

import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.livebroadcast.interaction.app.port.out.DonationRepositoryPort;
import com.livebroadcast.interaction.app.port.out.LiveEventPublisherPort;
import com.livebroadcast.interaction.domain.comment.Comment;
import com.livebroadcast.interaction.domain.comment.CommentDeleted;
import com.livebroadcast.interaction.domain.comment.CommentEdited;
import com.livebroadcast.interaction.domain.comment.CommentWritten;
import com.livebroadcast.interaction.domain.donation.Donation;
import com.livebroadcast.interaction.domain.donation.DonationCompleted;

/**
 * 댓글/후원 도메인 이벤트를 시청자 화면용 실시간 이벤트로 바꿔 보낸다.
 * 커밋이 확정된 뒤에만 보내서, 롤백된 댓글/후원이 화면에 뜨지 않게 한다.
 */
@Component
public class LiveEventRelay {

    private final LiveEventPublisherPort liveEvents;
    private final DonationRepositoryPort donationRepository;

    public LiveEventRelay(LiveEventPublisherPort liveEvents, DonationRepositoryPort donationRepository) {
        this.liveEvents = liveEvents;
        this.donationRepository = donationRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(CommentWritten event) {
        Comment c = event.comment();
        liveEvents.publish(c.getBroadcastId(), "comment.created", CommentPayload.from(c));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(CommentEdited event) {
        Comment c = event.comment();
        liveEvents.publish(c.getBroadcastId(), "comment.updated", CommentPayload.from(c));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(CommentDeleted event) {
        liveEvents.publish(event.broadcastId(), "comment.deleted", Map.of("id", event.commentId()));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(DonationCompleted event) {
        Donation d = event.donation();
        liveEvents.publish(d.getBroadcastId(), "donation.completed", DonationPayload.from(d));
        // 커밋된 뒤에 합계를 다시 계산하므로 방금 확정된 후원까지 포함된다.
        liveEvents.publish(d.getBroadcastId(), "donation.summary", donationRepository.summarize(d.getBroadcastId()));
    }

    public record CommentPayload(Long id, Long broadcastId, Long authorId, String authorNickname, String content,
                                 String createdAt, boolean edited) {
        static CommentPayload from(Comment c) {
            return new CommentPayload(c.getId(), c.getBroadcastId(), c.getAuthorId(), c.getAuthorNickname(),
                    c.getContent(), c.getCreatedAt().toString(), c.isEdited());
        }
    }

    public record DonationPayload(Long id, Long broadcastId, String donorNickname, long amount, String message,
                                  String approvedAt) {
        static DonationPayload from(Donation d) {
            return new DonationPayload(d.getId(), d.getBroadcastId(), d.getDonorNickname(), d.getAmount(),
                    d.getMessage(), d.getApprovedAt() == null ? null : d.getApprovedAt().toString());
        }
    }
}
