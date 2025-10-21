package com.perpdex;

import com.perpdex.bot.TradingBot;
import com.perpdex.bot.TradingConfig;
import com.perpdex.config.ExchangeConfig;
import com.perpdex.exchange.ExchangeFactory;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.math.BigDecimal;
import java.util.concurrent.Callable;

/**
 * Main application entry point for Perp DEX Trading Tools
 */
@Slf4j
@Command(
        name = "perp-dex-tools",
        mixinStandardHelpOptions = true,
        version = "1.0.0",
        description = "Automated trading bot for perpetual DEX platforms",
        subcommands = {
                PerpDexToolsApplication.RunCommand.class,
                PerpDexToolsApplication.HedgeCommand.class,
                PerpDexToolsApplication.ListExchangesCommand.class
        }
)
public class PerpDexToolsApplication implements Callable<Integer> {

    public static void main(String[] args) {
        int exitCode = new CommandLine(new PerpDexToolsApplication()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() {
        System.out.println("Use --help to see available commands");
        return 0;
    }

    /**
     * Run the trading bot
     */
    @Command(name = "run", description = "Run the trading bot")
    static class RunCommand implements Callable<Integer> {

        @Option(names = {"-e", "--exchange"}, required = true, description = "Exchange name (e.g., lighter, edgex, backpack)")
        private String exchange;

        @Option(names = {"-t", "--ticker"}, required = true, description = "Trading ticker (e.g., BTC-PERP)")
        private String ticker;

        @Option(names = {"-q", "--quantity"}, required = true, description = "Quantity per order")
        private BigDecimal quantity;

        @Option(names = {"-d", "--direction"}, required = true, description = "Trading direction (buy or sell)")
        private String direction;

        @Option(names = {"--take-profit"}, defaultValue = "0.5", description = "Take profit percentage (default: 0.5)")
        private BigDecimal takeProfit;

        @Option(names = {"--max-orders"}, defaultValue = "10", description = "Maximum number of concurrent orders (default: 10)")
        private int maxOrders;

        @Option(names = {"--wait-time"}, defaultValue = "60", description = "Wait time between orders in seconds (default: 60)")
        private int waitTime;

        @Option(names = {"--grid-step"}, defaultValue = "1.0", description = "Grid step percentage (default: 1.0)")
        private BigDecimal gridStep;

        @Option(names = {"--stop-price"}, defaultValue = "-1", description = "Stop price (trading stops if reached)")
        private BigDecimal stopPrice;

        @Option(names = {"--pause-price"}, defaultValue = "-1", description = "Pause price (trading pauses if reached)")
        private BigDecimal pausePrice;

        @Option(names = {"--boost-mode"}, defaultValue = "false", description = "Enable boost mode (immediate market close)")
        private boolean boostMode;

        @Option(names = {"--api-key"}, description = "API key (can also be set via environment variable)")
        private String apiKey;

        @Option(names = {"--api-secret"}, description = "API secret (can also be set via environment variable)")
        private String apiSecret;

        @Option(names = {"--private-key"}, description = "Private key (can also be set via environment variable)")
        private String privateKey;

        @Override
        public Integer call() {
            try {
                log.info("Starting Perp DEX Trading Bot");
                log.info("Exchange: {}", exchange);
                log.info("Ticker: {}", ticker);
                log.info("Direction: {}", direction);

                // Build trading config
                TradingConfig tradingConfig = TradingConfig.builder()
                        .ticker(ticker)
                        .contractId("") // Will be fetched from exchange
                        .quantity(quantity)
                        .takeProfit(takeProfit)
                        .tickSize(BigDecimal.ZERO) // Will be fetched from exchange
                        .direction(direction)
                        .maxOrders(maxOrders)
                        .waitTime(waitTime)
                        .exchange(exchange)
                        .gridStep(gridStep)
                        .stopPrice(stopPrice)
                        .pausePrice(pausePrice)
                        .boostMode(boostMode)
                        .build();

                // Build exchange config
                ExchangeConfig exchangeConfig = ExchangeConfig.builder()
                        .exchangeName(exchange)
                        .apiKey(apiKey != null ? apiKey : System.getenv("API_KEY"))
                        .apiSecret(apiSecret != null ? apiSecret : System.getenv("API_SECRET"))
                        .privateKey(privateKey != null ? privateKey : System.getenv("PRIVATE_KEY"))
                        .tickSize(BigDecimal.ZERO)
                        .build();

                // Create and run bot
                TradingBot bot = new TradingBot(tradingConfig, exchangeConfig);
                bot.run().block();

                return 0;

            } catch (Exception e) {
                log.error("Failed to run trading bot", e);
                return 1;
            }
        }
    }

    /**
     * Run the hedge mode
     */
    @Command(name = "hedge", description = "Run hedge mode (pair trading)")
    static class HedgeCommand implements Callable<Integer> {

        @Option(names = {"--primary"}, required = true, description = "Primary exchange")
        private String primaryExchange;

        @Option(names = {"--secondary"}, required = true, description = "Secondary exchange (always lighter)")
        private String secondaryExchange;

        @Option(names = {"-t", "--ticker"}, required = true, description = "Trading ticker")
        private String ticker;

        @Option(names = {"-q", "--quantity"}, required = true, description = "Quantity per order")
        private BigDecimal quantity;

        @Option(names = {"--max-orders"}, defaultValue = "10", description = "Maximum number of concurrent orders")
        private int maxOrders;

        @Override
        public Integer call() {
            try {
                log.info("Starting Hedge Mode");
                log.info("Primary Exchange: {}", primaryExchange);
                log.info("Secondary Exchange: {}", secondaryExchange);
                log.info("Ticker: {}", ticker);

                // TODO: Implement hedge mode
                log.error("Hedge mode not yet implemented");
                return 1;

            } catch (Exception e) {
                log.error("Failed to run hedge mode", e);
                return 1;
            }
        }
    }

    /**
     * List supported exchanges
     */
    @Command(name = "list-exchanges", description = "List all supported exchanges")
    static class ListExchangesCommand implements Callable<Integer> {

        @Override
        public Integer call() {
            System.out.println("Supported exchanges:");
            ExchangeFactory.getSupportedExchanges().forEach(exchange ->
                    System.out.println("  - " + exchange)
            );
            return 0;
        }
    }
}
