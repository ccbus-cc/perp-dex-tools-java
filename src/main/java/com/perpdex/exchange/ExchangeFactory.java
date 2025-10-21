package com.perpdex.exchange;

import com.perpdex.config.ExchangeConfig;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Factory class for creating exchange clients dynamically.
 * Uses a registry pattern to support multiple exchange implementations.
 */
@Slf4j
public class ExchangeFactory {
    private static final Map<String, Function<ExchangeConfig, BaseExchangeClient>> EXCHANGE_REGISTRY = new ConcurrentHashMap<>();

    static {
        // Register all supported exchanges
        // These will be implemented in subsequent steps
        registerExchange("edgex", config -> {
            throw new UnsupportedOperationException("EdgeX client not yet implemented");
        });
        registerExchange("backpack", config -> {
            throw new UnsupportedOperationException("Backpack client not yet implemented");
        });
        registerExchange("paradex", config -> {
            throw new UnsupportedOperationException("Paradex client not yet implemented");
        });
        registerExchange("aster", config -> {
            throw new UnsupportedOperationException("Aster client not yet implemented");
        });
        registerExchange("lighter", config -> {
            throw new UnsupportedOperationException("Lighter client not yet implemented");
        });
        registerExchange("grvt", config -> {
            throw new UnsupportedOperationException("GRVT client not yet implemented");
        });
        registerExchange("extended", config -> {
            throw new UnsupportedOperationException("Extended client not yet implemented");
        });
        registerExchange("apex", config -> {
            throw new UnsupportedOperationException("Apex client not yet implemented");
        });
    }

    /**
     * Create an exchange client instance
     *
     * @param exchangeName Name of the exchange (e.g., 'edgex', 'lighter')
     * @param config       Configuration for the exchange
     * @return Exchange client instance
     * @throws IllegalArgumentException if the exchange is not supported
     */
    public static BaseExchangeClient createExchange(String exchangeName, ExchangeConfig config) {
        String normalizedName = exchangeName.toLowerCase().trim();

        if (!EXCHANGE_REGISTRY.containsKey(normalizedName)) {
            String available = String.join(", ", getSupportedExchanges());
            throw new IllegalArgumentException(
                    String.format("Unsupported exchange: %s. Available exchanges: %s", exchangeName, available)
            );
        }

        try {
            BaseExchangeClient client = EXCHANGE_REGISTRY.get(normalizedName).apply(config);
            log.info("Created exchange client for: {}", exchangeName);
            return client;
        } catch (Exception e) {
            log.error("Failed to create exchange client for: {}", exchangeName, e);
            throw new RuntimeException("Failed to create exchange client: " + exchangeName, e);
        }
    }

    /**
     * Register a new exchange client
     *
     * @param name    Exchange name (will be normalized to lowercase)
     * @param factory Factory function to create the exchange client
     */
    public static void registerExchange(String name, Function<ExchangeConfig, BaseExchangeClient> factory) {
        String normalizedName = name.toLowerCase().trim();
        EXCHANGE_REGISTRY.put(normalizedName, factory);
        log.debug("Registered exchange: {}", normalizedName);
    }

    /**
     * Get list of supported exchanges
     *
     * @return Sorted list of supported exchange names
     */
    public static List<String> getSupportedExchanges() {
        return new ArrayList<>(EXCHANGE_REGISTRY.keySet()).stream()
                .sorted()
                .toList();
    }

    /**
     * Check if an exchange is supported
     *
     * @param exchangeName The exchange name to check
     * @return true if supported
     */
    public static boolean isSupported(String exchangeName) {
        return EXCHANGE_REGISTRY.containsKey(exchangeName.toLowerCase().trim());
    }

    /**
     * Unregister an exchange (useful for testing)
     *
     * @param name Exchange name to unregister
     */
    public static void unregisterExchange(String name) {
        String normalizedName = name.toLowerCase().trim();
        EXCHANGE_REGISTRY.remove(normalizedName);
        log.debug("Unregistered exchange: {}", normalizedName);
    }
}
