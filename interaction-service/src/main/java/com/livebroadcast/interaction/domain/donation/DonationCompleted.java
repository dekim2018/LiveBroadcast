package com.livebroadcast.interaction.domain.donation;

/** 후원이 처음으로 확정됨. 같은 결제의 중복 승인 통지에서는 발행되지 않는다. */
public record DonationCompleted(Donation donation) {
}
