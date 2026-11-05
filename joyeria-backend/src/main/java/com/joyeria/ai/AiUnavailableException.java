package com.joyeria.ai;

/**
 * Error controlado cuando el proveedor de IA no está disponible (rechazo,
 * error de red o timeout). La tienda nunca se rompe por un fallo de IA:
 * el asistente continúa en modo determinista.
 */
public class AiUnavailableException extends RuntimeException {

    public AiUnavailableException(String message) {
        super(message);
    }

    public AiUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
