package com.livebroadcast.interaction.in.web;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.livebroadcast.common.security.AuthUser;
import com.livebroadcast.common.security.LoginUser;
import com.livebroadcast.interaction.app.PaymentProperties;
import com.livebroadcast.interaction.app.port.in.DonationUseCase;
import com.livebroadcast.interaction.app.port.in.DonationUseCase.ConfirmCommand;
import com.livebroadcast.interaction.app.port.in.DonationUseCase.CreateOrderCommand;
import com.livebroadcast.interaction.domain.donation.DonationSummary;
import com.livebroadcast.interaction.in.web.dto.DonationConfirmRequest;
import com.livebroadcast.interaction.in.web.dto.DonationOrderRequest;
import com.livebroadcast.interaction.in.web.dto.DonationOrderResponse;
import com.livebroadcast.interaction.in.web.dto.DonationResponse;
import com.livebroadcast.interaction.in.web.dto.PageResponse;
import com.livebroadcast.interaction.in.web.dto.TossWebhookRequest;

import jakarta.validation.Valid;

@RestController
public class DonationController {

    private static final Logger log = LoggerFactory.getLogger(DonationController.class);

    private final DonationUseCase donationUseCase;
    private final PaymentProperties paymentProperties;

    public DonationController(DonationUseCase donationUseCase, PaymentProperties paymentProperties) {
        this.donationUseCase = donationUseCase;
        this.paymentProperties = paymentProperties;
    }

    /** 결제창 호출용 공개 설정(클라이언트 키는 공개되어도 되는 값) */
    @GetMapping("/api/donations/config")
    public Map<String, Object> config() {
        return Map.of(
                "clientKey", paymentProperties.clientKey() == null ? "" : paymentProperties.clientKey(),
                "minAmount", paymentProperties.minAmount(),
                "maxAmount", paymentProperties.maxAmount()
        );
    }

    @PostMapping("/api/donations/orders")
    public ResponseEntity<DonationOrderResponse> createOrder(@LoginUser AuthUser user,
                                                             @Valid @RequestBody DonationOrderRequest request) {
        var donation = donationUseCase.createOrder(new CreateOrderCommand(
                request.broadcastId(), user.id(), user.nickname(), request.amount(), request.message()));
        return ResponseEntity.status(HttpStatus.CREATED).body(DonationOrderResponse.from(donation));
    }

    /** 결제 성공 페이지에서 호출. 서버가 토스에 직접 승인을 요청하고 결과를 검증한다. */
    @PostMapping("/api/donations/confirm")
    public DonationResponse confirm(@LoginUser AuthUser user, @Valid @RequestBody DonationConfirmRequest request) {
        return DonationResponse.from(donationUseCase.confirm(
                new ConfirmCommand(user.id(), request.paymentKey(), request.orderId(), request.amount())));
    }

    @PostMapping("/api/donations/orders/{orderId}/cancel")
    public ResponseEntity<Void> cancel(@LoginUser AuthUser user, @PathVariable String orderId,
                                       @RequestParam(required = false) String reason) {
        donationUseCase.cancel(orderId, user.id(), reason);
        return ResponseEntity.noContent().build();
    }

    /**
     * 토스페이먼츠 웹훅. 같은 통지가 여러 번 와도 한 번만 적립된다.
     * 처리 실패 시 5xx 를 돌려줘서 토스가 재전송하도록 한다.
     */
    @PostMapping("/api/donations/webhook")
    public ResponseEntity<Void> webhook(@RequestBody TossWebhookRequest request) {
        if (request.data() != null && "PAYMENT_STATUS_CHANGED".equals(request.eventType())) {
            log.info("토스 웹훅 수신 orderId={} status={}", request.data().orderId(), request.data().status());
            donationUseCase.handleWebhook(request.data().paymentKey(), request.data().orderId());
        }
        return ResponseEntity.ok().build();
    }

    @GetMapping("/api/broadcasts/{broadcastId}/donations")
    public PageResponse<DonationResponse> list(@PathVariable Long broadcastId,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        return PageResponse.from(donationUseCase.listDone(broadcastId, page, size).map(DonationResponse::from));
    }

    @GetMapping("/api/broadcasts/{broadcastId}/donations/summary")
    public DonationSummary summary(@PathVariable Long broadcastId) {
        return donationUseCase.summary(broadcastId);
    }
}
