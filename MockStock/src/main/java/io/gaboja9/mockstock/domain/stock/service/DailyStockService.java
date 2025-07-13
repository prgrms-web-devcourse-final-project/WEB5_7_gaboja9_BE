package io.gaboja9.mockstock.domain.stock.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.WriteApi;
import com.influxdb.client.domain.WritePrecision;

import io.gaboja9.mockstock.domain.stock.dto.HantuTokenResponse;
import io.gaboja9.mockstock.domain.stock.measurement.DailyStockPrice;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class DailyStockService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final InfluxDBClient dailyClient;

    @Value("${hantu-openapi.domain}")
    private String apiDomain;

    @Value("${hantu-openapi.appkey}")
    private String appKey;

    @Value("${hantu-openapi.appsecret}")
    private String appSecret;

    private String cachedAccessToken;
    private long tokenExpirationTime; // 토큰 만료 시간을 밀리초 단위로 저장

    public DailyStockService(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            @Qualifier("dailyInfluxDBClient") InfluxDBClient dailyClient) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.dailyClient = dailyClient;
    }

    /** 단일 종목의 일별 주식 데이터를 가져와 InfluxDB에 저장합니다. */
    public void fetchAndSaveDailyData(
            String marketCode,
            String stockCode,
            String startDate,
            String endDate,
            String periodCode) {

        log.info("단일 종목 데이터 수집 시작 - 종목: {}, 기간: {} ~ {}", stockCode, startDate, endDate);

        // 매번 새 토큰을 발급받는 대신, 유효한 토큰을 가져오는 메소드 호출
        String accessToken = getValidAccessToken();
        if (accessToken == null) {
            log.error("접근 토큰 발급 실패 - 데이터 수집 중단");
            return;
        }

        String responseBody =
                getStockPriceData(
                        marketCode, stockCode, startDate, endDate, periodCode, accessToken);

        if (responseBody != null) {
            saveDailyStockDataToInflux(responseBody, stockCode);
        }

        log.info("단일 종목 데이터 수집 완료 - {}", stockCode);
    }

    private String getStockPriceData(
            String marketCode,
            String stockCode,
            String startDate,
            String endDate,
            String periodCode,
            String accessToken) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("authorization", "Bearer " + accessToken);
        headers.set("appkey", appKey);
        headers.set("appsecret", appSecret);
        headers.set("tr_id", "FHKST03010100");

        UriComponentsBuilder builder =
                UriComponentsBuilder.fromHttpUrl(
                                apiDomain
                                        + "/uapi/domestic-stock/v1/quotations/inquire-daily-itemchartprice")
                        .queryParam("FID_COND_MRKT_DIV_CODE", marketCode)
                        .queryParam("FID_INPUT_ISCD", stockCode)
                        .queryParam("FID_INPUT_DATE_1", startDate)
                        .queryParam("FID_INPUT_DATE_2", endDate)
                        .queryParam("FID_PERIOD_DIV_CODE", periodCode)
                        .queryParam("FID_ORG_ADJ_PRC", "0");

        HttpEntity<?> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response =
                    restTemplate.exchange(
                            builder.toUriString(), HttpMethod.GET, entity, String.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("API 요청 실패 - 종목: {}, URL: {}", stockCode, builder.toUriString(), e);
            return null;
        }
    }

    private void saveDailyStockDataToInflux(String responseBody, String stockCode) {
        try {
            JsonNode rootNode = objectMapper.readTree(responseBody);
            JsonNode output2 = rootNode.path("output2");

            if (output2.isMissingNode() || !output2.isArray()) {
                log.warn("{} 종목에 대한 데이터 없음. 응답: {}", stockCode, responseBody);
                return;
            }

            List<DailyStockPrice> pricePoints = new ArrayList<>();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

            for (JsonNode node : output2) {
                DailyStockPrice point = new DailyStockPrice();
                LocalDate date = LocalDate.parse(node.path("stck_bsop_date").asText(), formatter);
                point.setTimestamp(date.atTime(0, 0).toInstant(ZoneOffset.UTC));
                point.setStockCode(stockCode);
                point.setOpenPrice(Long.parseLong(node.path("stck_oprc").asText()));
                point.setClosePrice(Long.parseLong(node.path("stck_clpr").asText()));
                point.setMaxPrice(Long.parseLong(node.path("stck_hgpr").asText()));
                point.setMinPrice(Long.parseLong(node.path("stck_lwpr").asText()));
                point.setAccumTrans(Long.parseLong(node.path("acml_vol").asText()));
                pricePoints.add(point);
            }

            if (!pricePoints.isEmpty()) {
                try (WriteApi writeApi = dailyClient.getWriteApi()) {
                    writeApi.writeMeasurements(WritePrecision.NS, pricePoints);
                    log.info("{} 종목의 일별 데이터 {}건 저장 완료", stockCode, pricePoints.size());
                }
            }
        } catch (Exception e) {
            log.error("{} 종목 데이터 파싱 또는 InfluxDB 저장 중 에러 발생", stockCode, e);
        }
    }

    /**
     * 유효하고 만료되지 않은 토큰이 있는지 확인하고 반환합니다. 없거나 만료되었다면 새로 발급받습니다. 이 메소드는 동기화(thread-safe) 처리됩니다.
     *
     * @return 유효한 액세스 토큰, 실패 시 null
     */
    private String getValidAccessToken() {
        // 1. 첫 번째 검사 (Lock 없이)
        // 토큰이 유효한 경우, 불필요한 동기화 없이 바로 반환하여 성능 향상
        if (cachedAccessToken != null && System.currentTimeMillis() < tokenExpirationTime - 60000) {
            return cachedAccessToken;
        }
        // 2. 동기화 블록 진입
        // 토큰이 없거나 만료된 경우에만 동기화 블록에 진입
        synchronized (this) {
            // 3. 두 번째 검사 (Lock 상태에서)
            // 다른 스레드가 이미 토큰을 갱신했을 수 있으므로 다시 한번 확인
            if (cachedAccessToken == null
                    || System.currentTimeMillis() >= tokenExpirationTime - 60000) {
                log.info("액세스 토큰이 없거나 만료되어 새로 발급합니다.");
                if (!fetchNewAccessToken()) {
                    return null; // 토큰 발급 실패
                }
            }
        }
        return cachedAccessToken;
    }

    /**
     * API로부터 새로운 액세스 토큰을 발급받아 캐시합니다.
     *
     * @return 토큰 발급 및 캐시 성공 시 true, 실패 시 false
     */
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
                // 'expires_in'은 초 단위이므로, 현재 시간에 더해 만료 시각(밀리초)을 계산
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
            // 실패 시 기존 토큰 정보 초기화
            this.cachedAccessToken = null;
            this.tokenExpirationTime = 0;
            return false;
        }
    }
}
