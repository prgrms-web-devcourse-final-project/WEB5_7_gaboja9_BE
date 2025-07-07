package io.gaboja9.mockstock.domain.portfolios.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class PortfoliosResponseDto {

    private int cashBalance;

    private int totalEvaluationAmount;

    private int totalProfit;

    private double totalProfitRate;

    private List<PortfolioResponseDto> portfolios;
}
