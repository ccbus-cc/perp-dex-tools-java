package com.perpdex.config;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Configuration for exchange clients
 */
@Data
@Builder
public class ExchangeConfig {
    /**
     * Exchange name (e.g., "lighter", "edgex", "backpack")
     */
    private String exchangeName;

    /**
     * API key
     */
    private String apiKey;

    /**
     * API secret
     */
    private String apiSecret;

    /**
     * Private key (for some exchanges)
     */
    private String privateKey;

    /**
     * Contract ID to trade
     */
    private String contractId;

    /**
     * Tick size for price rounding
     */
    private BigDecimal tickSize;

    /**
     * Base URL for REST API
     */
    private String baseUrl;

    /**
     * WebSocket URL
     */
    private String wsUrl;

    /**
     * Additional exchange-specific parameters
     */
    private Map<String, Object> additionalParams;

    /**
     * Whether to enable WebSocket order updates
     */
    @Builder.Default
    private boolean enableOrderUpdates = true;

    /**
     * Request timeout in milliseconds
     */
    @Builder.Default
    private long requestTimeoutMs = 10000;

    /**
     * Gets an additional parameter
     */
    public Object getAdditionalParam(String key) {
        return additionalParams != null ? additionalParams.get(key) : null;
    }

    /**
     * Gets an additional parameter with a default value
     */
    @SuppressWarnings("unchecked")
    public <T> T getAdditionalParam(String key, T defaultValue) {
        if (additionalParams == null || !additionalParams.containsKey(key)) {
            return defaultValue;
        }
        return (T) additionalParams.get(key);
    }
}
