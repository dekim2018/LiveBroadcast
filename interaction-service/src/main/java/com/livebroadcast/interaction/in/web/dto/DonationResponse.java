package com.livebroadcast.interaction.in.web.dto;

import java.time.Instant;

import com.livebroadcast.interaction.domain.donation.Donation;
import com.livebroadcast.interaction.domain.donation.DonationStatus;

public record DonationResponse(Long id, String orderId, Long broadcastId, String streamerNickname,
                               String donorNickname, long amount, String message, DonationStatus status,
                               Instant approvedAt) {

    public static DonationResponse from(Donation d) {
        return new DonationResponse(d.getId(), d.getOrderId(), d.getBroadcastId(), d.getStreamerNickname(),
                d.getDonorNickname(), d.getAmount(), d.getMessage(), d.getStatus(), d.getApprovedAt());
    }
}
