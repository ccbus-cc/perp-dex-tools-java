package com.perpdex.bot;

import com.perpdex.exchange.BaseExchangeClient;
import com.perpdex.exchange.ExchangeFactory;
import com.perpdex.helper.TradingLogger;
import com.perpdex.helper.NotificationService;
import com.perpdex.model.OrderInfo;
import com.perpdex.model.OrderResult;
import com.perpdex.config.ExchangeConfig;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Modular Trading Bot - Main trading logic supporting multiple exchanges
 */
@Slf4j
public class TradingBot {
    private final TradingConfig config;
    private final TradingLogger logger;
    private final BaseExchangeClient exchangeClient;
    private final NotificationService notificationService;

    // Trading state
    private final List<OrderTracker> activeCloseOrders = new ArrayList<>();
    private int lastCloseOrdersCount = 0;
    private Instant lastOpenOrderTime = Instant.EPOCH;
    private Instant lastLogTime = Instant.EPOCH;

    // Order monitoring
    private final AtomicReference<String> currentOrderStatus = new AtomicReference<>("PENDING");
    private final AtomicReference<BigDecimal> orderFilledAmount = new AtomicReference<>(BigDecimal.ZERO);
    private final CountDownLatch orderFilledLatch = new CountDownLatch(1);
    private final CountDownLatch orderCancelledLatch = new CountDownLatch(1);

    // Shutdown control
    private final AtomicBoolean shutdownRequested = new AtomicBoolean(false);

    public TradingBot(TradingConfig tradingConfig, ExchangeConfig exchangeConfig) {
        this.config = tradingConfig;
        this.logger = new TradingLogger(config.getExchange(), config.getTicker(), true);
        this.notificationService = new NotificationService();

        // Create exchange client
        try {
            this.exchangeClient = ExchangeFactory.createExchange(config.getExchange(), exchangeConfig);
            setupOrderUpdateHandler();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create exchange client: " + e.getMessage(), e);
        }
    }

    /**
     * Setup WebSocket handlers for order updates
     */
    private void setupOrderUpdateHandler() {
        exchangeClient.setupOrderUpdateHandler(orderInfo -> {
            try {
                // Check if this is for our contract
                if (!orderInfo.getOrderId().equals(config.getContractId())) {
                    return;
                }

                String status = orderInfo.getStatus();
                String side = orderInfo.getSide();
                BigDecimal filledSize = orderInfo.getFilledSize();

                // Determine order type based on side
                String orderType = side.equalsIgnoreCase(config.getCloseOrderSide()) ? "CLOSE" : "OPEN";

                if (orderType.equals("OPEN")) {
                    currentOrderStatus.set(status);
                }

                if ("FILLED".equalsIgnoreCase(status)) {
                    if (orderType.equals("OPEN")) {
                        orderFilledAmount.set(filledSize);
                        orderFilledLatch.countDown();
                    }

                    logger.log(String.format("[%s] [%s] %s %s @ %s",
                            orderType, orderInfo.getOrderId(), status,
                            orderInfo.getSize(), orderInfo.getPrice()), "INFO");

                    logger.logTransaction(orderInfo.getOrderId(), side,
                            orderInfo.getSize(), orderInfo.getPrice(), status);

                } else if ("CANCELLED".equalsIgnoreCase(status) || "CANCELED".equalsIgnoreCase(status)) {
                    if (orderType.equals("OPEN")) {
                        orderFilledAmount.set(filledSize);
                        orderCancelledLatch.countDown();

                        if (filledSize.compareTo(BigDecimal.ZERO) > 0) {
                            logger.logTransaction(orderInfo.getOrderId(), side,
                                    filledSize, orderInfo.getPrice(), status);
                        }
                    }

                    logger.log(String.format("[%s] [%s] %s %s @ %s",
                            orderType, orderInfo.getOrderId(), status,
                            orderInfo.getSize(), orderInfo.getPrice()), "INFO");

                } else if ("PARTIALLY_FILLED".equalsIgnoreCase(status)) {
                    logger.log(String.format("[%s] [%s] %s %s @ %s",
                            orderType, orderInfo.getOrderId(), status,
                            filledSize, orderInfo.getPrice()), "INFO");
                } else {
                    logger.log(String.format("[%s] [%s] %s %s @ %s",
                            orderType, orderInfo.getOrderId(), status,
                            orderInfo.getSize(), orderInfo.getPrice()), "INFO");
                }

            } catch (Exception e) {
                logger.log("Error handling order update: " + e.getMessage(), "ERROR");
                log.error("Error handling order update", e);
            }
        });
    }

