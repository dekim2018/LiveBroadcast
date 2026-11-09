package com.livebroadcast.interaction.in.web.dto;

import com.livebroadcast.interaction.domain.donation.Donation;

/**
 * 결제창 호출에 필요한 값. customerKey 는 토스 SDK 가 요구하는 구매자 식별자다.
 */
public record DonationOrderResponse(String orderId, String orderName, long amount, Long broadcastId,
                                    String streamerNickname, String customerKey, String donorNickname) {

    public static DonationOrderResponse from(Donation d) {
        return new DonationOrderResponse(d.getOrderId(), d.orderName(), d.getAmount(), d.getBroadcastId(),
                d.getStreamerNickname(), "user_" + d.getDonorId(), d.getDonorNickname());
    }
}
