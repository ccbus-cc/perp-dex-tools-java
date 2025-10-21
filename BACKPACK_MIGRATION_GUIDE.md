# Backpack Exchange Migration Guide

This guide shows the step-by-step migration from `backpack.py` to Java implementation.

## Python File Structure

```
exchanges/backpack.py (595 lines)
├── BackpackWebSocketManager (150 lines)
│   ├── ED25519 signature generation
│   ├── WebSocket connection/reconnection
│   └── Order update handling
└── BackpackClient (445 lines)
    ├── REST API operations (using BPX SDK)
    ├── Order placement with retry logic
    ├── Position tracking
    └── Order management
```

## Java File Structure (Recommended)

```
src/main/java/com/perpdex/exchange/backpack/
├── BackpackClient.java              (Main client)
├── BackpackWebSocketManager.java    (WebSocket handling)
├── BackpackApiClient.java           (REST API calls)
└── BackpackSignatureUtil.java       (ED25519 signing)

src/main/java/com/perpdex/exchange/backpack/model/
├── BackpackOrder.java
├── BackpackOrderBook.java
└── BackpackPosition.java
```

## Step-by-Step Migration Path

### Step 1: Create Signature Utility (ED25519)

**Python** (`backpack.py` lines 36-50):
```python
from cryptography.hazmat.primitives.asymmetric import ed25519

class BackpackWebSocketManager:
    def __init__(self, public_key: str, secret_key: str, ...):
        self.private_key = ed25519.Ed25519PrivateKey.from_private_bytes(
            base64.b64decode(secret_key)
        )

    def _generate_signature(self, instruction: str, timestamp: int, window: int = 5000) -> str:
        message = f"instruction={instruction}&timestamp={timestamp}&window={window}"
        signature_bytes = self.private_key.sign(message.encode())
        return base64.b64encode(signature_bytes).decode()
```

**Java** (`BackpackSignatureUtil.java`):
```java
package com.perpdex.exchange.backpack;

import net.i2p.crypto.eddsa.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class BackpackSignatureUtil {
    private final EdDSAPrivateKey privateKey;

    public BackpackSignatureUtil(String base64SecretKey) {
        byte[] keyBytes = Base64.getDecoder().decode(base64SecretKey);
        EdDSAParameterSpec spec = EdDSANamedCurveTable.getByName("Ed25519");
        this.privateKey = new EdDSAPrivateKey(new EdDSAPrivateKeySpec(keyBytes, spec));
    }

    public String generateSignature(String instruction, long timestamp, int window) {
        String message = String.format("instruction=%s&timestamp=%d&window=%d",
                                      instruction, timestamp, window);

        EdDSAEngine engine = new EdDSAEngine();
        engine.initSign(privateKey);

        byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
        byte[] signature = engine.signOneShot(messageBytes);

        return Base64.getEncoder().encodeToString(signature);
    }
}
```

**Required Dependency** (add to `pom.xml`):
```xml
<dependency>
    <groupId>net.i2p.crypto</groupId>
    <artifactId>eddsa</artifactId>
    <version>0.3.0</version>
</dependency>
```

### Step 2: Create REST API Client

**Python** (`backpack.py` uses BPX SDK):
```python
from bpx.public import Public
from .bp_client import Account

self.public_client = Public()
self.account_client = Account(
    public_key=self.public_key,
    secret_key=self.secret_key
)
```

