package io.gaboja9.mockstock.domain.stock.controller;

import io.gaboja9.mockstock.domain.stock.dto.StockResponse;
import io.gaboja9.mockstock.domain.stock.service.DailyStockService;
import io.gaboja9.mockstock.domain.stock.service.MinuteStockService;
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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

// 주식 데이터 수집을 위한 컨트롤러
@Slf4j
@RestController
@RequestMapping("/api/stocks")
@RequiredArgsConstructor
public class StockController implements StockControllerSpec {

    private final DailyStockService dailyStockService;
    private final MinuteStockService minuteStockService;
    private final TodayMinuteStockService todayMinuteStockService;
    private final StocksService stocksService;

    @GetMapping("/long-term-daily")
    public ResponseEntity<?> fetchLongTermDailyStockData(
            @RequestParam(defaultValue = "J") String marketCode,
            @RequestParam String stockCode,
            @RequestParam String startDate, // 20240701 형태로 받기
            @RequestParam String endDate, // 20240731 형태로 받기
            @RequestParam(defaultValue = "D") String periodCode) {

        log.info("장기간 일봉 데이터 수집 요청: {}, 기간: {} ~ {}", stockCode, startDate, endDate);

        try {
            // yyyyMMdd 형태를 LocalDate로 변환 (날짜 계산용)
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
            LocalDate start = LocalDate.parse(startDate, formatter);
            LocalDate end = LocalDate.parse(endDate, formatter);
            LocalDate current = start;

            int batchCount = 0;
            int successCount = 0;
            int failCount = 0;

            while (current.isBefore(end) || current.isEqual(end)) {
                LocalDate batchEnd = current.plusDays(99);
                if (batchEnd.isAfter(end)) {
                    batchEnd = end;
                }

                batchCount++;
                log.info("일봉 배치 {}: {} ~ {}", batchCount, current, batchEnd);

                try {
                    // String 형태로 변환해서 기존 서비스 호출
                    String batchStartStr = current.format(formatter);
                    String batchEndStr = batchEnd.format(formatter);

                    dailyStockService.fetchAndSaveDailyData(
                            marketCode, stockCode, batchStartStr, batchEndStr, periodCode);
                    successCount++;

                } catch (Exception batchException) {
                    failCount++;
                    log.error("일봉 배치 {} 실패: {}", batchCount, batchException.getMessage());
                }

                current = batchEnd.plusDays(1);

                if (current.isBefore(end) || current.isEqual(end)) {
                    Thread.sleep(1000);
                }
            }

            String resultMessage =
                    String.format(
                            "장기간 일봉 데이터 수집 완료 - 총 배치: %d개, 성공: %d개, 실패: %d개",
                            batchCount, successCount, failCount);

            return ResponseEntity.ok(resultMessage);

        } catch (Exception e) {
            log.error("장기간 일봉 데이터 수집 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("장기간 일봉 데이터 수집 실패: " + e.getMessage());
        }
    }

    @GetMapping("/long-term-minute")
    public ResponseEntity<?> fetchLongTermMinuteStockData(
            @RequestParam(defaultValue = "J") String marketCode,
            @RequestParam String stockCode,
            @RequestParam String startDate, // 20240722
            @RequestParam String endDate, // 20240726
            @RequestParam(defaultValue = "Y") String includePastData) { // 실제로는 FID_PW_DATA_INCU_YN

        log.info("장기간 분봉 데이터 수집 요청: {}, 기간: {} ~ {}", stockCode, startDate, endDate);

        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
            LocalDate start = LocalDate.parse(startDate, formatter);
            LocalDate end = LocalDate.parse(endDate, formatter);
            LocalDate current = start;

            int totalBatchCount = 0;
            int successCount = 0;
            int failCount = 0;

            // 2시간 단위로 분할
            String[] timeSlots = {
                "110000", // 09:00~11:00
                "130000", // 11:00~13:00
                "150000", // 13:00~15:00
                "153000" // 15:00~15:30
            };

            while (current.isBefore(end) || current.isEqual(end)) {
                String dateStr = current.format(formatter);

                // 주말 제외
                if (current.getDayOfWeek().getValue() >= 6) {
                    current = current.plusDays(1);
                    continue;
                }

                log.info("날짜별 분봉 수집: {}", dateStr);

                for (String startHour : timeSlots) {
                    totalBatchCount++;
                    log.info("분봉 배치 {}: {} {} (2시간치)", totalBatchCount, dateStr, startHour);

                    try {
                        minuteStockService.fetchAndSaveMinuteData(
                                marketCode, stockCode, dateStr, startHour, includePastData);
                        successCount++;
                        Thread.sleep(1000);

                    } catch (Exception batchException) {
                        failCount++;
                        log.error(
                                "분봉 배치 {} 실패: {} {} - {}",
                                totalBatchCount,
                                dateStr,
                                startHour,
                                batchException.getMessage());
                    }
                }

                current = current.plusDays(1);
            }

            String resultMessage =
                    String.format(
                            "장기간 분봉 데이터 수집 완료 - 총 배치: %d개, 성공: %d개, 실패: %d개",
                            totalBatchCount, successCount, failCount);

            return ResponseEntity.ok(resultMessage);

        } catch (Exception e) {
            log.error("장기간 분봉 데이터 수집 중 오류 발생", e);
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
        dailyStockService.fetchAndSaveDailyData(
                marketCode, stockCode, startDate, endDate, periodCode);

        return ResponseEntity.status(HttpStatus.CREATED).body("데일리 주식 저장 완료");
    }

    @GetMapping("/minute-data")
    public ResponseEntity<?> mStockData(
            @RequestParam String marketCode,
            @RequestParam String stockCode,
            @RequestParam String startHour,
            @RequestParam String date,
            @RequestParam String includePastData) {
        minuteStockService.fetchAndSaveMinuteData(
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
}
