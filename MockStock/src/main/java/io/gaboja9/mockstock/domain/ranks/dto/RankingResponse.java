package io.gaboja9.mockstock.domain.ranks.dto;

import io.gaboja9.mockstock.domain.ranks.entity.RanksType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "랭킹 조회 응답 정보")
public class RankingResponse {
    @Schema(description = "랭킹 유형", example = "RETURN_RATE")
    private RanksType ranksType;

    @Schema(description = "상위 5명 랭킹 (고정)", example = "[{\"rank\": 1, \"nickname\": \"투자왕\", \"returnRate\": 125.30}]")
    private List<RanksDto> topRankers;

    @Schema(description = "내 랭킹 정보", example = "{\"rank\": 15, \"nickname\": \"내닉네임\", \"returnRate\": 103.20}")
    private RanksDto myRanking;

    @Schema(description = "현재 페이지 랭킹 목록")
    private List<RanksDto> rankers;

    @Schema(description = "페이지네이션 정보")
    private PaginationInfo pagination;

    @Schema(description = "랭킹 마지막 업데이트 시간", example = "2025-07-28T11:30:00")
    private LocalDateTime lastUpdated;
}
