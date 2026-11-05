package com.joyeria.integration.payment;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.UUID;

/**
 * Proveedor de desarrollo: simula la pasarela sin realizar ningún cobro real.
 * Se activa cuando payment.provider es "sandbox" (por defecto) y NUNCA en el
 * perfil "prod", donde se rechaza para no regalar mercancía.
 */
@Component
@Slf4j
@ConditionalOnProperty(name = "payment.provider", havingValue = "sandbox", matchIfMissing = true)
public class SandboxPaymentProvider implements PaymentProvider {

    private final Environment environment;
    private final String frontendUrl;

    public SandboxPaymentProvider(Environment environment,
                                  @Value("${app.frontend-url}") String frontendUrl) {
        this.environment = environment;
        this.frontendUrl = frontendUrl;
    }

    @Override
    public String name() {
        return "sandbox";
    }

    @Override
    public boolean configured() {
        return !isProd();
    }

    @Override
    public PaymentPreference createPreference(PaymentContext context) {
        if (isProd()) {
            throw new PaymentProcessingException(
                    "El modo sandbox no está permitido en producción. Configura una pasarela de pago real.");
        }
        String preferenceId = "SANDBOX-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        log.warn("CHECKOUT SIMULADO (sandbox): pedido {} por {} {}. No se realizará ningún cobro real.",
                context.externalReference(), context.amount(), context.currency());
        String initPoint = frontendUrl + "/pago/sandbox?ref=" + preferenceId;
        return new PaymentPreference(preferenceId, initPoint, initPoint);
    }

    private boolean isProd() {
        return Arrays.asList(environment.getActiveProfiles()).contains("prod");
    }
}
