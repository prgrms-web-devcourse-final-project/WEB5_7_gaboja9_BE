package io.gaboja9.mockstock.domain.stock.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.WriteApi;
import com.influxdb.client.domain.WritePrecision;
import io.gaboja9.mockstock.domain.stock.dto.HantuTokenResponse;
import io.gaboja9.mockstock.domain.stock.measurement.MinuteStockPrice;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
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
public class MinuteStockService {

  private final RestTemplate restTemplate;
  private final ObjectMapper objectMapper;
  private final InfluxDBClient minuteClient; // 분봉용 InfluxDBClient (설정에 따라 동일할 수 있음)

  @Value("${hantu-openapi.domain}")
  private String apiDomain;

  @Value("${hantu-openapi.appkey}")
  private String appKey;

  @Value("${hantu-openapi.appsecret}")
  private String appSecret;

  private String cachedAccessToken;
  private long tokenExpirationTime;

  public MinuteStockService(
      RestTemplate restTemplate,
      ObjectMapper objectMapper,
      @Qualifier("minuteInfluxDBClient") InfluxDBClient minuteClient) { // 의존성 주입 수정
    this.restTemplate = restTemplate;
    this.objectMapper = objectMapper;
    this.minuteClient = minuteClient;
  }

  /**
   * 단일 종목의 분봉 데이터를 가져와 InfluxDB에 저장합니다.
   */
  public void fetchAndSaveMinuteData(String marketCode, String stockCode, String date,
      String startHour, String periodCode) {
    log.info("단일 종목 분봉 데이터 수집 시작 - 시장: {}, 종목: {}, 날짜: {}, 시간: {}", marketCode, stockCode, date,
        startHour);

    String accessToken = getValidAccessToken();
    if (accessToken == null) {
      log.error("접근 토큰 발급 실패 - 데이터 수집 중단 (종목: {})", stockCode);
      return;
    }

    String responseBody = getStockMinuteData(marketCode, stockCode, date, startHour, accessToken);

    if (responseBody != null) {
      saveMinuteStockDataToInflux(responseBody, stockCode);
    }

    log.info("단일 종목 분봉 데이터 수집 완료 - {}", stockCode);
  }

  private String getStockMinuteData(
      String marketCode,
      String stockCode,
      String date,
      String time,
      String accessToken) {

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
    headers.set("authorization", "Bearer " + accessToken);
    headers.set("appkey", appKey);
    headers.set("appsecret", appSecret);
    // 분봉 조회용 tr_id
    headers.set("tr_id", "FHKST03010230");
    headers.set("custtype", "P");

    UriComponentsBuilder builder =
        UriComponentsBuilder.fromHttpUrl(
                // 분봉 조회 API 경로
                apiDomain + "/uapi/domestic-stock/v1/quotations/inquire-time-dailychartprice")
            // [수정] marketCode 파라미터 사용
            .queryParam("FID_COND_MRKT_DIV_CODE", marketCode)
            .queryParam("FID_INPUT_ISCD", stockCode)
            // [추가] 명세에 따라 날짜 파라미터 추가
            .queryParam("FID_INPUT_DATE_1", date)
            // [수정] time 파라미터 사용
            .queryParam("FID_INPUT_HOUR_1", time)
            // 과거 데이터 포함 여부. "N"으로 설정 시 해당 시간에 데이터가 없으면 미포함
            .queryParam("FID_PW_DATA_INCU_YN", "Y")
            .queryParam("FID_FAKE_TICK_INCU_YN", ""); // 명세에 따라 "공백" 값을 전송

    HttpEntity<?> entity = new HttpEntity<>(headers);

    try {
      ResponseEntity<String> response =
          restTemplate.exchange(builder.toUriString(), HttpMethod.GET, entity, String.class);
      return response.getBody();
    } catch (Exception e) {
      log.error("분봉 데이터 API 요청 실패 - 종목: {}, URL: {}", stockCode, builder.toUriString(), e);
      return null;
    }
  }

