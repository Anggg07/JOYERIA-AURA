package com.joyeria.integration.payment;

/**
 * Contrato de un proveedor de pago. Un proveedor crea el checkout (preferencia)
 * en la pasarela y sólo la pasarela puede confirmar el cobro a través del webhook
 * o del flujo de confirmación propio (sandbox).
 */
public interface PaymentProvider {

    /** Identificador del proveedor, p. ej. "mercadopago" o "sandbox". */
    String name();

    /** Indica si el proveedor tiene todo lo necesario para operar (credenciales). */
    boolean configured();

    /**
     * Crea una preferencia de pago y devuelve la URL de checkout.
     * Nunca debe aprobar el pago: sólo inicia el flujo en la pasarela.
     */
    PaymentPreference createPreference(PaymentContext context);
}
