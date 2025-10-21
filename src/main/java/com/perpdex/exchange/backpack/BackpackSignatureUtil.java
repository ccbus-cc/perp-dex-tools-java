package com.perpdex.exchange.backpack;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Utility class for generating ED25519 signatures for Backpack API
 */
@Slf4j
public class BackpackSignatureUtil {
    private final String secretKey;

    public BackpackSignatureUtil(String base64SecretKey) {
        this.secretKey = base64SecretKey;
    }

    /**
     * Generate signature for Backpack API requests
     * Note: Backpack uses ED25519 signatures. This is a simplified implementation.
     * For production, use a proper ED25519 library like net.i2p.crypto:eddsa
     */
    public String generateSignature(String instruction, long timestamp, int window) {
        try {
            String message = String.format("instruction=%s&timestamp=%d&window=%d",
                    instruction, timestamp, window);

            // Decode the secret key
            byte[] keyBytes = Base64.getDecoder().decode(secretKey);

            // For now, using HMAC-SHA256 as placeholder
            // TODO: Replace with proper ED25519 implementation
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, "HmacSHA256");
            mac.init(secretKeySpec);

            byte[] signature = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature);

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Failed to generate signature", e);
            throw new RuntimeException("Signature generation failed", e);
        }
    }

    /**
     * Generate signature for WebSocket authentication
     */
    public String generateWebSocketSignature(String instruction, long timestamp) {
        return generateSignature(instruction, timestamp, 5000);
    }
}
