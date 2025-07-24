package io.gaboja9.mockstock.domain.stock.measurement;

import com.influxdb.annotations.Column;
import com.influxdb.annotations.Measurement;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;

import java.time.Instant;

@Schema(description = "일봉 주식 가격 데이터", title = "DailyStockPrice")
@Data
@Measurement(name = "stock_daily")
public class DailyStockPrice {

    @Schema(
            description = "데이터 시점 (일봉의 경우 해당 날짜의 자정)",
            example = "2025-07-23T00:00:00Z",
            type = "string",
            format = "date-time")
    @Column(timestamp = true)
    private Instant timestamp;

    @Schema(description = "주식 코드 (6자리 숫자)", example = "005930", pattern = "^\\d{6}$")
    @Column(tag = true)
    private String stockCode;

    @Schema(description = "최고가 (해당 일의 가장 높은 거래 가격)", example = "62400", minimum = "0")
    @Column
    private Long maxPrice;

    @Schema(description = "최저가 (해당 일의 가장 낮은 거래 가격)", example = "61000", minimum = "0")
    @Column
    private Long minPrice;

    @Schema(description = "누적 거래량 (해당 일의 총 거래된 주식 수)", example = "20213724", minimum = "0")
    @Column
    private Long accumTrans;

    @Schema(description = "시가 (해당 일의 첫 거래 가격)", example = "61600", minimum = "0")
    @Column
    private Long openPrice;

    @Schema(description = "종가 (해당 일의 마지막 거래 가격)", example = "61400", minimum = "0")
    @Column
    private Long closePrice;
}
