package com.joyeria.service;

import com.joyeria.model.RefreshToken;
import com.joyeria.model.User;
import com.joyeria.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository tokenRepository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private User testUser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(refreshTokenService, "refreshExpirationMs", 604800000L);
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
    }

    private RefreshToken storedToken(boolean revoked, LocalDateTime expiresAt) {
        RefreshToken token = new RefreshToken();
        token.setUser(testUser);
        token.setTokenHash("hash");
        token.setRevoked(revoked);
        token.setExpiresAt(expiresAt);
        return token;
    }

    @Test
    void issueToken_StoresHashNotPlaintext() {
        String token = refreshTokenService.issueToken(testUser);

        assertNotNull(token);
        assertFalse(token.isBlank());

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(tokenRepository).save(captor.capture());
        RefreshToken saved = captor.getValue();
        assertEquals(64, saved.getTokenHash().length());
        assertNotEquals(token, saved.getTokenHash());
        assertFalse(saved.getRevoked());
        assertNotNull(saved.getExpiresAt());
        assertTrue(saved.getExpiresAt().isAfter(LocalDateTime.now()));
    }

    @Test
    void verifyAndRotate_WithValidToken_RevokesAndReturnsUser() {
        when(tokenRepository.findByTokenHash(any())).thenReturn(
                Optional.of(storedToken(false, LocalDateTime.now().plusDays(7))));

        User user = refreshTokenService.verifyAndRotate("plain-token");

        assertEquals(1L, user.getId());
        verify(tokenRepository).save(argThat(t -> Boolean.TRUE.equals(t.getRevoked())));
        verify(tokenRepository, never()).revokeAllForUser(any());
    }

    @Test
    void verifyAndRotate_WithRevokedToken_RevokesAllUserTokens() {
        when(tokenRepository.findByTokenHash(any())).thenReturn(
                Optional.of(storedToken(true, LocalDateTime.now().plusDays(7))));

        assertThrows(IllegalArgumentException.class,
                () -> refreshTokenService.verifyAndRotate("reused-token"));
        verify(tokenRepository).revokeAllForUser(1L);
    }

    @Test
    void verifyAndRotate_WithExpiredToken_Throws() {
        when(tokenRepository.findByTokenHash(any())).thenReturn(
                Optional.of(storedToken(false, LocalDateTime.now().minusMinutes(1))));

        assertThrows(IllegalArgumentException.class,
                () -> refreshTokenService.verifyAndRotate("expired-token"));
        verify(tokenRepository).save(argThat(t -> Boolean.TRUE.equals(t.getRevoked())));
        verify(tokenRepository, never()).revokeAllForUser(any());
    }

    @Test
    void verifyAndRotate_WithUnknownToken_Throws() {
        when(tokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> refreshTokenService.verifyAndRotate("unknown-token"));
    }

    @Test
    void revoke_WithValidToken_MarksRevoked() {
        when(tokenRepository.findByTokenHash(any())).thenReturn(
                Optional.of(storedToken(false, LocalDateTime.now().plusDays(7))));

        refreshTokenService.revoke("plain-token");

        verify(tokenRepository).save(argThat(t -> Boolean.TRUE.equals(t.getRevoked())));
    }

    @Test
    void revoke_WithNullToken_DoesNothing() {
        refreshTokenService.revoke(null);
        verifyNoInteractions(tokenRepository);
    }
}
