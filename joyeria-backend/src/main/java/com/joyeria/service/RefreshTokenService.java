package com.joyeria.service;

import com.joyeria.exception.SessionExpiredException;
import com.joyeria.model.RefreshToken;
import com.joyeria.model.User;
import com.joyeria.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RefreshTokenRepository tokenRepository;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpirationMs;

    public String issueToken(User user) {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(sha256Hex(token));
        refreshToken.setExpiresAt(LocalDateTime.now().plus(Duration.ofMillis(refreshExpirationMs)));
        refreshToken.setRevoked(false);
        tokenRepository.save(refreshToken);

        return token;
    }

    @Transactional
    public User verifyAndRotate(String token) {
        if (token == null || token.isBlank()) {
            throw new SessionExpiredException("Sesión inválida");
        }
        RefreshToken stored = tokenRepository.findByTokenHash(sha256Hex(token))
                .orElseThrow(() -> new SessionExpiredException("Sesión inválida"));

        if (Boolean.TRUE.equals(stored.getRevoked())) {
            tokenRepository.revokeAllForUser(stored.getUser().getId());
            throw new SessionExpiredException("Sesión inválida");
        }
        if (stored.getExpiresAt().isBefore(LocalDateTime.now())) {
            stored.setRevoked(true);
            tokenRepository.save(stored);
            throw new SessionExpiredException("Sesión inválida");
        }

        stored.setRevoked(true);
        tokenRepository.save(stored);
        return stored.getUser();
    }

    public void revoke(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        tokenRepository.findByTokenHash(sha256Hex(token)).ifPresent(stored -> {
            if (!Boolean.TRUE.equals(stored.getRevoked())) {
                stored.setRevoked(true);
                tokenRepository.save(stored);
            }
        });
    }

    public void revokeAllForUser(Long userId) {
        tokenRepository.revokeAllForUser(userId);
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
