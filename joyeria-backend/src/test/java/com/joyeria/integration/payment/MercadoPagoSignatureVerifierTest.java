package com.joyeria.integration.payment;

import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.*;

class MercadoPagoSignatureVerifierTest {

    private final MercadoPagoSignatureVerifier verifier = new MercadoPagoSignatureVerifier();

    private String sign(String secret, String manifest) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(manifest.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void validSignature_IsAccepted() throws Exception {
        String secret = "super-secret";
        String ts = "1704908010";
        String dataId = "123456";
        String requestId = "req-1";
        String manifest = "id:" + dataId + ";request-id:" + requestId + ";ts:" + ts + ";";
        String v1 = sign(secret, manifest);
        String header = "ts=" + ts + ",v1=" + v1;

        assertTrue(verifier.isValid(header, requestId, dataId, secret));
    }

    @Test
    void tamperedHash_IsRejected() throws Exception {
        String secret = "super-secret";
        String manifest = "id:123456;request-id:req-1;ts:1704908010;";
        String v1 = sign(secret, manifest);
        String header = "ts=1704908010,v1=" + v1;

        assertFalse(verifier.isValid(header, "req-1", "999999", secret));
    }

    @Test
    void missingSecret_IsRejected() {
        assertFalse(verifier.isValid("ts=1,v1=abc", "req", "123", ""));
        assertFalse(verifier.isValid("ts=1,v1=abc", "req", "123", null));
    }

    @Test
    void malformedHeader_IsRejected() {
        assertFalse(verifier.isValid("garbage", "req", "123", "secret"));
        assertFalse(verifier.isValid(null, "req", "123", "secret"));
    }
}
