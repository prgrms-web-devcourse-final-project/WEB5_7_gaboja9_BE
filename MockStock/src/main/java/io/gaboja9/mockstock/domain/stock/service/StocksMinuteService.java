package io.gaboja9.mockstock.domain.stock.service;

import io.gaboja9.mockstock.domain.stock.mapper.StocksMinuteMapper;
import io.gaboja9.mockstock.domain.stock.measurement.MinuteStockPrice;
import io.gaboja9.mockstock.domain.stock.repository.StocksMinuteRepository;
import java.time.DayOfWeek;
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
public class StocksMinuteService {

  private final RestTemplate restTemplate;
  private final StocksMinuteRepository repository;
  private final StocksMinuteMapper mapper;
  private final HantuAuthService hantuAuthService;

  @Value("${hantu-openapi.domain}")
  private String apiDomain;

  @Value("${hantu-openapi.appkey}")
  private String appKey;

  @Value("${hantu-openapi.appsecret}")
  private String appSecret;

  private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
  private static final String[] TIME_SLOTS = {"110000", "130000", "150000", "153000"};
  private static final int API_DELAY_MS = 1000;

  public StocksMinuteService(
      RestTemplate restTemplate,
      StocksMinuteRepository repository,
      StocksMinuteMapper mapper,
      HantuAuthService hantuAuthService) {
    this.restTemplate = restTemplate;
    this.repository = repository;
    this.mapper = mapper;
    this.hantuAuthService = hantuAuthService;
  }

  // 단일 종목의 분봉 데이터를 가져와 InfluxDB에 저장합니다.
  public void fetchAndSaveMinuteData(
      String marketCode,
      String stockCode,
      String date,
      String startHour,
      String includePastData) {

    log.info("분봉 데이터 수집 시작 - 종목: {}, 날짜: {}, 시간: {}",
        stockCode, date, startHour);

    try {
      // 1. API 호출
      String responseBody = callHantuMinuteApi(marketCode, stockCode, date, startHour,
          includePastData);

      // 2. 매퍼로 변환
      List<MinuteStockPrice> prices = mapper.mapToMinuteStockPrices(responseBody, stockCode);

      // 3. 기존 리포지토리로 저장
      repository.save(prices);

      log.info("분봉 데이터 수집 완료 - 종목: {}, 건수: {}", stockCode, prices.size());

    } catch (Exception e) {
      log.error("분봉 데이터 수집 실패 - 종목: {}", stockCode, e);
      throw new RuntimeException("분봉 데이터 수집 실패: " + stockCode, e);
    }
  }

  private String callHantuMinuteApi(
      String marketCode,
      String stockCode,
      String date,
      String time,
      String includePastData) {

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
    headers.set("tr_id", "FHKST03010230");
    headers.set("custtype", "P");

    String url = UriComponentsBuilder.fromHttpUrl(
            apiDomain + "/uapi/domestic-stock/v1/quotations/inquire-time-dailychartprice")
        .queryParam("FID_COND_MRKT_DIV_CODE", marketCode)
        .queryParam("FID_INPUT_ISCD", stockCode)
        .queryParam("FID_INPUT_DATE_1", date)
        .queryParam("FID_INPUT_HOUR_1", time)
        .queryParam("FID_PW_DATA_INCU_YN", includePastData)
        .queryParam("FID_FAKE_TICK_INCU_YN", "")
        .toUriString();

    try {
      ResponseEntity<String> response = restTemplate.exchange(
          url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
      return response.getBody();
    } catch (Exception e) {
      log.error("분봉 API 요청 실패 - 종목: {}, URL: {}", stockCode, url, e);
      throw new RuntimeException("분봉 API 호출 실패", e);
    }
  }

  public void fetchAndSaveLongTermMinuteData(
      String marketCode,
      String stockCode,
      String startDate,
      String endDate,
      String includePastData) throws InterruptedException {

    log.info("장기간 분봉 데이터 수집 시작: {}, 기간: {} ~ {}", stockCode, startDate, endDate);

    LocalDate start = LocalDate.parse(startDate, DATE_FMT);
    LocalDate end = LocalDate.parse(endDate, DATE_FMT);
    LocalDate current = start;

    int dayCount = 0;
    int successCount = 0;
    int failCount = 0;

    while (current.isBefore(end) || current.isEqual(end)) {
      // 주말 제외
      if (current.getDayOfWeek() == DayOfWeek.SATURDAY ||
          current.getDayOfWeek() == DayOfWeek.SUNDAY) {
        current = current.plusDays(1);
        continue;
      }

      dayCount++;
      String dateStr = current.format(DATE_FMT);
      log.info("날짜별 분봉 수집: {} ({})", stockCode, dateStr);

      int daySuccessCount = 0;
      int dayFailCount = 0;

      for (String startHour : TIME_SLOTS) {
        try {
          fetchAndSaveMinuteData(marketCode, stockCode, dateStr, startHour,
              includePastData);
          daySuccessCount++;
          Thread.sleep(API_DELAY_MS);

        } catch (Exception batchException) {
          dayFailCount++;
          log.error("분봉 배치 실패: {} {} {} - {}",
              stockCode, dateStr, startHour, batchException.getMessage());
        }
      }

      if (daySuccessCount > 0) {
        successCount++;
      }
      if (dayFailCount > 0) {
        failCount++;
      }

      current = current.plusDays(1);
    }

    log.info("장기간 분봉 데이터 수집 완료: {}, 총 일수: {}일, 성공: {}일, 실패: {}일",
        stockCode, dayCount, successCount, failCount);
  }
}
