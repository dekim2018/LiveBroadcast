package com.livebroadcast.interaction.domain.donation;

import com.livebroadcast.common.error.DomainException;

public final class DonationErrors {

    private DonationErrors() {
    }

    public static DomainException notFound() {
        return DomainException.notFound("DONATION_NOT_FOUND", "후원 주문을 찾을 수 없습니다.");
    }

    public static DomainException notDonor() {
        return DomainException.forbidden("NOT_DONOR", "본인의 후원 주문만 처리할 수 있습니다.");
    }

    public static DomainException amountMismatch() {
        return DomainException.badRequest("AMOUNT_MISMATCH", "결제 금액이 주문 금액과 일치하지 않습니다.");
    }

    public static DomainException orderMismatch() {
        return DomainException.badRequest("ORDER_MISMATCH", "결제 정보가 주문과 일치하지 않습니다.");
    }

    public static DomainException notApproved() {
        return DomainException.badRequest("PAYMENT_NOT_APPROVED", "결제가 승인되지 않았습니다.");
    }

    public static DomainException alreadyClosed() {
        return DomainException.conflict("DONATION_CLOSED", "이미 실패했거나 취소된 후원 주문입니다.");
    }

    public static DomainException differentPayment() {
        return DomainException.conflict("DIFFERENT_PAYMENT", "이미 다른 결제로 확정된 주문입니다.");
    }

    public static DomainException selfDonation() {
        return DomainException.badRequest("SELF_DONATION", "자신의 방송에는 후원할 수 없습니다.");
    }

    public static DomainException invalidAmount(long min, long max) {
        return DomainException.badRequest("INVALID_AMOUNT",
                "후원 금액은 " + min + "원 이상 " + max + "원 이하여야 합니다.");
    }

    public static DomainException invalidMessage() {
        return DomainException.badRequest("INVALID_MESSAGE", "후원 메시지는 200자 이하여야 합니다.");
    }
}
