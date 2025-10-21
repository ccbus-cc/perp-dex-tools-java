package com.perpdex.bot;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Configuration class for trading parameters
 */
@Data
@Builder
public class TradingConfig {
    /**
     * Trading ticker symbol (e.g., "BTC-PERP")
     */
    private String ticker;

    /**
     * Contract ID on the exchange
     */
    private String contractId;

    /**
     * Quantity per order
     */
    private BigDecimal quantity;

    /**
     * Take profit percentage
     */
    private BigDecimal takeProfit;

    /**
     * Tick size for price rounding
     */
    private BigDecimal tickSize;

    /**
     * Trading direction ("buy" or "sell")
     */
    private String direction;

    /**
     * Maximum number of concurrent orders
     */
    private int maxOrders;

    /**
     * Wait time between orders (seconds)
     */
    private int waitTime;

    /**
     * Exchange name
     */
    private String exchange;

    /**
     * Grid step percentage
     */
    private BigDecimal gridStep;

    /**
     * Stop price (trading stops if reached)
     */
    private BigDecimal stopPrice;

    /**
     * Pause price (trading pauses if reached)
     */
    private BigDecimal pausePrice;

    /**
     * Boost mode (immediate market close)
     */
    @Builder.Default
    private boolean boostMode = false;

    /**
     * Get the close order side based on bot direction
     */
    public String getCloseOrderSide() {
        return "buy".equalsIgnoreCase(direction) ? "sell" : "buy";
    }

    /**
     * Check if direction is buy
     */
    public boolean isBuyDirection() {
        return "buy".equalsIgnoreCase(direction);
    }

    /**
     * Check if direction is sell
     */
    public boolean isSellDirection() {
        return "sell".equalsIgnoreCase(direction);
    }
}