  //  분봉 데이터 저장 로직
  private void saveMinuteStockDataToInflux(String responseBody, String stockCode) {
    try {
      JsonNode rootNode = objectMapper.readTree(responseBody);
      JsonNode output2 = rootNode.path("output2");

      if (output2.isMissingNode() || !output2.isArray()) {
        log.warn("{} 종목에 대한 분봉 데이터 없음. 응답: {}", stockCode, responseBody);
        return;
      }

      List<MinuteStockPrice> pricePoints = new ArrayList<>();
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
      ZoneId koreaZone = ZoneId.of("Asia/Seoul");

      for (JsonNode node : output2) {
        MinuteStockPrice point = new MinuteStockPrice();

        String dateStr = node.path("stck_bsop_date").asText();
        String timeStr = node.path("stck_cntg_hour").asText();
        LocalDateTime dateTime = LocalDateTime.parse(dateStr + timeStr, formatter);

        // 1. API가 준 KST 시간을 기준으로 ZonedDateTime 객체를 만듭니다.
        ZonedDateTime zonedDateTime = dateTime.atZone(koreaZone);

        // 2. 정확하게 변환된 시간(Instant)을 최종적으로 설정합니다.
        point.setTimestamp(zonedDateTime.toInstant());

        // --- 이하 데이터 저장 코드는 동일합니다 ---
        point.setStockCode(stockCode);
        point.setOpenPrice(Long.parseLong(node.path("stck_oprc").asText()));
        point.setMaxPrice(Long.parseLong(node.path("stck_hgpr").asText()));
        point.setMinPrice(Long.parseLong(node.path("stck_lwpr").asText()));
        point.setClosePrice(Long.parseLong(node.path("stck_prpr").asText()));
        point.setAccumTrans(Long.parseLong(node.path("cntg_vol").asText()));

        pricePoints.add(point);
      }

      if (!pricePoints.isEmpty()) {
        try (WriteApi writeApi = minuteClient.getWriteApi()) {
          writeApi.writeMeasurements(WritePrecision.NS, pricePoints);
          log.info("{} 종목의 분봉 데이터 {}건 저장 완료", stockCode, pricePoints.size());
        }
      }
    } catch (Exception e) {
      log.error("{} 종목 분봉 데이터 파싱 또는 InfluxDB 저장 중 에러 발생", stockCode, e);
    }
  }
  // ---------------------------------------------------------------------------------
  // 아래 토큰 발급 관련 메소드(getValidAccessToken, fetchNewAccessToken)는 DailyStockService와 동일합니다.
  // 편의를 위해 포함시켰으며, 실제 프로젝트에서는 별도의 공통 서비스로 분리하는 것이 더 효율적일 수 있습니다.
  // ---------------------------------------------------------------------------------

  private synchronized String getValidAccessToken() {
    if (cachedAccessToken != null && System.currentTimeMillis() < tokenExpirationTime - 60000) {
      return cachedAccessToken;
    }
    synchronized (this) {
      if (cachedAccessToken == null || System.currentTimeMillis() >= tokenExpirationTime - 60000) {
        log.info("액세스 토큰이 없거나 만료되어 새로 발급합니다.");
        if (!fetchNewAccessToken()) {
          return null;
        }
      }
    }
    return cachedAccessToken;
  }

  private boolean fetchNewAccessToken() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    String requestBody = String.format(
        "{\"grant_type\":\"client_credentials\",\"appkey\":\"%s\",\"appsecret\":\"%s\"}", appKey,
        appSecret);
    HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
    try {
      ResponseEntity<HantuTokenResponse> response = restTemplate.exchange(
          apiDomain + "/oauth2/tokenP", HttpMethod.POST, entity, HantuTokenResponse.class);
      HantuTokenResponse tokenResponse = response.getBody();
      if (tokenResponse != null && tokenResponse.getAccessToken() != null) {
        this.cachedAccessToken = tokenResponse.getAccessToken();
        long expiresInMillis = (long) tokenResponse.getExpiresIn() * 1000;
        this.tokenExpirationTime = System.currentTimeMillis() + expiresInMillis;
        log.info("새로운 액세스 토큰을 발급했습니다. 만료까지 남은 시간: {}초", tokenResponse.getExpiresIn());
        return true;
      } else {
        log.error("액세스 토큰 응답 파싱 실패. 응답: {}", response.getBody());
        return false;
      }
    } catch (Exception e) {
      log.error("Access Token 발급 요청 실패", e);
      this.cachedAccessToken = null;
      this.tokenExpirationTime = 0;
      return false;
    }
  }
}