    /**
     * Calculate wait time between orders based on current state
     */
    private int calculateWaitTime() {
        int coolDownTime = config.getWaitTime();
        int activeCount = activeCloseOrders.size();

        // If orders were closed, don't wait
        if (activeCount < lastCloseOrdersCount) {
            lastCloseOrdersCount = activeCount;
            return 0;
        }

        lastCloseOrdersCount = activeCount;

        // If max orders reached, wait briefly
        if (activeCount >= config.getMaxOrders()) {
            return 1;
        }

        // Adjust cooldown based on how many orders are active
        double fillRatio = (double) activeCount / config.getMaxOrders();
        if (fillRatio >= 2.0 / 3.0) {
            coolDownTime = 2 * config.getWaitTime();
        } else if (fillRatio >= 1.0 / 3.0) {
            coolDownTime = config.getWaitTime();
        } else if (fillRatio >= 1.0 / 6.0) {
            coolDownTime = config.getWaitTime() / 2;
        } else {
            coolDownTime = config.getWaitTime() / 4;
        }

        // Initialize last order time if needed
        if (lastOpenOrderTime.equals(Instant.EPOCH) && activeCount > 0) {
            lastOpenOrderTime = Instant.now();
        }

        long elapsedSeconds = Duration.between(lastOpenOrderTime, Instant.now()).getSeconds();
        return elapsedSeconds > coolDownTime ? 0 : 1;
    }

    /**
     * Place an order and monitor its execution
     */
    private Mono<Boolean> placeAndMonitorOpenOrder() {
        return Mono.fromCallable(() -> {
            // Reset state
            currentOrderStatus.set("OPEN");
            orderFilledAmount.set(BigDecimal.ZERO);

            return true;
        }).flatMap(ready -> exchangeClient.placeOpenOrder(
                config.getContractId(),
                config.getQuantity(),
                config.getDirection()
        )).flatMap(orderResult -> {
            if (!orderResult.isSuccess()) {
                logger.log("Failed to place open order: " + orderResult.getErrorMessage(), "ERROR");
                return Mono.just(false);
            }

            if ("FILLED".equalsIgnoreCase(orderResult.getStatus())) {
                return handleOrderResult(orderResult);
            }

            // Wait for order to be filled or timeout
            try {
                orderFilledLatch.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            return handleOrderResult(orderResult);
        }).onErrorResume(error -> {
            logger.log("Error placing order: " + error.getMessage(), "ERROR");
            log.error("Error placing order", error);
            return Mono.just(false);
        });
    }

    /**
     * Handle the result of an order placement
     */
    private Mono<Boolean> handleOrderResult(OrderResult orderResult) {
        String orderId = orderResult.getOrderId();
        BigDecimal filledPrice = orderResult.getPrice();
        boolean isFilled = orderFilledLatch.getCount() == 0 || "FILLED".equalsIgnoreCase(orderResult.getStatus());

        if (isFilled) {
            lastOpenOrderTime = Instant.now();
            String closeSide = config.getCloseOrderSide();

            BigDecimal closePrice;
            if ("sell".equalsIgnoreCase(closeSide)) {
                closePrice = filledPrice.multiply(
                        BigDecimal.ONE.add(config.getTakeProfit().divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP))
                );
            } else {
                closePrice = filledPrice.multiply(
                        BigDecimal.ONE.subtract(config.getTakeProfit().divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP))
                );
            }

            return exchangeClient.placeCloseOrder(
                    config.getContractId(),
                    config.getQuantity(),
                    closePrice,
                    closeSide
            ).map(closeOrderResult -> {
                if (!closeOrderResult.isSuccess()) {
                    logger.log("[CLOSE] Failed to place close order: " + closeOrderResult.getErrorMessage(), "ERROR");
                    return false;
                }
                return true;
            });
        }

        // Order not filled, need to cancel and potentially replace
        return exchangeClient.cancelOrder(orderId)
                .flatMap(cancelResult -> {
                    BigDecimal partialFill = orderFilledAmount.get();

                    if (partialFill.compareTo(BigDecimal.ZERO) > 0) {
                        String closeSide = config.getCloseOrderSide();
                        BigDecimal closePrice;

                        if ("sell".equalsIgnoreCase(closeSide)) {
                            closePrice = filledPrice.multiply(
                                    BigDecimal.ONE.add(config.getTakeProfit().divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP))
                            );
                        } else {
                            closePrice = filledPrice.multiply(
                                    BigDecimal.ONE.subtract(config.getTakeProfit().divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP))
                            );
                        }

                        return exchangeClient.placeCloseOrder(
                                config.getContractId(),
                                partialFill,
                                closePrice,
                                closeSide
                        ).map(closeOrderResult -> {
                            lastOpenOrderTime = Instant.now();
                            if (!closeOrderResult.isSuccess()) {
                                logger.log("[CLOSE] Failed to place close order: " + closeOrderResult.getErrorMessage(), "ERROR");
                            }
                            return true;
                        });
                    }

