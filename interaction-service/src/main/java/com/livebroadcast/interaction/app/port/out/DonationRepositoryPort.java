package com.livebroadcast.interaction.app.port.out;

import java.util.Optional;

import com.livebroadcast.interaction.app.PageResult;
import com.livebroadcast.interaction.domain.donation.Donation;
import com.livebroadcast.interaction.domain.donation.DonationSummary;

public interface DonationRepositoryPort {

    Donation save(Donation donation);

    /** 같은 주문에 대한 승인 요청이 동시에 들어와도 하나씩 처리되도록 행 잠금으로 조회한다. */
    Optional<Donation> findByOrderIdForUpdate(String orderId);

    PageResult<Donation> findDoneByBroadcast(Long broadcastId, int page, int size);

    DonationSummary summarize(Long broadcastId);
}
