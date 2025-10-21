package com.perpdex.exchange;

import com.perpdex.config.ExchangeConfig;
import com.perpdex.exchange.apex.ApexClient;
import com.perpdex.exchange.aster.AsterClient;
import com.perpdex.exchange.backpack.BackpackClient;
import com.perpdex.exchange.edgex.EdgexClient;
import com.perpdex.exchange.extended.ExtendedClient;
import com.perpdex.exchange.grvt.GrvtClient;
import com.perpdex.exchange.lighter.LighterClient;
import com.perpdex.exchange.paradex.ParadexClient;
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
        // Backpack - Fully implemented with REST API + WebSocket
        registerExchange("backpack", BackpackClient::new);

        // Lighter - Skeleton implementation (needs REST API + WebSocket)
        registerExchange("lighter", LighterClient::new);

        // EdgeX - Skeleton implementation (needs REST API + WebSocket)
        registerExchange("edgex", EdgexClient::new);

        // Paradex - Skeleton implementation (needs Starknet signatures + API)
        registerExchange("paradex", ParadexClient::new);

        // Aster - Skeleton implementation (needs REST API + WebSocket)
        registerExchange("aster", AsterClient::new);

        // GRVT - Skeleton implementation (needs REST API + WebSocket)
        registerExchange("grvt", GrvtClient::new);

        // Extended - Skeleton implementation (needs REST API + WebSocket)
        registerExchange("extended", ExtendedClient::new);

        // Apex - Skeleton implementation (needs REST API + WebSocket)
        registerExchange("apex", ApexClient::new);
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
