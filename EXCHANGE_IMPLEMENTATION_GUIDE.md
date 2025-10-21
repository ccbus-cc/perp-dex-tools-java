## Complete Exchange Implementation Guide

This guide provides the complete implementation strategy for all 8 exchanges.

### Implementation Status

| Exchange  | Status | Files Created | Notes |
|-----------|--------|---------------|-------|
| Backpack  | ✅ COMPLETE | 4 files | Fully implemented with REST API + WebSocket |
| Lighter   | 🔧 TEMPLATE | 1 file | Skeleton created, needs API implementation |
| EdgeX     | 🔧 TEMPLATE | 1 file | Skeleton created, needs API implementation |
| Paradex   | 🔧 TEMPLATE | 1 file | Skeleton created, needs API implementation |
| Aster     | 🔧 TEMPLATE | 1 file | Skeleton created, needs API implementation |
| GRVT      | 🔧 TEMPLATE | 1 file | Skeleton created, needs API implementation |
| Extended  | 🔧 TEMPLATE | 1 file | Skeleton created, needs API implementation |
| Apex      | 🔧 TEMPLATE | 1 file | Skeleton created, needs API implementation |

### Backpack Implementation (✅ COMPLETE)

**Files:**
```
src/main/java/com/perpdex/exchange/backpack/
├── BackpackClient.java              (Main client - DONE)
├── BackpackApiClient.java           (REST API - DONE)
├── BackpackWebSocketManager.java    (WebSocket - DONE)
└── BackpackSignatureUtil.java       (Signatures - DONE)
```

**Features Implemented:**
- ✅ ED25519/HMAC signature generation
- ✅ REST API for order placement, cancellation, queries
- ✅ WebSocket for real-time order updates
- ✅ Retry logic for POST_ONLY rejections
- ✅ Order book fetching
- ✅ Position tracking
- ✅ Complete integration with BaseExchangeClient

### Implementation Pattern for Remaining Exchanges

Each exchange should follow this structure:

```
src/main/java/com/perpdex/exchange/{exchange}/
├── {Exchange}Client.java           (Main client extending BaseExchangeClient)
├── {Exchange}ApiClient.java        (REST API wrapper)
├── {Exchange}WebSocketManager.java (WebSocket handler - optional)
└── {Exchange}SignatureUtil.java    (Auth/signatures - if needed)
```

### Step-by-Step Implementation Process

#### Step 1: Read Python Implementation
```bash
# Location of Python files
/home/whereq/git/perp-dex-tools-java/exchanges/{exchange}.py
```

#### Step 2: Identify API Endpoints
From the Python code, identify:
- Base URL
- Authentication method (API key, signature, etc.)
- Order placement endpoint
- Order cancellation endpoint
- Order query endpoint
- Position query endpoint
- WebSocket URL (if applicable)

#### Step 3: Create Java Implementation

**Main Client Template:**
```java
package com.perpdex.exchange.{exchange};

import com.perpdex.config.ExchangeConfig;
import com.perpdex.exchange.BaseExchangeClient;
import com.perpdex.model.*;
import reactor.core.publisher.Mono;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Consumer;

public class {Exchange}Client extends BaseExchangeClient {
    private final {Exchange}ApiClient apiClient;
    private Consumer<OrderInfo> orderUpdateHandler;

    public {Exchange}Client(ExchangeConfig config) {
        super(config);
        // Initialize API client
        this.apiClient = new {Exchange}ApiClient(
            config.getApiKey(),
            config.getApiSecret()
        );
    }

    @Override
    protected void validateConfig() {
        // Validate required config
    }

    @Override
    public Mono<Void> connect() {
        // Initialize WebSocket if needed
        return Mono.empty();
    }

    @Override
    public Mono<Void> disconnect() {
        // Cleanup
        return Mono.empty();
    }

    @Override
    public Mono<OrderResult> placeOpenOrder(String contractId, BigDecimal quantity, String direction) {
        // Implement order placement
        return Mono.just(OrderResult.failure("Not implemented"));
    }

    @Override
    public Mono<OrderResult> placeCloseOrder(String contractId, BigDecimal quantity, BigDecimal price, String side) {
        // Implement close order
        return Mono.just(OrderResult.failure("Not implemented"));
    }

    @Override
    public Mono<OrderResult> cancelOrder(String orderId) {
        // Implement cancellation
        return Mono.just(OrderResult.failure("Not implemented"));
    }

    @Override
    public Mono<OrderInfo> getOrderInfo(String orderId) {
        // Implement order query
        return Mono.empty();
    }

    @Override
    public Mono<List<OrderInfo>> getActiveOrders(String contractId) {
        // Implement active orders query
        return Mono.just(new ArrayList<>());
    }

    @Override
    public Mono<BigDecimal> getAccountPositions() {
        // Implement position query
        return Mono.just(BigDecimal.ZERO);
    }

    @Override
    public void setupOrderUpdateHandler(Consumer<OrderInfo> handler) {
        this.orderUpdateHandler = handler;
    }

    @Override
    public String getExchangeName() {
        return "{exchange}";
    }

    @Override
    public boolean isConnected() {
        return true; // Update based on actual connection status
    }
}
```

### Exchange-Specific Details

#### 1. Lighter Exchange
- **Base URL**: `https://mainnet.zklighter.elliot.ai`
- **Auth**: Private key based (ED25519)
- **WebSocket**: Custom implementation required
- **Python SDK**: Uses `lighter` Python package
- **Key Features**:
  - Account index and API key index
  - Market multipliers for quantities
  - Post-only orders

**Implementation Notes:**
- Need to implement Lighter SDK equivalent in Java
- WebSocket requires custom message handling
- Signature generation uses ED25519

