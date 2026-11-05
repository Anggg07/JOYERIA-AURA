package com.joyeria.service;

import com.joyeria.dto.AuthRequest;
import com.joyeria.dto.AuthResponse;
import com.joyeria.dto.RegisterRequest;
import com.joyeria.model.User;
import com.joyeria.model.UserRole;
import com.joyeria.repository.UserRepository;
import com.joyeria.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenProvider tokenProvider;
    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private AuthRequest loginRequest;
    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setEmail("test@example.com");
        testUser.setPhone("3001234567");
        testUser.setPassword("encoded_password");
        testUser.setRole(UserRole.USER);
        testUser.setActive(true);

        loginRequest = new AuthRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password123");

        registerRequest = new RegisterRequest();
        registerRequest.setFirstName("Test");
        registerRequest.setLastName("User");
        registerRequest.setEmail("test@example.com");
        registerRequest.setPhone("3001234567");
        registerRequest.setPassword("password123");
        registerRequest.setConfirmPassword("password123");
    }

    @Test
    void login_WithValidCredentials_ReturnsToken() {
        Authentication authentication = new UsernamePasswordAuthenticationToken("test@example.com", null);
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(tokenProvider.generateToken(authentication)).thenReturn("jwt-token");
        when(refreshTokenService.issueToken(testUser)).thenReturn("refresh-token");

        AuthResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
        assertEquals("refresh-token", response.getRefreshToken());
        assertEquals("test@example.com", response.getEmail());
    }

    @Test
    void login_WithInactiveUser_ThrowsException() {
        testUser.setActive(false);
        Authentication authentication = new UsernamePasswordAuthenticationToken("test@example.com", null);
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        assertThrows(IllegalStateException.class, () -> authService.login(loginRequest));
    }

    @Test
    void register_WithMismatchedPasswords_ThrowsException() {
        registerRequest.setConfirmPassword("different");

        assertThrows(IllegalArgumentException.class, () -> authService.register(registerRequest));
    }

    @Test
    void register_WithExistingEmail_ThrowsException() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> authService.register(registerRequest));
    }

    @Test
    void register_WithValidData_CreatesUser() {
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(userRepository.existsByPhone("3001234567")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(tokenProvider.generateToken("test@example.com")).thenReturn("jwt-token");
        when(refreshTokenService.issueToken(testUser)).thenReturn("refresh-token");

        AuthResponse response = authService.register(registerRequest);

        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
        assertEquals("refresh-token", response.getRefreshToken());
    }

    @Test
    void refresh_WithValidToken_ReturnsNewTokens() {
        when(refreshTokenService.verifyAndRotate("valid-refresh")).thenReturn(testUser);
        when(tokenProvider.generateToken("test@example.com")).thenReturn("new-jwt");
        when(refreshTokenService.issueToken(testUser)).thenReturn("new-refresh");

        AuthResponse response = authService.refresh("valid-refresh");

        assertEquals("new-jwt", response.getToken());
        assertEquals("new-refresh", response.getRefreshToken());
        assertEquals(1L, response.getId());
    }

    @Test
    void refresh_WithInvalidToken_ThrowsException() {
        when(refreshTokenService.verifyAndRotate("bad-refresh"))
                .thenThrow(new IllegalArgumentException("Sesión inválida"));

        assertThrows(IllegalArgumentException.class, () -> authService.refresh("bad-refresh"));
    }

    @Test
    void logout_WithToken_RevokesRefreshToken() {
        authService.logout("some-refresh");

        org.mockito.Mockito.verify(refreshTokenService).revoke("some-refresh");
    }
}
