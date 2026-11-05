package com.joyeria.integration.payment;

/**
 * Error al iniciar o procesar un pago. Se expone al cliente como solicitud
 * inválida (400): el pago no pudo procesarse y NO se marca como pagado.
 */
public class PaymentProcessingException extends RuntimeException {

    public PaymentProcessingException(String message) {
        super(message);
    }

    public PaymentProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
