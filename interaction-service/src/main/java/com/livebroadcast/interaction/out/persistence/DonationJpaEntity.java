package com.livebroadcast.interaction.out.persistence;

import java.time.Instant;

import com.livebroadcast.interaction.domain.donation.DonationStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "donations")
public class DonationJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, unique = true, length = 64)
    private String orderId;

    @Column(name = "broadcast_id", nullable = false)
    private Long broadcastId;

    @Column(name = "streamer_id", nullable = false)
    private Long streamerId;

    @Column(name = "streamer_nickname", nullable = false, length = 20)
    private String streamerNickname;

    @Column(name = "donor_id", nullable = false)
    private Long donorId;

    @Column(name = "donor_nickname", nullable = false, length = 20)
    private String donorNickname;

    @Column(nullable = false)
    private long amount;

    @Column(nullable = false, length = 200)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private DonationStatus status;

    @Column(name = "payment_key", unique = true, length = 200)
    private String paymentKey;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "failure_reason", length = 200)
    private String failureReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected DonationJpaEntity() {
    }

    DonationJpaEntity(Long id, String orderId, Long broadcastId, Long streamerId, String streamerNickname,
                      Long donorId, String donorNickname, long amount, String message, DonationStatus status,
                      String paymentKey, Instant approvedAt, String failureReason, Instant createdAt,
                      Instant updatedAt) {
        this.id = id;
        this.orderId = orderId;
        this.broadcastId = broadcastId;
        this.streamerId = streamerId;
        this.streamerNickname = streamerNickname;
        this.donorId = donorId;
        this.donorNickname = donorNickname;
        this.amount = amount;
        this.message = message;
        this.status = status;
        this.paymentKey = paymentKey;
        this.approvedAt = approvedAt;
        this.failureReason = failureReason;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    Long getId() { return id; }
    String getOrderId() { return orderId; }
    Long getBroadcastId() { return broadcastId; }
    Long getStreamerId() { return streamerId; }
    String getStreamerNickname() { return streamerNickname; }
    Long getDonorId() { return donorId; }
    String getDonorNickname() { return donorNickname; }
    long getAmount() { return amount; }
    String getMessage() { return message; }
    DonationStatus getStatus() { return status; }
    String getPaymentKey() { return paymentKey; }
    Instant getApprovedAt() { return approvedAt; }
    String getFailureReason() { return failureReason; }
    Instant getCreatedAt() { return createdAt; }
    Instant getUpdatedAt() { return updatedAt; }
}
