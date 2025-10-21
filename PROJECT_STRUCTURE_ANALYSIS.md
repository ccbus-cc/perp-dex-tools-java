# Perpetual DEX Trading Bot - Python Project Structure Analysis

## Project Overview

This is a **modular automated trading bot** for perpetual futures trading across multiple decentralized exchanges (DEXs). The project is approximately **12,177 lines of Python code** organized into several key modules.

**Primary Purpose:** Execute automated trading strategies with support for:
- Multiple exchange integrations
- Hedge mode (paired trading across exchanges)
- Grid trading with configurable parameters
- Order lifecycle management and monitoring
- Comprehensive logging and notifications

**Repository:** perp-dex-tools (Note: repo name is "perp-dex-tools-java" but the current code is 100% Python)

---

## 1. PROJECT STRUCTURE

```
perp-dex-tools-java/
├── .env_example.txt              # Template for environment configuration
├── .flake8                        # Linting rules (max-line-length=129)
├── .gitignore                     # Standard Python + project ignores
├── LICENSE                        # Non-commercial license
├── README.md                      # Chinese documentation
├── README_EN.md                   # English documentation
├── requirements.txt               # Core dependencies
├── apex_requirements.txt           # Apex exchange specific deps
├── para_requirements.txt           # Paradex exchange specific deps
│
├── runbot.py                      # Main entry point (CLI arg parser)
├── trading_bot.py                 # Core trading bot logic
├── hedge_mode.py                  # Hedge mode entry point
│
├── exchanges/                     # Exchange client implementations
│   ├── __init__.py
│   ├── base.py                    # Abstract base exchange client class
│   ├── factory.py                 # Factory pattern for exchange instantiation
│   ├── edgex.py                   # EdgeX exchange implementation (573 LOC)
│   ├── backpack.py                # Backpack exchange implementation (595 LOC)
│   ├── aster.py                   # Aster exchange implementation (770 LOC)
│   ├── paradex.py                 # Paradex exchange implementation (669 LOC)
│   ├── lighter.py                 # Lighter exchange implementation (551 LOC)
│   ├── extended.py                # Extended exchange implementation (770 LOC)
│   ├── grvt.py                    # GRVT exchange implementation (539 LOC)
│   ├── apex.py                    # Apex exchange implementation
│   ├── bp_client.py               # Backpack low-level client (662 LOC)
│   └── lighter_custom_websocket.py # Custom WebSocket for Lighter (432 LOC)
│
├── hedge/                         # Hedge mode implementations
│   ├── hedge_mode_bp.py           # Backpack + Lighter hedge mode (1189 LOC)
│   ├── hedge_mode_ext.py          # Extended + Lighter hedge mode (1234 LOC)
│   ├── hedge_mode_apex.py         # Apex + Lighter hedge mode (1104 LOC)
│   └── hedge_mode_grvt.py         # GRVT + Lighter hedge mode (1077 LOC)
│
├── helpers/                       # Utility modules
│   ├── __init__.py
│   ├── logger.py                  # Trading logger with CSV/file output (112 LOC)
│   ├── telegram_bot.py            # Telegram notification helper (54 LOC)
│   └── lark_bot.py                # Lark notification helper (72 LOC)
│
├── tests/                         # Testing directory
│   └── test_query_retry.py        # Retry logic tests (72 LOC)
│
└── docs/                          # Documentation
    ├── ADDING_EXCHANGES.md        # Guide for adding new exchanges
    ├── telegram-bot-setup.md      # Telegram setup guide (Chinese)
    └── telegram-bot-setup-en.md   # Telegram setup guide (English)
```

---

## 2. CORE MODULES & FUNCTIONALITY

### A. Entry Points

#### `runbot.py` (Main Trading Bot)
- **Purpose:** CLI entry point for running the trading bot
- **Key Functions:**
  - `parse_arguments()` - Parses 15+ command-line arguments
  - `setup_logging()` - Configures global logging
  - `main()` - Async main function to initialize and run bot