**Java** (`BackpackApiClient.java`):
```java
package com.perpdex.exchange.backpack;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

public class BackpackApiClient {
    private static final String BASE_URL = "https://api.backpack.exchange";
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String publicKey;
    private final BackpackSignatureUtil signatureUtil;

    public BackpackApiClient(String publicKey, String secretKey) {
        this.publicKey = publicKey;
        this.signatureUtil = new BackpackSignatureUtil(secretKey);
        this.objectMapper = new ObjectMapper();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build();
    }

    /**
     * Get order book depth (public endpoint)
     */
    public Mono<JsonNode> getOrderBook(String symbol) {
        return Mono.fromCallable(() -> {
            String url = BASE_URL + "/api/v1/depth?symbol=" + symbol;

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new RuntimeException("Failed to get order book: " + response.code());
                }

                String body = response.body().string();
                return objectMapper.readTree(body);
            }
        });
    }

    /**
     * Place order (authenticated endpoint)
     */
    public Mono<JsonNode> placeOrder(String symbol, String side, String orderType,
                                     BigDecimal quantity, BigDecimal price, String timeInForce) {
        return Mono.fromCallable(() -> {
            long timestamp = System.currentTimeMillis();

            // Build request body
            JsonNode requestBody = objectMapper.createObjectNode()
                    .put("symbol", symbol)
                    .put("side", side)
                    .put("orderType", orderType)
                    .put("quantity", quantity.toPlainString())
                    .put("price", price.toPlainString())
                    .put("timeInForce", timeInForce);

            String bodyString = objectMapper.writeValueAsString(requestBody);

            // Generate signature
            String signature = signatureUtil.generateSignature("executeOrder", timestamp, 5000);

            RequestBody body = RequestBody.create(
                    bodyString,
                    MediaType.get("application/json")
            );

            Request request = new Request.Builder()
                    .url(BASE_URL + "/api/v1/order")
                    .post(body)
                    .addHeader("X-API-Key", publicKey)
                    .addHeader("X-Signature", signature)
                    .addHeader("X-Timestamp", String.valueOf(timestamp))
                    .addHeader("X-Window", "5000")
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new RuntimeException("Failed to place order: " + response.code());
                }

                String responseBody = response.body().string();
                return objectMapper.readTree(responseBody);
            }
        });
    }

    /**
     * Cancel order
     */
    public Mono<JsonNode> cancelOrder(String orderId, String symbol) {
        return Mono.fromCallable(() -> {
            long timestamp = System.currentTimeMillis();
            String signature = signatureUtil.generateSignature("cancelOrder", timestamp, 5000);

            JsonNode requestBody = objectMapper.createObjectNode()
                    .put("orderId", orderId)
                    .put("symbol", symbol);

            RequestBody body = RequestBody.create(
                    objectMapper.writeValueAsString(requestBody),
                    MediaType.get("application/json")
            );

            Request request = new Request.Builder()
                    .url(BASE_URL + "/api/v1/order")
                    .delete(body)
                    .addHeader("X-API-Key", publicKey)
                    .addHeader("X-Signature", signature)
                    .addHeader("X-Timestamp", String.valueOf(timestamp))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new RuntimeException("Failed to cancel order: " + response.code());
                }
                return objectMapper.readTree(response.body().string());
            }
        });
    }

    /**
     * Get open orders
     */
    public Mono<JsonNode> getOpenOrders(String symbol) {
        return Mono.fromCallable(() -> {
            long timestamp = System.currentTimeMillis();
            String signature = signatureUtil.generateSignature("getOpenOrders", timestamp, 5000);

            String url = BASE_URL + "/api/v1/orders?symbol=" + symbol;

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .addHeader("X-API-Key", publicKey)
                    .addHeader("X-Signature", signature)
                    .addHeader("X-Timestamp", String.valueOf(timestamp))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new RuntimeException("Failed to get orders: " + response.code());
                }
                return objectMapper.readTree(response.body().string());
            }
        });
    }

    /**
     * Get account balance/positions
     */
    public Mono<JsonNode> getBalance() {
        return Mono.fromCallable(() -> {
            long timestamp = System.currentTimeMillis();
            String signature = signatureUtil.generateSignature("getBalance", timestamp, 5000);

            Request request = new Request.Builder()
                    .url(BASE_URL + "/api/v1/capital")
                    .get()
                    .addHeader("X-API-Key", publicKey)
                    .addHeader("X-Signature", signature)
                    .addHeader("X-Timestamp", String.valueOf(timestamp))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new RuntimeException("Failed to get balance: " + response.code());
                }
                return objectMapper.readTree(response.body().string());
            }
        });
    }
}
```

### Step 3: Create WebSocket Manager

