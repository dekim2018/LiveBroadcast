package com.livebroadcast.interaction.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.livebroadcast.interaction.app.PageResult;
import com.livebroadcast.interaction.app.PaymentProperties;
import com.livebroadcast.interaction.app.port.in.DonationUseCase.ConfirmCommand;
import com.livebroadcast.interaction.app.port.in.DonationUseCase.CreateOrderCommand;
import com.livebroadcast.interaction.app.port.out.DonationRepositoryPort;
import com.livebroadcast.interaction.app.port.out.PaymentGatewayPort;
import com.livebroadcast.interaction.domain.broadcast.BroadcastInfo;
import com.livebroadcast.interaction.domain.donation.Donation;
import com.livebroadcast.interaction.domain.donation.DonationStatus;
import com.livebroadcast.interaction.domain.donation.DonationSummary;
import com.livebroadcast.interaction.domain.donation.PaymentApproval;
import com.livebroadcast.interaction.domain.donation.PaymentRejectedException;

/**
 * 스프링 컨텍스트 없이 포트를 가짜 구현으로 바꿔서 후원 확정 흐름(특히 중복 적립 방지)을 검증한다.
 */
class DonationServiceTest {

    private static final Long STREAMER = 1L;
    private static final Long DONOR = 2L;

    private final FakeDonationRepository repository = new FakeDonationRepository();
    private final FakePaymentGateway gateway = new FakePaymentGateway();
    private final List<String> events = new ArrayList<>();
    private DonationService service;

    @BeforeEach
    void setUp() {
        service = new DonationService(
                repository,
                id -> Optional.of(new BroadcastInfo(id, STREAMER, "방송자", "방송", "LIVE")),
                gateway,
                (broadcastId, name, payload) -> events.add(name),
                new PaymentProperties("ck", "sk", null, 1000, 1_000_000),
                Clock.fixed(Instant.parse("2026-11-10T00:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void 후원_대상은_방송_소유자로_정해진다() {
        Donation order = service.createOrder(new CreateOrderCommand(10L, DONOR, "시청자", 5000, "화이팅"));

        assertThat(order.getStreamerId()).isEqualTo(STREAMER);
        assertThat(order.getStatus()).isEqualTo(DonationStatus.PENDING);
    }

    @Test
    void 같은_승인_요청이_여러번_와도_한번만_적립된다() {
        Donation order = service.createOrder(new CreateOrderCommand(10L, DONOR, "시청자", 5000, ""));
        ConfirmCommand confirm = new ConfirmCommand(DONOR, "pk-1", order.getOrderId(), 5000);

        service.confirm(confirm);
        service.confirm(confirm);
        service.handleWebhook("pk-1", order.getOrderId());

        assertThat(gateway.confirmCalls.get()).isEqualTo(1);
        assertThat(service.summary(10L).totalAmount()).isEqualTo(5000);
        assertThat(events.stream().filter("donation.completed"::equals).count()).isEqualTo(1);
    }

    @Test
    void 브라우저가_보낸_금액이_주문과_다르면_결제사를_호출하지_않는다() {
        Donation order = service.createOrder(new CreateOrderCommand(10L, DONOR, "시청자", 5000, ""));

        assertThatThrownBy(() -> service.confirm(new ConfirmCommand(DONOR, "pk-1", order.getOrderId(), 100)))
                .hasMessageContaining("금액");
        assertThat(gateway.confirmCalls.get()).isZero();
    }

    @Test
    void 결제사가_거절하면_주문은_FAILED_가_된다() {
        Donation order = service.createOrder(new CreateOrderCommand(10L, DONOR, "시청자", 5000, ""));
        gateway.reject = true;

        assertThatThrownBy(() -> service.confirm(new ConfirmCommand(DONOR, "pk-1", order.getOrderId(), 5000)))
                .isInstanceOf(PaymentRejectedException.class);
        assertThat(repository.byOrderId.get(order.getOrderId()).getStatus()).isEqualTo(DonationStatus.FAILED);
    }

    @Test
    void 다른_사용자는_남의_주문을_승인할_수_없다() {
        Donation order = service.createOrder(new CreateOrderCommand(10L, DONOR, "시청자", 5000, ""));

        assertThatThrownBy(() -> service.confirm(new ConfirmCommand(99L, "pk-1", order.getOrderId(), 5000)))
                .hasMessageContaining("본인");
    }

    static class FakePaymentGateway implements PaymentGatewayPort {
        final AtomicInteger confirmCalls = new AtomicInteger();
        final Map<String, PaymentApproval> approved = new HashMap<>();
        boolean reject;

        @Override
        public PaymentApproval confirm(String paymentKey, String orderId, long amount) {
            confirmCalls.incrementAndGet();
            if (reject) {
                throw new PaymentRejectedException("REJECT_CARD_PAYMENT", "카드 거절");
            }
            PaymentApproval approval = new PaymentApproval(paymentKey, orderId, "DONE", amount, Instant.now());
            approved.put(paymentKey, approval);
            return approval;
        }

        @Override
        public PaymentApproval fetch(String paymentKey) {
            return approved.get(paymentKey);
        }
    }

    static class FakeDonationRepository implements DonationRepositoryPort {
        final Map<String, Donation> byOrderId = new HashMap<>();
        final AtomicLong seq = new AtomicLong();

        @Override
        public Donation save(Donation d) {
            Donation saved = new Donation(d.getId() == null ? seq.incrementAndGet() : d.getId(), d.getOrderId(),
                    d.getBroadcastId(), d.getStreamerId(), d.getStreamerNickname(), d.getDonorId(),
                    d.getDonorNickname(), d.getAmount(), d.getMessage(), d.getStatus(), d.getPaymentKey(),
                    d.getApprovedAt(), d.getFailureReason(), d.getCreatedAt(), d.getUpdatedAt());
            byOrderId.put(saved.getOrderId(), saved);
            return saved;
        }

        @Override
        public Optional<Donation> findByOrderIdForUpdate(String orderId) {
            return Optional.ofNullable(byOrderId.get(orderId));
        }

        @Override
        public PageResult<Donation> findDoneByBroadcast(Long broadcastId, int page, int size) {
            List<Donation> done = byOrderId.values().stream()
                    .filter(d -> d.getBroadcastId().equals(broadcastId) && d.getStatus() == DonationStatus.DONE)
                    .toList();
            return new PageResult<>(done, 0, size, done.size(), 1);
        }

        @Override
        public DonationSummary summarize(Long broadcastId) {
            List<Donation> done = findDoneByBroadcast(broadcastId, 0, 100).content();
            return new DonationSummary(broadcastId, done.stream().mapToLong(Donation::getAmount).sum(), done.size());
        }
    }
}