**Command-line Arguments:**
```
--exchange {edgex, backpack, paradex, aster, lighter, grvt, extended, apex}
--ticker {BTC, ETH, SOL, etc.}
--quantity {decimal} (default: 0.1)
--take-profit {decimal percentage, default: 0.02}
--direction {buy, sell} (default: buy)
--max-orders {int, default: 40}
--wait-time {int seconds, default: 450}
--grid-step {percentage, default: -100}
--stop-price {decimal, default: -1}
--pause-price {decimal, default: -1}
--boost {flag for volume boosting}
--env-file {path, default: .env}
```

#### `hedge_mode.py` (Hedge Mode Entry Point)
- **Purpose:** Entry point for hedge mode trading (paired trading strategy)
- **Supported Exchanges:** backpack, extended, apex, grvt
- **Key Functions:**
  - `parse_arguments()` - Parses hedge mode specific arguments
  - `validate_exchange()` - Validates supported exchanges
  - `get_hedge_bot_class()` - Dynamic import of hedge bot implementations
  - `main()` - Async main for hedge mode

**Command-line Arguments:**
```
--exchange {backpack, extended, apex, grvt} (required)
--ticker {BTC, ETH, etc., default: BTC}
--size {decimal, required}
--iter {int iterations, required}
--fill-timeout {int seconds, default: 5}
--env-file {path, default: .env}
```

#### `trading_bot.py` (Core Trading Bot Class)
- **Purpose:** Main trading orchestration logic
- **Key Classes:**
  - `TradingConfig` - Dataclass for configuration (13 fields)
  - `OrderMonitor` - Thread-safe order state tracking
  - `TradingBot` - Main bot with trading logic

**Core Features:**
- Order placement and monitoring
- Position tracking
- WebSocket order updates
- Graceful shutdown handling
- Retry logic with exponential backoff

---

### B. Exchange Implementations

All exchanges inherit from `BaseExchangeClient` (abstract base class).

#### Supported Exchanges (8 total):

| Exchange | File | LOC | Features |
|----------|------|-----|----------|
| **EdgeX** | edgex.py | 573 | Official SDK, post-only orders |
| **Backpack** | backpack.py | 595 | ED25519 auth, WebSocket support |
| **Aster** | aster.py | 770 | Full order lifecycle |
| **Paradex** | paradex.py | 669 | Starknet integration |
| **Lighter** | lighter.py | 551 | Custom WebSocket support |
| **Extended** | extended.py | 770 | Starknet support |
| **GRVT** | grvt.py | 539 | Official SDK integration |
| **Apex** | apex.py | 553 | Omni integration |

#### Base Exchange Interface (`base.py` - 129 LOC)

**Abstract Methods (all exchanges must implement):**
```python
# Connection management
async connect() -> None
async disconnect() -> None

# Order operations
async place_open_order(contract_id, quantity, direction) -> OrderResult
async place_close_order(contract_id, quantity, price, side) -> OrderResult
async cancel_order(order_id) -> OrderResult

# Information retrieval
async get_order_info(order_id) -> Optional[OrderInfo]
async get_active_orders(contract_id) -> List[OrderInfo]
async get_account_positions() -> Decimal

# WebSocket/Events
def setup_order_update_handler(handler) -> None
def get_exchange_name() -> str

# Validation
def _validate_config() -> None
```

**Key Data Structures:**
- `OrderResult` - Standardized result from order operations
- `OrderInfo` - Order information structure
- `query_retry()` - Decorator for automatic retry with exponential backoff

#### Exchange Factory (`factory.py` - 99 LOC)

**Purpose:** Factory pattern for dynamic exchange instantiation

**Key Methods:**
- `create_exchange(exchange_name, config)` - Creates exchange client
- `get_supported_exchanges()` - Lists available exchanges
- `register_exchange(name, class)` - Register new exchange (for extensibility)

---

### C. Hedge Mode Implementations

**Purpose:** Paired trading strategy - place orders on primary exchange, hedge with market orders on Lighter

**Files:**
- `hedge_mode_bp.py` - Backpack + Lighter (1189 LOC)
- `hedge_mode_ext.py` - Extended + Lighter (1234 LOC)
- `hedge_mode_apex.py` - Apex + Lighter (1104 LOC)
- `hedge_mode_grvt.py` - GRVT + Lighter (1077 LOC)

