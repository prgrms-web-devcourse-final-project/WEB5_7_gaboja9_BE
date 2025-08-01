package io.gaboja9.mockstock.global.websocket.mapper;

import io.gaboja9.mockstock.global.websocket.dto.StockPriceDto;

public class StockPriceMapper {

    // 문자열 배열 데이터를 StockPriceData로 파싱
    public static StockPriceDto parseStockPriceData(String[] fields) {
        return StockPriceDto.builder()
                .stockCode(fields[0])
                .tradeTime(fields[1])
                .currentPrice(parseInt(fields[2]))
                .dayOverDayPercent(parseDouble(fields[5]))
                .tradeVolume(parseLong(fields[12]))
                .highPrice(parseInt(fields[8])) // 고가 (STCK_HGPR)
                .lowPrice(parseInt(fields[9])) // 저가 (STCK_LWPR)
                .cumulativeVolume(parseLong(fields[13])) // 누적 거래량 (ACML_VOL)
                .build();
    }

    private static int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return 0;
        }
    }

    public static long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (Exception e) {
            return 0;
        }
    }

    private static double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (Exception e) {
            return 0.0;
        }
    }
}
