package io.gaboja9.mockstock.domain.portfolios.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class PortfolioResponseDto {

    private String stockCode;

    private String stockName;

    private int quantity;

    private int avgPrice;

    private int currentPrice;

    private int evaluationAmount;

    private int profit;

    private double profitRate;
}
