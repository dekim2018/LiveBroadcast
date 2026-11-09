package com.livebroadcast.interaction.app.port.in;

import com.livebroadcast.interaction.app.PageResult;
import com.livebroadcast.interaction.domain.donation.Donation;
import com.livebroadcast.interaction.domain.donation.DonationSummary;

public interface DonationUseCase {

    Donation createOrder(CreateOrderCommand command);

    Donation confirm(ConfirmCommand command);

    void cancel(String orderId, Long donorId, String reason);

    /** 결제사 웹훅. 본문은 신뢰하지 않고 paymentKey 로 결제사에 다시 조회해서 처리한다. */
    void handleWebhook(String paymentKey, String orderId);

    PageResult<Donation> listDone(Long broadcastId, int page, int size);

    DonationSummary summary(Long broadcastId);

    record CreateOrderCommand(Long broadcastId, Long donorId, String donorNickname, long amount, String message) {
    }

    record ConfirmCommand(Long donorId, String paymentKey, String orderId, long amount) {
    }
}
