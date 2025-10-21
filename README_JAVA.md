# Perp DEX Tools - Java Version

A modular trading bot for perpetual DEX platforms, implemented in Java with modern tech stack and best practices.

## Features

- **Multi-Exchange Support**: EdgeX, Backpack, Paradex, Aster, Lighter, GRVT, Extended, Apex
- **Grid Trading Strategy**: Automated order placement and profit-taking
- **Hedge Mode**: Pair trading across two exchanges
- **Reactive Architecture**: Built with Spring WebFlux and Project Reactor for efficient async operations
- **Comprehensive Logging**: Structured logging with CSV transaction logs
- **Notifications**: Telegram and Lark (Feishu) notifications for alerts
- **CLI Interface**: Easy-to-use command-line interface powered by Picocli
- **Retry Logic**: Robust error handling with automatic retries using Failsafe
- **Modern Java**: Built with Java 21, leveraging latest language features

## Tech Stack

- **Java 21**: Latest LTS version with modern language features
- **Spring Boot 3.3.5**: Framework for dependency injection and configuration
- **Spring WebFlux**: Reactive programming support
- **Project Reactor**: Reactive streams implementation
- **OkHttp**: HTTP client for API communications
- **Jackson**: JSON processing
- **Picocli**: CLI framework
- **Failsafe**: Resilience library for retry logic
- **Lombok**: Reduces boilerplate code
- **SLF4J + Logback**: Logging framework
- **Maven**: Build and dependency management

## Requirements

- Java 21 or higher
- Maven 3.8+
- Valid API credentials for your chosen exchange(s)

## Installation

### 1. Clone the Repository

```bash
git clone <repository-url>
cd perp-dex-tools-java
```

### 2. Build the Project

```bash
mvn clean package
```

This will create an executable JAR file in the `target` directory.

### 3. Set Up Environment Variables

Create a `.env` file or export environment variables:

```bash
# Common settings
export TIMEZONE=Asia/Shanghai
export ACCOUNT_NAME=my_account

# Exchange API credentials (example for Lighter)
export API_KEY_PRIVATE_KEY=your_private_key_here
export LIGHTER_ACCOUNT_INDEX=0
export LIGHTER_API_KEY_INDEX=0

# Notification settings (optional)
export TELEGRAM_BOT_TOKEN=your_telegram_bot_token
export TELEGRAM_CHAT_ID=your_telegram_chat_id
export LARK_TOKEN=your_lark_webhook_token
```

## Usage

### Run the Trading Bot

```bash
java -jar target/perp-dex-tools-1.0.0.jar run \
  --exchange lighter \
  --ticker BTC-PERP \
  --quantity 0.001 \
  --direction buy \
  --take-profit 0.5 \
  --max-orders 10 \
  --wait-time 60 \
  --grid-step 1.0
```

### Command-Line Options

#### Required Options:

- `-e, --exchange <name>`: Exchange name (lighter, edgex, backpack, etc.)
- `-t, --ticker <symbol>`: Trading ticker (e.g., BTC-PERP, ETH-PERP)
- `-q, --quantity <amount>`: Quantity per order
- `-d, --direction <buy|sell>`: Trading direction

#### Optional Options:

- `--take-profit <percent>`: Take profit percentage (default: 0.5)
- `--max-orders <number>`: Maximum concurrent orders (default: 10)
- `--wait-time <seconds>`: Wait time between orders (default: 60)
- `--grid-step <percent>`: Grid step percentage (default: 1.0)
- `--stop-price <price>`: Stop trading if price reaches this level
- `--pause-price <price>`: Pause trading if price reaches this level
- `--boost-mode`: Enable boost mode (immediate market close)
- `--api-key <key>`: API key (overrides environment variable)
- `--api-secret <secret>`: API secret (overrides environment variable)
- `--private-key <key>`: Private key (overrides environment variable)

### List Supported Exchanges

```bash
java -jar target/perp-dex-tools-1.0.0.jar list-exchanges
```

### Run Hedge Mode (Coming Soon)

