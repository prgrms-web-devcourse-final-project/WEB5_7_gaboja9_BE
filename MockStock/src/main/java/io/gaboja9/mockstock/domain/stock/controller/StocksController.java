package io.gaboja9.mockstock.domain.stock.controller;

import io.gaboja9.mockstock.domain.stock.dto.StockResponse;
import io.gaboja9.mockstock.domain.stock.service.StocksBulkService;
import io.gaboja9.mockstock.domain.stock.service.StocksDataService;
import io.gaboja9.mockstock.domain.stock.service.StocksMinuteService;
import io.gaboja9.mockstock.domain.stock.service.StocksService;
import io.gaboja9.mockstock.domain.stock.service.TodayMinuteStockService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// 주식 데이터 수집을 위한 컨트롤러
@Slf4j
@RestController
@RequestMapping("/api/stocks")
@RequiredArgsConstructor
public class StocksController implements StocksControllerSpec {

    private final StocksDataService stockDataService;
    private final StocksMinuteService stocksMinuteService;
    private final TodayMinuteStockService todayMinuteStockService;
    private final StocksService stocksService;
    private final StocksBulkService stocksBulkService;

    @GetMapping("/long-term-daily")
    public ResponseEntity<?> fetchLongTermDailyStockData(
            @RequestParam(defaultValue = "J") String marketCode,
            @RequestParam String stockCode,
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(defaultValue = "D") String periodCode) {

        log.info("장기간 데이터 수집 요청 접수: {}, 기간: {} ~ {}", stockCode, startDate, endDate);
        try {
            stockDataService.fetchAndSaveLongTermData(
                    marketCode, stockCode, startDate, endDate, periodCode);
            String resultMessage =
                    String.format(
                            "장기간 데이터 수집 작업 성공적으로 위임: %s, 기간: %s ~ %s",
                            stockCode, startDate, endDate);
            return ResponseEntity.ok(resultMessage);
        } catch (Exception e) {
            log.error("장기간 데이터 수집 중 컨트롤러 레벨 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("장기간 데이터 수집 실패: " + e.getMessage());
        }
    }

    @GetMapping("/long-term-minute")
    public ResponseEntity<?> fetchLongTermMinuteStockData(
            @RequestParam(defaultValue = "J") String marketCode,
            @RequestParam String stockCode,
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(defaultValue = "Y") String includePastData) {

        log.info("장기간 분봉 데이터 수집 요청 접수: {}, 기간: {} ~ {}", stockCode, startDate, endDate);

        try {

            stocksMinuteService.fetchAndSaveLongTermMinuteData(
                    marketCode, stockCode, startDate, endDate, includePastData);

            String resultMessage =
                    String.format(
                            "장기간 분봉 데이터 수집 작업 성공적으로 위임: %s, 기간: %s ~ %s",
                            stockCode, startDate, endDate);

            return ResponseEntity.ok(resultMessage);

        } catch (Exception e) {
            log.error("장기간 분봉 데이터 수집 중 컨트롤러 레벨 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("장기간 분봉 데이터 수집 실패: " + e.getMessage());
        }
    }

    @GetMapping("/fetch-data")
    public ResponseEntity<?> fetchStockData(
            @RequestParam String marketCode, //  주식 J
            @RequestParam String stockCode, //  단일 종목 코드
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam String periodCode) {

        log.info("주식 데이터 수집 요청: {}, 기간: {} ~ {}", stockCode, startDate, endDate);

        // 단일 종목 처리용 서비스 호출
        stockDataService.fetchAndSaveData(marketCode, stockCode, startDate, endDate, periodCode);

        return ResponseEntity.status(HttpStatus.CREATED).body(" 주식 저장 완료");
    }

    @GetMapping("/minute-data")
    public ResponseEntity<?> mStockData(
            @RequestParam String marketCode,
            @RequestParam String stockCode,
            @RequestParam String startHour,
            @RequestParam String date,
            @RequestParam String includePastData) {
        stocksMinuteService.fetchAndSaveMinuteData(
                marketCode, stockCode, date, startHour, includePastData);
        return ResponseEntity.status(HttpStatus.CREATED).body("분 주식 저장 완료");
    }

    @GetMapping("/today-minute-data")
    public ResponseEntity<?> minuteStockData(
            @RequestParam String marketCode,
            @RequestParam String stockCode,
            @RequestParam String startTime,
            @RequestParam String includePastData,
            @RequestParam String clsCode) {

        todayMinuteStockService.fetchAndSaveCurrentDayMinuteData(
                marketCode, stockCode, startTime, includePastData, clsCode);
        return ResponseEntity.status(HttpStatus.CREATED).body("분 주식 저장 완료");
    }

    @GetMapping()
    public ResponseEntity<List<StockResponse>> getAllStocks() {
        return ResponseEntity.ok(stocksService.getAllStocks());
    }

    // 전체 주식 데이터 수집 (일봉+주봉+월봉 2년11개월 + 분봉 7일)
    @Override
    @GetMapping("/bulk/all-data")
    public ResponseEntity<String> fetchAllStocksAllData(
            @RequestParam(defaultValue = "J") String marketCode) {

        log.info("전체 주식 모든 데이터 수집 요청 시작");

        try {
            stocksBulkService.fetchAllStocksAllData(marketCode);
            return ResponseEntity.status(HttpStatus.CREATED).body("데이터 수집 작업 시작됨");

        } catch (Exception e) {
            log.error("데이터 수집 시작 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("데이터 수집 시작 실패");
        }
    }
}
