package io.gaboja9.mockstock.domain.payments.dto;

import io.gaboja9.mockstock.domain.payments.entity.PaymentHistory;
import io.gaboja9.mockstock.domain.payments.entity.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Schema(description = "충전 내역 DTO")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentHistoryDto {

    @Schema(description = "충전 내역 ID", example = "1")
    private Long id;

    @Schema(description = "주문 번호", example = "ORDER_1234567890_123")
    private String partnerOrderId;

    @Schema(description = "충전 금액 (원)", example = "100000")
    private int amount;

    @Schema(description = "결제 상태", example = "APPROVED")
    private PaymentStatus status;

    @Schema(description = "생성 시간", example = "2025-07-31T10:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "수정 시간", example = "2025-07-31T10:35:00")
    private LocalDateTime updatedAt;

    public static PaymentHistoryDto from(PaymentHistory paymentHistory) {
        return PaymentHistoryDto.builder()
                .id(paymentHistory.getId())
                .partnerOrderId(paymentHistory.getPartnerOrderId())
                .amount(paymentHistory.getAmount())
                .status(paymentHistory.getStatus())
                .createdAt(paymentHistory.getCreatedAt())
                .updatedAt(paymentHistory.getUpdatedAt())
                .build();
    }
}