package com.perpdex.exchange.backpack;

import com.fasterxml.jackson.databind.JsonNode;
import com.perpdex.config.ExchangeConfig;
import com.perpdex.exchange.BaseExchangeClient;
import com.perpdex.model.OrderInfo;
import com.perpdex.model.OrderResult;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Backpack exchange client implementation
 */
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
                    .then(Mono.delay(Duration.ofSeconds(2)))
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

                    final BigDecimal finalOrderPrice = roundToTick(orderPrice);

                    return apiClient.placeOrder(
                            contractId,
                            side,
                            "Limit",
                            quantity.toPlainString(),
                            finalOrderPrice.toPlainString(),
                            "PostOnly"
                    ).map(response -> {
                        String orderId = response.path("id").asText();
                        String status = response.path("status").asText();

                        return OrderResult.builder()
                                .success(true)
                                .orderId(orderId)
                                .side(direction)
                                .size(quantity)
                                .price(finalOrderPrice)
                                .status(status)
                                .build();
                    }).onErrorResume(error -> {
                        if (error.getMessage().contains("POST_ONLY")) {
                            // Retry on POST_ONLY rejection
                            return Mono.delay(Duration.ofMillis(100))
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
                quantity.toPlainString(),
                price.toPlainString(),
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
        // Backpack doesn't have a single order endpoint, so get from open orders
        return getActiveOrders(config.getContractId())
                .map(orders -> orders.stream()
                        .filter(order -> order.getOrderId().equals(orderId))
                        .findFirst()
                        .orElse(null));
    }

    @Override
    public Mono<List<OrderInfo>> getActiveOrders(String contractId) {
        return apiClient.getOpenOrders(contractId)
                .map(response -> {
                    List<OrderInfo> orders = new ArrayList<>();
                    JsonNode ordersArray = response.path("orders");

                    if (ordersArray.isArray()) {
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
                    }

                    return orders;
                })
                .onErrorResume(error -> {
                    log.error("Failed to get active orders", error);
                    return Mono.just(new ArrayList<>());
                });
    }

    @Override
    public Mono<BigDecimal> getAccountPositions() {
        return apiClient.getBalance()
                .map(response -> {
                    // Parse balance/position from response
                    // This depends on Backpack's response format
                    JsonNode balances = response.path("balances");
                    // For perpetuals, need to sum up positions
                    return BigDecimal.ZERO; // Placeholder
                })
                .onErrorResume(error -> {
                    log.error("Failed to get account positions", error);
                    return Mono.just(BigDecimal.ZERO);
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
        return wsManager != null && wsManager.isConnected();
    }

    /**
     * Fetch best bid and ask prices
     */
    private Mono<BigDecimal[]> fetchBestBidAsk(String symbol) {
        return apiClient.getOrderBook(symbol)
                .map(orderBook -> {
                    JsonNode bids = orderBook.path("bids");
                    JsonNode asks = orderBook.path("asks");

                    BigDecimal bestBid = bids.isArray() && bids.size() > 0
                            ? new BigDecimal(bids.get(0).get(0).asText())
                            : BigDecimal.ZERO;

                    BigDecimal bestAsk = asks.isArray() && asks.size() > 0
                            ? new BigDecimal(asks.get(0).get(0).asText())
                            : BigDecimal.ZERO;

                    return new BigDecimal[]{bestBid, bestAsk};
                });
    }
}
