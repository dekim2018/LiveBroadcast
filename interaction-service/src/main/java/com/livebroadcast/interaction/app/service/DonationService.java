package com.livebroadcast.interaction.app.service;

import java.time.Clock;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.livebroadcast.common.error.DomainException;
import com.livebroadcast.interaction.app.PageResult;
import com.livebroadcast.interaction.app.PaymentProperties;
import com.livebroadcast.interaction.app.port.in.DonationUseCase;
import com.livebroadcast.interaction.app.port.out.BroadcastQueryPort;
import com.livebroadcast.interaction.app.port.out.DomainEventPublisherPort;
import com.livebroadcast.interaction.app.port.out.DonationRepositoryPort;
import com.livebroadcast.interaction.app.port.out.PaymentGatewayPort;
import com.livebroadcast.interaction.domain.broadcast.BroadcastInfo;
import com.livebroadcast.interaction.domain.donation.Donation;
import com.livebroadcast.interaction.domain.donation.DonationCompleted;
import com.livebroadcast.interaction.domain.donation.DonationErrors;
import com.livebroadcast.interaction.domain.donation.DonationSummary;
import com.livebroadcast.interaction.domain.donation.PaymentApproval;
import com.livebroadcast.interaction.domain.donation.PaymentRejectedException;

@Service
public class DonationService implements DonationUseCase {

    private static final Logger log = LoggerFactory.getLogger(DonationService.class);

    private final DonationRepositoryPort donationRepository;
    private final BroadcastQueryPort broadcastQuery;
    private final PaymentGatewayPort paymentGateway;
    private final DomainEventPublisherPort events;
    private final PaymentProperties paymentProperties;
    private final Clock clock;

    public DonationService(DonationRepositoryPort donationRepository, BroadcastQueryPort broadcastQuery,
                           PaymentGatewayPort paymentGateway, DomainEventPublisherPort events,
                           PaymentProperties paymentProperties, Clock clock) {
        this.donationRepository = donationRepository;
        this.broadcastQuery = broadcastQuery;
        this.paymentGateway = paymentGateway;
        this.events = events;
        this.paymentProperties = paymentProperties;
        this.clock = clock;
    }

    /**
     * 후원 대상은 클라이언트가 보낸 값이 아니라 broadcast-service 에서 조회한 방송 소유자로 정한다.
     */
    @Override
    @Transactional
    public Donation createOrder(CreateOrderCommand command) {
        BroadcastInfo broadcast = broadcastQuery.findBroadcast(command.broadcastId())
                .orElseThrow(() -> DomainException.notFound("BROADCAST_NOT_FOUND", "방송을 찾을 수 없습니다."));
        Donation donation = Donation.order(
                "don_" + UUID.randomUUID().toString().replace("-", ""),
                broadcast.id(), broadcast.ownerId(), broadcast.ownerNickname(),
                command.donorId(), command.donorNickname(), command.amount(), command.message(),
                paymentProperties.minAmount(), paymentProperties.maxAmount(), clock.instant());
        return donationRepository.save(donation);
    }

    /**
     * 결제 승인 확정. 같은 주문에 대한 요청은 행 잠금으로 직렬화되고,
     * 이미 같은 paymentKey 로 확정된 주문은 결제사를 다시 호출하지 않고 그대로 돌려준다(중복 적립 방지).
     */
    @Override
    @Transactional(noRollbackFor = PaymentRejectedException.class)
    public Donation confirm(ConfirmCommand command) {
        Donation donation = donationRepository.findByOrderIdForUpdate(command.orderId())
                .orElseThrow(DonationErrors::notFound);
        donation.ensureDonor(command.donorId());
        if (donation.isAlreadyApprovedWith(command.paymentKey())) {
            return donation;
        }
        donation.ensureConfirmable();
        donation.ensureRequestedAmount(command.amount());

        PaymentApproval approval;
        try {
            approval = paymentGateway.confirm(command.paymentKey(), command.orderId(), command.amount());
        } catch (PaymentRejectedException e) {
            donation.fail(e.getCode() + ": " + e.getMessage(), clock.instant());
            donationRepository.save(donation);
            throw e;
        }
        return applyApproval(donation, approval, false);
    }

    @Override
    @Transactional
    public void cancel(String orderId, Long donorId, String reason) {
        Donation donation = donationRepository.findByOrderIdForUpdate(orderId).orElseThrow(DonationErrors::notFound);
        donation.ensureDonor(donorId);
        donation.fail(reason == null ? "USER_CANCELED" : reason, clock.instant());
        donationRepository.save(donation);
    }

    @Override
    @Transactional
    public void handleWebhook(String paymentKey, String orderId) {
        if (paymentKey == null || orderId == null) {
            return;
        }
        Donation donation = donationRepository.findByOrderIdForUpdate(orderId).orElse(null);
        if (donation == null || donation.isAlreadyApprovedWith(paymentKey)) {
            return;
        }
        if (donation.getStatus().isCompleted()) {
            // 오류로 응답하면 결제사가 같은 웹훅을 계속 재전송하므로, 기록만 남기고 정상 응답한다.
            log.warn("이미 다른 결제로 확정된 주문의 웹훅 orderId={} paymentKey={}", orderId, paymentKey);
            return;
        }
        PaymentApproval approval = paymentGateway.fetch(paymentKey);
        if (!approval.isDone()) {
            log.info("웹훅 수신, 아직 승인되지 않은 결제 orderId={} status={}", orderId, approval.status());
            return;
        }
        if (!donation.getStatus().acceptsConfirmRequest()) {
            log.warn("실패/취소된 주문의 결제 완료 웹훅, 결제사 기준으로 적립 orderId={} status={}",
                    orderId, donation.getStatus());
        }
        applyApproval(donation, approval, true);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Donation> listDone(Long broadcastId, int page, int size) {
        int safeSize = size <= 0 ? 20 : Math.min(size, 100);
        return donationRepository.findDoneByBroadcast(broadcastId, Math.max(page, 0), safeSize);
    }

    @Override
    @Transactional(readOnly = true)
    public DonationSummary summary(Long broadcastId) {
        return donationRepository.summarize(broadcastId);
    }

    private Donation applyApproval(Donation donation, PaymentApproval approval, boolean fromWebhook) {
        boolean newlyApproved = fromWebhook
                ? donation.reconcile(approval, clock.instant())
                : donation.approve(approval, clock.instant());
        Donation saved = donationRepository.save(donation);
        if (newlyApproved) {
            log.info("후원 확정 orderId={} amount={} broadcastId={}", saved.getOrderId(), saved.getAmount(),
                    saved.getBroadcastId());
            events.publish(new DonationCompleted(saved));
        }
        return saved;
    }
}
