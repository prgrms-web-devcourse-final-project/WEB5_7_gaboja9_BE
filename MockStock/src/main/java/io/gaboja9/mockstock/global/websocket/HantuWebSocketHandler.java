package io.gaboja9.mockstock.global.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.gaboja9.mockstock.global.websocket.dto.StockPriceDto;
import io.gaboja9.mockstock.global.websocket.mapper.StockPriceMapper;
import io.gaboja9.mockstock.global.websocket.service.CandleMakerService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@Slf4j
@RequiredArgsConstructor
public class HantuWebSocketHandler extends TextWebSocketHandler {

    @Value("${hantu-openapi.appkey}")
    private String appKey;

    @Value("${hantu-openapi.appsecret}")
    private String appSecret;

    @Value("${hantu-openapi.websocket-domain:https://openapi.koreainvestment.com:9443}")
    private String websocketDomain;

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final HantuWebSocketSessionManager eventService;
    private final SimpMessagingTemplate messagingTemplate;
    private final CandleMakerService candleMakerService;

    private WebSocketSession session;
    private String approvalKey;
    private final ExecutorService executorService = Executors.newFixedThreadPool(5);
    private final Map<String, String> subscribedStocks = new ConcurrentHashMap<>();
    private final Map<String, StockPriceDto> latestPrices = new ConcurrentHashMap<>();

    // 웹소켓 세션이 열렸을 때 호출됨

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        this.session = session;
        eventService.setSession(session);

        // 접속 승인키 얻기
        approvalKey = getApprovalKey();

