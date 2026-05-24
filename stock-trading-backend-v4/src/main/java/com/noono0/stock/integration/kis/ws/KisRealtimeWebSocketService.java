package com.noono0.stock.integration.kis.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.noono0.stock.integration.kis.KisEffectiveMode;
import com.noono0.stock.integration.kis.config.KisProperties;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 국내주식 실시간 체결가(H0STCNT0) WebSocket. 장 운영 시간·키·방화벽에 따라 연결이 끊길 수 있습니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KisRealtimeWebSocketService {

    private static final String TR_ID = "H0STCNT0";

    private final KisProperties kisProperties;
    private final KisApprovalClient approvalClient;
    private final ObjectMapper objectMapper;

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "kis-realtime-ws");
        t.setDaemon(true);
        return t;
    });

    private final AtomicBoolean shutdown = new AtomicBoolean(false);
    private final AtomicReference<String> lastError = new AtomicReference<>();
    private final AtomicReference<Instant> lastMessageAt = new AtomicReference<>();
    private final Deque<String> recent = new ArrayDeque<>();
    private static final int MAX_RECENT = 20;

    private volatile WebSocketClient currentClient;

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        if (!kisProperties.getRealtime().isEnabled()) {
            log.info("KIS 실시간 WebSocket 비활성(app.kis.realtime.enabled=false)");
            return;
        }
        executor.submit(this::runLoop);
    }

    @PreDestroy
    public void stop() {
        shutdown.set(true);
        WebSocketClient c = currentClient;
        if (c != null) {
            try {
                c.close();
            } catch (Exception ignored) {
            }
        }
        executor.shutdownNow();
        try {
            executor.awaitTermination(3, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private void runLoop() {
        while (!shutdown.get()) {
            try {
                String mode = KisEffectiveMode.fromConfigOnly(kisProperties);
                String approval = approvalClient.approvalKey(mode);
                URI uri = wsUri(mode);
                String stock = nz(kisProperties.getRealtime().getSubscribeStock(), "005930");
                String payload = buildSubscribe(approval, stock);

                AtomicReference<Boolean> opened = new AtomicReference<>(false);
                WebSocketClient client =
                        new WebSocketClient(uri) {
                            @Override
                            public void onOpen(ServerHandshake handshakedata) {
                                opened.set(true);
                                try {
                                    send(payload);
                                } catch (Exception exception) {
                                    log.warn("KIS WS subscribe 전송 실패: {}", exception.getMessage());
                                }
                            }

                            @Override
                            public void onMessage(String message) {
                                lastMessageAt.set(Instant.now());
                                synchronized (recent) {
                                    while (recent.size() >= MAX_RECENT) recent.pollFirst();
                                    recent.addLast(message);
                                }
                            }

                            @Override
                            public void onClose(int code, String reason, boolean remote) {
                                log.info("KIS WS 종료 code={} reason={} remote={}", code, reason, remote);
                            }

                            @Override
                            public void onError(Exception ex) {
                                lastError.set(ex.getMessage());
                                log.warn("KIS WS 오류: {}", ex.getMessage());
                            }
                        };
                client.setConnectionLostTimeout(60);
                currentClient = client;
                lastError.set(null);

                boolean ok = client.connectBlocking(20, TimeUnit.SECONDS);
                if (!ok || !Boolean.TRUE.equals(opened.get())) {
                    throw new IllegalStateException("WebSocket 연결 실패: " + uri);
                }

                while (!shutdown.get() && client.isOpen()) {
                    Thread.sleep(500);
                }
                try {
                    client.closeBlocking();
                } catch (Exception ignored) {
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception exception) {
                lastError.set(exception.getMessage());
                log.warn("KIS 실시간 WS 루프: {} — 5초 후 재시도", exception.getMessage());
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    private URI wsUri(String mode) throws Exception {
        String override = kisProperties.getRealtime().getWsUrl();
        if (StringUtils.hasText(override)) {
            return URI.create(override.trim());
        }
        int port = "real".equalsIgnoreCase(mode) ? 21000 : 31000;
        return URI.create("ws://ops.koreainvestment.com:" + port + "/tryitout");
    }

    private String buildSubscribe(String approvalKey, String trKey) throws Exception {
        ObjectNode root = objectMapper.createObjectNode();
        ObjectNode header = root.putObject("header");
        header.put("approval_key", approvalKey);
        header.put("custtype", "P");
        header.put("tr_type", "1");
        header.put("content-type", "utf-8");
        ObjectNode body = root.putObject("body");
        ObjectNode input = body.putObject("input");
        input.put("tr_id", TR_ID);
        input.put("tr_key", trKey);
        return objectMapper.writeValueAsString(root);
    }

    public Map<String, Object> status() {
        String mode = KisEffectiveMode.fromConfigOnly(kisProperties);
        boolean connected = currentClient != null && currentClient.isOpen();
        Map<String, Object> m = new HashMap<>();
        m.put("enabled", kisProperties.getRealtime().isEnabled());
        m.put("mode", mode);
        m.put("connected", connected);
        m.put("trId", TR_ID);
        m.put("subscribeStock", nz(kisProperties.getRealtime().getSubscribeStock(), "005930"));
        m.put("lastMessageAt", lastMessageAt.get() != null ? lastMessageAt.get().toString() : null);
        m.put("lastError", lastError.get());
        synchronized (recent) {
            m.put("recentSample", new ArrayList<>(recent));
        }
        return m;
    }

    private static String nz(String s, String d) {
        return StringUtils.hasText(s) ? s : d;
    }
}
