package io.gaboja9.mockstock.domain.stock;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockPriceData {  //받아오는 것에서 필요한 데이터만

    private String stockCode;     // 종목코드
    private int currentPrice;     // 현재가
    private String tradeTime;     // 체결 시간 (HHmmss 형식)

    /**
     * 단순 JSON 출력용 (stockCode, currentPrice, tradeTime만)
     */
    public String toSimpleJson() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode node = mapper.createObjectNode();

        node.put("stockCode", stockCode);
        node.put("currentPrice", currentPrice);
        node.put("tradeTime", formatTime(tradeTime));

        return node.toString();
    }

    /**
     * 143212 → 14:32:12 형태로 포맷
     */
    private String formatTime(String timeStr) {
        if (timeStr == null || timeStr.length() != 6) return "N/A";
        return timeStr.substring(0, 2) + ":" + timeStr.substring(2, 4) + ":" + timeStr.substring(4, 6);
    }
}