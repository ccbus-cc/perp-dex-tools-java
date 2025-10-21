package com.perpdex.helper;

import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Notification service for sending alerts via Telegram and Lark
 */
@Slf4j
public class NotificationService {
    private final OkHttpClient httpClient;
    private final String telegramToken;
    private final String telegramChatId;
    private final String larkToken;

    public NotificationService() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build();

        this.telegramToken = System.getenv("TELEGRAM_BOT_TOKEN");
        this.telegramChatId = System.getenv("TELEGRAM_CHAT_ID");
        this.larkToken = System.getenv("LARK_TOKEN");
    }

    /**
     * Send notification to all configured channels
     */
    public Mono<Void> sendNotification(String message) {
        Mono<Void> telegramMono = telegramToken != null && telegramChatId != null
                ? sendTelegramMessage(message)
                : Mono.empty();

        Mono<Void> larkMono = larkToken != null
                ? sendLarkMessage(message)
                : Mono.empty();

        return Mono.when(telegramMono, larkMono)
                .onErrorResume(error -> {
                    log.error("Failed to send notification", error);
                    return Mono.empty();
                });
    }

    /**
     * Send message via Telegram
     */
    private Mono<Void> sendTelegramMessage(String message) {
        return Mono.fromCallable(() -> {
            String url = String.format("https://api.telegram.org/bot%s/sendMessage", telegramToken);

            String jsonPayload = String.format("{\"chat_id\":\"%s\",\"text\":\"%s\"}",
                    telegramChatId, escapeJson(message));

            RequestBody body = RequestBody.create(
                    jsonPayload,
                    MediaType.get("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.warn("Failed to send Telegram message: {}", response.code());
                }
                return null;
            }
        }).then();
    }

    /**
     * Send message via Lark (Feishu)
     */
    private Mono<Void> sendLarkMessage(String message) {
        return Mono.fromCallable(() -> {
            String url = String.format("https://open.feishu.cn/open-apis/bot/v2/hook/%s", larkToken);

            String jsonPayload = String.format("{\"msg_type\":\"text\",\"content\":{\"text\":\"%s\"}}",
                    escapeJson(message));

            RequestBody body = RequestBody.create(
                    jsonPayload,
                    MediaType.get("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.warn("Failed to send Lark message: {}", response.code());
                }
                return null;
            }
        }).then();
    }

    /**
     * Escape JSON special characters
     */
    private String escapeJson(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * Close the HTTP client
     */
    public void close() {
        httpClient.dispatcher().executorService().shutdown();
        httpClient.connectionPool().evictAll();
    }
}
