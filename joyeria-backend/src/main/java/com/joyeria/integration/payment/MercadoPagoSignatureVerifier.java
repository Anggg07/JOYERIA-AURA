package com.joyeria.integration.payment;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Verifica la firma de los webhooks de Mercado Pago.
 * Manifest: {@code id:<data.id>;request-id:<x-request-id>;ts:<ts>;} firmado con
 * HMAC-SHA256 usando el secreto del webhook; se compara con el header
 * {@code x-signature} formato {@code ts=<ts>,v1=<hash>}.
 */
@Component
@Slf4j
public class MercadoPagoSignatureVerifier {

    public boolean isValid(String xSignature, String xRequestId, String dataId, String secret) {
        if (secret == null || secret.isBlank() || xSignature == null || xSignature.isBlank()) {
            return false;
        }
        String ts = extract(xSignature, "ts");
        String v1 = extract(xSignature, "v1");
        if (ts == null || v1 == null) {
            return false;
        }
        String manifest = "id:" + normalize(dataId) + ";request-id:" + normalize(xRequestId) + ";ts:" + ts + ";";
        String expected = hmacSha256Hex(secret, manifest);
        if (expected == null) {
            return false;
        }
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                v1.getBytes(StandardCharsets.UTF_8));
    }

    private String extract(String header, String key) {
        for (String part : header.split(",")) {
            String[] kv = part.trim().split("=", 2);
            if (kv.length == 2 && kv[0].equals(key)) {
                return kv[1].trim();
            }
        }
        return null;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase();
    }

    private String hmacSha256Hex(String secret, String message) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            log.error("No se pudo calcular la firma HMAC", e);
            return null;
        }
    }
}
