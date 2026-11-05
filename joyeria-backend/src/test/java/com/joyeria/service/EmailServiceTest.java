package com.joyeria.service;

import com.joyeria.event.OrderCreatedEvent;
import com.joyeria.model.User;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailService(mailSender);
        ReflectionTestUtils.setField(emailService, "frontendUrl", "http://localhost:5173");
        ReflectionTestUtils.setField(emailService, "from", "no-reply@aurajoyeria.com");
        ReflectionTestUtils.setField(emailService, "mailHost", "");
        ReflectionTestUtils.setField(emailService, "emailEnabled", false);
        ReflectionTestUtils.setField(emailService, "logLink", false);
    }

    private User user() {
        User u = new User();
        u.setEmail("cliente@example.com");
        u.setFirstName("Ana");
        return u;
    }

    @Test
    void isConfigured_RequiresEnabledFlagAndHost() {
        assertFalse(emailService.isConfigured());
        ReflectionTestUtils.setField(emailService, "mailHost", "smtp.example.com");
        assertFalse(emailService.isConfigured());
        ReflectionTestUtils.setField(emailService, "emailEnabled", true);
        assertTrue(emailService.isConfigured());
    }

    @Test
    void sendPasswordReset_WhenNotConfigured_DoesNotSendAndDoesNotThrow() {
        emailService.sendPasswordReset(user(), "token-123");
        verifyNoInteractions(mailSender);
    }

    @Test
    void sendPasswordReset_WhenConfigured_SendsEmail() {
        ReflectionTestUtils.setField(emailService, "mailHost", "smtp.example.com");
        ReflectionTestUtils.setField(emailService, "emailEnabled", true);
        when(mailSender.createMimeMessage()).thenReturn(mock(MimeMessage.class));

        emailService.sendPasswordReset(user(), "token-123");

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void onOrderCreated_WhenConfigured_SendsConfirmation() {
        ReflectionTestUtils.setField(emailService, "mailHost", "smtp.example.com");
        ReflectionTestUtils.setField(emailService, "emailEnabled", true);
        when(mailSender.createMimeMessage()).thenReturn(mock(MimeMessage.class));

        emailService.onOrderCreated(new OrderCreatedEvent(
                "cliente@example.com", "Ana", "ORD-ABCD1234", new BigDecimal("215000")));

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void onOrderCreated_WhenNotConfigured_DoesNotSend() {
        emailService.onOrderCreated(new OrderCreatedEvent(
                "cliente@example.com", "Ana", "ORD-ABCD1234", new BigDecimal("215000")));
        verifyNoInteractions(mailSender);
    }
}
