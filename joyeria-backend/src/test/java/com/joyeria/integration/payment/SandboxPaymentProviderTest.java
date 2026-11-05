package com.joyeria.integration.payment;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class SandboxPaymentProviderTest {

    private final PaymentContext context = new PaymentContext(
            "ORD-1", new BigDecimal("215000"), "COP", "Pedido ORD-1",
            "cliente@example.com", "http://localhost:5173/pedido-confirmado/ORD-1", null);

    @Test
    void createPreference_ReturnsSandboxCheckout() {
        MockEnvironment env = new MockEnvironment();
        SandboxPaymentProvider provider = new SandboxPaymentProvider(env, "http://localhost:5173");

        assertTrue(provider.configured());
        PaymentPreference preference = provider.createPreference(context);
        assertTrue(preference.preferenceId().startsWith("SANDBOX-"));
        assertTrue(preference.checkoutUrl().contains("/pago/sandbox?ref="));
    }

    @Test
    void createPreference_InProd_Throws() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");
        SandboxPaymentProvider provider = new SandboxPaymentProvider(env, "http://localhost:5173");

        assertFalse(provider.configured());
        assertThrows(PaymentProcessingException.class, () -> provider.createPreference(context));
    }
}
