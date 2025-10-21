package com.perpdex.exchange.backpack;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.perpdex.model.OrderInfo;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Backpack WebSocket manager for real-time order updates
 */
@Slf4j
public class BackpackWebSocketManager {
    private static final String WS_URL = "wss://ws.backpack.exchange";

    private final String publicKey;
    private final String symbol;
    private final BackpackSignatureUtil signatureUtil;
    private final Consumer<OrderInfo> orderUpdateCallback;
    private final ObjectMapper objectMapper;

    private WebSocket webSocket;
    private volatile boolean running;

    public BackpackWebSocketManager(String publicKey, String secretKey,
                                   String symbol, Consumer<OrderInfo> orderUpdateCallback) {
        this.publicKey = publicKey;
        this.symbol = symbol;
        this.signatureUtil = new BackpackSignatureUtil(secretKey);
        this.orderUpdateCallback = orderUpdateCallback;
        this.objectMapper = new ObjectMapper();
    }

    public Mono<Void> connect() {
        return Mono.fromRunnable(() -> {
            OkHttpClient client = new OkHttpClient.Builder()
                    .readTimeout(0, TimeUnit.MILLISECONDS)
                    .pingInterval(30, TimeUnit.SECONDS)
                    .build();

            Request request = new Request.Builder()
                    .url(WS_URL)
                    .build();

            webSocket = client.newWebSocket(request, new WebSocketListener() {
                @Override
                public void onOpen(WebSocket webSocket, Response response) {
                    log.info("WebSocket connected to Backpack");
                    running = true;
                    subscribe(webSocket);
                }

                @Override
                public void onMessage(WebSocket webSocket, String text) {
                    handleMessage(text);
                }

                @Override
                public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                    log.error("WebSocket error", t);
                    running = false;
                    // Auto-reconnect logic could be added here
                }

                @Override
                public void onClosing(WebSocket webSocket, int code, String reason) {
                    log.info("WebSocket closing: {}", reason);
                    running = false;
                }

                @Override
                public void onClosed(WebSocket webSocket, int code, String reason) {
                    log.info("WebSocket closed: {}", reason);
                    running = false;
                }
            });
        });
    }

    private void subscribe(WebSocket webSocket) {
        try {
            long timestamp = System.currentTimeMillis();
            String signature = signatureUtil.generateWebSocketSignature("subscribe", timestamp);

            ObjectNode subscribeMessage = objectMapper.createObjectNode();
            subscribeMessage.put("method", "SUBSCRIBE");

            ArrayNode params = objectMapper.createArrayNode();
            params.add("account.orderUpdate." + symbol);
            subscribeMessage.set("params", params);

            ArrayNode signatureArray = objectMapper.createArrayNode();
            signatureArray.add(publicKey);
            signatureArray.add(signature);
            signatureArray.add(String.valueOf(timestamp));
            signatureArray.add("5000");
            subscribeMessage.set("signature", signatureArray);

            webSocket.send(objectMapper.writeValueAsString(subscribeMessage));
            log.info("Subscribed to order updates for {}", symbol);

        } catch (Exception e) {
            log.error("Failed to subscribe to WebSocket", e);
        }
    }

    private void handleMessage(String message) {
        try {
            JsonNode data = objectMapper.readTree(message);
            String stream = data.path("stream").asText("");

            if (stream.contains("orderUpdate")) {
                handleOrderUpdate(data.path("data"));
            } else {
                log.debug("Received non-order update message: {}", message);
            }

        } catch (Exception e) {
            log.error("Failed to handle WebSocket message: {}", message, e);
        }
    }

    private void handleOrderUpdate(JsonNode orderData) {
        try {
            String eventType = orderData.path("e").asText();
            String orderId = orderData.path("i").asText();
            String orderSymbol = orderData.path("s").asText();
            String side = orderData.path("S").asText();
            String quantity = orderData.path("q").asText();
            String price = orderData.path("p").asText();
            String filledQty = orderData.path("z").asText("0");

            // Only process orders for our symbol
            if (!orderSymbol.equals(symbol)) {
                return;
            }

            // Map Backpack side to our side
            String orderSide = "BID".equalsIgnoreCase(side) ? "buy" : "sell";

            // Determine status
            String status;
            if ("orderFill".equals(eventType) && quantity.equals(filledQty)) {
                status = "FILLED";
            } else if ("orderFill".equals(eventType)) {
                status = "PARTIALLY_FILLED";
            } else if ("orderAccepted".equals(eventType)) {
                status = "OPEN";
            } else if ("orderCancelled".equals(eventType) || "orderExpired".equals(eventType)) {
                status = "CANCELLED";
            } else {
                log.debug("Unknown event type: {}", eventType);
                return;
            }

            // Create OrderInfo and call callback
            OrderInfo orderInfo = OrderInfo.builder()
                    .orderId(orderId)
                    .side(orderSide)
                    .size(new BigDecimal(quantity))
                    .price(new BigDecimal(price))
                    .status(status)
                    .filledSize(new BigDecimal(filledQty))
                    .remainingSize(new BigDecimal(quantity).subtract(new BigDecimal(filledQty)))
                    .build();

            if (orderUpdateCallback != null) {
                orderUpdateCallback.accept(orderInfo);
            }

        } catch (Exception e) {
            log.error("Failed to handle order update", e);
        }
    }

    public Mono<Void> disconnect() {
        return Mono.fromRunnable(() -> {
            running = false;
            if (webSocket != null) {
                webSocket.close(1000, "Normal closure");
                log.info("WebSocket disconnected");
            }
        });
    }

    public boolean isConnected() {
        return running;
    }
}
