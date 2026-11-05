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
import com.joyeria.repository.OrderRepository;
import com.joyeria.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Orquesta el ciclo de pago. El pedido NUNCA se marca como pagado aquí a partir
 * de datos del cliente: sólo la pasarela (webhook firmado de Mercado Pago o el
 * flujo de confirmación sandbox) puede aprobarlo.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private static final String PROVIDER_SANDBOX = "sandbox";

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final PaymentProvider paymentProvider;
    private final MercadoPagoClient mercadoPagoClient;
    private final MercadoPagoSignatureVerifier signatureVerifier;
    private final PushNotificationService pushNotificationService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${payment.currency:COP}")
    private String currency;

    @Value("${payment.mercado-pago.access-token:}")
    private String mpAccessToken;

    @Value("${payment.mercado-pago.webhook-secret:}")
    private String webhookSecret;

    @Value("${payment.mercado-pago.notification-url:}")
    private String notificationUrl;

    @Transactional
    public CheckoutResponse createCheckout(String userEmail, PaymentRequest request) {
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado"));
        if (order.getUser() != null) {
            if (userEmail == null || !order.getUser().getEmail().equals(userEmail)) {
                throw new ResourceNotFoundException("Pedido no encontrado");
            }
        } else {
            if (request.getGuestToken() == null || !request.getGuestToken().equals(order.getGuestToken())) {
                throw new ResourceNotFoundException("Pedido no encontrado");
            }
        }
        String email = order.getUser() != null ? order.getUser().getEmail() : order.getGuestEmail();

        Payment approved = paymentRepository
                .findFirstByOrderIdAndStatusOrderByCreatedAtDesc(order.getId(), Payment.PaymentStatus.APPROVED)
                .orElse(null);
        if (approved != null) {
            return buildResponse(order, approved, confirmationUrl(order));
        }

        if (order.getStatus() != Order.OrderStatus.PENDING) {
            throw new IllegalStateException("El pedido no está pendiente de pago");
        }

        Payment payment = paymentRepository
                .findFirstByOrderIdAndStatusOrderByCreatedAtDesc(order.getId(), Payment.PaymentStatus.PENDING)
                .orElseGet(Payment::new);
        payment.setOrder(order);
        payment.setProvider(paymentProvider.name());
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setAmount(order.getTotal());
        payment.setCurrency(currency);
        payment.setStatus(Payment.PaymentStatus.PENDING);
        payment = paymentRepository.save(payment);

        PaymentContext context = new PaymentContext(
                order.getOrderNumber(),
                order.getTotal(),
                currency,
                "Pedido " + order.getOrderNumber(),
                email,
                confirmationUrl(order),
                (notificationUrl == null || notificationUrl.isBlank()) ? null : notificationUrl);

        PaymentPreference preference = paymentProvider.createPreference(context);
        payment.setPreferenceId(preference.preferenceId());
        payment = paymentRepository.save(payment);

        return buildResponse(order, payment, preference.checkoutUrl());
    }

    /**
     * Procesa una notificación (webhook) de Mercado Pago. Verifica la firma y
     * reconcilia el monto antes de aprobar el pedido. Idempotente.
     */
    @Transactional
    public void handleMercadoPagoWebhook(String xSignature, String xRequestId, String dataId,
                                         String type, Map<String, Object> body) {
        if (body == null) {
            body = Map.of();
        }
        String notificationType = type != null ? type : asString(body.get("type"));
        String paymentId = dataId != null ? dataId : nestedId(body);
        if (!"payment".equals(notificationType) || paymentId == null) {
            log.debug("Webhook de Mercado Pago ignorado (tipo={}, id={})", notificationType, paymentId);
            return;
        }

        if (!signatureVerifier.isValid(xSignature, xRequestId, paymentId, webhookSecret)) {
            throw new PaymentProcessingException("Firma de webhook inválida");
        }
        if (mpAccessToken == null || mpAccessToken.isBlank()) {
            throw new PaymentProcessingException("Mercado Pago no está configurado para verificar pagos");
        }

        Map<String, Object> mpPayment = mercadoPagoClient.getPayment(mpAccessToken, paymentId);
        String status = asString(mpPayment.get("status"));
        String externalReference = asString(mpPayment.get("external_reference"));
        BigDecimal amount = asDecimal(mpPayment.get("transaction_amount"));
        String mpCurrency = asString(mpPayment.get("currency_id"));

        if (externalReference == null) {
            log.warn("Webhook sin external_reference, se ignora");
            return;
        }
        Order order = orderRepository.findByOrderNumber(externalReference).orElse(null);
        if (order == null) {
            log.warn("Webhook para pedido inexistente {}", externalReference);
            return;
        }

        Payment payment = paymentRepository
                .findFirstByOrderIdAndStatusOrderByCreatedAtDesc(order.getId(), Payment.PaymentStatus.PENDING)
                .orElseGet(() -> paymentRepository.findByOrderId(order.getId()).stream().findFirst().orElse(null));
        if (payment == null) {
            log.warn("Webhook sin pago asociado para el pedido {}", externalReference);
            return;
        }

        if (payment.getStatus() == Payment.PaymentStatus.APPROVED) {
            return; // idempotente
        }

        if (!amountMatches(payment.getAmount(), amount) || (mpCurrency != null && !mpCurrency.equalsIgnoreCase(payment.getCurrency()))) {
            log.error("Reconciliación fallida para pedido {}: esperado {} {}, recibido {} {}",
                    externalReference, payment.getAmount(), payment.getCurrency(), amount, mpCurrency);
            return;
        }

        switch (status == null ? "" : status) {
            case "approved" -> approve(order, payment, String.valueOf(mpPayment.get("id")));
            case "rejected" -> reject(order, payment, Payment.PaymentStatus.REJECTED);
            case "cancelled" -> reject(order, payment, Payment.PaymentStatus.CANCELLED);
            case "refunded" -> reject(order, payment, Payment.PaymentStatus.REFUNDED);
            default -> payment.setStatus(Payment.PaymentStatus.IN_PROCESS);
        }
        paymentRepository.save(payment);
    }

    /** Confirmación del flujo sandbox (solo desarrollo). Simula el callback de la pasarela. */
    @Transactional
    public PaymentStatusDTO confirmSandbox(String userEmail, String guestToken, String preferenceId) {
        if (!PROVIDER_SANDBOX.equals(paymentProvider.name())) {
            throw new PaymentProcessingException("La confirmación sandbox no está disponible");
        }
        Payment payment = paymentRepository.findByPreferenceId(preferenceId)
                .orElseThrow(() -> new ResourceNotFoundException("Pago no encontrado"));
        Order order = payment.getOrder();
        if (order.getUser() != null) {
            if (userEmail == null || !order.getUser().getEmail().equals(userEmail)) {
                throw new ResourceNotFoundException("Pago no encontrado");
            }
        } else {
            if (guestToken == null || !guestToken.equals(order.getGuestToken())) {
                throw new ResourceNotFoundException("Pago no encontrado");
            }
        }
        approve(order, payment, "SANDBOX-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase());
        return toStatus(order, payment);
    }

    @Transactional(readOnly = true)
    public PaymentStatusDTO getPaymentStatus(String orderNumber, String userEmail, String guestToken, boolean isAdmin) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado"));
        if (!isAdmin) {
            if (order.getUser() != null) {
                if (userEmail == null || !order.getUser().getEmail().equals(userEmail)) {
                    throw new ResourceNotFoundException("Pedido no encontrado");
                }
            } else {
                if (guestToken == null || !guestToken.equals(order.getGuestToken())) {
                    throw new ResourceNotFoundException("Pedido no encontrado");
                }
            }
        }
        Payment payment = paymentRepository.findByOrderId(order.getId()).stream().findFirst().orElse(null);
        return toStatus(order, payment);
    }

    private void approve(Order order, Payment payment, String providerReference) {
        if (payment.getStatus() == Payment.PaymentStatus.APPROVED) {
            return;
        }
        payment.setStatus(Payment.PaymentStatus.APPROVED);
        if (providerReference != null) {
            payment.setProviderReference(providerReference);
        }
        paymentRepository.save(payment);

        order.setStatus(Order.OrderStatus.PAID);
        order.setPaymentStatus(Order.PaymentStatus.COMPLETED);
        orderRepository.save(order);
        if (order.getUser() != null) {
            pushNotificationService.sendToUser(order.getUser().getId(), "Pago aprobado",
                    "Tu pago para el pedido " + order.getOrderNumber() + " fue aprobado.", "/pedido-confirmado/" + order.getOrderNumber());
        }
        log.info("Pago aprobado para pedido {} (ref {})", order.getOrderNumber(), providerReference);
    }

    private void reject(Order order, Payment payment, Payment.PaymentStatus status) {
        payment.setStatus(status);
        order.setPaymentStatus(Order.PaymentStatus.FAILED);
        orderRepository.save(order);
        log.warn("Pago {} para pedido {}", status, order.getOrderNumber());
    }

    private boolean amountMatches(BigDecimal expected, BigDecimal received) {
        if (expected == null || received == null) {
            return false;
        }
        return expected.compareTo(received) == 0;
    }

    private String confirmationUrl(Order order) {
        return frontendUrl + "/pedido-confirmado/" + order.getOrderNumber();
    }

    private CheckoutResponse buildResponse(Order order, Payment payment, String checkoutUrl) {
        return CheckoutResponse.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .provider(payment.getProvider())
                .preferenceId(payment.getPreferenceId())
                .checkoutUrl(checkoutUrl)
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .paymentStatus(payment.getStatus().name())
                .build();
    }

    private PaymentStatusDTO toStatus(Order order, Payment payment) {
        return PaymentStatusDTO.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .orderStatus(order.getStatus().name())
                .paymentStatus(payment != null ? payment.getStatus().name() : order.getPaymentStatus().name())
                .provider(payment != null ? payment.getProvider() : null)
                .paymentMethod(payment != null ? payment.getPaymentMethod() : order.getPaymentMethod())
                .amount(payment != null ? payment.getAmount() : order.getTotal())
                .currency(payment != null ? payment.getCurrency() : currency)
                .build();
    }

    private String nestedId(Map<String, Object> body) {
        Object data = body.get("data");
        if (data instanceof Map<?, ?> map) {
            Object id = map.get("id");
            return id == null ? null : String.valueOf(id);
        }
        return null;
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private BigDecimal asDecimal(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
