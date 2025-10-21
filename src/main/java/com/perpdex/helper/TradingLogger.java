package com.perpdex.helper;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Trading logger with structured output and error handling.
 * Provides console logging and CSV transaction logging.
 */
@Slf4j
public class TradingLogger {
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String[] CSV_HEADERS = {"Timestamp", "OrderID", "Side", "Quantity", "Price", "Status"};

    private final String exchange;
    private final String ticker;
    private final Path logFile;
    private final Path debugLogFile;
    private final boolean logToConsole;
    private final ZoneId timezone;

    public TradingLogger(String exchange, String ticker, boolean logToConsole) {
        this.exchange = exchange;
        this.ticker = ticker;
        this.logToConsole = logToConsole;

        // Get timezone from environment or use default
        String timezoneStr = System.getenv().getOrDefault("TIMEZONE", "Asia/Shanghai");
        this.timezone = ZoneId.of(timezoneStr);

        // Create logs directory
        Path logsDir = Paths.get("logs");
        try {
            Files.createDirectories(logsDir);
        } catch (IOException e) {
            log.error("Failed to create logs directory", e);
        }

        // Determine log file names
        String accountName = System.getenv("ACCOUNT_NAME");
        String orderFileName;
        String debugLogFileName;

        if (accountName != null && !accountName.isEmpty()) {
            orderFileName = String.format("%s_%s_%s_orders.csv", exchange, ticker, accountName);
            debugLogFileName = String.format("%s_%s_%s_activity.log", exchange, ticker, accountName);
        } else {
            orderFileName = String.format("%s_%s_orders.csv", exchange, ticker);
            debugLogFileName = String.format("%s_%s_activity.log", exchange, ticker);
        }

        this.logFile = logsDir.resolve(orderFileName);
        this.debugLogFile = logsDir.resolve(debugLogFileName);

        // Initialize CSV file with headers if it doesn't exist
        if (!Files.exists(logFile)) {
            try (CSVPrinter printer = new CSVPrinter(new FileWriter(logFile.toFile()), CSVFormat.DEFAULT)) {
                printer.printRecord((Object[]) CSV_HEADERS);
            } catch (IOException e) {
                log.error("Failed to initialize CSV log file", e);
            }
        }
    }

    /**
     * Log a message with the specified level
     */
    public void log(String message, String level) {
        String formattedMessage = String.format("[%s_%s] %s",
                exchange.toUpperCase(),
                ticker.toUpperCase(),
                message);

        LocalDateTime now = LocalDateTime.now(timezone);
        String timestamp = now.format(TIMESTAMP_FORMATTER);
        String logMessage = String.format("%s - %s - %s", timestamp, level.toUpperCase(), formattedMessage);

        // Log to console if enabled
        if (logToConsole) {
            switch (level.toUpperCase()) {
                case "DEBUG" -> log.debug(formattedMessage);
                case "INFO" -> log.info(formattedMessage);
                case "WARNING", "WARN" -> log.warn(formattedMessage);
                case "ERROR" -> log.error(formattedMessage);
                default -> log.info(formattedMessage);
            }
        }

        // Log to file
        try (FileWriter writer = new FileWriter(debugLogFile.toFile(), true)) {
            writer.write(logMessage + System.lineSeparator());
        } catch (IOException e) {
            log.error("Failed to write to debug log file", e);
        }
    }

    /**
     * Log a transaction to CSV file
     */
    public void logTransaction(String orderId, String side, BigDecimal quantity, BigDecimal price, String status) {
        try {
            LocalDateTime now = LocalDateTime.now(timezone);
            String timestamp = now.format(TIMESTAMP_FORMATTER);

            try (CSVPrinter printer = new CSVPrinter(new FileWriter(logFile.toFile(), true), CSVFormat.DEFAULT)) {
                printer.printRecord(timestamp, orderId, side, quantity, price, status);
            }

        } catch (IOException e) {
            log("Failed to log transaction: " + e.getMessage(), "ERROR");
            log.error("Failed to log transaction", e);
        }
    }

    /**
     * Log informational message (convenience method)
     */
    public void info(String message) {
        log(message, "INFO");
    }

    /**
     * Log warning message (convenience method)
     */
    public void warn(String message) {
        log(message, "WARNING");
    }

    /**
     * Log error message (convenience method)
     */
    public void error(String message) {
        log(message, "ERROR");
    }

    /**
     * Log debug message (convenience method)
     */
    public void debug(String message) {
        log(message, "DEBUG");
    }
}
