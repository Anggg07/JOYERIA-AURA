package com.joyeria.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.joyeria.dto.CheckoutResponse;
import com.joyeria.dto.PaymentRequest;
import com.joyeria.dto.PaymentStatusDTO;
import com.joyeria.dto.SandboxConfirmRequest;
import com.joyeria.exception.GlobalExceptionHandler;
import com.joyeria.service.PaymentService;
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

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private PaymentController paymentController;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    private Authentication userAuth;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(paymentController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();

        userAuth = new UsernamePasswordAuthenticationToken(
                "user@example.com", "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Test
    void createCheckoutReturnsPreference() throws Exception {
        CheckoutResponse checkout = CheckoutResponse.builder()
                .orderId(3L).orderNumber("ORD-3").provider("sandbox").preferenceId("pref-1").amount(new BigDecimal("120000")).currency("COP")
                .build();
        when(paymentService.createCheckout(eq("user@example.com"), any(PaymentRequest.class))).thenReturn(checkout);

        mockMvc.perform(post("/api/payments/checkout")
                        .principal(userAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PaymentRequest(3L, "card", null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.preferenceId").value("pref-1"))
                .andExpect(jsonPath("$.data.provider").value("sandbox"));
    }

    @Test
    void createCheckoutRejectsMissingOrderId() throws Exception {
        mockMvc.perform(post("/api/payments/checkout")
                        .principal(userAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PaymentRequest(null, "card", null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Errores de validación"))
                .andExpect(jsonPath("$.data.orderId").exists());
    }

    @Test
    void webhookProcessesNotification() throws Exception {
        mockMvc.perform(post("/api/payments/webhook")
                        .header("x-signature", "ts=1,v1=abc")
                        .param("type", "payment")
                        .param("data.id", "12345")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Notificación procesada"));
    }

    @Test
    void confirmSandboxUpdatesStatus() throws Exception {
        PaymentStatusDTO status = PaymentStatusDTO.builder()
                .orderId(3L).orderStatus("PAID").paymentStatus("APPROVED").provider("sandbox").build();
        when(paymentService.confirmSandbox("user@example.com", null, "ref-1")).thenReturn(status);

        mockMvc.perform(post("/api/payments/sandbox/confirm")
                        .principal(userAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SandboxConfirmRequest("ref-1", null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paymentStatus").value("APPROVED"));
    }

    @Test
    void confirmSandboxRejectsBlankReference() throws Exception {
        mockMvc.perform(post("/api/payments/sandbox/confirm")
                        .principal(userAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SandboxConfirmRequest("", null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void statusUsesAuthenticatedUser() throws Exception {
        PaymentStatusDTO status = PaymentStatusDTO.builder().orderNumber("ORD-3").paymentStatus("PENDING").build();
        when(paymentService.getPaymentStatus("ORD-3", "user@example.com", null, false)).thenReturn(status);

        mockMvc.perform(get("/api/payments/status/ORD-3").principal(userAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paymentStatus").value("PENDING"));

        org.mockito.Mockito.verify(paymentService).getPaymentStatus("ORD-3", "user@example.com", null, false);
    }
}
