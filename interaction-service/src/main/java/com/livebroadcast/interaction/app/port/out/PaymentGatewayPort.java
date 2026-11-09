package com.livebroadcast.interaction.app.port.out;

import com.livebroadcast.interaction.domain.donation.PaymentApproval;

public interface PaymentGatewayPort {

    /**
     * 결제 승인 요청. 결제사가 거절하면 PaymentRejectedException,
     * 결제사와 통신이 안 되면 BAD_GATEWAY 예외를 던진다.
     */
    PaymentApproval confirm(String paymentKey, String orderId, long amount);

    PaymentApproval fetch(String paymentKey);
}
