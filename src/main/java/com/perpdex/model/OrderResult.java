package com.perpdex.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Standardized order result structure.
 * Represents the result of an order operation (place, cancel, etc.).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResult {
    /**
     * Whether the operation was successful
     */
    private boolean success;

    /**
     * The order ID (if created)
     */
    private String orderId;

    /**
     * The order side (BUY or SELL)
     */
    private String side;

    /**
     * The order size/quantity
     */
    private BigDecimal size;

    /**
     * The order price
     */
    private BigDecimal price;

    /**
     * The order status
     */
    private String status;

    /**
     * Error message if operation failed
     */
    private String errorMessage;

    /**
     * The filled size of the order
     */
    private BigDecimal filledSize;

    /**
     * Creates a failed order result with an error message
     */
    public static OrderResult failure(String errorMessage) {
        return OrderResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }

    /**
     * Creates a successful order result
     */
    public static OrderResult success(String orderId, String side, BigDecimal size, BigDecimal price, String status) {
        return OrderResult.builder()
                .success(true)
                .orderId(orderId)
                .side(side)
                .size(size)
                .price(price)
                .status(status)
                .build();
    }
}
