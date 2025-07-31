package io.gaboja9.mockstock.domain.stock.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.WriteApi;
import com.influxdb.client.domain.WritePrecision;

import io.gaboja9.mockstock.domain.stock.measurement.DailyStockPrice;

import java.time.Instant;
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

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.influxdb.client.write.Point;
import java.time.ZoneId;

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

    private final HantuAuthService hantuAuthService;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    public DailyStockService(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            @Qualifier("dailyInfluxDBClient") InfluxDBClient dailyClient,
            HantuAuthService hantuAuthService) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.dailyClient = dailyClient;
        this.hantuAuthService = hantuAuthService;
    }

    // 단일 종목의 일별 주식 데이터를 가져와 InfluxDB에 저장합니다.
    public void fetchAndSaveDailyData(
            String marketCode,
            String stockCode,
            String startDate,
            String endDate,
            String periodCode) {

        log.info("단일 종목 데이터 수집 시작 - 종목: {}, 기간: {} ~ {}", stockCode, startDate, endDate);

        // 매번 새 토큰을 발급받는 대신, 유효한 토큰을 가져오는 메소드 호출
        String accessToken = hantuAuthService.getValidAccessToken();
        if (accessToken == null) {
            log.error("접근 토큰 발급 실패 - 데이터 수집 중단");
            return;
        }

        String responseBody =
                getStockPriceData(
                        marketCode, stockCode, startDate, endDate, periodCode, accessToken);

        if (responseBody != null) {
            saveStockDataToInflux(responseBody, stockCode, periodCode);
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

    private void saveStockDataToInflux(String responseBody, String stockCode, String periodCode) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode arr = root.path("output2");
            if (!arr.isArray() || arr.isEmpty()) {
                log.warn("데이터 없음 - 종목: {}, 응답: {}", stockCode, responseBody);
                return;
            }

            String measurement = measurementFrom(periodCode);
            List<Point> points = new ArrayList<>(arr.size());

            for (JsonNode n : arr) {
                String ds = n.path("stck_bsop_date").asText(null);
                if (ds == null) continue;

                // ✅ KST 자정 → UTC 변환 (한국장 날짜 보존)
                Instant ts = LocalDate.parse(ds, DATE_FMT).atStartOfDay(KST).toInstant();

                Point p = Point
                    .measurement(measurement)          // 런타임에 measurement 지정
                    .time(ts, WritePrecision.NS)       // ⬅️ Point에서는 time(...)으로 타임스탬프 설정
                    .addTag("stockCode", stockCode)
                    .addField("openPrice",  Long.parseLong(n.path("stck_oprc").asText("0")))
                    .addField("closePrice", Long.parseLong(n.path("stck_clpr").asText("0")))
                    .addField("maxPrice",   Long.parseLong(n.path("stck_hgpr").asText("0")))
                    .addField("minPrice",   Long.parseLong(n.path("stck_lwpr").asText("0")))
                    .addField("accumTrans", Long.parseLong(n.path("acml_vol").asText("0")));

                points.add(p);
            }

            // 최신→과거로 오는 경우 대비: 오래된 것부터 쓰기
            Collections.reverse(points);

            try (WriteApi wa = dailyClient.getWriteApi()) {
                wa.writePoints(points);
                log.info("Influx 저장 완료 - 종목: {}, measurement: {}, 건수: {}",
                    stockCode, measurement, points.size());
            }
        } catch (Exception e) {
            log.error("데이터 파싱/저장 오류 - 종목: {}, period: {}", stockCode, periodCode, e);
        }
    }

    private String measurementFrom(String periodCode) {
        if ("D".equalsIgnoreCase(periodCode)) return "stock_daily";
        if ("M".equalsIgnoreCase(periodCode)) return "stock_monthly";
        if ("W".equalsIgnoreCase(periodCode)) return "stock_weekly";
        return "stock_yearly";
    }
}
