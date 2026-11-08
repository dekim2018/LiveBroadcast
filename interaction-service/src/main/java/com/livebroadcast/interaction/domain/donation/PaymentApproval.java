package com.livebroadcast.interaction.domain.donation;

import java.time.Instant;

/**
 * 결제사(토스페이먼츠)로부터 서버가 직접 받아온 승인 결과. 브라우저가 보낸 값은 신뢰하지 않는다.
 */
public record PaymentApproval(String paymentKey, String orderId, String status, long totalAmount, Instant approvedAt) {

    public boolean isDone() {
        return "DONE".equals(status);
    }
}
