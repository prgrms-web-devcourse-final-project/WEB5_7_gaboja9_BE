package io.gaboja9.mockstock.domain.stock.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gaboja9.mockstock.domain.stock.measurement.MinuteStockPrice;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class StocksMinuteMapper {

  private final ObjectMapper objectMapper;
  private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern(
      "yyyyMMddHHmmss");
  private static final ZoneId KST = ZoneId.of("Asia/Seoul");


  public List<MinuteStockPrice> mapToMinuteStockPrices(String responseBody, String stockCode) {
    try {
      JsonNode rootNode = objectMapper.readTree(responseBody);
      JsonNode output2 = rootNode.path("output2");

      if (output2.isMissingNode() || !output2.isArray()) {
        log.warn("응답에 분봉 데이터 없음 - 종목: {}", stockCode);
        return Collections.emptyList();
      }

      List<MinuteStockPrice> prices = new ArrayList<>();

      for (JsonNode node : output2) {
        String dateStr = node.path("stck_bsop_date").asText();
        String timeStr = node.path("stck_cntg_hour").asText();

        if (dateStr == null || timeStr == null) {
          continue;
        }

        MinuteStockPrice price = createMinuteStockPrice(node, stockCode, dateStr, timeStr);
        prices.add(price);
      }

      // 시간순 정렬 (오래된 것부터)
      prices.sort(Comparator.comparing(MinuteStockPrice::getTimestamp));

      return prices;

    } catch (Exception e) {
      log.error("분봉 JSON 파싱 실패 - 종목: {}", stockCode, e);
      throw new RuntimeException("분봉 데이터 변환 실패", e);
    }
  }

  private MinuteStockPrice createMinuteStockPrice(JsonNode node, String stockCode, String dateStr,
      String timeStr) {
    MinuteStockPrice price = new MinuteStockPrice();

    // KST 시간 → UTC 변환
    LocalDateTime dateTime = LocalDateTime.parse(dateStr + timeStr, DATETIME_FMT);
    ZonedDateTime zonedDateTime = dateTime.atZone(KST);

    price.setTimestamp(zonedDateTime.toInstant());
    price.setStockCode(stockCode);
    price.setOpenPrice(parseLongSafely(node.path("stck_oprc").asText()));
    price.setMaxPrice(parseLongSafely(node.path("stck_hgpr").asText()));
    price.setMinPrice(parseLongSafely(node.path("stck_lwpr").asText()));
    price.setClosePrice(parseLongSafely(node.path("stck_prpr").asText()));
    price.setAccumTrans(parseLongSafely(node.path("cntg_vol").asText()));

    return price;
  }

  private Long parseLongSafely(String value) {
    try {
      return value != null && !value.isEmpty() ? Long.parseLong(value) : 0L;
    } catch (NumberFormatException e) {
      log.warn("분봉 숫자 파싱 실패: {}", value);
      return 0L;
    }
  }
}