#### 2. EdgeX Exchange
- **Base URL**: `https://pro.edgex.exchange`
- **Auth**: API key + secret
- **Python SDK**: Custom implementation
- **Key Features**:
  - Standard REST API
  - WebSocket for order updates

#### 3. Paradex Exchange
- **Base URL**: `https://api.paradex.trade`
- **Auth**: Starknet signature
- **Python SDK**: Uses `paradex-py`
- **Key Features**:
  - Starknet-based (requires Starknet libraries)
  - Complex signature scheme

**Special Requirement:**
- Need Java Starknet library or implement signing manually

#### 4. Aster Exchange
- **Base URL**: `https://api.asterdex.com`
- **Auth**: API key + secret
- **Key Features**:
  - Standard REST API
  - Position tracking

#### 5. GRVT Exchange
- **Base URL**: `https://api.grvt.io`
- **Auth**: API key
- **Python SDK**: Uses `grvt` package
- **Key Features**:
  - Trading competition support
  - Referral tracking

#### 6. Extended Exchange
- **Base URL**: `https://api.extended.exchange`
- **Auth**: Private key
- **Key Features**:
  - Similar to other DEX platforms
  - Custom order handling

#### 7. Apex Exchange
- **Base URL**: `https://pro.apex.exchange`
- **Auth**: API key + signature
- **Python SDK**: Custom client
- **Key Features**:
  - Omni protocol support
  - Hedge mode support

### Common Implementation Patterns

#### Authentication Signatures

**HMAC-SHA256 (Most Common):**
```java
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public String generateHmacSignature(String message, String secretKey) {
    Mac mac = Mac.getInstance("HmacSHA256");
    SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(), "HmacSHA256");
    mac.init(keySpec);
    byte[] signature = mac.doFinal(message.getBytes());
    return Base64.getEncoder().encodeToString(signature);
}
```

**ED25519 (Lighter, some others):**
```java
// Requires: net.i2p.crypto:eddsa dependency
import net.i2p.crypto.eddsa.*;

public String generateEd25519Signature(String message, byte[] privateKey) {
    EdDSAParameterSpec spec = EdDSANamedCurveTable.getByName("Ed25519");
    EdDSAPrivateKey key = new EdDSAPrivateKey(new EdDSAPrivateKeySpec(privateKey, spec));
    EdDSAEngine engine = new EdDSAEngine();
    engine.initSign(key);
    byte[] signature = engine.signOneShot(message.getBytes());
    return Base64.getEncoder().encodeToString(signature);
}
```

#### WebSocket Pattern

```java
import okhttp3.*;

public class ExchangeWebSocketManager {
    private WebSocket webSocket;

    public Mono<Void> connect(String wsUrl) {
        return Mono.fromRunnable(() -> {
            OkHttpClient client = new OkHttpClient.Builder()
                    .readTimeout(0, TimeUnit.MILLISECONDS)
                    .pingInterval(30, TimeUnit.SECONDS)
                    .build();

            Request request = new Request.Builder().url(wsUrl).build();

            webSocket = client.newWebSocket(request, new WebSocketListener() {
                @Override
                public void onOpen(WebSocket webSocket, Response response) {
                    // Handle connection
                }

                @Override
                public void onMessage(WebSocket webSocket, String text) {
                    // Handle message
                }

                @Override
                public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                    // Handle error
                }
            });
        });
    }
}
```

#### REST API Pattern

```java
import okhttp3.*;
import reactor.core.publisher.Mono;

public Mono<JsonNode> makeRequest(String url, String method, JsonNode body) {
    return Mono.fromCallable(() -> {
        Request.Builder builder = new Request.Builder().url(url);

        if (method.equals("POST")) {
            RequestBody requestBody = RequestBody.create(
                body.toString(),
                MediaType.get("application/json")
            );
            builder.post(requestBody);
        } else {
            builder.get();
        }

        // Add auth headers
        builder.addHeader("X-API-Key", apiKey);
        builder.addHeader("X-Signature", generateSignature(...));

        try (Response response = httpClient.newCall(builder.build()).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Request failed: " + response.code());
            }
            return objectMapper.readTree(response.body().string());
        }
    });
}
```

### Testing Strategy

1. **Unit Tests** - Test each exchange client independently
2. **Integration Tests** - Test against exchange testnets if available
3. **Mock Tests** - Use MockWebServer for HTTP/WebSocket testing

### Priority Implementation Order

Based on complexity and Python code:

1. ✅ **Backpack** - COMPLETE (reference implementation)
2. 🔧 **Lighter** - Medium complexity, important for hedge mode
3. 🔧 **EdgeX** - Simpler REST API
4. 🔧 **Extended** - Similar to EdgeX
5. 🔧 **Apex** - More complex, hedge mode support
6. 🔧 **Aster** - Standard implementation
7. 🔧 **GRVT** - Standard implementation
8. 🔧 **Paradex** - Most complex (Starknet signatures)

### Next Steps

1. Review Backpack implementation as reference
2. Choose next exchange (recommend Lighter for hedge mode)
3. Read Python implementation
4. Create Java equivalent following the pattern
5. Test with exchange API
6. Repeat for remaining exchanges

### Resources

- **Backpack API**: https://docs.backpack.exchange/
- **OkHttp Docs**: https://square.github.io/okhttp/
- **Project Reactor**: https://projectreactor.io/docs
- **Jackson JSON**: https://github.com/FasterXML/jackson

### Conclusion

Backpack is fully implemented and serves as the template for all other exchanges. The pattern is:
1. Signature/Auth utility
2. REST API client
3. WebSocket manager (if needed)
4. Main client extending BaseExchangeClient

Follow this pattern for each remaining exchange!
