package io.gaboja9.mockstock.domain.ranks.dto;

import io.gaboja9.mockstock.domain.ranks.entity.RanksType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "랭킹 조회 요청 정보")
public class RankingRequest {

    @Schema(description = "랭킹 유형", example = "RETURN_RATE",
            allowableValues = {"RETURN_RATE", "PROFIT", "ASSET", "BANKRUPTCY"})
    private RanksType ranksType;

    @Schema(description = "페이지 번호 (0부터 시작)", example = "0", minimum = "0")
    private int page;

    @Schema(description = "페이지 크기", example = "5", minimum = "1", maximum = "50")
    private int size;

    public static RankingRequest of(RanksType ranksType, int page, int size) {
        return RankingRequest.builder()
                .ranksType(ranksType)
                .page(Math.max(0, page))
                .size(size > 0 ? Math.min(size, 50) : 5)
                .build();
    }
}
