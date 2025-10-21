# Migration Guide: Python to Java

This guide helps you migrate from the Python version to the Java version of Perp DEX Tools.

## Why Migrate to Java?

### Advantages

1. **Type Safety**: Compile-time type checking prevents many runtime errors
2. **Performance**: Better concurrency handling and memory management
3. **Reactive Programming**: Non-blocking I/O with Project Reactor for better resource utilization
4. **Enterprise Ready**: Robust error handling, logging, and monitoring
5. **IDE Support**: Better code completion, refactoring, and debugging tools
6. **Maintainability**: Cleaner architecture with dependency injection

### Trade-offs

1. **Setup**: Requires Java/Maven setup (vs Python/pip)
2. **Verbosity**: Java code is more verbose than Python
3. **Learning Curve**: If unfamiliar with Java ecosystem

## Architecture Comparison

### Python Version

```
trading_bot.py              → Core trading logic
exchanges/
  ├── base.py              → Base exchange interface
  ├── factory.py           → Exchange factory
  └── lighter.py           → Lighter implementation
helpers/
  ├── logger.py            → Trading logger
  └── telegram_bot.py      → Notifications
runbot.py                  → Entry point
```

### Java Version

```
src/main/java/com/perpdex/
├── bot/
│   ├── TradingBot.java            → Core trading logic (Reactive)
│   └── TradingConfig.java         → Configuration model
├── exchange/
│   ├── BaseExchangeClient.java    → Base exchange interface
│   ├── ExchangeFactory.java       → Exchange factory (Registry pattern)
│   └── (implementations)          → Exchange implementations
├── helper/
│   ├── TradingLogger.java         → Trading logger
│   └── NotificationService.java   → Notifications (Telegram + Lark)
├── model/
│   ├── OrderResult.java           → Order result model
│   ├── OrderInfo.java             → Order info model
│   └── (enums)                    → Type-safe enums
├── config/
│   ├── ExchangeConfig.java        → Exchange configuration
│   └── ConfigLoader.java          → Environment loader
└── PerpDexToolsApplication.java   → CLI entry point
```

## Key Differences

### 1. Asynchronous Programming

**Python (asyncio)**:
```python
async def place_order(self, contract_id, quantity, direction):
    result = await self.exchange_client.place_open_order(
        contract_id, quantity, direction
    )
    return result
```

**Java (Project Reactor)**:
```java
public Mono<OrderResult> placeOrder(String contractId, BigDecimal quantity, String direction) {
    return exchangeClient.placeOpenOrder(contractId, quantity, direction);
}
```

### 2. Configuration

**Python**:
```python
# Environment variables loaded automatically
api_key = os.getenv('API_KEY_PRIVATE_KEY')
```

**Java**:
```java
// Explicit configuration loading
ExchangeConfig config = ConfigLoader.loadExchangeConfig("lighter");
String apiKey = config.getPrivateKey();
```

### 3. Type System

**Python (dynamic typing)**:
```python
@dataclass
class OrderResult:
    success: bool
    order_id: Optional[str] = None
    price: Optional[Decimal] = None
```

**Java (static typing)**:
```java
@Data
@Builder
public class OrderResult {
    private boolean success;
    private String orderId;      // Can be null
    private BigDecimal price;    // Can be null
}
```

### 4. Decimal Handling

**Python**:
```python
from decimal import Decimal
price = Decimal("100.50")
```

**Java**:
```java
import java.math.BigDecimal;
BigDecimal price = new BigDecimal("100.50");
```

### 5. Logging

**Python**:
```python
self.logger.log(f"Order placed: {order_id}", "INFO")
```

**Java**:
```java
logger.log(String.format("Order placed: %s", orderId), "INFO");
// or
logger.info("Order placed: " + orderId);
```

## Migration Steps

### Step 1: Environment Setup

1. **Install Java 21+**
```bash
# Ubuntu/Debian
sudo apt-get install openjdk-21-jdk

# macOS
brew install openjdk@21
```

2. **Install Maven**
```bash
# Ubuntu/Debian
sudo apt-get install maven

# macOS
brew install maven
```

3. **Verify Installation**
```bash
java -version  # Should show 21 or higher
mvn -version   # Should show 3.8 or higher
```

### Step 2: Configuration Migration

1. **Copy your .env file** (most variables are compatible)
```bash
cp .env .env.backup  # Backup Python env
# Edit .env for Java (mostly the same)
```

2. **Check variable names** (most are identical):
```bash
# Python and Java both use:
API_KEY_PRIVATE_KEY
LIGHTER_ACCOUNT_INDEX
TELEGRAM_BOT_TOKEN
# etc.
```

### Step 3: Command Migration

**Python**:
```bash
python runbot.py --exchange lighter --ticker BTC-PERP --quantity 0.001 --direction buy
```

