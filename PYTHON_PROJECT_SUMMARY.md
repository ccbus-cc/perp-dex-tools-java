# PYTHON PROJECT STRUCTURE - EXECUTIVE SUMMARY

## Quick Overview

**Project:** Perpetual DEX Trading Bot (Multi-Exchange Automated Trading)
**Language:** 100% Python (despite repo name ending in "-java")
**Total Code:** 12,177 lines of Python across 25 files
**Purpose:** Automated trading bot for decentralized exchanges with multiple strategies

## Directory Structure

```
Root files:
├── runbot.py              → MAIN ENTRY POINT (CLI trading bot)
├── hedge_mode.py          → HEDGE MODE ENTRY POINT  
├── trading_bot.py         → Core trading bot logic
├── requirements.txt       → Core dependencies
├── apex_requirements.txt   → Apex exchange extra deps
├── para_requirements.txt   → Paradex exchange extra deps

Subdirectories:
├── exchanges/             → 8 exchange implementations + base class + factory
├── hedge/                 → 4 hedge mode strategy implementations
├── helpers/               → Logger, Telegram, Lark notification modules
├── tests/                 → Query retry test suite
└── docs/                  → Exchange integration guide + setup docs
```

## Key Statistics

| Item | Count |
|------|-------|
| Python Files | 25 |
| Total Lines of Code | 12,177 |
| Supported Exchanges | 8 |
| Main Entry Points | 2 (runbot, hedge_mode) |
| Exchange Implementations | 8 |
| Hedge Mode Variants | 4 |
| Helper Modules | 3 |
| Test Files | 1 |

## Main Components

### 1. Entry Points (3 files)
- **runbot.py** - Standard trading bot launcher with 15+ CLI arguments
- **hedge_mode.py** - Paired trading bot launcher (4 exchanges supported)
- **trading_bot.py** - Core trading orchestration engine

### 2. Exchange Clients (11 files, 6,184 LOC total)
```
exchanges/
├── base.py                    (129 LOC)  - Abstract interface for all exchanges
├── factory.py                 (99 LOC)   - Factory pattern for exchange creation
├── edgex.py                   (573 LOC)  - EdgeX implementation
├── backpack.py                (595 LOC)  - Backpack implementation
├── bp_client.py               (662 LOC)  - Backpack low-level client
├── aster.py                   (770 LOC)  - Aster implementation
├── paradex.py                 (669 LOC)  - Paradex implementation
├── lighter.py                 (551 LOC)  - Lighter implementation
├── lighter_custom_websocket.py (432 LOC)  - Lighter custom WebSocket
├── extended.py                (770 LOC)  - Extended implementation
├── grvt.py                    (539 LOC)  - GRVT implementation
└── apex.py                    (~550 LOC) - Apex implementation
```

**All exchanges implement BaseExchangeClient interface:**
- `async connect()` / `disconnect()`
- `async place_open_order()` / `place_close_order()`
- `async cancel_order()`
- `async get_order_info()` / `get_active_orders()` / `get_account_positions()`
- `setup_order_update_handler()` for WebSocket updates

### 3. Hedge Mode (4 files, 4,604 LOC total)
```
hedge/
├── hedge_mode_bp.py   (1189 LOC) - Backpack + Lighter hedge strategy
├── hedge_mode_ext.py  (1234 LOC) - Extended + Lighter hedge strategy
├── hedge_mode_apex.py (1104 LOC) - Apex + Lighter hedge strategy
└── hedge_mode_grvt.py (1077 LOC) - GRVT + Lighter hedge strategy
```

**Workflow:** Primary exchange (maker) + Lighter (hedge market orders)

### 4. Helper Modules (3 files, 238 LOC)
```
helpers/
├── logger.py        (112 LOC) - Trading logger (console + CSV + file logs)
├── telegram_bot.py  (54 LOC)  - Telegram notifications
└── lark_bot.py      (72 LOC)  - Lark/Feishu notifications
```

### 5. Testing (1 file, 72 LOC)
```
tests/
└── test_query_retry.py (72 LOC) - Tests for retry decorator logic
```

## Command-Line Arguments

### Standard Mode (runbot.py)
```
--exchange {edgex|backpack|paradex|aster|lighter|grvt|extended|apex}
--ticker {BTC|ETH|SOL|...}
--quantity {decimal}              # Order size
--take-profit {decimal}           # Profit target %
--direction {buy|sell}
--max-orders {int}                # Max concurrent orders
--wait-time {int}                 # Seconds between orders
--grid-step {percentage}          # Min distance between close orders
--stop-price {decimal}            # Exit trigger price
--pause-price {decimal}           # Pause trigger price
--boost                           # Volume boosting mode (Aster/Backpack only)
--env-file {path}                 # Config file path
```

### Hedge Mode (hedge_mode.py)
```
--exchange {backpack|extended|apex|grvt}
--ticker {BTC|ETH|...}
--size {decimal}                  # Order size (required)
--iter {int}                      # Number of iterations (required)
--fill-timeout {int}              # Maker order timeout in seconds
--env-file {path}                 # Config file path
```

## Environment Variables

### General
```
ACCOUNT_NAME                      # Multi-account support
TIMEZONE                          # Default: Asia/Shanghai
LOG_TO_CONSOLE                    # Boolean
LOG_TO_FILE                       # Boolean
LOG_FILE                          # CSV filename
```

