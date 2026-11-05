package com.joyeria.service;

import com.joyeria.exception.ResourceNotFoundException;
import com.joyeria.model.PasswordResetToken;
import com.joyeria.model.User;
import com.joyeria.repository.PasswordResetTokenRepository;
import com.joyeria.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final long EXPIRATION_HOURS = 2;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public void requestPasswordReset(String email) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return;
        }
        tokenRepository.deleteByUserId(user.getId());

        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setTokenHash(sha256Hex(token));
        resetToken.setExpiresAt(LocalDateTime.now().plusHours(EXPIRATION_HOURS));
        resetToken.setUsed(false);
        tokenRepository.save(resetToken);

        emailService.sendPasswordReset(user, token);
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        String tokenHash = sha256Hex(token);
        PasswordResetToken resetToken = tokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException("Token inválido o expirado"));

        if (Boolean.TRUE.equals(resetToken.getUsed()) || resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            tokenRepository.delete(resetToken);
            throw new IllegalArgumentException("Token inválido o expirado");
        }

        User user = userRepository.findById(resetToken.getUser().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetToken.setUsed(true);
        tokenRepository.save(resetToken);
        tokenRepository.deleteByUserId(user.getId());
        refreshTokenService.revokeAllForUser(user.getId());
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