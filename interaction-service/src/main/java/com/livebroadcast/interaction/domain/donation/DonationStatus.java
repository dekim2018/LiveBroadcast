package com.livebroadcast.interaction.domain.donation;

public enum DonationStatus {
    /** 주문만 생성되고 결제 승인 전 */
    PENDING,
    /** 결제 승인 완료, 후원 확정 */
    DONE,
    /** 결제 실패 또는 사용자가 취소 */
    FAILED
}
