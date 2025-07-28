package io.gaboja9.mockstock.domain.ranks.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "회원 랭킹 정보")
public class RanksDto {

    @Schema(description = "회원 고유 ID", example = "123")
    private Long memberId;

    @Schema(description = "회원 닉네임", example = "투자왕")
    private String nickname;

    @Schema(description = "수익률 (%)", example = "125.30", minimum = "0")
    private double returnRate;

    @Schema(description = "총 수익금 (원)", example = "2530000")
    private long totalProfit;

    @Schema(description = "파산 횟수", example = "0", minimum = "0")
    private int bankruptcyCount;

    @Schema(description = "총 자산 (원)", example = "12530000", minimum = "0")
    private long totalAsset;

    @Schema(description = "총 투자원금 (원)", example = "10000000", minimum = "0")
    private long totalInvestment;

    @Setter
    @Schema(description = "랭킹 순위", example = "1", minimum = "1")
    private int rank;

}
