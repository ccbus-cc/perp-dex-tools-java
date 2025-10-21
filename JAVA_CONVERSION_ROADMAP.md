# Java Conversion Roadmap for Perpetual DEX Trading Bot

## Overview

This document provides a roadmap for converting the Python trading bot (12,177 LOC) to Java, organized by priority and dependency order.

---

## Phase 1: Core Foundation (CRITICAL - Must Complete First)

### 1.1 Base Classes & Interfaces

**Source:** `exchanges/base.py` (129 LOC)

**Java Equivalent:**
- Create `BaseExchangeClient.java` interface
- Create data classes: `OrderResult.java`, `OrderInfo.java`
- Create `@QueryRetry` annotation for retry logic
- Create `RetryConfiguration.java` for retry settings

**Key Conversions:**
```
Python → Java

@dataclass OrderResult → class OrderResult with fields
@dataclass OrderInfo → class OrderInfo with fields
query_retry() decorator → @QueryRetry annotation + RetryInterceptor

async def connect() → CompletableFuture<Void> connect()
async def place_open_order() → CompletableFuture<OrderResult> placeOpenOrder()

Decimal → java.math.BigDecimal
Optional[T] → Optional<T>
```

**Java Libraries Needed:**
- Spring Retry or Tenacity equivalent (consider Failsafe, Polly4j)
- Java async: CompletableFuture, virtual threads (Java 21+), or Reactor

### 1.2 Factory Pattern

**Source:** `exchanges/factory.py` (99 LOC)

**Java Equivalent:**
- Create `ExchangeFactory.java` class
- Use Spring's `@Component` and constructor injection if using Spring
- Implement registry pattern for exchange lookup

**Key Conversions:**
```python
ExchangeFactory.create_exchange(exchange_name, config)
→ ExchangeFactory.createExchange(String exchangeName, ExchangeConfig config)

get_supported_exchanges() → List<String> getSupportedExchanges()
register_exchange() → void registerExchange(String name, Class<? extends BaseExchangeClient> clazz)
```

### 1.3 Configuration Management

**Source:** `.env` file + `env_example.txt`

**Java Equivalent:**
- Create `application.properties` or `application.yml`
- Use Spring Boot `@ConfigurationProperties` or similar
- Create `ExchangeConfig.java`, `TradingConfig.java` dataclasses

**Components:**
```
├── src/main/resources/
│   ├── application.properties
│   ├── application-dev.properties
│   ├── application-prod.properties
│   └── logback-spring.xml
├── src/main/java/
│   └── config/
│       ├── ExchangeProperties.java
│       ├── TradingProperties.java
│       └── ConfigurationLoader.java
```

---

## Phase 2: Exchange Implementations (HIGH PRIORITY)

### 2.1 Start with Simplest Exchange

**Recommendation: Start with Lighter (551 LOC)**

**Why:** 
- Simplest implementation
- Used as hedge exchange for all others
- Learning curve before complex SDKs

**Structure:**
```
src/main/java/com/trading/exchanges/
├── lighter/
│   ├── LighterClient.java (551 LOC)
│   ├── LighterWebSocketManager.java
│   └── LighterConfig.java
```

### 2.2 Parallel: Medium Complexity Exchanges

**Start with these in parallel:**

1. **EdgeX (573 LOC)**
   - Official SDK: Java equivalent available
   - Need: Stark key signing

2. **Backpack (595 LOC + 662 LOC bp_client.py)**
   - ED25519 signing
   - WebSocket auth more complex
   - Consider: ~1,257 LOC total

3. **Aster (770 LOC)**
   - Similar complexity to Backpack
   - Good for learning pattern

### 2.3 Complex Exchanges (Do After 3-4 Simple Ones)

1. **Paradex (669 LOC)** - Starknet integration
2. **Extended (770 LOC)** - Complex Starknet 
3. **GRVT (539 LOC)** - SDK dependency
4. **Apex (~550 LOC)** - Omni integration

### 2.4 Exchange Implementation Template

