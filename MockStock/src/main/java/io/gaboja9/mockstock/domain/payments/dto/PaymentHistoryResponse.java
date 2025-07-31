package io.gaboja9.mockstock.domain.payments.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "충전 내역 조회 응답 DTO")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentHistoryResponse {

    @Schema(description = "충전 내역 목록")
    private List<PaymentHistoryDto> payments;

    @Schema(description = "페이지네이션 정보")
    private PaginationInfo pagination;

    @Schema(description = "충전 요약 통계")
    private PaymentSummary summary;

    @Schema(description = "페이지네이션 정보")
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaginationInfo {

        @Schema(description = "현재 페이지 번호", example = "0")
        private int currentPage;

        @Schema(description = "페이지 크기", example = "10")
        private int pageSize;

        @Schema(description = "전체 페이지 수", example = "5")
        private int totalPages;

        @Schema(description = "전체 요소 수", example = "47")
        private long totalElements;

        @Schema(description = "다음 페이지 존재 여부", example = "true")
        private boolean hasNext;

        @Schema(description = "이전 페이지 존재 여부", example = "false")
        private boolean hasPrevious;
    }

    @Schema(description = "충전 요약 통계")
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentSummary {

        @Schema(description = "총 충전 금액 (원)", example = "500000")
        private long totalChargedAmount;

        @Schema(description = "총 충전 시도 횟수", example = "6")
        private int totalChargeCount;

        @Schema(description = "승인된 충전 금액 (원)", example = "450000")
        private long approvedAmount;

        @Schema(description = "승인된 충전 횟수", example = "5")
        private int approvedCount;
    }
}