**Common Workflow:**
1. Place maker order on primary exchange (e.g., Backpack)
2. Wait for fill with configurable timeout
3. Hedge position with market order on Lighter
4. Close position with another maker order
5. Close hedge with market order
6. Repeat for configured iterations

**Key Features:**
- Automatic position tracking
- Fill timeout handling
- CSV logging of all trades
- Error recovery and logging

---

### D. Helper Modules

#### `helpers/logger.py` (112 LOC)
**Class: `TradingLogger`**

**Functionality:**
- Structured logging with timezone support (Asia/Shanghai default)
- Dual output: console + file
- CSV trading log generation
- Automatic log directory creation
- Multi-account support via `ACCOUNT_NAME` env var

**Log Files Generated:**
- `logs/{exchange}_{ticker}_orders.csv` - Trade records
- `logs/{exchange}_{ticker}_activity.log` - Detailed activity log

#### `helpers/telegram_bot.py` (54 LOC)
**Class: `TelegramBot`**

**Functionality:**
- Send trading notifications via Telegram
- Uses environment variables: `TELEGRAM_BOT_TOKEN`, `TELEGRAM_CHAT_ID`

#### `helpers/lark_bot.py` (72 LOC)
**Class: `LarkBot`**

**Functionality:**
- Send trading notifications via Lark/Feishu
- Uses environment variable: `LARK_TOKEN`

---

## 3. DEPENDENCIES

### Core Dependencies (`requirements.txt`)

**Essential Libraries:**
```
python-dotenv>=1.0.0      # Environment variable loading
pytz>=2025.2              # Timezone support
asyncio==4.0.0            # Async/await support
aiohttp>=3.8.0            # Async HTTP client
websocket-client>=1.6.0   # WebSocket client
pydantic>=1.8.0           # Data validation
pycryptodome>=3.15.0      # Cryptography
ecdsa>=0.17.0             # ECDSA signing
requests==2.32.5          # HTTP library
eval_type_backport        # Type annotation support
tenacity>=9.1.2           # Retry library

# Exchange SDKs
websockets>=12.0          # WebSocket server
cryptography>=41.0.0      # Crypto operations
bpx-py==2.0.11            # Backpack SDK
x10-python-trading-starknet # Extended exchange SDK

# Forked SDKs (from GitHub)
git+https://github.com/your-quantguy/edgex-python-sdk.git
git+https://github.com/elliottech/lighter-python.git
```

### Exchange-Specific Requirements

**`apex_requirements.txt`**
- apexomni SDK
- web3==6.0.0
- eth-account==0.13.7
- eth_keys
- dateparser==1.0.0
- pytest, numpy, sympy, mpmath, tox, setuptools

**`para_requirements.txt`**
- paradex-py SDK (from GitHub)
- Same base dependencies as requirements.txt

### Python Version Requirements
- **grvt:** Python 3.10+
- **Paradex:** Python 3.9-3.12
- **Other exchanges:** Python 3.8+
- **Recommended:** Python 3.10-3.12

---

## 4. ENVIRONMENT CONFIGURATION

### Environment Variables (`.env`)

**General:**
- `ACCOUNT_NAME` - Multi-account support (optional)
- `TIMEZONE` - Default: Asia/Shanghai
- `LOG_TO_CONSOLE` - Boolean flag
- `LOG_TO_FILE` - Boolean flag
- `LOG_FILE` - CSV log filename

**EdgeX:**
- `EDGEX_ACCOUNT_ID`
- `EDGEX_STARK_PRIVATE_KEY`
- `EDGEX_BASE_URL` - Default: https://pro.edgex.exchange
- `EDGEX_WS_URL` - Default: wss://quote.edgex.exchange

**Backpack:**
- `BACKPACK_PUBLIC_KEY`
- `BACKPACK_SECRET_KEY`

**Paradex:**
- `PARADEX_L1_ADDRESS`
- `PARADEX_L2_PRIVATE_KEY`
- `PARADEX_ENVIRONMENT` - prod/testnet