**Java**:
```bash
./run.sh run --exchange lighter --ticker BTC-PERP --quantity 0.001 --direction buy
```

### Step 4: Testing

1. **Build the project**
```bash
./build.sh
```

2. **Test with list command**
```bash
./run.sh list-exchanges
```

3. **Run with small quantities**
```bash
./run.sh run --exchange lighter --ticker BTC-PERP --quantity 0.0001 --direction buy
```

4. **Monitor logs**
```bash
tail -f logs/lighter_BTC-PERP_activity.log
```

## Feature Parity

### ✅ Implemented

- [x] Grid trading strategy
- [x] Multi-exchange support (factory pattern)
- [x] Order placement and monitoring
- [x] Take profit calculation
- [x] Position tracking
- [x] Logging (activity + CSV)
- [x] Notifications (Telegram + Lark)
- [x] CLI interface
- [x] Environment configuration
- [x] Retry logic
- [x] Graceful shutdown

### 🚧 In Progress

- [ ] Complete exchange implementations (8 exchanges)
  - Python uses exchange-specific SDKs
  - Java needs REST/WebSocket implementations
- [ ] Hedge mode
- [ ] Boost mode
- [ ] WebSocket order updates

### 📋 Planned

- [ ] Unit tests
- [ ] Integration tests
- [ ] Performance benchmarks
- [ ] Docker support

## Exchange Implementation Status

| Exchange  | Python | Java Status |
|-----------|--------|-------------|
| Lighter   | ✅     | 🔧 Skeleton |
| EdgeX     | ✅     | 🔧 Skeleton |
| Backpack  | ✅     | 🔧 Skeleton |
| Paradex   | ✅     | 🔧 Skeleton |
| Aster     | ✅     | 🔧 Skeleton |
| GRVT      | ✅     | 🔧 Skeleton |
| Extended  | ✅     | 🔧 Skeleton |
| Apex      | ✅     | 🔧 Skeleton |

**Note**: Exchange implementations require REST API and WebSocket integration. The Python version uses exchange-specific Python SDKs, while the Java version needs direct API implementations.

## Common Issues

### Issue 1: Exchange SDK Dependencies

**Problem**: Python uses official exchange SDKs (e.g., `lighter` Python package)

**Solution**: Java version needs to:
- Implement REST API calls directly using OkHttp
- Implement WebSocket connections using OkHttp or Spring WebSocket
- Parse JSON responses using Jackson

### Issue 2: Async/Await vs Reactive

**Problem**: Different concurrency models

**Solution**:
- Python's `async/await` → Java's `Mono<T>` and `Flux<T>`
- `asyncio.create_task()` → `Mono.subscribeOn(Schedulers.parallel())`
- `await asyncio.sleep()` → `Mono.delay(Duration)`

### Issue 3: Dynamic vs Static Typing

**Problem**: Python allows dynamic types

**Solution**:
- Use generics where appropriate
- Use `Optional<T>` for nullable values
- Create type-safe enums for constants

## Performance Comparison

| Metric | Python | Java |
|--------|--------|------|
| Startup time | Fast (~1s) | Medium (~3s) |
| Memory usage | Low | Medium-High |
| Concurrency | Good (asyncio) | Excellent (Virtual Threads, Reactor) |
| Type safety | Runtime | Compile-time |
| Hot reload | Yes (edit & run) | No (requires rebuild) |

## Best Practices

### 1. Use Builder Pattern

```java
TradingConfig config = TradingConfig.builder()
    .ticker("BTC-PERP")
    .quantity(new BigDecimal("0.001"))
    .direction("buy")
    .build();
```

### 2. Handle Monos Properly

```java
// ❌ Bad - blocking
OrderResult result = mono.block();

// ✅ Good - reactive chain
mono.flatMap(result -> processResult(result))
    .subscribe();
```

### 3. Use Lombok

```java
@Data
@Builder
public class MyClass {
    // Automatic getters, setters, builder
}
```

## FAQ

**Q: Can I run both Python and Java versions simultaneously?**

A: Yes, but they should use different log files (set different `ACCOUNT_NAME`).

**Q: Will the Java version have feature parity?**

A: Yes, eventually. Core trading logic is complete. Exchange implementations are in progress.

**Q: Is the Java version production-ready?**

A: The framework is production-ready, but exchange implementations need completion and testing.

**Q: Can I contribute?**

A: Yes! Exchange implementations, tests, and documentation are welcome.

## Resources

- [Java Documentation](https://docs.oracle.com/en/java/)
- [Project Reactor Guide](https://projectreactor.io/docs)
- [Spring WebFlux](https://docs.spring.io/spring-framework/reference/web/webflux.html)
- [Maven Guide](https://maven.apache.org/guides/)

## Support

- Java-specific issues: GitHub Issues
- General trading questions: Original Python project
- Exchange API questions: Exchange documentation
