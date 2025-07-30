package io.gaboja9.mockstock.global.websocket.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;

@Slf4j
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "분봉 캔들 데이터")
public class StocksCandleDto {

    @Schema(description = "종목 코드", example = "005930")
    private String stockCode;

    @Schema(description = "분봉 시간 (타임스탬프)", example = "1640995200000")
    private Long timestamp;

    @Schema(description = "시가", example = "75000")
    private Long open;

    @Schema(description = "고가", example = "75200")
    private Long high;

    @Schema(description = "저가", example = "74800")
    private Long low;

    @Schema(description = "종가", example = "75100")
    private Long close;

    @Schema(description = "거래량", example = "1523")
    private Long volume;

    @Schema(description = "틱 개수", example = "42")
    @Builder.Default
    private Integer tickCount = 1;

    /** 새로운 체결가로 분봉 업데이트 */
    public StocksCandleDto updateWith(long price, long newVolume) { // int → long 변경
        return StocksCandleDto.builder()
                .stockCode(this.stockCode)
                .timestamp(this.timestamp)
                .open(this.open)
                .high(Math.max(this.high, price))
                .low(Math.min(this.low, price))
                .close(price)
                .volume(this.volume + newVolume)
                .tickCount(this.tickCount + 1)
                .build();
    }

    /** 새로운 분봉 생성 (정적 팩토리 메서드) */
    public static StocksCandleDto createNew(
            String stockCode, long timestamp, long price, long volume) { // int → long 변경
        StocksCandleDto candle =
                StocksCandleDto.builder()
                        .stockCode(stockCode)
                        .timestamp(timestamp)
                        .open(price)
                        .high(price)
                        .low(price)
                        .close(price)
                        .volume(volume)
                        .tickCount(1)
                        .build();

        //        log.info("createNew 완료 - {}", candle.toString());
        return candle;
    }

    /** 타임스탬프를 Instant로 변환 */
    public Instant getInstant() {
        return Instant.ofEpochMilli(timestamp);
    }

    /** OHLCV 문자열 표현 */
    public String getOHLCVString() {
        return String.format("O:%d H:%d L:%d C:%d V:%d", open, high, low, close, volume);
    }

    /** 분봉 요약 정보 */
    @Override
    public String toString() {
        return String.format(
                "[%s] %s | %s T:%d", stockCode, getInstant(), getOHLCVString(), tickCount);
    }
}