**GRVT:**
- `GRVT_TRADING_ACCOUNT_ID`
- `GRVT_PRIVATE_KEY`
- `GRVT_API_KEY`
- `GRVT_ENVIRONMENT` - prod/testnet/staging/dev

**Aster:**
- `ASTER_API_KEY`
- `ASTER_SECRET_KEY`

**Lighter:**
- `API_KEY_PRIVATE_KEY`
- `LIGHTER_ACCOUNT_INDEX`
- `LIGHTER_API_KEY_INDEX`

**Extended:**
- `EXTENDED_API_KEY`
- `EXTENDED_STARK_KEY_PUBLIC`
- `EXTENDED_STARK_KEY_PRIVATE`
- `EXTENDED_VAULT`

**Apex:**
- `APEX_API_KEY`
- `APEX_API_KEY_PASSPHRASE`
- `APEX_API_KEY_SECRET`
- `APEX_OMNI_KEY_SEED`

**Notifications (Optional):**
- `TELEGRAM_BOT_TOKEN`
- `TELEGRAM_CHAT_ID`
- `LARK_TOKEN`

---

## 5. TRADING STRATEGIES

### Strategy 1: Grid Trading (Standard Mode)

**How it works:**
1. Place limit order near market price (slightly above for buy, below for sell)
2. Wait for order to fill
3. Upon fill, immediately place close order at profit target
4. Repeat until max orders reached or price conditions trigger stop

**Parameters:**
- `quantity` - Size per order
- `take-profit` - Profit target in percentage (0.02% for 0.02%)
- `max-orders` - Max concurrent open orders
- `wait-time` - Seconds between orders
- `grid-step` - Minimum distance between close orders (prevents overlapping)
- `stop-price` - Exit price if price moves against direction
- `pause-price` - Pause when price reaches threshold, resume when it reverts

### Strategy 2: Boost Mode

**Available on:** Aster, Backpack

**How it works:**
1. Place maker order to open position
2. Upon fill, immediately place taker order to close
3. Repeat for volume
4. Costs: Maker fee + Taker fee + Slippage

### Strategy 3: Hedge Mode

**Available on:** Backpack, Extended, Apex, GRVT (+ Lighter as hedge)

**How it works:**
1. Place maker order on primary exchange
2. Upon fill, hedge with market order on Lighter
3. Place another maker order on primary exchange to close
4. Close hedge position on Lighter with market order
5. Repeat for configured iterations

**Benefits:** Risk reduction through paired positions, volume on 2 exchanges

---

## 6. TESTING

### Current Test Suite

**File:** `tests/test_query_retry.py` (72 LOC)

**Tests:**
1. `test_success_function()` - Verify successful execution without retry
2. `test_network_error_function()` - Verify retry on TimeoutError
3. `test_business_error_function()` - Verify exception propagation
4. `test_timing_function()` - Verify exponential backoff timing

**Test Approach:** Async test functions using mocking

---

## 7. CONFIGURATION & CODE STYLE

### Code Style Configuration

**File:** `.flake8`
```
max-line-length = 129
```

### Project Conventions

- **Async/Await:** Heavy use of Python asyncio for concurrent operations
- **Type Hints:** Used throughout (Decimal for prices, int for quantities)
- **Dataclasses:** For configuration and data structures
- **Error Handling:** Retry decorators with exponential backoff
- **Logging:** Structured logging with timestamps and levels
- **Environment Loading:** .env files with python-dotenv

---

## 8. KEY DESIGN PATTERNS

### 1. Factory Pattern
- `ExchangeFactory` for exchange instantiation
- Lazy loading of exchange modules
- Easy to add new exchanges

### 2. Abstract Base Class Pattern
- `BaseExchangeClient` defines interface
- All exchanges inherit and implement methods
- Ensures consistent API

### 3. Data Classes Pattern
- `TradingConfig` for configuration
- `OrderResult`, `OrderInfo` for standardized data
- Type-safe and concise

### 4. Decorator Pattern
- `@query_retry()` for automatic retry logic
- Configurable retry strategies
- Exponential backoff

