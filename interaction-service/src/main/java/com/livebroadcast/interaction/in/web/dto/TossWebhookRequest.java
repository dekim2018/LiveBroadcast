package com.livebroadcast.interaction.in.web.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 토스페이먼츠 웹훅(PAYMENT_STATUS_CHANGED) 본문 중 필요한 값만 받는다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TossWebhookRequest(String eventType, Data data) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Data(String paymentKey, String orderId, String status) {
    }
}
