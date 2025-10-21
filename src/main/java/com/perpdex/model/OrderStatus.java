package com.perpdex.model;

/**
 * Enum representing order status
 */
public enum OrderStatus {
    PENDING,
    OPEN,
    PARTIALLY_FILLED,
    FILLED,
    CANCELLED,
    REJECTED,
    EXPIRED;

    public static OrderStatus fromString(String status) {
        if (status == null) {
            return null;
        }
        String normalized = status.toUpperCase().trim();
        return switch (normalized) {
            case "PENDING", "NEW", "ACCEPTED" -> PENDING;
            case "OPEN", "ACTIVE" -> OPEN;
            case "PARTIALLY_FILLED", "PARTIAL", "PARTIAL_FILL" -> PARTIALLY_FILLED;
            case "FILLED", "COMPLETED", "COMPLETE" -> FILLED;
            case "CANCELLED", "CANCELED" -> CANCELLED;
            case "REJECTED" -> REJECTED;
            case "EXPIRED" -> EXPIRED;
            default -> throw new IllegalArgumentException("Unknown order status: " + status);
        };
    }

    public boolean isTerminal() {
        return this == FILLED || this == CANCELLED || this == REJECTED || this == EXPIRED;
    }

    public boolean isActive() {
        return this == PENDING || this == OPEN || this == PARTIALLY_FILLED;
    }
}
