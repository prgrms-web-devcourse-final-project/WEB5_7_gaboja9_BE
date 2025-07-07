package io.gaboja9.mockstock.global.config;


import com.fasterxml.jackson.databind.ObjectMapper;
import io.gaboja9.mockstock.global.websocket.KoreaInvestmentWebSocketHandler;
import io.gaboja9.mockstock.global.websocket.MyWebSocketConnectionManager;
import io.gaboja9.mockstock.global.websocket.WebSocketEventService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Value("${hantu-openapi.websocket-uri:ws://ops.koreainvestment.com:31000}")
    private String websocketUri;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 서버 측 웹소켓 핸들러 등록 - 필요한 경우에만 사용
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public WebSocketClient webSocketClient() {
        StandardWebSocketClient client = new StandardWebSocketClient();
        client.getUserProperties().put("org.apache.tomcat.websocket.IO_TIMEOUT_MS", "60000");
        return client;
    }

    // WebSocketEventService 빈 생성
    @Bean
    public WebSocketEventService webSocketEventService() {
        return new WebSocketEventService();
    }

    // KoreaInvestmentWebSocketHandler 빈 생성
    @Bean
    public KoreaInvestmentWebSocketHandler koreaInvestmentWebSocketHandler(
            ObjectMapper objectMapper,
            RestTemplate restTemplate,
            WebSocketEventService webSocketEventService) {
        return new KoreaInvestmentWebSocketHandler(
                objectMapper,
                restTemplate,
                webSocketEventService
        );
    }

    // MyWebSocketConnectionManager 빈 생성
    @Bean
    public MyWebSocketConnectionManager koreaInvestmentWebSocketConnectionManager(
            WebSocketClient webSocketClient,
            WebSocketEventService webSocketEventService,
            ApplicationContext applicationContext) {

        // KoreaInvestmentWebSocketHandler를 ApplicationContext에서 가져오는 대신
        // 클래스를 생성한 후 주입
        KoreaInvestmentWebSocketHandler handler = applicationContext.getBean(KoreaInvestmentWebSocketHandler.class);

        MyWebSocketConnectionManager connectionManager = new MyWebSocketConnectionManager(
                webSocketClient,
                handler,
                websocketUri,
                webSocketEventService
        );

        // 자동 시작 비활성화 (서비스 레이어에서 명시적으로 시작)
        connectionManager.setAutoStartup(false);
        return connectionManager;
    }

    // 초기화 로직을 위한 빈
    @Bean(initMethod = "startConnection", destroyMethod = "stopConnection")
    public ConnectionInitializer connectionInitializer(MyWebSocketConnectionManager connectionManager) {
        return new ConnectionInitializer(connectionManager);
    }

    // 내부 초기화 클래스
    public static class ConnectionInitializer {
        private final MyWebSocketConnectionManager connectionManager;

        public ConnectionInitializer(MyWebSocketConnectionManager connectionManager) {
            this.connectionManager = connectionManager;
        }

        public void startConnection() {
            connectionManager.startConnection();
        }

        public void stopConnection() {
            connectionManager.stopConnection();
        }
    }
}