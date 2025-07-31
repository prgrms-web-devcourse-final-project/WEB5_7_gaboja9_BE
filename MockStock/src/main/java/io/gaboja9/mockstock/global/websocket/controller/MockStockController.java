package io.gaboja9.mockstock.global.websocket.controller;

import io.gaboja9.mockstock.global.websocket.dto.StockPriceDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@RestController
@RequiredArgsConstructor
public class MockStockController {

    private final SimpMessagingTemplate messagingTemplate;
    private static final DateTimeFormatter HHMMSS = DateTimeFormatter.ofPattern("HHmmss");

    @PostMapping("/mockPrice/{stockCode}")
    public List<StockPriceDto> sendMockStockPrice(@PathVariable String stockCode) {

        List<StockPriceDto> sent = new ArrayList<>();

        for (int i = 0; i < 10; i++) { // 10회 반복

            for (int j = 0; j < 2; j++) { // 1초 동안 2개씩 보냄

                int currentPrice = 71400 + ThreadLocalRandom.current().nextInt(-100, 101);
                int highPrice = currentPrice + ThreadLocalRandom.current().nextInt(1, 6);
                int lowPrice = currentPrice - ThreadLocalRandom.current().nextInt(1, 6);

                StockPriceDto priceData =
                        StockPriceDto.builder()
                                .stockCode(stockCode)
                                .currentPrice(currentPrice)
                                .dayOverDayPercent(-1.65 + (j * 0.01))
                                .tradeTime(LocalTime.now().format(HHMMSS))
                                .tradeVolume(100L + j)
                                .highPrice(highPrice)
                                .lowPrice(lowPrice)
                                .cumulativeVolume(25000L + i * 2 + j)
                                .build();

                messagingTemplate.convertAndSend("/topic/stock/" + stockCode, priceData);
                log.info("[MOCK→STOMP] {}: {}", stockCode, priceData);
                sent.add(priceData);
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        return sent;
    }
}
