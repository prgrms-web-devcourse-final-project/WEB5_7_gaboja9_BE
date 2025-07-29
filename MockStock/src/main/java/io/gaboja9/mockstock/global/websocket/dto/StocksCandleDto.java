package io.gaboja9.mockstock.global.websocket.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

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
    private Integer open;

    @Schema(description = "고가", example = "75200")
    private Integer high;

    @Schema(description = "저가", example = "74800")
    private Integer low;

    @Schema(description = "종가", example = "75100")
    private Integer close;

    @Schema(description = "거래량", example = "1523")
    private Long volume;

    @Schema(description = "틱 개수", example = "42")
    @Builder.Default
    private Integer tickCount = 1;

    /** 새로운 체결가로 분봉 업데이트 */
    public StocksCandleDto updateWith(int price, long newVolume) {
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
            String stockCode, long timestamp, int price, long volume) {
        return StocksCandleDto.builder()
                .stockCode(stockCode)
                .timestamp(timestamp)
                .open(price)
                .high(price)
                .low(price)
                .close(price)
                .volume(volume)
                .tickCount(1)
                .build();
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

    /** 불변 객체 복사 (방어적 복사) */
    public StocksCandleDto copy() {
        return StocksCandleDto.builder()
                .stockCode(this.stockCode)
                .timestamp(this.timestamp)
                .open(this.open)
                .high(this.high)
                .low(this.low)
                .close(this.close)
                .volume(this.volume)
                .tickCount(this.tickCount)
                .build();
    }
}