```java
// File: src/main/java/com/trading/exchanges/{ExchangeName}Client.java

public class {ExchangeName}Client extends BaseExchangeClient {
    
    private static final Logger logger = LoggerFactory.getLogger({ExchangeName}Client.class);
    
    // Configuration
    private final {ExchangeName}Config config;
    private final ExchangeProperties properties;
    
    // External clients
    private final {ExchangeName}ApiClient apiClient;
    private final {ExchangeName}WebSocketManager wsManager;
    
    // State
    private OrderUpdateHandler orderUpdateHandler;
    
    public {ExchangeName}Client(ExchangeProperties props, {ExchangeName}Config cfg) {
        super(cfg);
        this.config = cfg;
        this.properties = props;
        this.apiClient = new {ExchangeName}ApiClient(cfg);
        this.wsManager = new {ExchangeName}WebSocketManager(cfg);
    }
    
    @Override
    public CompletableFuture<Void> connect() {
        // Implementation
    }
    
    @Override
    public CompletableFuture<OrderResult> placeOpenOrder(
        String contractId, BigDecimal quantity, String direction) {
        // Implementation
    }
    
    @Override
    public CompletableFuture<OrderResult> placeCloseOrder(
        String contractId, BigDecimal quantity, BigDecimal price, String side) {
        // Implementation
    }
    
    // ... other abstract methods
}
```

---

## Phase 3: Core Trading Bot

### 3.1 Configuration Classes

**Source:** `trading_bot.py` (TradingConfig, OrderMonitor)

**Java Equivalent:**
```
src/main/java/com/trading/bot/
├── config/
│   ├── TradingConfig.java
│   ├── OrderMonitor.java
│   └── TradingParameters.java
└── ...
```

### 3.2 Trading Bot Engine

**Source:** `trading_bot.py` (Core TradingBot class)

**Java Equivalent:**
```
src/main/java/com/trading/bot/
├── TradingBot.java
├── OrderManagement.java
├── PositionTracker.java
├── WebSocketManager.java
└── GracefulShutdown.java
```

**Key Methods to Implement:**
```java
public class TradingBot {
    
    public CompletableFuture<Void> run()
    
    public CompletableFuture<OrderResult> placeOpenOrder()
    
    public CompletableFuture<Void> monitorOpenOrder()
    
    public CompletableFuture<OrderResult> placeCloseOrder()
    
    public CompletableFuture<Void> gracefulShutdown(String reason)
    
    // Event handlers
    private void onOrderFilled(OrderInfo orderInfo)
    
    private void onOrderCanceled(OrderInfo orderInfo)
}
```

---

## Phase 4: Hedge Mode

### 4.1 Hedge Mode Base

**Source:** `hedge/` directory (4,604 LOC total)

**Structure:**
```
src/main/java/com/trading/hedge/
├── HedgeBot.java (Abstract)
├── HedgeConfiguration.java
├── PositionManager.java
└── implementations/
    ├── BackpackLighterHedgeBot.java (1189 LOC equivalent)
    ├── ExtendedLighterHedgeBot.java (1234 LOC equivalent)
    ├── ApexLighterHedgeBot.java (1104 LOC equivalent)
    └── GrvtLighterHedgeBot.java (1077 LOC equivalent)
```

### 4.2 Hedge Mode Pattern

```java
public abstract class HedgeBot {
    
    protected final String ticker;
    protected final BigDecimal orderQuantity;
    protected final int fillTimeout;
    protected final int iterations;
    
    protected BaseExchangeClient primaryExchange;
    protected LighterClient lighterExchange;
    
    public abstract CompletableFuture<Void> run();
    
    protected CompletableFuture<Void> executeSingleCycle() {
        // 1. Place maker order on primary exchange
        // 2. Wait for fill with timeout
        // 3. Hedge with market order on Lighter
        // 4. Place close order on primary
        // 5. Close hedge on Lighter
    }
}
```

---

## Phase 5: Helper Modules & CLI

### 5.1 Logging

**Source:** `helpers/logger.py` (112 LOC)

**Java Equivalent:**
```
src/main/java/com/trading/logging/
├── TradingLogger.java
├── CsvTradeRecorder.java
└── src/main/resources/logback-spring.xml
```

**Conversion Notes:**
- Use SLF4J + Logback
- OpenCSV for CSV writing
- Timezone handling with ZonedDateTime

### 5.2 Notifications

**Source:** `helpers/telegram_bot.py`, `helpers/lark_bot.py`

**Java Equivalent:**
```
src/main/java/com/trading/notifications/
├── NotificationService.java (Interface)
├── TelegramNotificationService.java
├── LarkNotificationService.java
├── NotificationFactory.java
└── NotificationProperties.java
```

### 5.3 CLI Entry Points

**Source:** `runbot.py`, `hedge_mode.py`

**Java Equivalents:**
```
src/main/java/com/trading/cli/
├── TradingBotCli.java (Spring Boot main)
├── TradingBotCommand.java
├── HedgeModeCli.java
├── HedgeModeCommand.java
└── CliArguments.java (for Picocli)
```

**CLI Library:** Use **Picocli** for argument parsing