        // 초기 종목 구독
        subscribeInitialStocks();
    }

    /** 애플리케이션 시작 시 구독할 초기 종목들을 설정합니다. */
    private void subscribeInitialStocks() {
        //        log.info("Subscribing to initial stocks...");
        // 삼성전자, 카카오, 네이버를 순서대로 구독 요청
        subscribeStockPrice("005930", "H1"); // 삼성전자
        subscribeStockPrice("247540", "H1"); // 에코프로비엠
        subscribeStockPrice("005380", "H1"); // 현대차
        subscribeStockPrice("035420", "H1"); // NAVER
        subscribeStockPrice("035720", "H1"); // 카카오
        subscribeStockPrice("259960", "H1"); // 크래프톤
        subscribeStockPrice("068270", "H1"); // 셀트리온
        subscribeStockPrice("128940", "H1"); // 한미약품
        subscribeStockPrice("096770", "H1"); // SK이노베이션
        subscribeStockPrice("051910", "H1"); // LG화학
        subscribeStockPrice("005490", "H1"); // POSCO홀딩스
        subscribeStockPrice("017670", "H1"); // SK텔레콤
        subscribeStockPrice("105560", "H1"); // KB금융
        subscribeStockPrice("323410", "H1"); // 카카오뱅크
        subscribeStockPrice("139480", "H1"); // 이마트
        subscribeStockPrice("000120", "H1"); // CJ대한통운
        subscribeStockPrice("003490", "H1"); // 대한항공
        subscribeStockPrice("009540", "H1"); // 한국조선해양
        subscribeStockPrice("375500", "H1"); // DL이앤씨
        subscribeStockPrice("006400", "H1"); // 삼성SDI
    }

    // 메세지 호출시 호출
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message)
            throws Exception {
        String payload = message.getPayload();

        executorService.submit(() -> processMessage(payload));
    }

    // 연결 종료시 호출
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        this.session = null;
        eventService.setConnectionActive(false);
        eventService.setSession(null);
    }

    // 에러 발생시 호출
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        eventService.setConnectionActive(false);
    }

    // 웹소켓 접근키 발급
    public String getApprovalKey() {
        if (approvalKey != null) {
            return approvalKey;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> requestMap = new HashMap<>();
            requestMap.put("grant_type", "client_credentials");
            requestMap.put("appkey", appKey);
            requestMap.put("secretkey", appSecret);

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestMap, headers);

            ResponseEntity<String> response =
                    restTemplate.exchange(
                            websocketDomain + "/oauth2/Approval",
                            HttpMethod.POST,
                            entity,
                            String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode rootNode = objectMapper.readTree(response.getBody());
                approvalKey = rootNode.get("approval_key").asText();
                //                log.info("Approval key obtained: {}", approvalKey);
                return approvalKey;
            } else {
                throw new RuntimeException(
                        "Failed to obtain approval key. Status code: " + response.getStatusCode());
            }
        } catch (Exception e) {
            //            log.error("Error getting approval key", e);
            throw new RuntimeException("Failed to obtain approval key", e);
        }
    }

    // 실시간체결가 구독
    public void subscribeStockPrice(String stockCode, String marketCode) {
        if (session == null || !session.isOpen()) {
            //            log.warn("WebSocket not connected. Cannot subscribe to {}", stockCode);
            return;
        }

        try {
            Map<String, Object> header = new HashMap<>();
            header.put("approval_key", getApprovalKey());
            header.put("custtype", "P");
            header.put("tr_type", "1"); // 1: 등록
            header.put("content-type", "utf-8");

            Map<String, String> input = new HashMap<>();
            input.put("tr_id", "H0STCNT0"); // 실시간 주식 체결가
            input.put("tr_key", stockCode);

            Map<String, Object> body = new HashMap<>();
            body.put("input", input);

            Map<String, Object> request = new HashMap<>();
            request.put("header", header);
            request.put("body", body);

            String requestJson = objectMapper.writeValueAsString(request);
            session.sendMessage(new TextMessage(requestJson));
            subscribedStocks.put(stockCode, marketCode);
            //            log.info("Subscription request sent for stock: {}", stockCode);
        } catch (Exception e) {
            //            log.error("Error subscribing to stock price for code: {}", stockCode, e);
        }
    }

    /** 실시간 시세 구독 해제 */
    public void unsubscribeStockPrice(String stockCode, String marketCode) {
        if (session == null || !session.isOpen()) {
            //            log.warn("WebSocket not connected. Cannot unsubscribe.");
            return;
        }

        try {
            Map<String, Object> header = new HashMap<>();
            header.put("approval_key", getApprovalKey());
            header.put("custtype", "P");
            header.put("tr_type", "2"); // 2: 해제
            header.put("content-type", "utf-8");

            Map<String, String> input = new HashMap<>();
            input.put("tr_id", "H0STCNT0");
            input.put("tr_key", stockCode);

            Map<String, Object> body = new HashMap<>();
            body.put("input", input);

            Map<String, Object> request = new HashMap<>();
            request.put("header", header);
            request.put("body", body);

            String requestJson = objectMapper.writeValueAsString(request);
            session.sendMessage(new TextMessage(requestJson));
            subscribedStocks.remove(stockCode);
            //            log.info("Unsubscribed from real-time price for stock: {}", stockCode);
        } catch (Exception e) {
            //            log.error("Error unsubscribing from stock price for code: {}", stockCode,
            // e);
        }
    }

    // 메세지 처리
    private void processMessage(String message) {
        if (message.startsWith("0|H0STCNT0|")) {
            try {
                String[] parts = message.split("\\|");
                if (parts.length >= 4) {
                    String[] fields = parts[3].split("\\^");
                    StockPriceDto priceData = StockPriceMapper.parseStockPriceData(fields);

                    // og.info(priceData.toString());
                    //  STOMP 브로드캐스트 추가

                    messagingTemplate.convertAndSend(
                            "/topic/stock/" + priceData.getStockCode(), priceData);
                    latestPrices.put(priceData.getStockCode(), priceData);

                    candleMakerService.processTick(priceData);
                }
            } catch (Exception e) {
                //                log.error("Error processing real-time data: {}", message, e);
            }
            return;
        }

        try {
            JsonNode rootNode = objectMapper.readTree(message);
            if (rootNode.has("header")) {
                JsonNode header = rootNode.get("header");
                String trId = header.has("tr_id") ? header.get("tr_id").asText() : "";
                // pingpong이 왔을때 그대로 보내준다.
                if ("PINGPONG".equals(trId)) {
                    //                    log.debug("PINGPONG check received from server. Sending
                    // response.");
                    // 받은 메시지를 그대로 다시 보내주는 것이 가장 간단하고 확실한 응답 방식
                    session.sendMessage(new TextMessage(message));
                    return; // 응답 후 처리 종료
                }

                // 기타 응답 메시지 처리 (구독 성공/실패 등)
                if (rootNode.has("body")) {
                    JsonNode body = rootNode.get("body");
                    if (body.has("msg_cd")) {
                        //                        log.info(
                        //                                "Response received: [{}], {}",
                        //                                body.get("msg_cd").asText(),
                        //                                body.get("msg1").asText());
                    }
                }
            }
        } catch (Exception e) {
            //            log.warn("Could not parse JSON message: {}", message, e);
        }
    }

    public StockPriceDto getLatestPrice(String stockCode) {
        return latestPrices.get(stockCode);
    }
}
