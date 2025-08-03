package io.gaboja9.mockstock.domain.stock.service;


import com.influxdb.client.write.Point;
import io.gaboja9.mockstock.domain.stock.mapper.StocksDataMapper;
import io.gaboja9.mockstock.domain.stock.measurement.DailyStockPrice;
import io.gaboja9.mockstock.domain.stock.repository.StocksDailyRepository;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@Slf4j
public class StocksDataService {

  private final RestTemplate restTemplate;
  private final StocksDailyRepository repository;
  private final StocksDataMapper mapper;
  private final HantuAuthService hantuAuthService;

  @Value("${hantu-openapi.domain}")
  private String apiDomain;

  @Value("${hantu-openapi.appkey}")
  private String appKey;

  @Value("${hantu-openapi.appsecret}")
  private String appSecret;

  private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
  private static final int BATCH_DAYS = 99;
  private static final int API_DELAY_MS = 1000;

  public StocksDataService(
      RestTemplate restTemplate,
      StocksDailyRepository repository,
      StocksDataMapper mapper,
      HantuAuthService hantuAuthService) {
    this.restTemplate = restTemplate;
    this.repository = repository;
    this.mapper = mapper;
    this.hantuAuthService = hantuAuthService;
  }

  // 단일 종목의 주식 데이터를 가져와 InfluxDB에 저장합니다.
  public void fetchAndSaveData(
      String marketCode,
      String stockCode,
      String startDate,
      String endDate,
      String periodCode) {

    log.info("주식 데이터 수집 시작 - 종목: {}, 기간: {} ~ {}, 주기: {}",
        stockCode, startDate, endDate, periodCode);

    try {
      // 1. API 호출
      String responseBody = callHantuApi(marketCode, stockCode, startDate, endDate, periodCode);

      // 2. 매퍼로 Point 변환
      List<Point> points = mapper.mapToInfluxPoints(responseBody, stockCode, periodCode);

      // 3. 리포지토리로 Point 저장
      repository.savePoints(points);

      log.info("주식 데이터 수집 완료 - 종목: {}, 주기: {}, 건수: {}", stockCode, periodCode, points.size());

    } catch (Exception e) {
      log.error("주식 데이터 수집 실패 - 종목: {}", stockCode, e);
      throw new RuntimeException("주식 데이터 수집 실패: " + stockCode, e);
    }
  }

  private String callHantuApi(
      String marketCode,
      String stockCode,
      String startDate,
      String endDate,
      String periodCode) {

    String accessToken = hantuAuthService.getValidAccessToken();
    if (accessToken == null) {
      throw new RuntimeException("한투 API 토큰 발급 실패");
    }

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
    headers.set("authorization", "Bearer " + accessToken);
    headers.set("appkey", appKey);
    headers.set("appsecret", appSecret);
    headers.set("tr_id", "FHKST03010100");

    String url = UriComponentsBuilder.fromHttpUrl(
            apiDomain + "/uapi/domestic-stock/v1/quotations/inquire-daily-itemchartprice")
        .queryParam("FID_COND_MRKT_DIV_CODE", marketCode)
        .queryParam("FID_INPUT_ISCD", stockCode)
        .queryParam("FID_INPUT_DATE_1", startDate)
        .queryParam("FID_INPUT_DATE_2", endDate)
        .queryParam("FID_PERIOD_DIV_CODE", periodCode)
        .queryParam("FID_ORG_ADJ_PRC", "0")
        .toUriString();

    try {
      ResponseEntity<String> response = restTemplate.exchange(
          url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
      return response.getBody();
    } catch (Exception e) {
      log.error("API 요청 실패 - 종목: {}, URL: {}", stockCode, url, e);
      throw new RuntimeException("API 호출 실패", e);
    }
  }

  public void fetchAndSaveLongTermData(
      String marketCode,
      String stockCode,
      String startDate,
      String endDate,
      String periodCode) throws InterruptedException {

    log.info("장기간 {}봉 데이터 수집 시작: {}, 기간: {} ~ {}",
        periodCode, stockCode, startDate, endDate);

    LocalDate start = LocalDate.parse(startDate, DATE_FMT);
    LocalDate end = LocalDate.parse(endDate, DATE_FMT);
    LocalDate current = start;

    int batchCount = 0;
    int successCount = 0;
    int failCount = 0;

    while (current.isBefore(end) || current.isEqual(end)) {
      LocalDate batchEnd = current.plusDays(BATCH_DAYS);
      if (batchEnd.isAfter(end)) {
        batchEnd = end;
      }

      batchCount++;
      log.info("배치 {}: {} ~ {} (종목: {}, 주기: {})", batchCount, current, batchEnd, stockCode, periodCode);

      try {
        String batchStartStr = current.format(DATE_FMT);
        String batchEndStr = batchEnd.format(DATE_FMT);

        fetchAndSaveData(marketCode, stockCode, batchStartStr, batchEndStr, periodCode);
        successCount++;

      } catch (Exception batchException) {
        failCount++;
        log.error("배치 {} 실패 (종목: {}): {}", batchCount, stockCode, batchException.getMessage());
      }

      current = batchEnd.plusDays(1);

      if (current.isBefore(end) || current.isEqual(end)) {
        Thread.sleep(API_DELAY_MS);
      }
    }

    log.info("장기간 {}봉 데이터 수집 완료 - 종목: {}, 총 배치: {}개, 성공: {}개, 실패: {}개",
        periodCode, stockCode, batchCount, successCount, failCount);
  }
}