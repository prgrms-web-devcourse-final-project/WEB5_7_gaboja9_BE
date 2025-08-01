package io.gaboja9.mockstock.global.websocket;

import io.gaboja9.mockstock.global.config.JwtChannelInterceptor;
import io.gaboja9.mockstock.global.websocket.exeception.WebSocketExceptionHandler;

import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.handler.WebSocketHandlerDecoratorFactory;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class StompWebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StockSubscriptionInterceptor stockSubscriptionInterceptor;
    private final WebSocketExceptionHandler webSocketExceptionHandler;
    private final JwtChannelInterceptor jwtChannelInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 클라이언트용 토픽 설정
        config.enableSimpleBroker("/topic", "/queue");
        // 클라이언트에서 서버로 메시지 전송시
        config.setApplicationDestinationPrefixes("/app");
        // 개인 메시지용 prefix
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-stock")
                .setAllowedOrigins(
                        "https://mock-stock.pages.dev",
                        "http://localhost:3000",
                        "https://mockstocks.duckdns.org",
                        "https://mockstocks.duckdns.org:3000",
                        "http://localhost:5500",
                        "http://127.0.0.1:5500",
                        "http://localhost:5173",
                        "https://localhost:5173",
                        "http://127.0.0.1:5500",
                        "https://127.0.0.1:5500")
                .withSockJS();

        registry.setErrorHandler(webSocketExceptionHandler);
    }

    // 추가: 메시지 인터셉터 등록
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stockSubscriptionInterceptor, jwtChannelInterceptor);
    }

    @Bean
    public WebSocketHandlerDecoratorFactory webSocketHandlerDecoratorFactory(
            WebSocketSessionManager sessionManager) {
        return delegate -> new CustomWebSocketHandlerDecorator(delegate, sessionManager);
    }
}
