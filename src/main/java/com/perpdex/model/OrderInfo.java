package com.perpdex.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Standardized order information structure.
 * Contains detailed information about an order.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderInfo {
    /**
     * The unique order identifier
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
     * The order status (PENDING, FILLED, CANCELLED, etc.)
     */
    private String status;

    /**
     * The filled size of the order
     */
    @Builder.Default
    private BigDecimal filledSize = BigDecimal.ZERO;

    /**
     * The remaining size to be filled
     */
    @Builder.Default
    private BigDecimal remainingSize = BigDecimal.ZERO;

    /**
     * The reason for cancellation (if applicable)
     */
    @Builder.Default
    private String cancelReason = "";

    /**
     * Checks if the order is fully filled
     */
    public boolean isFullyFilled() {
        return filledSize != null && size != null && filledSize.compareTo(size) >= 0;
    }

    /**
     * Checks if the order is cancelled
     */
    public boolean isCancelled() {
        return status != null && (status.equalsIgnoreCase("CANCELLED") ||
                                 status.equalsIgnoreCase("CANCELED"));
    }

    /**
     * Checks if the order is active (pending or partially filled)
     */
    public boolean isActive() {
        return status != null && !isCancelled() && !isFullyFilled();
    }
}
