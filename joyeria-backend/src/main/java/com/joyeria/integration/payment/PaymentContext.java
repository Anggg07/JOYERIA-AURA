package com.joyeria.integration.payment;

import java.math.BigDecimal;

/**
 * Datos necesarios para iniciar un checkout en la pasarela.
 *
 * @param externalReference referente del lado del comercio (número de pedido)
 * @param amount            monto total a cobrar (fuente de verdad: backend)
 * @param currency          moneda ISO-4217
 * @param description       descripción mostrada al pagador
 * @param payerEmail        email del pagador
 * @param backUrl           URL a la que la pasarela devuelve al usuario
 * @param notificationUrl   URL del webhook (puede ser nula si no hay URL pública)
 */
public record PaymentContext(String externalReference, BigDecimal amount, String currency,
                             String description, String payerEmail, String backUrl,
                             String notificationUrl) {
}
