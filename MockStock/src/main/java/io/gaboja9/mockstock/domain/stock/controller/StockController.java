package io.gaboja9.mockstock.domain.stock.controller;

import io.gaboja9.mockstock.domain.stock.service.DailyStockService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 주식 데이터 수집을 위한 컨트롤러 */
@Slf4j
@RestController
@RequestMapping("/api/stocks")
@RequiredArgsConstructor
public class StockController {

    private final DailyStockService stockService;

    @PostMapping("/fetch-data")
    public String fetchStockData(
            @RequestParam String marketCode, //  주식 J
            @RequestParam String stockCode, //  단일 종목 코드
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam String periodCode) {

        log.info("주식 데이터 수집 요청: {}, 기간: {} ~ {}", stockCode, startDate, endDate);

        // 단일 종목 처리용 서비스 호출
        stockService.fetchAndSaveDailyData(marketCode, stockCode, startDate, endDate, periodCode);

        return String.format(
                "%s 종목에 대한 데이터 수집 작업이 시작되었습니다. (%s ~ %s)", stockCode, startDate, endDate);
    }
}
