package io.gaboja9.mockstock.domain.stock.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import io.gaboja9.mockstock.domain.stock.measurement.DailyStockPrice;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class StocksDataMapper {

  private final ObjectMapper objectMapper;
  private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
  private static final ZoneId KST = ZoneId.of("Asia/Seoul");

  public StocksDataMapper(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  // Point 객체 리스트로 반환 (저장용)
  public List<Point> mapToInfluxPoints(String responseBody, String stockCode, String periodCode) {
    try {
      JsonNode root = objectMapper.readTree(responseBody);
      JsonNode arr = root.path("output2");

      if (!arr.isArray() || arr.isEmpty()) {
        log.warn("응답에 데이터 없음 - 종목: {}", stockCode);
        return Collections.emptyList();
      }

      String measurement = measurementFrom(periodCode);  //  동적 measurement
      List<Point> points = new ArrayList<>();

      for (JsonNode node : arr) {
        String dateStr = node.path("stck_bsop_date").asText(null);
        if (dateStr == null)
          continue;

        Point point = createInfluxPoint(node, stockCode, dateStr, measurement);
        points.add(point);
      }

      return points;

    } catch (Exception e) {
      log.error("JSON 파싱 실패 - 종목: {}", stockCode, e);
      throw new RuntimeException("데이터 변환 실패", e);
    }
  }

  public List<DailyStockPrice> mapToStockPrices(String responseBody, String stockCode,
      String periodCode) {
    try {
      JsonNode root = objectMapper.readTree(responseBody);
      JsonNode arr = root.path("output2");

      if (!arr.isArray() || arr.isEmpty()) {
        log.warn("응답에 데이터 없음 - 종목: {}", stockCode);
        return Collections.emptyList();
      }

      List<DailyStockPrice> prices = new ArrayList<>();

      for (JsonNode node : arr) {
        String dateStr = node.path("stck_bsop_date").asText(null);
        if (dateStr == null)
          continue;

        DailyStockPrice price = createStockPrice(node, stockCode, dateStr);
        prices.add(price);
      }
      // 시간순 정렬 (오래된 것부터)
      prices.sort(Comparator.comparing(DailyStockPrice::getTimestamp));

      return prices;

    } catch (Exception e) {
      log.error("JSON 파싱 실패 - 종목: {}", stockCode, e);
      throw new RuntimeException("데이터 변환 실패", e);
    }
  }

  private Point createInfluxPoint(JsonNode node, String stockCode, String dateStr,
      String measurement) {
    // KST 자정 → UTC 변환
    Instant timestamp = LocalDate.parse(dateStr, DATE_FMT)
        .atStartOfDay(KST)
        .toInstant();

    return Point.measurement(measurement)
        .time(timestamp, WritePrecision.NS)
        .addTag("stockCode", stockCode)
        .addField("openPrice", parseLongSafely(node.path("stck_oprc").asText("0")))
        .addField("closePrice", parseLongSafely(node.path("stck_clpr").asText("0")))
        .addField("maxPrice", parseLongSafely(node.path("stck_hgpr").asText("0")))
        .addField("minPrice", parseLongSafely(node.path("stck_lwpr").asText("0")))
        .addField("accumTrans", parseLongSafely(node.path("acml_vol").asText("0")));
  }

  private DailyStockPrice createStockPrice(JsonNode node, String stockCode, String dateStr) {
    DailyStockPrice price = new DailyStockPrice();

    // KST 자정 → UTC 변환
    Instant timestamp = LocalDate.parse(dateStr, DATE_FMT)
        .atStartOfDay(KST)
        .toInstant();

    price.setTimestamp(timestamp);
    price.setStockCode(stockCode);
    price.setOpenPrice(parseLongSafely(node.path("stck_oprc").asText("0")));
    price.setClosePrice(parseLongSafely(node.path("stck_clpr").asText("0")));
    price.setMaxPrice(parseLongSafely(node.path("stck_hgpr").asText("0")));
    price.setMinPrice(parseLongSafely(node.path("stck_lwpr").asText("0")));
    price.setAccumTrans(parseLongSafely(node.path("acml_vol").asText("0")));

    return price;
  }

  private Long parseLongSafely(String value) {
    try {
      return value != null ? Long.parseLong(value) : 0L;
    } catch (NumberFormatException e) {
      log.warn("숫자 파싱 실패: {}", value);
      return 0L;
    }
  }

  private String measurementFrom(String periodCode) {
    if ("D".equalsIgnoreCase(periodCode))
      return "stock_daily";
    if ("M".equalsIgnoreCase(periodCode))
      return "stock_monthly";
    if ("W".equalsIgnoreCase(periodCode))
      return "stock_weekly";
    return "stock_yearly";
  }
}