**Python** (`backpack.py` lines 23-150):
```python
class BackpackWebSocketManager:
    def __init__(self, public_key, secret_key, symbol, order_update_callback):
        # Initialize WebSocket connection
        # Handle authentication
        # Listen for order updates
```

**Java** (`BackpackWebSocketManager.java`):
```java
package com.perpdex.exchange.backpack;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.perpdex.model.OrderInfo;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

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
                }

                @Override
                public void onClosing(WebSocket webSocket, int code, String reason) {
                    log.info("WebSocket closing: {}", reason);
                    running = false;
                }
            });
        });
    }

    private void subscribe(WebSocket webSocket) {
        try {
            long timestamp = System.currentTimeMillis();
            String signature = signatureUtil.generateSignature("subscribe", timestamp, 5000);

            JsonNode subscribeMessage = objectMapper.createObjectNode()
                    .put("method", "SUBSCRIBE")
                    .set("params", objectMapper.createArrayNode()
                            .add("account.orderUpdate." + symbol))
                    .set("signature", objectMapper.createArrayNode()
                            .add(publicKey)
                            .add(signature)
                            .add(String.valueOf(timestamp))
                            .add("5000"));

            webSocket.send(objectMapper.writeValueAsString(subscribeMessage));
            log.info("Subscribed to order updates for {}", symbol);

        } catch (Exception e) {
            log.error("Failed to subscribe to WebSocket", e);
        }
    }

    private void handleMessage(String message) {
        try {
            JsonNode data = objectMapper.readTree(message);
            String stream = data.path("stream").asText();

            if (stream.contains("orderUpdate")) {
                handleOrderUpdate(data.path("data"));
            }

        } catch (Exception e) {
            log.error("Failed to handle WebSocket message", e);
        }
    }

    private void handleOrderUpdate(JsonNode orderData) {
        try {
            String eventType = orderData.path("e").asText();
            String orderId = orderData.path("i").asText();
            String symbol = orderData.path("s").asText();
            String side = orderData.path("S").asText();
            String quantity = orderData.path("q").asText();
            String price = orderData.path("p").asText();
            String filledQty = orderData.path("z").asText();

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
                return;
            }

            // Create OrderInfo and call callback
            OrderInfo orderInfo = OrderInfo.builder()
                    .orderId(orderId)
                    .side(orderSide)
                    .size(new java.math.BigDecimal(quantity))
                    .price(new java.math.BigDecimal(price))
                    .status(status)
                    .filledSize(new java.math.BigDecimal(filledQty))
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
}
```

### Step 4: Create Main Backpack Client