```java
@Command(
    name = "trading-bot",
    description = "Automated trading bot for DEXs",
    subcommands = {TradingBotCommand.class, HedgeModeCommand.class}
)
public class TradingBotCli {
    public static void main(String[] args) {
        int exitCode = new CommandLine(new TradingBotCli())
            .execute(args);
        System.exit(exitCode);
    }
}
```

### 5.4 Testing

**Source:** `tests/test_query_retry.py` (72 LOC)

**Java Equivalent:**
```
src/test/java/com/trading/
├── exchanges/
│   └── BaseExchangeClientTest.java
├── bot/
│   ├── TradingBotTest.java
│   └── RetryLogicTest.java
└── integration/
    └── ExchangeIntegrationTest.java
```

**Testing Framework:** JUnit 5 + Mockito + TestContainers

---

## File Structure Plan (Target Java Project)

```
perp-dex-trading-bot-java/
├── pom.xml (or build.gradle)
├── src/
│   ├── main/
│   │   ├── java/com/trading/
│   │   │   ├── Application.java (Spring Boot main)
│   │   │   ├── exchanges/
│   │   │   │   ├── BaseExchangeClient.java
│   │   │   │   ├── OrderResult.java
│   │   │   │   ├── OrderInfo.java
│   │   │   │   ├── ExchangeFactory.java
│   │   │   │   ├── edgex/
│   │   │   │   ├── backpack/
│   │   │   │   ├── aster/
│   │   │   │   ├── paradex/
│   │   │   │   ├── lighter/
│   │   │   │   ├── extended/
│   │   │   │   ├── grvt/
│   │   │   │   └── apex/
│   │   │   ├── bot/
│   │   │   │   ├── TradingBot.java
│   │   │   │   ├── TradingConfig.java
│   │   │   │   ├── OrderMonitor.java
│   │   │   │   ├── OrderManagement.java
│   │   │   │   └── PositionTracker.java
│   │   │   ├── hedge/
│   │   │   │   ├── HedgeBot.java
│   │   │   │   ├── implementations/
│   │   │   │   │   ├── BackpackLighterHedgeBot.java
│   │   │   │   │   ├── ExtendedLighterHedgeBot.java
│   │   │   │   │   ├── ApexLighterHedgeBot.java
│   │   │   │   │   └── GrvtLighterHedgeBot.java
│   │   │   │   └── PositionManager.java
│   │   │   ├── cli/
│   │   │   │   ├── TradingBotCommand.java
│   │   │   │   ├── HedgeModeCommand.java
│   │   │   │   └── CliArguments.java
│   │   │   ├── config/
│   │   │   │   ├── ExchangeProperties.java
│   │   │   │   ├── TradingProperties.java
│   │   │   │   └── ConfigurationLoader.java
│   │   │   ├── logging/
│   │   │   │   ├── TradingLogger.java
│   │   │   │   └── CsvTradeRecorder.java
│   │   │   ├── notifications/
│   │   │   │   ├── NotificationService.java
│   │   │   │   ├── TelegramNotificationService.java
│   │   │   │   ├── LarkNotificationService.java
│   │   │   │   └── NotificationFactory.java
│   │   │   └── retry/
│   │   │       ├── QueryRetry.java (annotation)
│   │   │       ├── RetryInterceptor.java
│   │   │       └── RetryConfiguration.java
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── application-dev.properties
│   │       ├── application-prod.properties
│   │       └── logback-spring.xml
│   └── test/
│       └── java/com/trading/
│           ├── exchanges/
│           ├── bot/
│           └── integration/
├── docs/
│   ├── ARCHITECTURE.md
│   ├── EXCHANGE_IMPLEMENTATION_GUIDE.md
│   └── TRADING_STRATEGY_DOCS.md
└── README.md
```

---

## Dependency Management (Maven pom.xml)

