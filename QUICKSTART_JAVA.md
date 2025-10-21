# Quick Start Guide - Java Version

Get started with Perp DEX Tools Java in 5 minutes!

## Prerequisites

- Java 21+ installed
- Maven 3.8+ installed
- API credentials for your exchange

## Step 1: Clone and Build

```bash
# Clone the repository
git clone <repository-url>
cd perp-dex-tools-java

# Build the project
./build.sh
```

The build script will:
- Check your Java and Maven versions
- Clean previous builds
- Compile and package the application
- Create an executable JAR in `target/`

## Step 2: Configure Environment

```bash
# Copy the example environment file
cp .env.example .env

# Edit .env with your credentials
nano .env  # or use your preferred editor
```

Minimum required configuration for Lighter exchange:
```bash
API_KEY_PRIVATE_KEY=your_private_key_here
LIGHTER_ACCOUNT_INDEX=0
LIGHTER_API_KEY_INDEX=0
```

## Step 3: Run the Bot

```bash
./run.sh run \
  --exchange lighter \
  --ticker BTC-PERP \
  --quantity 0.001 \
  --direction buy \
  --take-profit 0.5 \
  --max-orders 10
```

## Common Commands

### List Supported Exchanges

```bash
./run.sh list-exchanges
```

### View Help

```bash
./run.sh --help
./run.sh run --help
```

### Run with Custom Parameters

```bash
./run.sh run \
  --exchange lighter \
  --ticker ETH-PERP \
  --quantity 0.01 \
  --direction sell \
  --take-profit 1.0 \
  --max-orders 15 \
  --wait-time 30 \
  --grid-step 2.0
```

### Enable Boost Mode (Immediate Market Close)

```bash
./run.sh run \
  --exchange lighter \
  --ticker BTC-PERP \
  --quantity 0.001 \
  --direction buy \
  --boost-mode
```

## Environment Variables

All settings can be configured via environment variables or command-line arguments.

Priority: Command-line args > Environment variables > Defaults

Example .env file:
```bash
# General
TIMEZONE=Asia/Shanghai
ACCOUNT_NAME=my_trading_account

# Exchange credentials (Lighter example)
API_KEY_PRIVATE_KEY=0x1234...
LIGHTER_ACCOUNT_INDEX=0
LIGHTER_API_KEY_INDEX=0

# Notifications (optional)
TELEGRAM_BOT_TOKEN=your_bot_token
TELEGRAM_CHAT_ID=your_chat_id
LARK_TOKEN=your_lark_webhook_token
```

## Logs

Logs are automatically created in the `logs/` directory:

- `{exchange}_{ticker}_activity.log` - Detailed activity log
- `{exchange}_{ticker}_orders.csv` - Transaction history

If `ACCOUNT_NAME` is set:
- `{exchange}_{ticker}_{account}_activity.log`
- `{exchange}_{ticker}_{account}_orders.csv`

## Troubleshooting

### Build Fails

**Error**: Maven not found
```bash
# Install Maven (Ubuntu/Debian)
sudo apt-get install maven

# Install Maven (macOS)
brew install maven
```

**Error**: Java version too old
```bash
# Check Java version
java -version

# Install Java 21 (Ubuntu/Debian)
sudo apt-get install openjdk-21-jdk

# Install Java 21 (macOS)
brew install openjdk@21
```

### Runtime Errors

**Error**: Missing API credentials
- Ensure your `.env` file is properly configured
- Check that environment variables are exported
- Verify credentials with your exchange

**Error**: Connection failed
- Check your internet connection
- Verify exchange API endpoints are accessible
- Check if exchange is experiencing downtime

**Error**: Invalid ticker
- Verify the ticker symbol is correct for your exchange
- Check exchange documentation for valid symbols

## Next Steps

- Read the full [README_JAVA.md](README_JAVA.md) for detailed documentation
- Check [.env.example](.env.example) for all configuration options
- Review logs in `logs/` directory to monitor trading activity
- Set up Telegram or Lark notifications for important alerts

## Support

- GitHub Issues: Report bugs or request features
- Original Python project: For general trading strategy questions

## Safety Tips

1. **Start Small**: Test with minimal quantities first
2. **Monitor Logs**: Watch the `logs/` directory closely
3. **Set Stop Prices**: Use `--stop-price` to limit losses
4. **Enable Notifications**: Stay informed with Telegram/Lark alerts
5. **Test on Testnet**: If available, test on exchange testnets first

## Example Workflow

```bash
# 1. Build the project
./build.sh

# 2. Set up environment
cp .env.example .env
# Edit .env with your credentials

# 3. Test with list-exchanges
./run.sh list-exchanges

# 4. Run with conservative settings
./run.sh run \
  --exchange lighter \
  --ticker BTC-PERP \
  --quantity 0.0001 \
  --direction buy \
  --take-profit 0.1 \
  --max-orders 3 \
  --wait-time 120

# 5. Monitor logs
tail -f logs/lighter_BTC-PERP_activity.log

# 6. Check transactions
cat logs/lighter_BTC-PERP_orders.csv
```

Happy trading! 🚀