**Java** (`BackpackClient.java`):
```java
package com.perpdex.exchange.backpack;

import com.fasterxml.jackson.databind.JsonNode;
import com.perpdex.config.ExchangeConfig;
import com.perpdex.exchange.BaseExchangeClient;
import com.perpdex.model.OrderInfo;
import com.perpdex.model.OrderResult;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Slf4j
public class BackpackClient extends BaseExchangeClient {
    private static final int MAX_RETRIES = 15;

    private final BackpackApiClient apiClient;
    private final String publicKey;
    private final String secretKey;
    private BackpackWebSocketManager wsManager;
    private Consumer<OrderInfo> orderUpdateHandler;

    public BackpackClient(ExchangeConfig config) {
        super(config);

        this.publicKey = config.getApiKey();
        this.secretKey = config.getApiSecret();

        if (publicKey == null || secretKey == null) {
            throw new IllegalArgumentException("Backpack requires API key and secret");
        }

        this.apiClient = new BackpackApiClient(publicKey, secretKey);
    }

    @Override
    protected void validateConfig() {
        if (config.getApiKey() == null || config.getApiSecret() == null) {
            throw new IllegalArgumentException(
                "Missing BACKPACK_PUBLIC_KEY or BACKPACK_SECRET_KEY"
            );
        }
    }

    @Override
    public Mono<Void> connect() {
        return Mono.defer(() -> {
            wsManager = new BackpackWebSocketManager(
                    publicKey,
                    secretKey,
                    config.getContractId(),
                    orderUpdateHandler
            );

            return wsManager.connect()
                    .then(Mono.delay(java.time.Duration.ofSeconds(2)))
                    .then();
        });
    }

    @Override
    public Mono<Void> disconnect() {
        if (wsManager != null) {
            return wsManager.disconnect();
        }
        return Mono.empty();
    }

    @Override
    public Mono<OrderResult> placeOpenOrder(String contractId, BigDecimal quantity, String direction) {
        return placeOpenOrderWithRetry(contractId, quantity, direction, 0);
    }

    private Mono<OrderResult> placeOpenOrderWithRetry(String contractId, BigDecimal quantity,
                                                       String direction, int retryCount) {
        if (retryCount >= MAX_RETRIES) {
            return Mono.just(OrderResult.failure("Max retries exceeded"));
        }

        return fetchBestBidAsk(contractId)
                .flatMap(bidAsk -> {
                    BigDecimal bestBid = bidAsk[0];
                    BigDecimal bestAsk = bidAsk[1];

                    if (bestBid.compareTo(BigDecimal.ZERO) <= 0 || bestAsk.compareTo(BigDecimal.ZERO) <= 0) {
                        return Mono.just(OrderResult.failure("Invalid bid/ask prices"));
                    }

                    BigDecimal orderPrice;
                    String side;

                    if ("buy".equalsIgnoreCase(direction)) {
                        orderPrice = bestAsk.subtract(config.getTickSize());
                        side = "Bid";
                    } else {
                        orderPrice = bestBid.add(config.getTickSize());
                        side = "Ask";
                    }

                    orderPrice = roundToTick(orderPrice);

                    return apiClient.placeOrder(
                            contractId,
                            side,
                            "Limit",
                            quantity,
                            orderPrice,
                            "PostOnly"
                    ).map(response -> {
                        String orderId = response.path("id").asText();
                        String status = response.path("status").asText();

                        return OrderResult.builder()
                                .success(true)
                                .orderId(orderId)
                                .side(direction)
                                .size(quantity)
                                .price(orderPrice)
                                .status(status)
                                .build();
                    }).onErrorResume(error -> {
                        if (error.getMessage().contains("POST_ONLY")) {
                            // Retry on POST_ONLY rejection
                            return Mono.delay(java.time.Duration.ofMillis(100))
                                    .then(placeOpenOrderWithRetry(contractId, quantity, direction, retryCount + 1));
                        }
                        return Mono.just(OrderResult.failure(error.getMessage()));
                    });
                });
    }

    @Override
    public Mono<OrderResult> placeCloseOrder(String contractId, BigDecimal quantity,
                                            BigDecimal price, String side) {
        String backpackSide = "buy".equalsIgnoreCase(side) ? "Bid" : "Ask";

        return apiClient.placeOrder(
                contractId,
                backpackSide,
                "Limit",
                quantity,
                price,
                "PostOnly"
        ).map(response -> OrderResult.builder()
                .success(true)
                .orderId(response.path("id").asText())
                .side(side)
                .size(quantity)
                .price(price)
                .status("OPEN")
                .build())
        .onErrorResume(error -> Mono.just(OrderResult.failure(error.getMessage())));
    }

    @Override
    public Mono<OrderResult> cancelOrder(String orderId) {
        return apiClient.cancelOrder(orderId, config.getContractId())
                .map(response -> OrderResult.builder()
                        .success(true)
                        .orderId(orderId)
                        .build())
                .onErrorResume(error -> Mono.just(OrderResult.failure(error.getMessage())));
    }

    @Override
    public Mono<OrderInfo> getOrderInfo(String orderId) {
        // Implementation depends on Backpack API endpoint for single order
        return Mono.empty();
    }

    @Override
    public Mono<List<OrderInfo>> getActiveOrders(String contractId) {
        return apiClient.getOpenOrders(contractId)
                .map(response -> {
                    List<OrderInfo> orders = new ArrayList<>();
                    JsonNode ordersArray = response.path("orders");

                    ordersArray.forEach(order -> {
                        String side = "Bid".equals(order.path("side").asText()) ? "buy" : "sell";

                        orders.add(OrderInfo.builder()
                                .orderId(order.path("id").asText())
                                .side(side)
                                .size(new BigDecimal(order.path("quantity").asText()))
                                .price(new BigDecimal(order.path("price").asText()))
                                .status(order.path("status").asText())
                                .filledSize(new BigDecimal(order.path("executedQuantity").asText("0")))
                                .build());
                    });

                    return orders;
                });
    }

    @Override
    public Mono<BigDecimal> getAccountPositions() {
        return apiClient.getBalance()
                .map(response -> {
                    // Parse balance/position from response
                    // This depends on Backpack's response format
                    return BigDecimal.ZERO; // Placeholder
                });
    }

    @Override
    public void setupOrderUpdateHandler(Consumer<OrderInfo> handler) {
        this.orderUpdateHandler = handler;
    }

    @Override
    public String getExchangeName() {
        return "backpack";
    }

    @Override
    public boolean isConnected() {
        return wsManager != null;
    }

    /**
     * Fetch best bid and ask prices
     */
    private Mono<BigDecimal[]> fetchBestBidAsk(String symbol) {
        return apiClient.getOrderBook(symbol)
                .map(orderBook -> {
                    JsonNode bids = orderBook.path("bids");
                    JsonNode asks = orderBook.path("asks");

                    BigDecimal bestBid = bids.size() > 0
                            ? new BigDecimal(bids.get(0).get(0).asText())
                            : BigDecimal.ZERO;

                    BigDecimal bestAsk = asks.size() > 0
                            ? new BigDecimal(asks.get(0).get(0).asText())
                            : BigDecimal.ZERO;

                    return new BigDecimal[]{bestBid, bestAsk};
                });
    }
}
```

