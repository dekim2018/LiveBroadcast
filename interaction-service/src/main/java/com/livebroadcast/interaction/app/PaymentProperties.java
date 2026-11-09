package com.livebroadcast.interaction.app;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 토스페이먼츠 설정. 테스트 키(test_ck_, test_sk_)를 쓰면 실제 결제가 일어나지 않는다.
 */
@ConfigurationProperties(prefix = "app.payment")
public record PaymentProperties(
        String clientKey,
        String secretKey,
        String apiUrl,
        long minAmount,
        long maxAmount
) {
    public PaymentProperties {
        if (apiUrl == null || apiUrl.isBlank()) apiUrl = "https://api.tosspayments.com";
        if (minAmount <= 0) minAmount = 1000;
        if (maxAmount <= 0) maxAmount = 1_000_000;
    }
}
