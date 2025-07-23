package io.gaboja9.mockstock.domain.portfolios.mapper;

import io.gaboja9.mockstock.domain.portfolios.dto.response.PortfolioResponseDto;
import io.gaboja9.mockstock.domain.portfolios.entity.Portfolios;
import io.gaboja9.mockstock.global.websocket.HantuWebSocketHandler;
import io.gaboja9.mockstock.global.websocket.dto.StockPrice;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class PortfoliosMapper {

    private final HantuWebSocketHandler hantuWebSocketHandler;

    public PortfolioResponseDto toDto(Portfolios p) {
        int quantity = p.getQuantity();
        int avgPrice = p.getAvgPrice();

        Optional<StockPrice> optionalPrice =
                Optional.ofNullable(hantuWebSocketHandler.getLatestPrice(p.getStockCode()));

        if (optionalPrice.isEmpty()) {
            log.warn("실시간 가격 정보 없음 - stockCode={}", p.getStockCode());

            return PortfolioResponseDto.builder()
                    .stockCode(p.getStockCode())
                    .stockName(p.getStockName())
                    .quantity(quantity)
                    .avgPrice(avgPrice)
                    .currentPrice(0)
                    .evaluationAmount(0)
                    .profit(0)
                    .profitRate(0)
                    .build();
        }

        int currentPrice = optionalPrice.get().getCurrentPrice();

        int evaluationAmount = currentPrice * quantity;
        int profit = (currentPrice - avgPrice) * quantity;
        int investment = avgPrice * quantity;
        double profitRate =
                (investment == 0
                        ? 0.00
                        : Math.round((double) profit / investment * 100 * 100) / 100.0);

        return PortfolioResponseDto.builder()
                .stockCode(p.getStockCode())
                .stockName(p.getStockName())
                .quantity(quantity)
                .avgPrice(avgPrice)
                .currentPrice(currentPrice)
                .evaluationAmount(evaluationAmount)
                .profit(profit)
                .profitRate(profitRate)
                .build();
    }
}