### Step 5: Update ExchangeFactory

Add to `ExchangeFactory.java`:

```java
import com.perpdex.exchange.backpack.BackpackClient;

static {
    // Update the backpack registration
    registerExchange("backpack", BackpackClient::new);
}
```

## Required Dependencies

Add to `pom.xml`:

```xml
<!-- ED25519 for Backpack signature -->
<dependency>
    <groupId>net.i2p.crypto</groupId>
    <artifactId>eddsa</artifactId>
    <version>0.3.0</version>
</dependency>
```

## Testing

```java
// Test the implementation
ExchangeConfig config = ExchangeConfig.builder()
        .exchangeName("backpack")
        .apiKey(System.getenv("BACKPACK_PUBLIC_KEY"))
        .apiSecret(System.getenv("BACKPACK_SECRET_KEY"))
        .contractId("BTC_USDC")
        .tickSize(new BigDecimal("0.01"))
        .build();

BackpackClient client = new BackpackClient(config);
client.connect().block();

// Place test order
OrderResult result = client.placeOpenOrder("BTC_USDC",
        new BigDecimal("0.001"), "buy").block();
```

## Migration Checklist

- [ ] Create `BackpackSignatureUtil.java`
- [ ] Create `BackpackApiClient.java`
- [ ] Create `BackpackWebSocketManager.java`
- [ ] Create `BackpackClient.java`
- [ ] Add ED25519 dependency to pom.xml
- [ ] Update ExchangeFactory registration
- [ ] Test with real API credentials
- [ ] Add unit tests
- [ ] Add integration tests
- [ ] Document API endpoints used

## File Locations

```
/home/whereq/git/perp-dex-tools-java/
└── src/main/java/com/perpdex/exchange/backpack/
    ├── BackpackClient.java
    ├── BackpackApiClient.java
    ├── BackpackWebSocketManager.java
    └── BackpackSignatureUtil.java
```

## Next Steps

1. Create the directory structure
2. Implement each class step by step
3. Test signature generation first
4. Test REST API calls
5. Test WebSocket connection
6. Integrate with trading bot
7. Add comprehensive error handling
8. Add logging
9. Write tests

This provides a complete path from `backpack.py` to a full Java implementation!
