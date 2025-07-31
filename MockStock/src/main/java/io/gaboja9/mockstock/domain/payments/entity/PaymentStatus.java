package io.gaboja9.mockstock.domain.payments.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "결제 상태")
public enum PaymentStatus {
    @Schema(description = "결제 준비 완료 - 사용자가 결제 페이지로 이동한 상태")
    READY,

    @Schema(description = "결제 승인 완료 - 결제가 성공적으로 완료된 상태")
    APPROVED,

    @Schema(description = "결제 취소 - 사용자가 결제를 취소한 상태")
    CANCELLED,

    @Schema(description = "결제 실패 - 결제 과정에서 오류가 발생한 상태")
    FAILED
}
