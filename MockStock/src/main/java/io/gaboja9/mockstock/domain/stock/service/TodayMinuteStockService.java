package io.gaboja9.mockstock.domain.stock.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.WriteApi;
import com.influxdb.client.domain.WritePrecision;

import io.gaboja9.mockstock.domain.stock.dto.HantuTokenResponse;
import io.gaboja9.mockstock.domain.stock.measurement.MinuteStockPrice;

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

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class TodayMinuteStockService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final InfluxDBClient minuteClient;

    @Value("${hantu-openapi.domain}")
    private String apiDomain;

    @Value("${hantu-openapi.appkey}")
    private String appKey;

    @Value("${hantu-openapi.appsecret}")
    private String appSecret;

    private String cachedAccessToken;
    private long tokenExpirationTime;

    public TodayMinuteStockService(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            @Qualifier("minuteInfluxDBClient") InfluxDBClient minuteClient) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.minuteClient = minuteClient;
    }

    /** [수정됨] 단일 종목의 '당일' 분봉 데이터를 가져와 InfluxDB에 저장합니다. */
    public void fetchAndSaveCurrentDayMinuteData(
            String marketCode,
            String stockCode,
            String startTime,
            String periodCode,
            String ClsCode) {
        log.info(
                "당일 분봉 데이터 수집 시작 - 시장: {}, 종목: {}, 시작시간: {}, 기간분류: {}, 체결분류: {}",
                marketCode,
                stockCode,
                startTime,
                periodCode,
                ClsCode);

        String accessToken = getValidAccessToken();
        if (accessToken == null) {
            log.error("접근 토큰 발급 실패 - 데이터 수집 중단 (종목: {})", stockCode);
            return;
        }

        // [수정됨] 새로운 파라미터를 받는 메소드 호출
        String responseBody =
                getCurrentDayStockMinuteData(
                        marketCode, stockCode, startTime, periodCode, ClsCode, accessToken);

        if (responseBody != null) {
            saveMinuteStockDataToInflux(responseBody, stockCode);
        }

        log.info("당일 분봉 데이터 수집 완료 - {}", stockCode);
    }

    private String getCurrentDayStockMinuteData(
            String marketCode,
            String stockCode,
            String startTime,
            String periodCode,
            String clsCode,
            String accessToken) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("authorization", "Bearer " + accessToken);
        headers.set("appkey", appKey);
        headers.set("appsecret", appSecret);
        headers.set("tr_id", "FHKST03010200");
        headers.set("custtype", "P");

        UriComponentsBuilder builder =
                UriComponentsBuilder.fromHttpUrl(
                                // [수정됨] 당일 분봉 조회 API 경로로 변경
                                apiDomain
                                        + "/uapi/domestic-stock/v1/quotations/inquire-time-itemchartprice")
                        .queryParam("FID_COND_MRKT_DIV_CODE", marketCode)
                        .queryParam("FID_INPUT_ISCD", stockCode)
                        // [수정됨] 요청 파라미터 변경
                        .queryParam("FID_INPUT_HOUR_1", startTime) // 시작 시간
                        .queryParam("FID_PW_DATA_INCU_YN", periodCode)
                        .queryParam("FID_ETC_CLS_CODE", clsCode);

        HttpEntity<?> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response =
                    restTemplate.exchange(
                            builder.toUriString(), HttpMethod.GET, entity, String.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("당일 분봉 데이터 API 요청 실패 - 종목: {}, URL: {}", stockCode, builder.toUriString(), e);
            return null;
        }
    }

    // 분봉 데이터 저장 로직 (기존과 동일)
    private void saveMinuteStockDataToInflux(String responseBody, String stockCode) {
        try {
            log.info("API 응답 원본 (종목코드: {}): {}", stockCode, responseBody);

            JsonNode rootNode = objectMapper.readTree(responseBody);
            JsonNode output2 = rootNode.path("output2");

            if (output2.isMissingNode() || !output2.isArray()) {
                log.warn("{} 종목에 대한 분봉 데이터 없음. 응답: {}", stockCode, responseBody);
                return;
            }

            List<MinuteStockPrice> pricePoints = new ArrayList<>();
            // 당일 데이터는 날짜가 포함되지 않을 수 있으므로, 응답 형식을 확인해야 합니다.
            // 여기서는 기존과 같이 'stck_bsop_date' 필드가 있다고 가정합니다.
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
            ZoneId koreaZone = ZoneId.of("Asia/Seoul");

            for (JsonNode node : output2) {
                MinuteStockPrice point = new MinuteStockPrice();

                String dateStr = node.path("stck_bsop_date").asText();
                String timeStr = node.path("stck_cntg_hour").asText();
                LocalDateTime dateTime = LocalDateTime.parse(dateStr + timeStr, formatter);

                ZonedDateTime zonedDateTime = dateTime.atZone(koreaZone);
                point.setTimestamp(zonedDateTime.toInstant());

                point.setStockCode(stockCode);
                point.setOpenPrice(Long.parseLong(node.path("stck_oprc").asText()));
                point.setMaxPrice(Long.parseLong(node.path("stck_hgpr").asText()));
                point.setMinPrice(Long.parseLong(node.path("stck_lwpr").asText()));
                point.setClosePrice(Long.parseLong(node.path("stck_prpr").asText()));
                point.setAccumTrans(Long.parseLong(node.path("cntg_vol").asText()));

                pricePoints.add(point);
            }

            // [추가된 부분] InfluxDB에 쓰기 직전, 리스트의 모든 타임스탬프를 로그로 출력
            log.info("InfluxDB 저장 전 데이터 확인 (총 {}건)", pricePoints.size());
            for (MinuteStockPrice p : pricePoints) {
                log.info(" -> Timestamp: {}, ClosePrice: {}", p.getTimestamp(), p.getClosePrice());
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

    // 토큰 발급 관련 메소드 (기존과 동일)
    private synchronized String getValidAccessToken() {
        if (cachedAccessToken != null && System.currentTimeMillis() < tokenExpirationTime - 60000) {
            return cachedAccessToken;
        }
        synchronized (this) {
            if (cachedAccessToken == null
                    || System.currentTimeMillis() >= tokenExpirationTime - 60000) {
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
        String requestBody =
                String.format(
                        "{\"grant_type\":\"client_credentials\",\"appkey\":\"%s\",\"appsecret\":\"%s\"}",
                        appKey, appSecret);
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
        try {
            ResponseEntity<HantuTokenResponse> response =
                    restTemplate.exchange(
                            apiDomain + "/oauth2/tokenP",
                            HttpMethod.POST,
                            entity,
                            HantuTokenResponse.class);
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
