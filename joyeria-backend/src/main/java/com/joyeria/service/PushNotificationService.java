package com.joyeria.service;

import com.joyeria.model.PushSubscription;
import com.joyeria.model.User;
import com.joyeria.repository.PushSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.Signature;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PushNotificationService {

    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final VapidKeyService vapidKeyService;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Transactional
    public void subscribe(Long userId, String endpoint, String p256dh, String auth, String userAgent, User user) {
        pushSubscriptionRepository.findByEndpoint(endpoint).ifPresent(existing -> {
            pushSubscriptionRepository.delete(existing);
            pushSubscriptionRepository.flush();
        });
        PushSubscription sub = PushSubscription.builder()
                .user(user)
                .endpoint(endpoint)
                .p256dh(p256dh)
                .auth(auth)
                .userAgent(userAgent)
                .build();
        pushSubscriptionRepository.save(sub);
    }

    @Transactional
    public void unsubscribe(Long userId, String endpoint) {
        pushSubscriptionRepository.findByEndpoint(endpoint).ifPresent(sub -> {
            if (sub.getUser().getId().equals(userId)) {
                pushSubscriptionRepository.delete(sub);
            }
        });
    }

    public void sendToUser(Long userId, String title, String body, String url) {
        if (!vapidKeyService.isEnabled()) {
            log.debug("Push deshabilitado (VAPID no disponible).");
            return;
        }
        List<PushSubscription> subs = pushSubscriptionRepository.findByUserId(userId);
        for (PushSubscription sub : subs) {
            sendToSubscription(sub, title, body, url);
        }
    }

    private void sendToSubscription(PushSubscription sub, String title, String body, String url) {
        try {
            URI endpointUri = URI.create(sub.getEndpoint());
            String origin = endpointUri.getScheme() + "://" + endpointUri.getHost()
                    + (endpointUri.getPort() > 0 ? ":" + endpointUri.getPort() : "");

            String jwt = createVapidJwt(origin);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(endpointUri)
                    .header("Authorization", "vapid t=" + jwt + ", k=" + vapidKeyService.getPublicKeyBase64Url())
                    .header("TTL", "86400")
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());

            if (response.statusCode() == 201 || response.statusCode() == 200) {
                sub.setLastUsedAt(java.time.LocalDateTime.now());
                pushSubscriptionRepository.save(sub);
            } else if (response.statusCode() == 410 || response.statusCode() == 404) {
                log.info("Suscripción push expirada ({}), eliminando.", response.statusCode());
                pushSubscriptionRepository.delete(sub);
            } else {
                log.warn("Push respondió {} para {}", response.statusCode(), sub.getEndpoint());
            }
        } catch (Exception e) {
            log.warn("No se pudo enviar push a {}: {}", sub.getEndpoint(), e.getMessage());
        }
    }

    private String createVapidJwt(String audience) throws Exception {
        PrivateKey privateKey = vapidKeyService.getPrivateKey();
        long now = Instant.now().getEpochSecond();
        long exp = now + 12 * 3600;

        String headerJson = "{\"typ\":\"JWT\",\"alg\":\"ES256\"}";
        String payloadJson = "{\"aud\":\"" + audience + "\",\"exp\":" + exp + ",\"sub\":\"" + vapidKeyService.getSubject() + "\"}";

        String header = base64Url(headerJson.getBytes(StandardCharsets.UTF_8));
        String payload = base64Url(payloadJson.getBytes(StandardCharsets.UTF_8));
        String signingInput = header + "." + payload;

        Signature signature = Signature.getInstance("SHA256withECDSA");
        signature.initSign(privateKey);
        signature.update(signingInput.getBytes(StandardCharsets.UTF_8));
        byte[] derSig = signature.sign();
        byte[] rawSig = derToRaw(derSig);

        String sig = base64Url(rawSig);
        return signingInput + "." + sig;
    }

    private byte[] derToRaw(byte[] der) throws Exception {
        int offset = 2;
        if (der[offset] != 0x02) throw new IllegalArgumentException("DER inválido");
        offset++;
        int rLen = der[offset++] & 0xFF;
        byte[] r = new byte[rLen];
        System.arraycopy(der, offset, r, 0, rLen);
        offset += rLen;
        if (der[offset] != 0x02) throw new IllegalArgumentException("DER inválido");
        offset++;
        int sLen = der[offset++] & 0xFF;
        byte[] s = new byte[sLen];
        System.arraycopy(der, offset, s, 0, sLen);

        byte[] raw = new byte[64];
        int rStart = r.length > 32 ? r.length - 32 : 0;
        int rDest = 32 - (r.length - rStart);
        System.arraycopy(r, rStart, raw, rDest, r.length - rStart);
        int sStart = s.length > 32 ? s.length - 32 : 0;
        int sDest = 64 - (s.length - sStart);
        System.arraycopy(s, sStart, raw, sDest, s.length - sStart);
        return raw;
    }

    private String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}