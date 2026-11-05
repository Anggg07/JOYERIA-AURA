package com.joyeria.integration.payment;

/**
 * Preferencia creada en la pasarela.
 *
 * @param preferenceId   id de la preferencia en la pasarela
 * @param initPoint      URL de checkout para producción
 * @param sandboxInitPoint URL de checkout para pruebas (puede ser nula)
 */
public record PaymentPreference(String preferenceId, String initPoint, String sandboxInitPoint) {

    public String checkoutUrl() {
        return sandboxInitPoint != null && !sandboxInitPoint.isBlank() ? sandboxInitPoint : initPoint;
    }
}
