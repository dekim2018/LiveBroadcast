package com.livebroadcast.interaction.out.persistence;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.livebroadcast.interaction.domain.donation.DonationStatus;

import jakarta.persistence.LockModeType;

public interface DonationJpaRepository extends JpaRepository<DonationJpaEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from DonationJpaEntity d where d.orderId = :orderId")
    Optional<DonationJpaEntity> findByOrderIdForUpdate(@Param("orderId") String orderId);

    Page<DonationJpaEntity> findByBroadcastIdAndStatus(Long broadcastId, DonationStatus status, Pageable pageable);

    @Query("select coalesce(sum(d.amount), 0L) from DonationJpaEntity d "
            + "where d.broadcastId = :broadcastId and d.status = :status")
    long sumAmount(@Param("broadcastId") Long broadcastId, @Param("status") DonationStatus status);

    long countByBroadcastIdAndStatus(Long broadcastId, DonationStatus status);
}