```bash
java -jar target/perp-dex-tools-1.0.0.jar hedge \
  --primary backpack \
  --secondary lighter \
  --ticker BTC-PERP \
  --quantity 0.001 \
  --max-orders 10
```

## Configuration

### Environment Variables by Exchange

#### Lighter Exchange

```bash
export API_KEY_PRIVATE_KEY=your_private_key
export LIGHTER_ACCOUNT_INDEX=0
export LIGHTER_API_KEY_INDEX=0
```

#### Backpack Exchange

```bash
export BACKPACK_API_KEY=your_api_key
export BACKPACK_API_SECRET=your_api_secret
export BACKPACK_ACCOUNT_INDEX=0
```

#### Other Exchanges

Similar patterns apply. Check the exchange documentation for required credentials.

### Logging

Logs are stored in the `logs/` directory:

- `{exchange}_{ticker}_activity.log`: Debug and activity logs
- `{exchange}_{ticker}_orders.csv`: Transaction history in CSV format

If `ACCOUNT_NAME` is set, logs include the account name:
- `{exchange}_{ticker}_{account}_activity.log`
- `{exchange}_{ticker}_{account}_orders.csv`

## Development

### Project Structure

```
src/main/java/com/perpdex/
├── bot/                    # Trading bot logic
│   ├── TradingBot.java
│   └── TradingConfig.java
├── config/                 # Configuration management
│   ├── ExchangeConfig.java
│   └── ConfigLoader.java
├── exchange/               # Exchange implementations
│   ├── BaseExchangeClient.java
│   ├── ExchangeFactory.java
│   └── (exchange implementations)
├── helper/                 # Helper utilities
│   ├── TradingLogger.java
│   └── NotificationService.java
├── model/                  # Data models
│   ├── OrderResult.java
│   ├── OrderInfo.java
│   ├── OrderSide.java
│   └── OrderStatus.java
├── util/                   # Utilities
│   └── RetryUtil.java
└── PerpDexToolsApplication.java  # Main entry point
```

### Running Tests

```bash
mvn test
```

### Building a Docker Image (Optional)

```dockerfile
FROM eclipse-temurin:21-jre
COPY target/perp-dex-tools-1.0.0.jar /app/perp-dex-tools.jar
WORKDIR /app
ENTRYPOINT ["java", "-jar", "perp-dex-tools.jar"]
```

Build and run:

```bash
docker build -t perp-dex-tools .
docker run -e API_KEY_PRIVATE_KEY=... perp-dex-tools run --exchange lighter --ticker BTC-PERP ...
```

## Migration from Python

This is a Java reimplementation of the original Python project. The core logic and algorithms remain the same, but the codebase benefits from:

- **Type Safety**: Compile-time type checking
- **Performance**: Better concurrency and resource management
- **Modern Reactive**: Non-blocking I/O with Project Reactor
- **Enterprise Ready**: Better error handling, logging, and monitoring capabilities

## Contributing

Contributions are welcome! Please ensure:

1. Code follows Java best practices
2. All tests pass
3. New features include appropriate tests
4. Logging is comprehensive but not excessive

## License

Please see the original repository for license information.

## Disclaimer

This software is for educational purposes only. Use at your own risk. Trading cryptocurrencies carries significant risk.

## Support

For issues specific to the Java implementation, please open an issue on GitHub.

For general trading questions, please refer to the original Python project documentation.

## Roadmap

- [x] Core trading bot logic
- [x] Multi-exchange factory pattern
- [x] Logging and notifications
- [x] CLI interface
- [x] Configuration management
- [ ] Complete exchange implementations (8 exchanges)
- [ ] Hedge mode implementation
- [ ] WebSocket support for all exchanges
- [ ] Comprehensive unit tests
- [ ] Integration tests
- [ ] Performance benchmarks
- [ ] Docker support
- [ ] Kubernetes deployment examples

## Acknowledgments

Based on the original Python implementation. Converted to Java with modern best practices and enterprise-grade architecture.
