package io.gaboja9.mockstock.domain.stock.measurement;

import com.influxdb.annotations.Column;
import com.influxdb.annotations.Measurement;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lombok.Data;

@Schema(description = "분봉 주식 가격 데이터", title = "MinuteStockPrice")
@Data
@Measurement(name = "stock_minute")
public class MinuteStockPrice {

  @Schema(
      description = "데이터 시점 (분봉의 경우 해당 분의 시작 시간)",
      example = "2025-07-23T13:30:00Z",
      type = "string",
      format = "date-time"
  )
  @Column(timestamp = true)
  private Instant timestamp;

  @Schema(
      description = "주식 코드 (6자리 숫자)",
      example = "005930",
      pattern = "^\\d{6}$"
  )
  @Column(tag = true)
  private String stockCode;

  @Schema(
      description = "최고가 (해당 분의 가장 높은 거래 가격)",
      example = "61500",
      minimum = "0"
  )
  @Column
  private Long maxPrice;

  @Schema(
      description = "최저가 (해당 분의 가장 낮은 거래 가격)",
      example = "61300",
      minimum = "0"
  )
  @Column
  private Long minPrice;

  @Schema(
      description = "누적 거래량 (해당 분의 총 거래된 주식 수)",
      example = "120000",
      minimum = "0"
  )
  @Column
  private Long accumTrans;

  @Schema(
      description = "시가 (해당 분의 첫 거래 가격)",
      example = "61400",
      minimum = "0"
  )
  @Column
  private Long openPrice;

  @Schema(
      description = "종가 (해당 분의 마지막 거래 가격)",
      example = "61450",
      minimum = "0"
  )
  @Column
  private Long closePrice;
}