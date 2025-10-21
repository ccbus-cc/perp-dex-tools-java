package com.perpdex.exchange;

import com.perpdex.config.ExchangeConfig;
import com.perpdex.model.OrderInfo;
import com.perpdex.model.OrderResult;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.function.Consumer;

/**
 * Base class for all exchange clients.
 * All exchange implementations should extend this class.
 */
@Slf4j
@Getter
public abstract class BaseExchangeClient {
    protected final ExchangeConfig config;

    protected BaseExchangeClient(ExchangeConfig config) {
        this.config = config;
        validateConfig();
    }

    /**
     * Rounds a price to the exchange's tick size
     */
    public BigDecimal roundToTick(BigDecimal price) {
        if (price == null) {
            return null;
        }
        BigDecimal tickSize = config.getTickSize();
        if (tickSize == null) {
            return price;
        }
        // Calculate how many ticks and round
        return price.divide(tickSize, 0, RoundingMode.HALF_UP).multiply(tickSize);
    }

    /**
     * Validate the exchange-specific configuration
     */
    protected abstract void validateConfig();

    /**
     * Connect to the exchange (WebSocket, etc.)
     */
    public abstract Mono<Void> connect();

    /**
     * Disconnect from the exchange
     */
    public abstract Mono<Void> disconnect();

    /**
     * Place an open order (market or limit based on exchange)
     *
     * @param contractId The contract to trade
     * @param quantity   The quantity to trade
     * @param direction  The direction (BUY or SELL)
     * @return Order result
     */
    public abstract Mono<OrderResult> placeOpenOrder(String contractId, BigDecimal quantity, String direction);

    /**
     * Place a close order (to close a position)
     *
     * @param contractId The contract to trade
     * @param quantity   The quantity to close
     * @param price      The price
     * @param side       The side (BUY or SELL)
     * @return Order result
     */
    public abstract Mono<OrderResult> placeCloseOrder(String contractId, BigDecimal quantity, BigDecimal price, String side);

    /**
     * Cancel an order
     *
     * @param orderId The order ID to cancel
     * @return Order result
     */
    public abstract Mono<OrderResult> cancelOrder(String orderId);

    /**
     * Get order information
     *
     * @param orderId The order ID
     * @return Order information, or null if not found
     */
    public abstract Mono<OrderInfo> getOrderInfo(String orderId);

    /**
     * Get active orders for a contract
     *
     * @param contractId The contract ID
     * @return List of active orders
     */
    public abstract Mono<List<OrderInfo>> getActiveOrders(String contractId);

    /**
     * Get account positions
     *
     * @return Current position size (positive for long, negative for short)
     */
    public abstract Mono<BigDecimal> getAccountPositions();

    /**
     * Setup order update handler for WebSocket
     *
     * @param handler The handler to call when order updates are received
     */
    public abstract void setupOrderUpdateHandler(Consumer<OrderInfo> handler);

    /**
     * Get the exchange name
     *
     * @return The exchange name
     */
    public abstract String getExchangeName();

    /**
     * Check if the exchange is connected
     *
     * @return true if connected
     */
    public abstract boolean isConnected();
}