### 5. Observer Pattern
- Order update handlers for WebSocket events
- Callback-based notification system

---

## 9. CRITICAL COMPONENTS SUMMARY

| Component | Purpose | Complexity |
|-----------|---------|-----------|
| **trading_bot.py** | Core trading loop orchestration | HIGH |
| **exchanges/base.py** | Exchange abstraction | HIGH |
| **Individual exchange clients** | Exchange-specific API handling | HIGH |
| **hedge mode implementations** | Paired trading strategy | VERY HIGH |
| **helpers/logger.py** | Logging & CSV output | MEDIUM |
| **runbot.py** | CLI & arg parsing | MEDIUM |
| **factories** | Dynamic loading | LOW |

---

## 10. SCALABILITY & EXTENSIBILITY

### Adding New Exchange Support

**Steps (documented in `docs/ADDING_EXCHANGES.md`):**
1. Create new file in `exchanges/` directory
2. Implement `BaseExchangeClient` abstract methods
3. Add to `ExchangeFactory._registered_exchanges` dictionary
4. Create environment variables for credentials
5. Test with provided test utilities

### Adding New Strategies

1. Create new strategy class (can extend `TradingBot`)
2. Implement trading logic
3. Use existing exchange abstraction
4. Create entry point similar to `runbot.py` or `hedge_mode.py`

---

## 11. DEPLOYMENT CONSIDERATIONS

### Virtual Environment Setup
```bash
python3 -m venv env
source env/bin/activate  # or env\Scripts\activate on Windows
pip install -r requirements.txt
```

### Multi-Account Support
- Create separate `.env` files (e.g., account_1.env, account_2.env)
- Set `ACCOUNT_NAME` variable for log distinction
- Run with `--env-file` argument

### Multi-Exchange Support
- Configure all exchanges in single `.env`
- Run with different `--exchange` arguments

### Multi-Ticker Support
- Configure all tickers for an exchange
- Run with different `--ticker` arguments

### Logging
- Automatic logs/ directory creation
- Per-exchange-ticker log files
- CSV trading history
- Activity logs with timestamps

---

## 12. SECURITY CONSIDERATIONS

- Private keys stored in `.env` (not version controlled)
- `.gitignore` includes .env files
- API keys kept in environment variables
- No hardcoded credentials in source
- Environment-based configuration

---

## 13. TOTAL PROJECT METRICS

| Metric | Value |
|--------|-------|
| **Total Lines of Code** | 12,177 |
| **Python Files** | 25 |
| **Supported Exchanges** | 8 |
| **Hedge Mode Variants** | 4 |
| **Helper Modules** | 3 |
| **Main Entry Points** | 2 |
| **Average Exchange LOC** | ~600 |
| **Average Hedge Mode LOC** | ~1,100 |

---

## 14. DEPENDENCIES VISUALIZATION

```
runbot.py / hedge_mode.py
    ↓
TradingBot
    ↓
ExchangeFactory → Creates appropriate Exchange Client
    ↓
BaseExchangeClient (abstract)
    ↓
Specific Exchange Implementation
    (EdgeX, Backpack, Aster, Paradex, Lighter, Extended, GRVT, Apex)
    
    ↓
Helpers (Logger, TelegramBot, LarkBot)
    ↓
External SDKs (bpx-py, edgex-sdk, paradex-py, lighter-python, etc.)
```

---

## 15. RECOMMENDATIONS FOR JAVA CONVERSION

### High Priority (Core)
1. Convert base.py → BaseExchangeClient.java interface
2. Convert factory.py → ExchangeFactory.java
3. Convert trading_bot.py → TradingBot.java
4. Convert each exchange implementation

### Medium Priority (Supporting)
1. Convert logger utilities
2. Convert configuration handling
3. Convert notification helpers

### Low Priority (Testing)
1. Convert test utilities
2. Set up JUnit tests

### Architecture Notes for Java
- Use interfaces for abstract base class
- Dependency injection for exchange clients
- Decimal types for financial values
- CompletableFuture/Async for async operations
- Annotations for configuration loading
- Proper exception handling and logging

