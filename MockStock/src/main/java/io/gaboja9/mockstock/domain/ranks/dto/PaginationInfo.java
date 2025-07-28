package io.gaboja9.mockstock.domain.ranks.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "페이지네이션 정보")
public class PaginationInfo {

    @Schema(description = "현재 페이지 번호 (0부터 시작)", example = "0", minimum = "0")
    private int currentPage;

    @Schema(description = "페이지 크기 (한 페이지당 항목 수)", example = "5", minimum = "1", maximum = "50")
    private int pageSize;

    @Schema(description = "전체 항목 수", example = "100", minimum = "0")
    private long totalElements;

    @Schema(description = "전체 페이지 수", example = "20", minimum = "0")
    private int totalPages;

    @Schema(description = "다음 페이지 존재 여부", example = "true")
    private boolean hasNext;

    @Schema(description = "이전 페이지 존재 여부", example = "false")
    private boolean hasPrevious;
}
