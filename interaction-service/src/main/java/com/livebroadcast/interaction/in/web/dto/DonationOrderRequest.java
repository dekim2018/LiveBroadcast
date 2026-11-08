package com.livebroadcast.interaction.in.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record DonationOrderRequest(
        @NotNull(message = "후원할 방송을 선택해 주세요.") Long broadcastId,
        @Positive(message = "후원 금액을 입력해 주세요.") long amount,
        @Size(max = 200, message = "후원 메시지는 200자 이하여야 합니다.") String message
) {
}