```xml
<properties>
    <java.version>17</java.version>
    <spring-boot.version>3.2.0</spring-boot.version>
</properties>

<dependencies>
    <!-- Spring Boot -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter</artifactId>
    </dependency>
    
    <!-- Async & Reactive -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webflux</artifactId>
    </dependency>
    
    <!-- CLI Parsing -->
    <dependency>
        <groupId>info.picocli</groupId>
        <artifactId>picocli-spring-boot-starter</artifactId>
        <version>4.7.5</version>
    </dependency>
    
    <!-- Retry Logic -->
    <dependency>
        <groupId>dev.failsafe</groupId>
        <artifactId>failsafe</artifactId>
        <version>3.3.2</version>
    </dependency>
    
    <!-- WebSocket -->
    <dependency>
        <groupId>org.java-websocket</groupId>
        <artifactId>Java-WebSocket</artifactId>
        <version>1.5.3</version>
    </dependency>
    
    <!-- JSON Processing -->
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
    </dependency>
    
    <!-- CSV -->
    <dependency>
        <groupId>org.apache.commons</groupId>
        <artifactId>commons-csv</artifactId>
        <version>1.10.0</version>
    </dependency>
    
    <!-- Cryptography -->
    <dependency>
        <groupId>org.bouncycastle</groupId>
        <artifactId>bcprov-jdk15on</artifactId>
        <version>1.70</version>
    </dependency>
    
    <!-- HTTP Client -->
    <dependency>
        <groupId>org.apache.httpcomponents.client5</groupId>
        <artifactId>httpclient5</artifactId>
    </dependency>
    
    <!-- Logging -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-logging</artifactId>
    </dependency>
    
    <!-- Testing -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.mockito</groupId>
        <artifactId>mockito-core</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

---

## Implementation Timeline Estimate

| Phase | Component | Est. LOC | Est. Hours | Priority |
|-------|-----------|----------|-----------|----------|
| **1** | BaseClient + Factory | 500 | 20 | CRITICAL |
| **1** | Config Management | 300 | 12 | CRITICAL |
| **2a** | Lighter Exchange | 551 | 30 | HIGH |
| **2b** | EdgeX Exchange | 573 | 35 | HIGH |
| **2b** | Backpack Exchange | 1,257 | 50 | HIGH |
| **2c** | Aster/Paradex/Extended | 2,109 | 80 | MEDIUM |
| **2d** | GRVT/Apex | 1,089 | 50 | MEDIUM |
| **3** | Trading Bot Core | 2,500 | 100 | HIGH |
| **4** | Hedge Mode | 4,604 | 150 | MEDIUM |
| **5a** | Logging/Notifications | 300 | 20 | MEDIUM |
| **5b** | CLI & Integration | 500 | 25 | MEDIUM |
| **5c** | Testing Suite | 800 | 40 | LOW |
| **6** | Documentation | - | 30 | MEDIUM |
| **TOTAL** | | ~15,500 | ~662 | |

**Estimated Timeline:**
- Phases 1-2a: 2 weeks (foundation + first exchange)
- Phases 2b-3: 3-4 weeks (core exchanges + bot)
- Phase 4: 2-3 weeks (hedge mode)
- Phase 5-6: 2 weeks (polish + docs)

**Total: 10-12 weeks with dedicated developer**

---

## Architecture Decisions

### 1. Async Framework
**Recommendation: Spring WebFlux + Project Reactor**
- Native async support
- Better than CompletableFuture for complex chains
- Good WebSocket support

### 2. Configuration
**Recommendation: Spring Boot Properties + Typesafe Config**
- Instead of .env files, use `application.yml`
- Better environment management
- Type-safe property injection

### 3. Dependency Injection
**Recommendation: Spring Dependency Injection**
- Factory pattern becomes bean creation
- Cleaner code
- Better testability

### 4. Exchange SDK Integration
**Challenges:**
- Not all exchanges have Java SDKs
- May need to use REST APIs + custom WebSocket
- Some Python SDKs need wrapping via REST calls

### 5. Financial Types
**Recommendation: BigDecimal everywhere**
- Always use `java.math.BigDecimal` for prices/quantities
- Never use `double` or `float` for financial calculations
- Use `@Validated` for precision validation

---

## Risk Mitigation

1. **SDK Availability**
   - Risk: Not all exchanges have Java SDKs
   - Mitigation: Use REST APIs as fallback, write API wrappers

2. **WebSocket Complexity**
   - Risk: Each exchange has different WebSocket format
   - Mitigation: Abstract WebSocket layer, create common interface

3. **Cryptography**
   - Risk: Different signing algorithms per exchange (ED25519, ECDSA, etc.)
   - Mitigation: Create `CryptoProvider` interface, implement per exchange

4. **Testing**
   - Risk: Hard to test with real exchanges
   - Mitigation: Use testnet environments, mock exchange implementations

---

## Success Criteria

1. All 8 exchanges functional in Java
2. Grid trading strategy working
3. Hedge mode working (all 4 variants)
4. CLI interface identical to Python version
5. Performance comparable to Python
6. Comprehensive test coverage (>70%)
7. Complete documentation
8. Seamless migration path from Python

---

## Next Steps

1. **Immediate:** Set up Maven/Gradle project structure
2. **Week 1:** Implement Phase 1 (base classes, config, factory)
3. **Week 2:** Implement Lighter exchange (simplest)
4. **Week 3:** Implement EdgeX and Backpack
5. **Week 4+:** Continue with other exchanges
6. **Later:** Core bot, hedge mode, CLI, testing, docs

