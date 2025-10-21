package com.perpdex.model;

/**
 * Enum representing order sides
 */
public enum OrderSide {
    BUY,
    SELL;

    public static OrderSide fromString(String side) {
        if (side == null) {
            return null;
        }
        String normalized = side.toUpperCase().trim();
        return switch (normalized) {
            case "BUY", "LONG", "BID" -> BUY;
            case "SELL", "SHORT", "ASK" -> SELL;
            default -> throw new IllegalArgumentException("Unknown order side: " + side);
        };
    }

    /**
     * Returns the opposite side
     */
    public OrderSide opposite() {
        return this == BUY ? SELL : BUY;
    }
}
