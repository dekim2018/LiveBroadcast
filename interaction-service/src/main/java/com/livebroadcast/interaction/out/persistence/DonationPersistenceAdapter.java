package com.livebroadcast.interaction.out.persistence;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import com.livebroadcast.interaction.app.PageResult;
import com.livebroadcast.interaction.app.port.out.DonationRepositoryPort;
import com.livebroadcast.interaction.domain.donation.Donation;
import com.livebroadcast.interaction.domain.donation.DonationStatus;
import com.livebroadcast.interaction.domain.donation.DonationSummary;

@Component
public class DonationPersistenceAdapter implements DonationRepositoryPort {

    private final DonationJpaRepository repository;

    public DonationPersistenceAdapter(DonationJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Donation save(Donation d) {
        return toDomain(repository.save(new DonationJpaEntity(d.getId(), d.getOrderId(), d.getBroadcastId(),
                d.getStreamerId(), d.getStreamerNickname(), d.getDonorId(), d.getDonorNickname(), d.getAmount(),
                d.getMessage(), d.getStatus(), d.getPaymentKey(), d.getApprovedAt(), d.getFailureReason(),
                d.getCreatedAt(), d.getUpdatedAt())));
    }

    @Override
    public Optional<Donation> findByOrderIdForUpdate(String orderId) {
        return repository.findByOrderIdForUpdate(orderId).map(DonationPersistenceAdapter::toDomain);
    }

    @Override
    public PageResult<Donation> findDoneByBroadcast(Long broadcastId, int page, int size) {
        Page<DonationJpaEntity> result = repository.findByBroadcastIdAndStatus(broadcastId, DonationStatus.DONE,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "approvedAt")));
        return new PageResult<>(result.getContent().stream().map(DonationPersistenceAdapter::toDomain).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Override
    public DonationSummary summarize(Long broadcastId) {
        return new DonationSummary(broadcastId,
                repository.sumAmount(broadcastId, DonationStatus.DONE),
                repository.countByBroadcastIdAndStatus(broadcastId, DonationStatus.DONE));
    }

    private static Donation toDomain(DonationJpaEntity e) {
        return new Donation(e.getId(), e.getOrderId(), e.getBroadcastId(), e.getStreamerId(),
                e.getStreamerNickname(), e.getDonorId(), e.getDonorNickname(), e.getAmount(), e.getMessage(),
                e.getStatus(), e.getPaymentKey(), e.getApprovedAt(), e.getFailureReason(), e.getCreatedAt(),
                e.getUpdatedAt());
    }
}
