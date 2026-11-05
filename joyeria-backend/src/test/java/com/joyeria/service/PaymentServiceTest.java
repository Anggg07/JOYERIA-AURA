package com.joyeria.service;

import com.joyeria.dto.CheckoutResponse;
import com.joyeria.dto.PaymentRequest;
import com.joyeria.dto.PaymentStatusDTO;
import com.joyeria.exception.ResourceNotFoundException;
import com.joyeria.integration.payment.MercadoPagoClient;
import com.joyeria.integration.payment.MercadoPagoSignatureVerifier;
import com.joyeria.integration.payment.PaymentContext;
import com.joyeria.integration.payment.PaymentPreference;
import com.joyeria.integration.payment.PaymentProcessingException;
import com.joyeria.integration.payment.PaymentProvider;
import com.joyeria.model.Order;
import com.joyeria.model.Payment;
import com.joyeria.model.User;
import com.joyeria.repository.OrderRepository;
import com.joyeria.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private PaymentProvider paymentProvider;
    @Mock private MercadoPagoClient mercadoPagoClient;
    @Mock private MercadoPagoSignatureVerifier signatureVerifier;
    @Mock private PushNotificationService pushNotificationService;
    @InjectMocks private PaymentService paymentService;

    private static final String EMAIL = "cliente@example.com";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentService, "frontendUrl", "http://localhost:5173");
        ReflectionTestUtils.setField(paymentService, "currency", "COP");
        ReflectionTestUtils.setField(paymentService, "mpAccessToken", "token");
        ReflectionTestUtils.setField(paymentService, "webhookSecret", "secret");
        ReflectionTestUtils.setField(paymentService, "notificationUrl", "");
    }

    private User user(String email) {
        User u = new User();
        u.setId(1L);
        u.setEmail(email);
        u.setFirstName("Ana");
        return u;
    }

    private Order order(String status) {
        Order o = new Order();
        o.setId(10L);
        o.setOrderNumber("ORD-ABC123");
        o.setUser(user(EMAIL));
        o.setTotal(new BigDecimal("215000"));
        o.setStatus(Order.OrderStatus.valueOf(status));
        o.setPaymentStatus(Order.PaymentStatus.PENDING);
        return o;
    }

    private Payment payment(Order order, Payment.PaymentStatus status) {
        Payment p = new Payment();
        p.setId(5L);
        p.setOrder(order);
        p.setProvider("sandbox");
        p.setAmount(order.getTotal());
        p.setCurrency("COP");
        p.setStatus(status);
        return p;
    }

    @Test
    void createCheckout_CreatesPreferenceAndReturnsCheckoutUrl() {
        Order order = order("PENDING");
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(paymentRepository.findFirstByOrderIdAndStatusOrderByCreatedAtDesc(10L, Payment.PaymentStatus.APPROVED))
                .thenReturn(Optional.empty());
        when(paymentRepository.findFirstByOrderIdAndStatusOrderByCreatedAtDesc(10L, Payment.PaymentStatus.PENDING))
                .thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));
        when(paymentProvider.name()).thenReturn("sandbox");
        when(paymentProvider.createPreference(any(PaymentContext.class)))
                .thenReturn(new PaymentPreference("PREF-1", "https://mp/init", "https://mp/sandbox-init"));

        CheckoutResponse response = paymentService.createCheckout(EMAIL, new PaymentRequest(10L, "card", null));

        assertEquals("sandbox", response.getProvider());
        assertEquals("PREF-1", response.getPreferenceId());
        assertEquals("https://mp/sandbox-init", response.getCheckoutUrl());
        assertEquals("PENDING", response.getPaymentStatus());
        verify(paymentProvider).createPreference(any(PaymentContext.class));
    }

    @Test
    void createCheckout_NotOwner_ThrowsNotFound() {
        Order order = order("PENDING");
        order.setUser(user("otro@example.com"));
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        assertThrows(ResourceNotFoundException.class,
                () -> paymentService.createCheckout(EMAIL, new PaymentRequest(10L, "card", null)));
        verifyNoInteractions(paymentProvider);
    }

    @Test
    void createCheckout_AlreadyApproved_ReturnsExistingWithoutNewPreference() {
        Order order = order("PENDING");
        Payment approved = payment(order, Payment.PaymentStatus.APPROVED);
        approved.setPreferenceId("PREF-OLD");
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(paymentRepository.findFirstByOrderIdAndStatusOrderByCreatedAtDesc(10L, Payment.PaymentStatus.APPROVED))
                .thenReturn(Optional.of(approved));

        CheckoutResponse response = paymentService.createCheckout(EMAIL, new PaymentRequest(10L, "card", null));

        assertEquals("APPROVED", response.getPaymentStatus());
        assertTrue(response.getCheckoutUrl().endsWith("/pedido-confirmado/ORD-ABC123"));
        verify(paymentProvider, never()).createPreference(any());
    }

    @Test
    void createCheckout_NonPendingOrder_Throws() {
        Order order = order("PAID");
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(paymentRepository.findFirstByOrderIdAndStatusOrderByCreatedAtDesc(10L, Payment.PaymentStatus.APPROVED))
                .thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class,
                () -> paymentService.createCheckout(EMAIL, new PaymentRequest(10L, "card", null)));
    }

    @Test
    void createCheckout_ProviderNotConfigured_Throws() {
        Order order = order("PENDING");
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(paymentRepository.findFirstByOrderIdAndStatusOrderByCreatedAtDesc(10L, Payment.PaymentStatus.APPROVED))
                .thenReturn(Optional.empty());
        when(paymentRepository.findFirstByOrderIdAndStatusOrderByCreatedAtDesc(10L, Payment.PaymentStatus.PENDING))
                .thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));
        when(paymentProvider.name()).thenReturn("mercadopago");
        when(paymentProvider.createPreference(any()))
                .thenThrow(new PaymentProcessingException("Mercado Pago no está configurado"));

        assertThrows(PaymentProcessingException.class,
                () -> paymentService.createCheckout(EMAIL, new PaymentRequest(10L, "card", null)));
    }

    @Test
    void confirmSandbox_ApprovesAndMarksOrderPaid() {
        Order order = order("PENDING");
        Payment pending = payment(order, Payment.PaymentStatus.PENDING);
        pending.setPreferenceId("SANDBOX-1");
        when(paymentProvider.name()).thenReturn("sandbox");
        when(paymentRepository.findByPreferenceId("SANDBOX-1")).thenReturn(Optional.of(pending));

        PaymentStatusDTO status = paymentService.confirmSandbox(EMAIL, null, "SANDBOX-1");

        assertEquals(Payment.PaymentStatus.APPROVED, pending.getStatus());
        assertEquals(Order.OrderStatus.PAID, order.getStatus());
        assertEquals(Order.PaymentStatus.COMPLETED, order.getPaymentStatus());
        assertEquals("PAID", status.getOrderStatus());
        assertEquals("APPROVED", status.getPaymentStatus());
        verify(orderRepository).save(order);
        verify(paymentRepository).save(pending);
    }

    @Test
    void confirmSandbox_WhenProviderNotSandbox_Throws() {
        when(paymentProvider.name()).thenReturn("mercadopago");
        assertThrows(PaymentProcessingException.class,
                () -> paymentService.confirmSandbox(EMAIL, null, "REF"));
        verifyNoInteractions(paymentRepository);
    }

    @Test
    void confirmSandbox_NotOwner_ThrowsNotFound() {
        Order order = order("PENDING");
        order.setUser(user("otro@example.com"));
        Payment pending = payment(order, Payment.PaymentStatus.PENDING);
        when(paymentProvider.name()).thenReturn("sandbox");
        when(paymentRepository.findByPreferenceId("SANDBOX-1")).thenReturn(Optional.of(pending));

        assertThrows(ResourceNotFoundException.class, () -> paymentService.confirmSandbox(EMAIL, null, "SANDBOX-1"));
    }

    @Test
    void webhook_InvalidSignature_Throws() {
        when(signatureVerifier.isValid(any(), any(), eq("123"), eq("secret"))).thenReturn(false);

        assertThrows(PaymentProcessingException.class, () -> paymentService.handleMercadoPagoWebhook(
                "bad", "req", "123", "payment", Map.of()));
        verifyNoInteractions(mercadoPagoClient);
    }

    @Test
    void webhook_ApprovedPayment_MarksOrderPaid() {
        Order order = order("PENDING");
        Payment pending = payment(order, Payment.PaymentStatus.PENDING);
        when(signatureVerifier.isValid(any(), any(), eq("123"), eq("secret"))).thenReturn(true);
        when(mercadoPagoClient.getPayment("token", "123")).thenReturn(Map.of(
                "id", 999, "status", "approved", "external_reference", "ORD-ABC123",
                "transaction_amount", "215000", "currency_id", "COP"));
        when(orderRepository.findByOrderNumber("ORD-ABC123")).thenReturn(Optional.of(order));
        when(paymentRepository.findFirstByOrderIdAndStatusOrderByCreatedAtDesc(10L, Payment.PaymentStatus.PENDING))
                .thenReturn(Optional.of(pending));

        paymentService.handleMercadoPagoWebhook("sig", "req", "123", "payment", Map.of());

        assertEquals(Payment.PaymentStatus.APPROVED, pending.getStatus());
        assertEquals(Order.OrderStatus.PAID, order.getStatus());
        assertEquals("999", pending.getProviderReference());
    }

    @Test
    void webhook_AmountMismatch_DoesNotApprove() {
        Order order = order("PENDING");
        Payment pending = payment(order, Payment.PaymentStatus.PENDING);
        when(signatureVerifier.isValid(any(), any(), eq("123"), eq("secret"))).thenReturn(true);
        when(mercadoPagoClient.getPayment("token", "123")).thenReturn(Map.of(
                "id", 999, "status", "approved", "external_reference", "ORD-ABC123",
                "transaction_amount", "1", "currency_id", "COP"));
        when(orderRepository.findByOrderNumber("ORD-ABC123")).thenReturn(Optional.of(order));
        when(paymentRepository.findFirstByOrderIdAndStatusOrderByCreatedAtDesc(10L, Payment.PaymentStatus.PENDING))
                .thenReturn(Optional.of(pending));

        paymentService.handleMercadoPagoWebhook("sig", "req", "123", "payment", Map.of());

        assertEquals(Payment.PaymentStatus.PENDING, pending.getStatus());
        assertEquals(Order.OrderStatus.PENDING, order.getStatus());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void webhook_NoTypeOrId_IsIgnored() {
        paymentService.handleMercadoPagoWebhook(null, null, null, null, null);
        verifyNoInteractions(mercadoPagoClient);
    }

    @Test
    void getPaymentStatus_NotOwner_ThrowsNotFound() {
        Order order = order("PENDING");
        order.setUser(user("otro@example.com"));
        when(orderRepository.findByOrderNumber("ORD-ABC123")).thenReturn(Optional.of(order));

        assertThrows(ResourceNotFoundException.class,
                () -> paymentService.getPaymentStatus("ORD-ABC123", EMAIL, null, false));
    }
}

