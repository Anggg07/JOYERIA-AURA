package com.joyeria.integration.payment;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MercadoPagoPaymentProviderTest {

    private final MercadoPagoClient client = mock(MercadoPagoClient.class);
    private final PaymentContext context = new PaymentContext(
            "ORD-1", new BigDecimal("215000"), "COP", "Pedido ORD-1",
            "cliente@example.com", "http://localhost:5173/pedido-confirmado/ORD-1", "https://api/notify");

    @Test
    void notConfigured_Throws() {
        MercadoPagoPaymentProvider provider = new MercadoPagoPaymentProvider(client, "", "");
        assertFalse(provider.configured());
        assertThrows(PaymentProcessingException.class, () -> provider.createPreference(context));
        verifyNoInteractions(client);
    }

    @Test
    void configured_CreatesPreferenceParsingResponse() {
        MercadoPagoPaymentProvider provider = new MercadoPagoPaymentProvider(client, "test-token", "https://api/notify");
        assertTrue(provider.configured());
        when(client.createPreference(eq("test-token"), anyMap())).thenReturn(Map.of(
                "id", "PREF-123",
                "init_point", "https://mp/init",
                "sandbox_init_point", "https://mp/sandbox"));

        PaymentPreference preference = provider.createPreference(context);

        assertEquals("PREF-123", preference.preferenceId());
        assertEquals("https://mp/init", preference.initPoint());
        assertEquals("https://mp/sandbox", preference.checkoutUrl());
    }

    @Test
    void missingPreferenceId_Throws() {
        MercadoPagoPaymentProvider provider = new MercadoPagoPaymentProvider(client, "test-token", "");
        when(client.createPreference(eq("test-token"), anyMap())).thenReturn(Map.of("init_point", "https://mp/init"));

        assertThrows(PaymentProcessingException.class, () -> provider.createPreference(context));
    }
}
