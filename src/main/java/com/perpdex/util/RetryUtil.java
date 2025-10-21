package com.perpdex.util;

import dev.failsafe.Failsafe;
import dev.failsafe.RetryPolicy;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * Utility class for retry logic using Failsafe library.
 * Provides similar functionality to Python's tenacity @retry decorator.
 */
@Slf4j
public class RetryUtil {

    /**
     * Default retry policy for query operations
     * - Max 5 attempts
     * - Exponential backoff (1s to 10s)
     * - Retries on all exceptions
     */
    public static final RetryPolicy<Object> DEFAULT_QUERY_RETRY_POLICY = RetryPolicy.builder()
            .handle(Exception.class)
            .withMaxRetries(5)
            .withBackoff(Duration.ofSeconds(1), Duration.ofSeconds(10))
            .onRetry(event -> log.warn(
                    "Operation failed (attempt {}/{}): {}",
                    event.getAttemptCount(),
                    event.getAttemptCount() < 5 ? 5 : event.getAttemptCount(),
                    event.getLastException() != null ? event.getLastException().getMessage() : "Unknown error"
            ))
            .onFailure(event -> log.error(
                    "Operation failed after {} retries: {}",
                    event.getAttemptCount(),
                    event.getException() != null ? event.getException().getMessage() : "Unknown error"
            ))
            .build();

    /**
     * Execute a synchronous operation with retry logic
     *
     * @param operation     The operation to execute
     * @param retryPolicy   The retry policy to use
     * @param defaultReturn Default value to return on failure
     * @param <T>           Return type
     * @return The result or default value
     */
    public static <T> T executeWithRetry(
            Supplier<T> operation,
            RetryPolicy<T> retryPolicy,
            T defaultReturn
    ) {
        try {
            return Failsafe.with(retryPolicy).get(operation::get);
        } catch (Exception e) {
            log.error("Operation failed after all retries, returning default value", e);
            return defaultReturn;
        }
    }

    /**
     * Execute a synchronous operation with default retry policy
     *
     * @param operation     The operation to execute
     * @param defaultReturn Default value to return on failure
     * @param <T>           Return type
     * @return The result or default value
     */
    public static <T> T executeWithRetry(Supplier<T> operation, T defaultReturn) {
        @SuppressWarnings("unchecked")
        RetryPolicy<T> policy = (RetryPolicy<T>) DEFAULT_QUERY_RETRY_POLICY;
        return executeWithRetry(operation, policy, defaultReturn);
    }

    /**
     * Execute a reactive operation with retry logic
     *
     * @param operation   The reactive operation to execute
     * @param maxRetries  Maximum number of retries
     * @param minBackoff  Minimum backoff duration
     * @param maxBackoff  Maximum backoff duration
     * @param <T>         Return type
     * @return Mono with retry logic applied
     */
    public static <T> Mono<T> executeReactiveWithRetry(
            Mono<T> operation,
            int maxRetries,
            Duration minBackoff,
            Duration maxBackoff
    ) {
        return operation
                .retry(maxRetries)
                .retryWhen(reactor.util.retry.Retry.backoff(maxRetries, minBackoff)
                        .maxBackoff(maxBackoff)
                        .doBeforeRetry(signal -> log.warn(
                                "Retrying operation (attempt {}): {}",
                                signal.totalRetries() + 1,
                                signal.failure().getMessage()
                        ))
                )
                .doOnError(error -> log.error(
                        "Operation failed after {} retries: {}",
                        maxRetries,
                        error.getMessage()
                ));
    }

    /**
     * Execute a reactive operation with default retry settings
     *
     * @param operation The reactive operation to execute
     * @param <T>       Return type
     * @return Mono with retry logic applied
     */
    public static <T> Mono<T> executeReactiveWithRetry(Mono<T> operation) {
        return executeReactiveWithRetry(
                operation,
                5,
                Duration.ofSeconds(1),
                Duration.ofSeconds(10)
        );
    }

    /**
     * Create a custom retry policy for specific needs
     *
     * @param <T> Return type
     * @param maxRetries Maximum number of retries
     * @param minBackoff Minimum backoff duration
     * @param maxBackoff Maximum backoff duration
     * @return RetryPolicy
     */
    public static <T> RetryPolicy<T> customRetryPolicy(int maxRetries, Duration minBackoff, Duration maxBackoff) {
        return RetryPolicy.<T>builder()
                .handle(Exception.class)
                .withMaxRetries(maxRetries)
                .withBackoff(minBackoff, maxBackoff)
                .build();
    }
}
