#!/bin/bash

# Script to create skeleton implementations for remaining exchanges

EXCHANGES=("edgex" "paradex" "aster" "grvt" "extended" "apex")
BASE_DIR="/home/whereq/git/perp-dex-tools-java/src/main/java/com/perpdex/exchange"

for exchange in "${EXCHANGES[@]}"; do
    EXCHANGE_CAP="$(tr '[:lower:]' '[:upper:]' <<< ${exchange:0:1})${exchange:1}"
    FILE="${BASE_DIR}/${exchange}/${EXCHANGE_CAP}Client.java"
    
    cat > "$FILE" << JAVA
package com.perpdex.exchange.${exchange};

import com.perpdex.config.ExchangeConfig;
import com.perpdex.exchange.BaseExchangeClient;
import com.perpdex.model.OrderInfo;
import com.perpdex.model.OrderResult;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * ${EXCHANGE_CAP} exchange client implementation
 * 
 * Reference: /home/whereq/git/perp-dex-tools-java/exchanges/${exchange}.py
 * TODO: Implement REST API and WebSocket functionality
 */
@Slf4j
public class ${EXCHANGE_CAP}Client extends BaseExchangeClient {
    private Consumer<OrderInfo> orderUpdateHandler;

    public ${EXCHANGE_CAP}Client(ExchangeConfig config) {
        super(config);
        log.info("${EXCHANGE_CAP} client initialized");
    }

    @Override
    protected void validateConfig() {
        // TODO: Validate required configuration
    }

    @Override
    public Mono<Void> connect() {
        log.warn("${EXCHANGE_CAP} connect() not yet implemented");
        return Mono.empty();
    }

    @Override
    public Mono<Void> disconnect() {
        log.warn("${EXCHANGE_CAP} disconnect() not yet implemented");
        return Mono.empty();
    }

    @Override
    public Mono<OrderResult> placeOpenOrder(String contractId, BigDecimal quantity, String direction) {
        log.warn("${EXCHANGE_CAP} placeOpenOrder() not yet implemented");
        return Mono.just(OrderResult.failure("Not implemented"));
    }

    @Override
    public Mono<OrderResult> placeCloseOrder(String contractId, BigDecimal quantity, BigDecimal price, String side) {
        log.warn("${EXCHANGE_CAP} placeCloseOrder() not yet implemented");
        return Mono.just(OrderResult.failure("Not implemented"));
    }

    @Override
    public Mono<OrderResult> cancelOrder(String orderId) {
        log.warn("${EXCHANGE_CAP} cancelOrder() not yet implemented");
        return Mono.just(OrderResult.failure("Not implemented"));
    }

    @Override
    public Mono<OrderInfo> getOrderInfo(String orderId) {
        log.warn("${EXCHANGE_CAP} getOrderInfo() not yet implemented");
        return Mono.empty();
    }

    @Override
    public Mono<List<OrderInfo>> getActiveOrders(String contractId) {
        log.warn("${EXCHANGE_CAP} getActiveOrders() not yet implemented");
        return Mono.just(new ArrayList<>());
    }

    @Override
    public Mono<BigDecimal> getAccountPositions() {
        log.warn("${EXCHANGE_CAP} getAccountPositions() not yet implemented");
        return Mono.just(BigDecimal.ZERO);
    }

    @Override
    public void setupOrderUpdateHandler(Consumer<OrderInfo> handler) {
        this.orderUpdateHandler = handler;
    }

    @Override
    public String getExchangeName() {
        return "${exchange}";
    }

    @Override
    public boolean isConnected() {
        return false;
    }
}
JAVA

    echo "Created $FILE"
done

echo "All exchange skeletons created!"