### Exchange Credentials (varies by exchange)
```
EdgeX:      EDGEX_ACCOUNT_ID, EDGEX_STARK_PRIVATE_KEY, EDGEX_BASE_URL, EDGEX_WS_URL
Backpack:   BACKPACK_PUBLIC_KEY, BACKPACK_SECRET_KEY
Paradex:    PARADEX_L1_ADDRESS, PARADEX_L2_PRIVATE_KEY, PARADEX_ENVIRONMENT
GRVT:       GRVT_TRADING_ACCOUNT_ID, GRVT_PRIVATE_KEY, GRVT_API_KEY, GRVT_ENVIRONMENT
Aster:      ASTER_API_KEY, ASTER_SECRET_KEY
Lighter:    API_KEY_PRIVATE_KEY, LIGHTER_ACCOUNT_INDEX, LIGHTER_API_KEY_INDEX
Extended:   EXTENDED_API_KEY, EXTENDED_STARK_KEY_PUBLIC, EXTENDED_STARK_KEY_PRIVATE, EXTENDED_VAULT
Apex:       APEX_API_KEY, APEX_API_KEY_PASSPHRASE, APEX_API_KEY_SECRET, APEX_OMNI_KEY_SEED
```

### Notifications (Optional)
```
TELEGRAM_BOT_TOKEN, TELEGRAM_CHAT_ID
LARK_TOKEN
```

## Dependencies

### Core (requirements.txt)
- python-dotenv, pytz, asyncio, aiohttp, websocket-client
- pydantic, pycryptodome, ecdsa, requests, tenacity
- Exchange SDKs: bpx-py, x10-python-trading-starknet
- GitHub SDKs: edgex-python-sdk, lighter-python

### Exchange-Specific
- **Apex:** apexomni, web3, eth-account, eth_keys, dateparser
- **Paradex:** paradex-py SDK

### Python Version
- GRVT: 3.10+
- Paradex: 3.9-3.12
- Others: 3.8+
- Recommended: 3.10-3.12

## Trading Strategies

### 1. Grid Trading (Standard Mode)
- Place limit orders near market price
- Auto-close at profit target
- Configurable max orders and wait time
- Optional grid-step to prevent order overlap

### 2. Boost Mode
- Fast maker-taker cycling
- Available on Aster & Backpack
- Higher volume, higher costs

### 3. Hedge Mode
- Place maker orders on primary exchange
- Hedge with market orders on Lighter
- Available on: Backpack, Extended, Apex, GRVT
- Risk reduction through paired positions

## Key Design Patterns

1. **Factory Pattern** - ExchangeFactory for dynamic exchange creation
2. **Abstract Base Class** - BaseExchangeClient interface
3. **Data Classes** - TradingConfig, OrderResult, OrderInfo
4. **Decorator Pattern** - @query_retry() for automatic retries
5. **Observer Pattern** - WebSocket order update callbacks

## Important Features

- Async/await throughout (concurrent operations)
- Structured logging with timezone support
- CSV trade history generation
- Multi-account support
- Automatic retry with exponential backoff
- Graceful shutdown handling
- Type hints (Decimal for prices)
- WebSocket order monitoring

## Output Files

Application creates:
```
logs/
├── {exchange}_{ticker}_orders.csv           # Trade records
├── {exchange}_{ticker}_activity.log         # Detailed logs
└── {exchange}_{ticker}_{account}_*          # Per-account logs
```

## Extensibility

### Adding New Exchange
1. Create exchanges/new_exchange.py
2. Inherit from BaseExchangeClient
3. Implement all abstract methods
4. Add to ExchangeFactory._registered_exchanges
5. Set environment variables

### Adding New Strategy
1. Create new bot class or entry point
2. Use existing exchange abstraction
3. Implement trading logic
4. Create CLI wrapper similar to runbot.py

## Security Model

- Private keys in .env (not version controlled)
- API credentials in environment variables
- No hardcoded secrets in source code
- .gitignore excludes env files and logs

## Python Version & Platform

- Recommended: Python 3.10-3.12
- Cross-platform: Linux, macOS, Windows
- Async support required
- WebSocket support required

## Development Notes

- Code style: max line length 129 chars (per .flake8)
- Testing: Async test suite with mocking
- Logging: Structured with timestamps
- Configuration: .env file based
- Error handling: Retry decorators + try-catch blocks

---

## QUICK START EXAMPLE

```bash
# Install
python3 -m venv env
source env/bin/activate
pip install -r requirements.txt

# Configure
cp env_example.txt .env
# Edit .env with your API credentials

# Run standard trading
python runbot.py --exchange backpack --ticker ETH --quantity 0.1 \
                  --take-profit 0.02 --max-orders 40 --wait-time 450

# Run hedge mode
python hedge_mode.py --exchange backpack --ticker BTC --size 0.05 --iter 20
```

---

## File Sizes Summary

| Component | Files | Total LOC |
|-----------|-------|-----------|
| Entry Points | 3 | ~3,000+ |
| Exchanges | 11 | 6,184 |
| Hedge Mode | 4 | 4,604 |
| Helpers | 3 | 238 |
| Tests | 1 | 72 |
| Config/Docs | 6 | ~100 |
| **TOTAL** | **25** | **12,177** |

