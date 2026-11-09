package com.livebroadcast.interaction.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.livebroadcast.common.error.DomainException;
import com.livebroadcast.interaction.domain.donation.Donation;
import com.livebroadcast.interaction.domain.donation.DonationStatus;
import com.livebroadcast.interaction.domain.donation.PaymentApproval;

class DonationTest {

    private final Instant now = Instant.parse("2026-11-10T00:00:00Z");

    private Donation pending() {
        return Donation.order("order-1", 10L, 1L, "방송자", 2L, "시청자", 5000, "응원합니다",
                1000, 1_000_000, now);
    }

    @Test
    void 승인_결과가_주문과_일치하면_확정된다() {
        Donation d = pending();
        boolean newly = d.approve(new PaymentApproval("pk-1", "order-1", "DONE", 5000, now), now);

        assertThat(newly).isTrue();
        assertThat(d.getStatus()).isEqualTo(DonationStatus.DONE);
        assertThat(d.getPaymentKey()).isEqualTo("pk-1");
    }

    @Test
    void 같은_승인_통지가_반복되면_두번째부터는_false() {
        Donation d = pending();
        PaymentApproval approval = new PaymentApproval("pk-1", "order-1", "DONE", 5000, now);

        assertThat(d.approve(approval, now)).isTrue();
        assertThat(d.approve(approval, now)).isFalse();
        assertThat(d.approve(approval, now)).isFalse();
    }

    @Test
    void 금액이_다르면_거절한다() {
        Donation d = pending();
        assertThatThrownBy(() -> d.approve(new PaymentApproval("pk", "order-1", "DONE", 100, now), now))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("금액");
        assertThatThrownBy(() -> d.ensureRequestedAmount(4999)).isInstanceOf(DomainException.class);
    }

    @Test
    void 주문번호가_다르거나_승인되지_않았으면_거절한다() {
        Donation d = pending();
        assertThatThrownBy(() -> d.approve(new PaymentApproval("pk", "other", "DONE", 5000, now), now))
                .isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> d.approve(new PaymentApproval("pk", "order-1", "WAITING_FOR_DEPOSIT", 5000, now), now))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void 다른_결제로_이미_확정된_주문은_거절한다() {
        Donation d = pending();
        d.approve(new PaymentApproval("pk-1", "order-1", "DONE", 5000, now), now);

        assertThatThrownBy(() -> d.approve(new PaymentApproval("pk-2", "order-1", "DONE", 5000, now), now))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void 자기_방송에는_후원할_수_없고_금액_범위를_지켜야_한다() {
        assertThatThrownBy(() -> Donation.order("o", 10L, 1L, "a", 1L, "a", 5000, "", 1000, 1_000_000, now))
                .isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> Donation.order("o", 10L, 1L, "a", 2L, "b", 500, "", 1000, 1_000_000, now))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void 본인_주문이_아니면_처리할_수_없다() {
        assertThatThrownBy(() -> pending().ensureDonor(3L)).isInstanceOf(DomainException.class);
    }
}
