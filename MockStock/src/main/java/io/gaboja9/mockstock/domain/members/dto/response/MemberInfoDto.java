package io.gaboja9.mockstock.domain.members.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MemberInfoDto {

    private String nickname;

    private String profileImage;

    private double totalProfit;

    private int totalEvaluationAmount;

    private int tradeCnt;

    private int ranking;

    private int period;
}
