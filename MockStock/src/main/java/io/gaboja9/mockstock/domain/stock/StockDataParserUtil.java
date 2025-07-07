package io.gaboja9.mockstock.domain.stock;

import com.fasterxml.jackson.databind.JsonNode;

public class StockDataParserUtil {

    /**
     * 문자열 배열 데이터를 간단한 StockPriceData로 파싱
     */
    public static StockPriceData parseStockPriceData(String[] fields) {
        return StockPriceData.builder()
                .stockCode(fields[0])
                .tradeTime(fields[1])
                .currentPrice(parseInt(fields[2]))
                .build();
    }

    /**
     * JSON 노드를 간단한 StockPriceData로 파싱
     */
    public static StockPriceData parseStockPriceData(JsonNode output) {
        if (output.isTextual()) {
            String[] fields = output.asText().split("\\^");
            return parseStockPriceData(fields);
        }

        return StockPriceData.builder()
                .stockCode(getString(output, "mksc_shrn_iscd"))
                .tradeTime(getString(output, "stck_cntg_hour"))
                .currentPrice(getInt(output, "stck_prpr"))
                .build();
    }

    // 안전한 파싱 유틸
    private static int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return 0;
        }
    }

    private static String getString(JsonNode node, String field) {
        return node.has(field) ? node.get(field).asText() : "";
    }

    private static int getInt(JsonNode node, String field) {
        return node.has(field) ? parseInt(node.get(field).asText()) : 0;
    }
}