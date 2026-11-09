package com.livebroadcast.interaction.domain.donation;

import com.livebroadcast.common.error.DomainException;
import com.livebroadcast.common.error.ErrorType;

/**
 * 결제사가 승인을 거절한 경우(잔액 부족, 카드 거절 등). 주문을 FAILED 로 남기기 위해 롤백하지 않는다.
 */
public class PaymentRejectedException extends DomainException {

    public PaymentRejectedException(String code, String message) {
        super(ErrorType.BAD_REQUEST, code == null ? "PAYMENT_REJECTED" : code,
                message == null ? "결제가 승인되지 않았습니다." : message);
    }
}
