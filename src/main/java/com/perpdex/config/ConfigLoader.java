package com.perpdex.config;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration loader for loading exchange configurations from environment variables
 */
@Slf4j
public class ConfigLoader {

    /**
     * Load exchange configuration from environment variables
     */
    public static ExchangeConfig loadExchangeConfig(String exchangeName) {
        String upperExchange = exchangeName.toUpperCase();

        ExchangeConfig.ExchangeConfigBuilder builder = ExchangeConfig.builder()
                .exchangeName(exchangeName);

        // Load common configuration
        String apiKey = getEnvOrDefault(upperExchange + "_API_KEY", "API_KEY");
        String apiSecret = getEnvOrDefault(upperExchange + "_API_SECRET", "API_SECRET");
        String privateKey = getEnvOrDefault(upperExchange + "_PRIVATE_KEY", "PRIVATE_KEY", "API_KEY_PRIVATE_KEY");

        builder.apiKey(apiKey);
        builder.apiSecret(apiSecret);
        builder.privateKey(privateKey);

        // Load exchange-specific URLs
        String baseUrl = System.getenv(upperExchange + "_BASE_URL");
        String wsUrl = System.getenv(upperExchange + "_WS_URL");

        if (baseUrl != null) {
            builder.baseUrl(baseUrl);
        }
        if (wsUrl != null) {
            builder.wsUrl(wsUrl);
        }

        // Load additional parameters
        Map<String, Object> additionalParams = new HashMap<>();

        // Lighter-specific parameters
        if ("lighter".equalsIgnoreCase(exchangeName)) {
            String accountIndex = getEnvOrDefault("LIGHTER_ACCOUNT_INDEX", "0");
            String apiKeyIndex = getEnvOrDefault("LIGHTER_API_KEY_INDEX", "0");

            additionalParams.put("accountIndex", Integer.parseInt(accountIndex));
            additionalParams.put("apiKeyIndex", Integer.parseInt(apiKeyIndex));

            builder.baseUrl("https://mainnet.zklighter.elliot.ai");
        }

        // Backpack-specific parameters
        else if ("backpack".equalsIgnoreCase(exchangeName)) {
            String accountIndex = getEnvOrDefault("BACKPACK_ACCOUNT_INDEX", "0");
            additionalParams.put("accountIndex", Integer.parseInt(accountIndex));
        }

        // Add more exchange-specific configurations as needed

        builder.additionalParams(additionalParams);

        // Load request timeout
        String timeoutStr = System.getenv("REQUEST_TIMEOUT_MS");
        if (timeoutStr != null) {
            try {
                builder.requestTimeoutMs(Long.parseLong(timeoutStr));
            } catch (NumberFormatException e) {
                log.warn("Invalid REQUEST_TIMEOUT_MS value: {}, using default", timeoutStr);
            }
        }

        return builder.build();
    }

    /**
     * Get environment variable with fallback options
     */
    private static String getEnvOrDefault(String... keys) {
        for (String key : keys) {
            String value = System.getenv(key);
            if (value != null && !value.isEmpty()) {
                return value;
            }
        }
        return null;
    }

    /**
     * Get environment variable as integer with default
     */
    public static int getEnvAsInt(String key, int defaultValue) {
        String value = System.getenv(key);
        if (value != null && !value.isEmpty()) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                log.warn("Invalid integer value for {}: {}, using default: {}", key, value, defaultValue);
            }
        }
        return defaultValue;
    }

    /**
     * Get environment variable as BigDecimal with default
     */
    public static BigDecimal getEnvAsBigDecimal(String key, BigDecimal defaultValue) {
        String value = System.getenv(key);
        if (value != null && !value.isEmpty()) {
            try {
                return new BigDecimal(value);
            } catch (NumberFormatException e) {
                log.warn("Invalid decimal value for {}: {}, using default: {}", key, value, defaultValue);
            }
        }
        return defaultValue;
    }

    /**
     * Get environment variable as boolean with default
     */
    public static boolean getEnvAsBoolean(String key, boolean defaultValue) {
        String value = System.getenv(key);
        if (value != null && !value.isEmpty()) {
            return Boolean.parseBoolean(value);
        }
        return defaultValue;
    }

    /**
     * Validate required environment variables
     */
    public static void validateRequiredEnvVars(String... requiredVars) {
        StringBuilder missing = new StringBuilder();

        for (String var : requiredVars) {
            if (System.getenv(var) == null || System.getenv(var).isEmpty()) {
                if (missing.length() > 0) {
                    missing.append(", ");
                }
                missing.append(var);
            }
        }

        if (missing.length() > 0) {
            throw new IllegalStateException("Missing required environment variables: " + missing);
        }
    }
}
