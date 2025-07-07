package io.gaboja9.mockstock.global.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gaboja9.mockstock.domain.stock.StockDataParserUtil;
import io.gaboja9.mockstock.domain.stock.StockPriceData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@Slf4j
@RequiredArgsConstructor
public class KoreaInvestmentWebSocketHandler extends TextWebSocketHandler {
    @Value("${hantu-openapi.appkey}")
    private String appKey;
    @Value("${hantu-openapi.appsecret}")
    private String appSecret;
    @Value("${hantu-openapi.websocket-domain:https://openapi.koreainvestment.com:9443}")
    private String websocketDomain;

    //db 연결 현재 제외

    private final ObjectMapper objectMapper; // JSON 파싱을 위한 ObjectMapper
    private final RestTemplate restTemplate; // HTTP 요청을 위한 RestTemplate
    private final WebSocketEventService eventService; // 웹소켓 연결 상태를 관리하는 서비스
    // 멤버 변수
    private WebSocketSession session; // 현재 웹소켓 세션
    private String approvalKey; // 한투 API 승인 키
    private final ExecutorService executorService = Executors.newFixedThreadPool(5); // 메시지 처리를 위한 스레드 풀
    private final Map<String, String> subscribedStocks = new ConcurrentHashMap<>(); // 현재 구독 중인 종목 목록
    /**
     * 웹소켓 세션이 열렸을 때 호출됨
     * (이 부분은 기존과 동일하며, 하트비트 및 연결 유지와 관련됨)
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        this.session = session;
        eventService.setSession(session); // WebSocketEventService에 세션 등록
        eventService.setConnectionActive(true); // 연결 활성화 상태로 설정
        log.info("WebSocket connection established at {}: {}", LocalDateTime.now(), session.getId());
        // 접속 승인키 얻기 (한투 API 인증 과정)
        approvalKey = getApprovalKey();
        // 연결 헤더 전송 (웹소켓 연결 초기화 메시지)
        sendConnectionHeader();
        // 기존 구독 정보 복원 (재연결 시 유용)
        resubscribeStocks();
    }
    /**
     * 메시지 수신 시 호출됨
     * 수신된 메시지를 비동기적으로 처리하도록 `processMessage` 메서드를 호출합니다.
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload(); // 수신된 메시지 본문
        log.info("Received message at {}: {}", LocalDateTime.now(), payload);
        // 메시지 처리를 별도의 스레드 풀에서 비동기적으로 수행하여 메인 스레드 블로킹 방지
        executorService.submit(() -> processMessage(payload));
    }
    /**
     * 연결 종료 시 호출됨
     * (이 부분은 기존과 동일하며, 하트비트 및 연결 유지와 관련됨)
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info("WebSocket connection closed at {}: {}, status: {}",
                LocalDateTime.now(), session.getId(), status);
        this.session = null;
        eventService.setConnectionActive(false); // 연결 비활성화
        eventService.setSession(null);
    }
    /**
     * 에러 발생 시 호출됨
     * (이 부분은 기존과 동일하며, 하트비트 및 연결 유지와 관련됨)
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("WebSocket transport error in session {}", session.getId(), exception);
        eventService.setConnectionActive(false); // 에러 발생 시 연결 비활성화
    }
    /**
     * 웹소켓 접속키 발급 (기존 로직과 동일)
     */
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
            ResponseEntity<String> response = restTemplate.exchange(
                    websocketDomain + "/oauth2/Approval",
                    HttpMethod.POST,
                    entity,
                    String.class
            );
            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode rootNode = objectMapper.readTree(response.getBody());
                approvalKey = rootNode.get("approval_key").asText();
                log.info("Approval key obtained: {}", approvalKey);
                return approvalKey;
            } else {
                throw new RuntimeException("Failed to obtain approval key. Status code: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error getting approval key", e);
            throw new RuntimeException("Failed to obtain approval key", e);
        }
    }
    /**
     * 접속 헤더 전송 (기존 로직과 동일)
     */
    private void sendConnectionHeader() {
        try {
            Map<String, Object> header = new HashMap<>();
            header.put("approval_key", approvalKey);
            header.put("custtype", "P");
            header.put("tr_type", "1"); // 1: 연결
            header.put("content-type", "utf-8");
            Map<String, String> input = new HashMap<>();
            input.put("tr_id", "H0STCNT0"); // 실시간 체결가 TR ID
            input.put("tr_key", "005930");  // 기본 샘플 종목 코드 (초기 연결 시 불필요할 수 있으나, 예제에 따라 남겨둠)
            Map<String, Object> body = new HashMap<>();
            body.put("input", input);
            Map<String, Object> request = new HashMap<>();
            request.put("header", header);
            request.put("body", body);
            String requestJson = objectMapper.writeValueAsString(request);
            log.info("Sending connection header: {}", requestJson);
            session.sendMessage(new TextMessage(requestJson));
            log.info("Connection header sent successfully");
        } catch (Exception e) {
            log.error("Error sending connection header", e);
        }
    }
    /**
     * 실시간 체결가 구독 (기존 로직과 동일)
     */
    public void subscribeStockPrice(String stockCode, String marketCode) {
        if (session == null || !session.isOpen()) {
            log.warn("WebSocket not connected. Cannot subscribe to {}", stockCode);
            return;
        }
        try {
            Map<String, Object> header = new HashMap<>();
            header.put("approval_key", approvalKey);
            header.put("custtype", "P");
            header.put("tr_type", "1"); // 1: 구독
            header.put("content-type", "utf-8");
            Map<String, String> input = new HashMap<>();
            input.put("tr_id", "H0STCNT0"); // 실시간 체결가 TR ID
            input.put("tr_key", stockCode);
            Map<String, Object> body = new HashMap<>();
            body.put("input", input);
            Map<String, Object> request = new HashMap<>();
            request.put("header", header);
            request.put("body", body);
            String requestJson = objectMapper.writeValueAsString(request);
            log.info("Sending subscription request for stock {}: {}", stockCode, requestJson);
            session.sendMessage(new TextMessage(requestJson));
            subscribedStocks.put(stockCode, marketCode); // 구독 목록에 추가
            log.info("Subscription request sent for stock: {}", stockCode);
        } catch (Exception e) {
            log.error("Error subscribing to stock price for code: {}", stockCode, e);
        }
    }
    /**
     * 실시간 시세 구독 해제 (기존 로직과 동일)
     */
    public void unsubscribeStockPrice(String stockCode, String marketCode) {
        if (session == null || !session.isOpen()) {
            log.warn("WebSocket not connected. Cannot unsubscribe.");
            return;
        }
        try {
            // 한투 API 문서에 따라 tr_key 형식이 다를 수 있음 (예: H1_005930, H2_005930)
            String trKey = "H1".equals(marketCode) ? "H1_" + stockCode : "H2_" + stockCode; // H1: 국내주식, H2: 해외주식 (예시)
            Map<String, Object> header = new HashMap<>();
            header.put("tr_type", "2"); // 2: 구독 해제
            header.put("tr_id", "H0STCNT0");
            header.put("tr_key", trKey); // 해제할 종목의 tr_key
            Map<String, String> input = new HashMap<>();
            input.put("mksc_shrn_iscd", stockCode); // 종목코드 (필수 아닐 수 있음, API 문서 확인)
            Map<String, Object> body = new HashMap<>();
            body.put("input", input);
            Map<String, Object> request = new HashMap<>();
            request.put("header", header);
            request.put("body", body);
            String requestJson = objectMapper.writeValueAsString(request);
            session.sendMessage(new TextMessage(requestJson));
            subscribedStocks.remove(stockCode); // 구독 목록에서 제거
            log.info("Unsubscribed from real-time price for stock: {}", stockCode);
        } catch (Exception e) {
            log.error("Error unsubscribing from stock price for code: {}", stockCode, e);
        }
    }
    /**
     * 하트비트 메시지 전송 (기존 로직과 동일)
     */
    public void sendHeartbeat() {
        if (session == null || !session.isOpen()) {
            log.warn("WebSocket not connected. Cannot send heartbeat.");
            return;
        }
        try {
            Map<String, Object> header = new HashMap<>();
            header.put("tr_type", "3"); // 3: 하트비트
            Map<String, Object> request = new HashMap<>();
            request.put("header", header);
            String requestJson = objectMapper.writeValueAsString(request);
            session.sendMessage(new TextMessage(requestJson));
            log.debug("Heartbeat sent");
        } catch (Exception e) {
            log.error("Error sending heartbeat", e);
            eventService.setConnectionActive(false); // 에러 발생 시 연결 비활성화
        }
    }






    /**
     * 수신된 메시지를 파싱하고 처리하는 핵심 메서드
     * 이 메서드에서 StockDataParserUtil을 활용합니다.
     */
    private void processMessage(String message) {
        try {
            // --- 1. 파이프(|)로 구분된 실시간 데이터 메시지 처리 ---
            // 예시 메시지 형식: "0|H0STCNT0|001|005930^143212^98700^..."
            // (실제 데이터 필드 순서는 한투 API 문서 (실시간시세 TRID H0STCNT0)를 정확히 확인해야 함)
            if (message.matches("^\\d+\\|H0STCNT0\\|\\d+\\|.*")) {
                String[] parts = message.split("\\|"); // 파이프 기준으로 분리
                if (parts.length >= 4) { // 최소한 CD|TRID|개수|데이터 가 있어야 함
                    String data = parts[3]; // 실제 데이터 부분 (예: "005930^143212^98700^...")
                    String[] fields = data.split("\\^"); // ^ 기준으로 다시 분리
                    // StockPriceData 객체 생성 및 처리
                    // StockDataParserUtil의 parseStockPriceData(String[] fields)가
                    // fields[0], fields[1], fields[2]를 올바르게 매핑하는지 **반드시 확인하세요.**
                    if (fields.length >= 3) { // 종목코드, 체결시간, 현재가 최소 3개 필드 확인
                        StockPriceData priceData = StockDataParserUtil.parseStockPriceData(fields);
                        log.info("Received real-time update (pipe-delimited): {}", priceData.toSimpleJson());
                        // TODO: 파싱된 priceData를 활용하여 비즈니스 로직 수행 (DB 저장, 화면 업데이트, Kafka 전송 등)
                        // 예: stockInfoService.updateCurrentPrice(priceData.getStockCode(), priceData.getCurrentPrice());
                    } else {
                        log.warn("Incomplete pipe-delimited data fields received for H0STCNT0: {}", data);
                    }
                } else {
                    log.warn("Malformed pipe-delimited message: {}", message);
                }
                return; // 파이프 구분 메시지 처리가 완료되었으므로, 여기서 메서드 종료
            }
            // --- 2. JSON 형식 메시지 처리 (구독 응답, PINGPONG, 에러 메시지 등) ---
            JsonNode rootNode = objectMapper.readTree(message); // JSON 메시지를 JsonNode로 파싱
            if (rootNode.has("header")) {
                String trId = rootNode.get("header").has("tr_id") ? rootNode.get("header").get("tr_id").asText() : "";
                if ("H0STCNT0".equals(trId) && rootNode.has("body")) {
                    // JSON 형식의 주가 업데이트 또는 구독 응답 메시지 처리
                    handleStockPriceUpdate(rootNode.get("body"));
                } else if ("PINGPONG".equals(trId)) {
                    log.debug("Received PINGPONG message (heartbeat response).");
                } else if ("ACCESS_TOKEN_EXPIRED".equals(trId)) {
                    log.warn("Access token expired. Please re-authenticate or renew the token.");
                    // TODO: 토큰 재발급 로직 호출 또는 재연결 트리거
                } else {
                    log.info("Other header message type received (TR_ID: {}): {}", trId, message);
                }
            } else if (rootNode.has("body") && rootNode.get("body").has("msg1")) {
                // 특정 응답 메시지 (예: 구독 성공 메시지)
                log.info("General message from server: {}", rootNode.get("body").get("msg1").asText());
            } else {
                // 헤더나 특정 바디 없이 온 메시지 (예상치 못한 형식)
                log.info("Message without specific header/body handled: {}", message);
            }
        } catch (com.fasterxml.jackson.core.JsonParseException e) {
            // JSON 파싱 에러 (JSON 형식이 아니거나 잘못된 JSON일 경우)
            // 파이프 구분 메시지가 JSON 파서로 넘어가면 이 예외가 발생할 수 있으므로 DEBUG 레벨로 처리
            log.debug("JSON parse error (might be pipe-delimited message or malformed JSON): {}", e.getMessage());
        } catch (Exception e) {
            // 그 외 처리 중 발생한 모든 예외
            log.error("Error processing WebSocket message: {}", message, e);
        }
    }
    /**
     * 주식 가격 업데이트 처리 (JSON 형식 응답용)
     * StockDataParserUtil의 parseStockPriceData(JsonNode output)를 활용합니다.
     */
    private void handleStockPriceUpdate(JsonNode body) {
        try {
            // 구독 성공 메시지는 로그만 출력하고 종료
            if (body.has("msg1") && "SUBSCRIBE SUCCESS".equals(body.get("msg1").asText())) {
                log.info("Successfully subscribed to stock price updates from JSON response.");
                return;
            }
            if (body.has("output")) {
                JsonNode output = body.get("output"); // 실제 데이터가 담긴 'output' 노드
                StockPriceData priceData;
                if (output.isTextual()) {
                    // 'output' 필드 자체가 문자열인 경우 (JSON 내부에 문자열로 데이터가 인코딩된 형태)
                    String outputData = output.asText(); // 예: "005930^143212^98700^..."
                    String[] fields = outputData.split("\\^");
                    if (fields.length < 3) { // 최소 3개 필드 (종목코드,체결시간,현재가) 확인
                        log.warn("Received incomplete JSON-nested textual price data: {}", outputData);
                        return;
                    }
                    // 문자열 배열 파싱 메서드 호출
                    priceData = StockDataParserUtil.parseStockPriceData(fields);
                } else {
                    // 'output' 필드가 직접 JSON 객체인 경우
                    if (!output.has("mksc_shrn_iscd")) {
                        log.warn("Required field 'mksc_shrn_iscd' missing in JSON output: {}", output.toPrettyString());
                        return;
                    }
                    // JSON 노드 파싱 메서드 호출
                    priceData = StockDataParserUtil.parseStockPriceData(output);
                }
                // 파싱된 체결 정보를 사용자 정의 JSON 형식으로 로그 출력
                log.info("Processed stock update (from JSON body): {}", priceData.toSimpleJson());
                // TODO: 파싱된 priceData를 활용하여 비즈니스 로직 수행 (DB 저장, Kafka 전송 등)
                // 예: stockInfoService.updateCurrentPrice(priceData.getStockCode(), priceData.getCurrentPrice());
            } else {
                log.warn("No 'output' field found in stock price update body: {}", body.toPrettyString());
            }
        } catch (Exception e) {
            log.error("Error handling stock price update from JSON body: {}", body.toPrettyString(), e);
        }
    }
    /**
     * 재연결 후 기존 구독 복원 (기존 로직과 동일)
     */
    private void resubscribeStocks() {
        if (subscribedStocks.isEmpty()) {
            return;
        }
        log.info("Resubscribing to {} stocks after reconnection", subscribedStocks.size());
        subscribedStocks.forEach((stockCode, marketCode) -> {
            try {
                Thread.sleep(100);  // API 호출 제한 고려하여 짧은 딜레이
                subscribeStockPrice(stockCode, marketCode);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Thread interrupted during resubscription.", e);
            }
        });
    }
    /**
     * 메시지를 StockTickData로 변환 (Kafka 전송용) - 이 부분은 StockPriceData와는 별개의 목적이므로 주석 처리
     * 만약 Kafka로 보내는 데이터 형식이 StockPriceData와 다르다면 이 메서드를 활용하거나 수정해야 합니다.
     * 현재는 StockPriceData를 직접 활용하는 것으로 가정했습니다.
     */
    /*
    private Object convertToTickData(JsonNode output) {
        ObjectNode tickData = objectMapper.createObjectNode();
        tickData.put("stockCode", output.get("mksc_shrn_iscd").asText());
        tickData.put("currentPrice", output.has("stck_prpr") ?
                Double.parseDouble(output.get("stck_prpr").asText()) : 0);
        tickData.put("priceChange", output.has("prdy_vrss") ?
                Double.parseDouble(output.get("prdy_vrss").asText()) : 0);
        tickData.put("changeRate", output.has("prdy_ctrt") ?
                Double.parseDouble(output.get("prdy_ctrt").asText()) : 0);
        tickData.put("volume", output.has("acml_vol") ?
                Long.parseLong(output.get("acml_vol").asText()) : 0);
        tickData.put("timestamp", LocalDateTime.now().toString());
        return tickData;
    }
    */
    /**
     * 여러 종목 동시 구독 (기존 로직과 동일)
     */
    public void subscribeMultipleStocks(String[] stockCodes, String marketCode) {
        if (stockCodes == null || stockCodes.length == 0) {
            return;
        }
        log.info("Subscribing to {} stocks", stockCodes.length);
        for (String stockCode : stockCodes) {
            try {
                Thread.sleep(100);  // API 호출 제한 고려하여 짧은 딜레이
                subscribeStockPrice(stockCode, marketCode);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Thread interrupted during multiple stock subscription.", e);
            }
        }
    }
}