package com.joyeria.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.joyeria.dto.AuthRequest;
import com.joyeria.dto.AuthResponse;
import com.joyeria.dto.RegisterRequest;
import com.joyeria.exception.GlobalExceptionHandler;
import com.joyeria.model.User;
import com.joyeria.model.UserRole;
import com.joyeria.service.AuthService;
import com.joyeria.service.PasswordResetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;
    @Mock
    private PasswordResetService passwordResetService;

    @InjectMocks
    private AuthController authController;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void loginReturnsToken() throws Exception {
        when(authService.login(any(AuthRequest.class)))
                .thenReturn(AuthResponse.builder().token("jwt-token").id(1L).email("a@b.co").fullName("Ana Diaz").role("USER").build());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthRequest("a@b.co", "secret123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value("jwt-token"))
                .andExpect(jsonPath("$.data.role").value("USER"));
    }

    @Test
    void loginRejectsInvalidEmail() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthRequest("no-es-email", "secret123"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Errores de validación"))
                .andExpect(jsonPath("$.data.email").exists());
    }

    @Test
    void registerRejectsWeakPassword() throws Exception {
        RegisterRequest weak = new RegisterRequest("Ana", "Diaz", "ana@b.co", "3001234567", "sololetras", "sololetras");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(weak)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Errores de validación"))
                .andExpect(jsonPath("$.data.password").exists());
    }

    @Test
    void meReturnsUserDTOWhenAuthenticated() throws Exception {
        User user = new User();
        user.setId(7L);
        user.setFirstName("Ana");
        user.setLastName("Diaz");
        user.setEmail("ana@b.co");
        user.setPhone("3001234567");
        user.setRole(UserRole.USER);
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.of(2026, 1, 1, 12, 0));

        when(authService.getCurrentUser("ana@b.co")).thenReturn(user);

        Authentication auth = new UsernamePasswordAuthenticationToken(
                "ana@b.co", "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER")));

        mockMvc.perform(get("/api/auth/me").principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("ana@b.co"))
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andExpect(jsonPath("$.data.id").value(7));
    }

    @Test
    void meReturnsUnauthorizedWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("No autenticado"));
    }
}