                    return Mono.just(true);
                });
    }

    /**
     * Log status information periodically
     */
    private Mono<Boolean> logStatusPeriodically() {
        if (Duration.between(lastLogTime, Instant.now()).getSeconds() > 60 || lastLogTime.equals(Instant.EPOCH)) {
            System.out.println("--------------------------------");

            return exchangeClient.getActiveOrders(config.getContractId())
                    .flatMap(activeOrders -> {
                        // Filter close orders
                        activeCloseOrders.clear();
                        for (OrderInfo order : activeOrders) {
                            if (order.getSide().equalsIgnoreCase(config.getCloseOrderSide())) {
                                activeCloseOrders.add(new OrderTracker(
                                        order.getOrderId(),
                                        order.getPrice(),
                                        order.getSize()
                                ));
                            }
                        }

                        return exchangeClient.getAccountPositions();
                    })
                    .map(positionAmt -> {
                        BigDecimal activeCloseAmount = activeCloseOrders.stream()
                                .map(OrderTracker::getSize)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                        logger.log(String.format("Current Position: %s | Active closing amount: %s | Order quantity: %d",
                                positionAmt, activeCloseAmount, activeCloseOrders.size()), "INFO");

                        lastLogTime = Instant.now();

                        // Check for position mismatch
                        BigDecimal threshold = config.getQuantity().multiply(BigDecimal.valueOf(2));
                        boolean mismatch = positionAmt.subtract(activeCloseAmount).abs().compareTo(threshold) > 0;

                        if (mismatch) {
                            String errorMessage = String.format(
                                    "\n\nERROR: [%s_%s] Position mismatch detected\n" +
                                            "###### ERROR ###### ERROR ###### ERROR ###### ERROR #####\n" +
                                            "Please manually rebalance your position and take-profit orders\n" +
                                            "请手动平衡当前仓位和正在关闭的仓位\n" +
                                            "current position: %s | active closing amount: %s | Order quantity: %d\n" +
                                            "###### ERROR ###### ERROR ###### ERROR ###### ERROR #####\n",
                                    config.getExchange().toUpperCase(),
                                    config.getTicker().toUpperCase(),
                                    positionAmt,
                                    activeCloseAmount,
                                    activeCloseOrders.size()
                            );

                            logger.log(errorMessage, "ERROR");
                            notificationService.sendNotification(errorMessage.trim()).subscribe();
                            shutdownRequested.set(true);
                        }

                        System.out.println("--------------------------------");
                        return mismatch;
                    })
                    .onErrorResume(error -> {
                        logger.log("Error in periodic status check: " + error.getMessage(), "ERROR");
                        log.error("Error in periodic status check", error);
                        return Mono.just(false);
                    });
        }

        return Mono.just(false);
    }

    /**
     * Check if grid step condition is met
     */
    private Mono<Boolean> meetGridStepCondition() {
        if (activeCloseOrders.isEmpty()) {
            return Mono.just(true);
        }

        Comparator<OrderTracker> comparator = config.isBuyDirection()
                ? Comparator.comparing(OrderTracker::getPrice)
                : Comparator.comparing(OrderTracker::getPrice).reversed();

        OrderTracker nextCloseOrder = activeCloseOrders.stream()
                .min(comparator)
                .orElse(null);

        if (nextCloseOrder == null) {
            return Mono.just(true);
        }

        BigDecimal nextClosePrice = nextCloseOrder.getPrice();

        // This method would need to be added to BaseExchangeClient
        // For now, returning true as a placeholder
        return Mono.just(true);
    }

    /**
     * Check price conditions (stop/pause)
     */
    private Mono<PriceConditionResult> checkPriceCondition() {
        if (config.getPausePrice().compareTo(BigDecimal.valueOf(-1)) == 0 &&
                config.getStopPrice().compareTo(BigDecimal.valueOf(-1)) == 0) {
            return Mono.just(new PriceConditionResult(false, false));
        }

        // This would need fetch BBO from exchange
        // Placeholder implementation
        return Mono.just(new PriceConditionResult(false, false));
    }

    /**
     * Perform graceful shutdown
     */
    public Mono<Void> gracefulShutdown(String reason) {
        logger.log("Starting graceful shutdown: " + reason, "INFO");
        shutdownRequested.set(true);

        return exchangeClient.disconnect()
                .doOnSuccess(v -> logger.log("Graceful shutdown completed", "INFO"))
                .doOnError(error -> logger.log("Error during graceful shutdown: " + error.getMessage(), "ERROR"))
                .then();
    }

    /**
     * Main trading loop
     */
    public Mono<Void> run() {
        return Mono.defer(() -> {
            // Log configuration
            logger.log("=== Trading Configuration ===", "INFO");
            logger.log("Ticker: " + config.getTicker(), "INFO");
            logger.log("Contract ID: " + config.getContractId(), "INFO");
            logger.log("Quantity: " + config.getQuantity(), "INFO");
            logger.log("Take Profit: " + config.getTakeProfit() + "%", "INFO");
            logger.log("Direction: " + config.getDirection(), "INFO");
            logger.log("Max Orders: " + config.getMaxOrders(), "INFO");
            logger.log("Wait Time: " + config.getWaitTime() + "s", "INFO");
            logger.log("Exchange: " + config.getExchange(), "INFO");
            logger.log("Grid Step: " + config.getGridStep() + "%", "INFO");
            logger.log("Stop Price: " + config.getStopPrice(), "INFO");
            logger.log("Pause Price: " + config.getPausePrice(), "INFO");
            logger.log("Boost Mode: " + config.isBoostMode(), "INFO");
            logger.log("=============================", "INFO");

            return exchangeClient.connect();
        }).then(Mono.delay(Duration.ofSeconds(5)))
                .thenMany(reactor.core.publisher.Flux.interval(Duration.ofSeconds(1))
                        .takeWhile(tick -> !shutdownRequested.get())
                        .flatMap(tick -> tradingLoopIteration()))
                .then()
                .doFinally(signalType -> {
                    exchangeClient.disconnect().subscribe();
                    logger.log("Trading bot stopped", "INFO");
                });
    }

    /**
     * Single iteration of the trading loop
     */
    private Mono<Void> tradingLoopIteration() {
        return logStatusPeriodically()
                .flatMap(mismatchDetected -> {
                    if (mismatchDetected) {
                        return Mono.empty();
                    }

                    return checkPriceCondition();
                })
                .flatMap(priceCondition -> {
                    if (priceCondition.isStopTrading()) {
                        String msg = String.format("\n\nWARNING: [%s_%s] \nStopped trading due to stop price triggered\n价格已经达到停止交易价格，脚本将停止交易\n",
                                config.getExchange().toUpperCase(), config.getTicker().toUpperCase());
                        notificationService.sendNotification(msg.trim()).subscribe();
                        return gracefulShutdown(msg);
                    }

                    if (priceCondition.isPauseTrading()) {
                        return Mono.delay(Duration.ofSeconds(5)).then();
                    }

                    int waitTime = calculateWaitTime();
                    if (waitTime > 0) {
                        return Mono.delay(Duration.ofSeconds(waitTime)).then();
                    }

                    return meetGridStepCondition()
                            .flatMap(meetCondition -> {
                                if (!meetCondition) {
                                    return Mono.delay(Duration.ofSeconds(1)).then();
                                }

                                return placeAndMonitorOpenOrder()
                                        .doOnSuccess(success -> {
                                            if (success) {
                                                lastCloseOrdersCount++;
                                            }
                                        })
                                        .then();
                            });
                })
                .then()
                .onErrorResume(error -> {
                    logger.log("Critical error: " + error.getMessage(), "ERROR");
                    log.error("Critical error in trading loop", error);
                    return gracefulShutdown("Critical error: " + error.getMessage());
                });
    }

    /**
     * Helper class to track orders
     */
    private static class OrderTracker {
        private final String id;
        private final BigDecimal price;
        private final BigDecimal size;

        public OrderTracker(String id, BigDecimal price, BigDecimal size) {
            this.id = id;
            this.price = price;
            this.size = size;
        }

        public String getId() {
            return id;
        }

        public BigDecimal getPrice() {
            return price;
        }

        public BigDecimal getSize() {
            return size;
        }
    }

    /**
     * Result of price condition check
     */
    private static class PriceConditionResult {
        private final boolean stopTrading;
        private final boolean pauseTrading;

        public PriceConditionResult(boolean stopTrading, boolean pauseTrading) {
            this.stopTrading = stopTrading;
            this.pauseTrading = pauseTrading;
        }

        public boolean isStopTrading() {
            return stopTrading;
        }

        public boolean isPauseTrading() {
            return pauseTrading;
        }
    }
}
