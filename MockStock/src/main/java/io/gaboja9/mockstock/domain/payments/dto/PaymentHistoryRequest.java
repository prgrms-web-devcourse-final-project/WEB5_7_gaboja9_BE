package io.gaboja9.mockstock.domain.payments.dto;

import io.gaboja9.mockstock.domain.payments.entity.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "충전 내역 조회 요청 DTO")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentHistoryRequest {

    @Schema(
            description = "페이지 번호 (0부터 시작)",
            example = "0",
            minimum = "0"
    )
    @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다")
    private int page = 0;

    @Schema(
            description = "페이지 크기 (한 페이지당 항목 수)",
            example = "10",
            minimum = "1",
            maximum = "100"
    )
    @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다")
    @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다")
    private int size = 10;

    @Schema(
            description = "결제 상태 필터 (선택사항)",
            example = "APPROVED",
            allowableValues = {"READY", "APPROVED", "CANCELLED", "FAILED"}
    )
    private PaymentStatus status; // 선택적 필터링
}