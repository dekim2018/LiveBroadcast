package com.livebroadcast.interaction.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record DonationConfirmRequest(
        @NotBlank String paymentKey,
        @NotBlank String orderId,
        @Positive long amount
) {
}
