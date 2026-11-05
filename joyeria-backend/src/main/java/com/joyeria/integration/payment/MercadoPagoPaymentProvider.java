package com.joyeria.integration.payment;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Proveedor de pago real (Checkout Pro) seleccionado al definir
 * payment.provider=mercadopago. Requiere access token; nunca aprueba un cobro
 * por sí mismo, sólo crea la preferencia y devuelve la URL de la pasarela.
 */
@Component
@Slf4j
@ConditionalOnProperty(name = "payment.provider", havingValue = "mercadopago")
public class MercadoPagoPaymentProvider implements PaymentProvider {

    private final MercadoPagoClient client;
    private final String accessToken;
    private final String notificationUrl;

    public MercadoPagoPaymentProvider(MercadoPagoClient client,
                                      @Value("${payment.mercado-pago.access-token:}") String accessToken,
                                      @Value("${payment.mercado-pago.notification-url:}") String notificationUrl) {
        this.client = client;
        this.accessToken = accessToken;
        this.notificationUrl = notificationUrl;
    }

    @Override
    public String name() {
        return "mercadopago";
    }

    @Override
    public boolean configured() {
        return accessToken != null && !accessToken.isBlank();
    }

    @Override
    public PaymentPreference createPreference(PaymentContext context) {
        if (!configured()) {
            throw new PaymentProcessingException(
                    "Mercado Pago no está configurado (payment.mercado-pago.access-token). El pago no puede procesarse.");
        }

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("title", context.description());
        item.put("quantity", 1);
        item.put("unit_price", context.amount());
        item.put("currency_id", context.currency());

        Map<String, Object> payer = new LinkedHashMap<>();
        payer.put("email", context.payerEmail());

        Map<String, Object> backUrls = new LinkedHashMap<>();
        backUrls.put("success", context.backUrl());
        backUrls.put("failure", context.backUrl());
        backUrls.put("pending", context.backUrl());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("items", java.util.List.of(item));
        body.put("external_reference", context.externalReference());
        body.put("payer", payer);
        body.put("back_urls", backUrls);
        body.put("auto_return", "approved");
        if (notificationUrl != null && !notificationUrl.isBlank()) {
            body.put("notification_url", notificationUrl);
        }

        Map<String, Object> response = client.createPreference(accessToken, body);
        String preferenceId = asString(response.get("id"));
        if (preferenceId == null) {
            throw new PaymentProcessingException("Mercado Pago no devolvió una preferencia válida.");
        }
        return new PaymentPreference(preferenceId,
                asString(response.get("init_point")),
                asString(response.get("sandbox_init_point")));
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
