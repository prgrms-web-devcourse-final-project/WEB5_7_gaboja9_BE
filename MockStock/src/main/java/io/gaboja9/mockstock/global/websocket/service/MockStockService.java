package io.gaboja9.mockstock.global.websocket.service;

import io.gaboja9.mockstock.global.websocket.dto.StockPriceDto;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MockStockService {

  private final SimpMessagingTemplate messagingTemplate;
  private static final DateTimeFormatter HHMMSS = DateTimeFormatter.ofPattern("HHmmss");

  /**
   * 주어진 종목코드에 대해 10초간(매초 2회) 랜덤 주가를 생성하여 STOMP 토픽으로 전송하고, 보낸 데이터를 리스트로 반환합니다.
   */
  public List<StockPriceDto> sendMockPrices(String stockCode) {
    List<StockPriceDto> sent = new ArrayList<>();

    for (int i = 0; i < 10; i++) {
      for (int j = 0; j < 2; j++) {
        int currentPrice = 71400 + ThreadLocalRandom.current().nextInt(-100, 101);
        int highPrice = currentPrice + ThreadLocalRandom.current().nextInt(1, 6);
        int lowPrice = currentPrice - ThreadLocalRandom.current().nextInt(1, 6);

        StockPriceDto dto = StockPriceDto.builder()
            .stockCode(stockCode)
            .currentPrice(currentPrice)
            .dayOverDayPercent(-1.65 + (j * 0.01))
            .tradeTime(LocalTime.now().format(HHMMSS))
            .tradeVolume(100L + j)
            .highPrice(highPrice)
            .lowPrice(lowPrice)
            .cumulativeVolume(25_000L + i * 2 + j)
            .build();

        // STOMP 메시지 전송
        messagingTemplate.convertAndSend("/topic/stock/" + stockCode, dto);
        log.info("[MOCK→STOMP] {}: {}", stockCode, dto);
        sent.add(dto);
      }

      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      }
    }

    return sent;
  }
}