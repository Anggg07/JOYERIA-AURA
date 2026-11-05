package com.joyeria.controller;

import com.joyeria.dto.ApiResponse;
import com.joyeria.dto.PushSubscriptionRequest;
import com.joyeria.dto.VapidPublicKeyResponse;
import com.joyeria.model.User;
import com.joyeria.repository.UserRepository;
import com.joyeria.service.PushNotificationService;
import com.joyeria.service.VapidKeyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/push")
@RequiredArgsConstructor
public class PushController {

    private final PushNotificationService pushNotificationService;
    private final VapidKeyService vapidKeyService;
    private final UserRepository userRepository;

    @GetMapping("/vapid-public-key")
    public ResponseEntity<ApiResponse<VapidPublicKeyResponse>> getVapidPublicKey() {
        VapidPublicKeyResponse resp = new VapidPublicKeyResponse(
                vapidKeyService.getPublicKeyBase64Url(), vapidKeyService.isEnabled());
        return ResponseEntity.ok(ApiResponse.success(resp));
    }

    @PostMapping("/subscribe")
    public ResponseEntity<ApiResponse<Void>> subscribe(
            Authentication authentication, @Valid @RequestBody PushSubscriptionRequest request) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        pushNotificationService.subscribe(user.getId(), request.getEndpoint(),
                request.getP256dh(), request.getAuth(), request.getUserAgent(), user);
        return ResponseEntity.ok(ApiResponse.success("Suscripción registrada", null));
    }

    @DeleteMapping("/subscribe")
    public ResponseEntity<ApiResponse<Void>> unsubscribe(
            Authentication authentication, @RequestParam String endpoint) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        pushNotificationService.unsubscribe(user.getId(), endpoint);
        return ResponseEntity.ok(ApiResponse.success("Suscripción eliminada", null));
    }

    @PostMapping("/test")
    public ResponseEntity<ApiResponse<Void>> test(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        pushNotificationService.sendToUser(user.getId(), "AURA", "Notificación de prueba", "/");
        return ResponseEntity.ok(ApiResponse.success("Notificación enviada", null));
    }
}