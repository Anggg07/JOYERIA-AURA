package com.joyeria.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;

@Service
@Slf4j
public class VapidKeyService {

    @Value("${push.vapid.public-key:}")
    private String configuredPublicKey;

    @Value("${push.vapid.private-key:}")
    private String configuredPrivateKey;

    @Value("${push.vapid.subject:mailto:admin@aurajoyeria.com}")
    private String subject;

    @Getter
    private String publicKeyBase64Url;

    @Getter
    private PrivateKey privateKey;

    @Getter
    private boolean enabled;

    @jakarta.annotation.PostConstruct
    public void init() {
        try {
            if (configuredPublicKey != null && !configuredPublicKey.isBlank()
                    && configuredPrivateKey != null && !configuredPrivateKey.isBlank()) {
                publicKeyBase64Url = configuredPublicKey.trim();
                privateKey = loadPrivateKey(configuredPrivateKey.trim());
                enabled = true;
                log.info("VAPID cargado desde configuración.");
            } else {
                KeyPair kp = generateKeyPair();
                this.privateKey = kp.getPrivate();
                this.publicKeyBase64Url = encodePublicKey(kp.getPublic());
                enabled = true;
                log.warn("VAPID no configurado: se generó un par de claves efímero. Clave pública: {}", publicKeyBase64Url);
            }
        } catch (Exception e) {
            enabled = false;
            log.warn("No se pudo inicializar VAPID: {}", e.getMessage());
        }
    }

    public String getSubject() {
        return subject;
    }

    private KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
        kpg.initialize(new ECGenParameterSpec("secp256r1"));
        return kpg.generateKeyPair();
    }

    private PrivateKey loadPrivateKey(String base64Url) throws Exception {
        byte[] bytes = Base64.getUrlDecoder().decode(base64Url);
        java.security.KeyFactory kf = java.security.KeyFactory.getInstance("EC");
        try {
            return kf.generatePrivate(new java.security.spec.PKCS8EncodedKeySpec(bytes));
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo cargar la clave privada VAPID.", e);
        }
    }

    private String encodePublicKey(PublicKey publicKey) {
        byte[] encoded = publicKey.getEncoded();
        int idx = -1;
        for (int i = encoded.length - 65; i >= 0; i--) {
            if (encoded[i] == 0x04) { idx = i; break; }
        }
        if (idx < 0 || idx + 65 > encoded.length) {
            throw new IllegalStateException("No se pudo extraer el punto público EC.");
        }
        byte[] point = java.util.Arrays.copyOfRange(encoded, idx, idx + 65);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(point);
    }
}