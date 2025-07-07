package io.gaboja9.mockstock.global.websocket;

import lombok.extern.slf4j.Slf4j;

import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.WebSocketConnectionManager;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
public class MyWebSocketConnectionManager extends WebSocketConnectionManager {

    private final ScheduledExecutorService scheduler;
    private ScheduledFuture<?> heartbeatTask;
    private final WebSocketEventService eventService;

    /** 생성자 */
    public MyWebSocketConnectionManager(
            WebSocketClient client,
            WebSocketHandler webSocketHandler,
            String uriTemplate,
            WebSocketEventService eventService) {
        super(client, webSocketHandler, uriTemplate);
        this.scheduler = new ScheduledThreadPoolExecutor(1);
        this.eventService = eventService;
    }

    /** 연결 시작 */
    public void startConnection() {
        log.info("Starting WebSocket connection manager");
        super.start();
        startHeartbeat();
    }

    /** 연결 종료 */
    public void stopConnection() {
        log.info("Stopping WebSocket connection manager");
        stopHeartbeat();
        super.stop();
    }

    /** 하트비트 시작 */
    private void startHeartbeat() {
        stopHeartbeat(); // 기존 하트비트 중지

        heartbeatTask =
                scheduler.scheduleAtFixedRate(
                        () -> {
                            WebSocketSession session = eventService.getSession();
                            if (session != null && session.isOpen()) {
                                // 하트비트 이벤트 발생 - 구현은 나중에 추가
                                log.debug("Heartbeat check - session is active");
                            } else if (eventService.isConnectionActive()) {
                                // 세션이 없거나 닫혀있는데 활성 상태라고 생각하는 경우
                                log.warn(
                                        "Heartbeat detected inactive session while connection"
                                            + " marked as active. Reconnecting...");
                                reconnect();
                            }
                        },
                        30,
                        30,
                        TimeUnit.SECONDS);

        log.info("WebSocket heartbeat started");
    }

    /** 하트비트 중지 */
    private void stopHeartbeat() {
        if (heartbeatTask != null && !heartbeatTask.isCancelled()) {
            heartbeatTask.cancel(false);
            log.info("WebSocket heartbeat stopped");
        }
    }

    /** 재연결 시도 */
    private void reconnect() {
        log.info("Attempting to reconnect WebSocket");
        stopConnection();
        try {
            Thread.sleep(1000); // 약간의 지연 후 재연결
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        startConnection();
    }
}
