package io.gaboja9.mockstock.domain.members.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MemberInfoDto {

    @Schema(description = "닉네임")
    private String nickname;

    @Schema(description = "프로필 이미지")
    private String profileImage;

    @Schema(description = "수익률")
    private double totalProfitRate;

    @Schema(description = "총 자산")
    private int totalCashBalance;

    @Schema(description = "총 거래 횟수")
    private int tradeCnt;

    @Schema(description = "랭킹")
    private int ranking;

    @Schema(description = "활동 기간")
    private int period;

    @Schema(description = "파산 횟수")
    private int bankruptcyCnt;
}
