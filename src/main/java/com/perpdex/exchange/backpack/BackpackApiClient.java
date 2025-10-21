package com.perpdex.exchange.backpack;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

/**
 * Backpack REST API client
 */
@Slf4j
public class BackpackApiClient {
    private static final String BASE_URL = "https://api.backpack.exchange";
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String publicKey;
    private final BackpackSignatureUtil signatureUtil;

    public BackpackApiClient(String publicKey, String secretKey) {
        this.publicKey = publicKey;
        this.signatureUtil = new BackpackSignatureUtil(secretKey);
        this.objectMapper = new ObjectMapper();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build();
    }

    /**
     * Get order book depth (public endpoint)
     */
    public Mono<JsonNode> getOrderBook(String symbol) {
        return Mono.fromCallable(() -> {
            String url = BASE_URL + "/api/v1/depth?symbol=" + symbol;

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("Failed to get order book: " + response.code());
                }
                return objectMapper.readTree(response.body().string());
            }
        });
    }

    /**
     * Place order (authenticated endpoint)
     */
    public Mono<JsonNode> placeOrder(String symbol, String side, String orderType,
                                     String quantity, String price, String timeInForce) {
        return Mono.fromCallable(() -> {
            long timestamp = System.currentTimeMillis();

            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("symbol", symbol);
            requestBody.put("side", side);
            requestBody.put("orderType", orderType);
            requestBody.put("quantity", quantity);
            requestBody.put("price", price);
            requestBody.put("timeInForce", timeInForce);

            String bodyString = objectMapper.writeValueAsString(requestBody);
            String signature = signatureUtil.generateSignature("executeOrder", timestamp, 5000);

            RequestBody body = RequestBody.create(
                    bodyString,
                    MediaType.get("application/json")
            );

            Request request = new Request.Builder()
                    .url(BASE_URL + "/api/v1/order")
                    .post(body)
                    .addHeader("X-API-Key", publicKey)
                    .addHeader("X-Signature", signature)
                    .addHeader("X-Timestamp", String.valueOf(timestamp))
                    .addHeader("X-Window", "5000")
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "";
                    throw new IOException("Failed to place order: " + response.code() + " - " + errorBody);
                }
                return objectMapper.readTree(response.body().string());
            }
        });
    }

    /**
     * Cancel order
     */
    public Mono<JsonNode> cancelOrder(String orderId, String symbol) {
        return Mono.fromCallable(() -> {
            long timestamp = System.currentTimeMillis();
            String signature = signatureUtil.generateSignature("cancelOrder", timestamp, 5000);

            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("orderId", orderId);
            requestBody.put("symbol", symbol);

            RequestBody body = RequestBody.create(
                    objectMapper.writeValueAsString(requestBody),
                    MediaType.get("application/json")
            );

            Request request = new Request.Builder()
                    .url(BASE_URL + "/api/v1/order")
                    .delete(body)
                    .addHeader("X-API-Key", publicKey)
                    .addHeader("X-Signature", signature)
                    .addHeader("X-Timestamp", String.valueOf(timestamp))
                    .addHeader("X-Window", "5000")
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("Failed to cancel order: " + response.code());
                }
                return objectMapper.readTree(response.body().string());
            }
        });
    }

    /**
     * Get open orders
     */
    public Mono<JsonNode> getOpenOrders(String symbol) {
        return Mono.fromCallable(() -> {
            long timestamp = System.currentTimeMillis();
            String signature = signatureUtil.generateSignature("getOpenOrders", timestamp, 5000);

            String url = BASE_URL + "/api/v1/orders?symbol=" + symbol;

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .addHeader("X-API-Key", publicKey)
                    .addHeader("X-Signature", signature)
                    .addHeader("X-Timestamp", String.valueOf(timestamp))
                    .addHeader("X-Window", "5000")
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("Failed to get orders: " + response.code());
                }
                return objectMapper.readTree(response.body().string());
            }
        });
    }

    /**
     * Get account balance/positions
     */
    public Mono<JsonNode> getBalance() {
        return Mono.fromCallable(() -> {
            long timestamp = System.currentTimeMillis();
            String signature = signatureUtil.generateSignature("getBalance", timestamp, 5000);

            Request request = new Request.Builder()
                    .url(BASE_URL + "/api/v1/capital")
                    .get()
                    .addHeader("X-API-Key", publicKey)
                    .addHeader("X-Signature", signature)
                    .addHeader("X-Timestamp", String.valueOf(timestamp))
                    .addHeader("X-Window", "5000")
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("Failed to get balance: " + response.code());
                }
                return objectMapper.readTree(response.body().string());
            }
        });
    }

    /**
     * Get markets/tickers
     */
    public Mono<JsonNode> getMarkets() {
        return Mono.fromCallable(() -> {
            Request request = new Request.Builder()
                    .url(BASE_URL + "/api/v1/markets")
                    .get()
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("Failed to get markets: " + response.code());
                }
                return objectMapper.readTree(response.body().string());
            }
        });
    }
}
