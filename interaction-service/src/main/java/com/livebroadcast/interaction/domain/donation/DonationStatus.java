package com.livebroadcast.interaction.domain.donation;

/**
 * 후원 주문 상태와 상태별 규칙. 상태를 추가할 때(예: 환불)는 이 enum 의 규칙부터 정한다.
 *
 * <pre>
 * PENDING -> DONE    (결제 승인)
 * PENDING -> FAILED  (결제 거절, 사용자 취소)
 * FAILED  -> DONE    (결제사가 결제 완료를 알려온 경우에만)
 * </pre>
 */
public enum DonationStatus {
    /** 주문만 생성되고 결제 승인 전 */
    PENDING,
    /** 결제 승인 완료, 후원 확정 */
    DONE,
    /** 결제 실패 또는 사용자가 취소 */
    FAILED;

    /** 브라우저의 승인 요청을 받아 결제사에 승인을 요청할 수 있는 상태 */
    public boolean acceptsConfirmRequest() {
        return this == PENDING;
    }

    /**
     * 결제사가 결제 완료를 알려왔을 때 적립할 수 있는 상태.
     * 결제사 기준으로 돈이 실제로 빠져나갔으므로 실패/취소 처리한 주문도 적립한다.
     */
    public boolean acceptsGatewayApproval() {
        return this == PENDING || this == FAILED;
    }

    /** 실패/취소로 닫을 수 있는 상태 */
    public boolean canFail() {
        return this == PENDING;
    }

    public boolean isCompleted() {
        return this == DONE;
    }
}
