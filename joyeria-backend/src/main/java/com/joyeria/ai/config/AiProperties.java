package com.joyeria.ai.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración del asistente de IA, totalmente controlada por variables de entorno.
 * Por defecto permanece deshabilitado: el asistente funciona en modo determinista
 * (sin proveedor externo) consultando siempre productos reales de PostgreSQL.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    /** Habilita el uso de un proveedor de IA externo. */
    private boolean enabled = false;

    /** Identificador informativo del proveedor (ej. openai-compatible, groq, ollama). */
    private String provider = "openai-compatible";

    /** Clave de API del proveedor. Nunca debe llegar al frontend. */
    private String apiKey = "";

    /** URL base compatible con la API de OpenAI (ej. https://api.openai.com/v1). */
    private String baseUrl = "https://api.openai.com/v1";

    /** Modelo a utilizar. */
    private String model = "gpt-4o-mini";

    /** Timeout (conexión y lectura) en segundos. */
    private int timeoutSeconds = 15;

    /** Longitud máxima aceptada para el mensaje del usuario. */
    private int maxMessageLength = 500;

    /** Número máximo de productos devueltos al frontend. */
    private int maxProducts = 4;

    /** Número máximo de productos candidatos consultados en la base de datos. */
    private int maxCandidates = 50;

    /** Permite que la IA redacte el mensaje final (si está deshabilitado, se usa un texto fijo). */
    private boolean composeResponse = true;

    /** Temperatura de generación. */
    private double temperature = 0.2;

    /**
     * Configuración del asistente interno del panel de administración. Es
     * independiente del asistente de compras: puede usar su propia clave, modelo
     * y temperatura, y recurre a los valores principales cuando no se define uno.
     */
    private Admin admin = new Admin();

    /** Indica si hay un proveedor de IA correctamente configurado. */
    public boolean isConfigured() {
        return enabled
                && apiKey != null && !apiKey.isBlank()
                && baseUrl != null && !baseUrl.isBlank()
                && model != null && !model.isBlank();
    }

    /**
     * Ajustes del asistente exclusivo del panel de administración. Sus campos son
     * opcionales: si se dejan vacíos, se heredan de la configuración general de
     * {@code ai} (proveedor, URL base, modelo y timeout).
     */
    @Getter
    @Setter
    public static class Admin {

        /**
         * Habilita el asistente de administración. Requiere un proveedor
         * configurado (propio o heredado del asistente de compras).
         */
        private boolean enabled = true;

        /** Clave de API propia del asistente de administración (opcional). */
        private String apiKey = "";

        /** URL base propia (opcional). */
        private String baseUrl = "";

        /** Modelo propio (opcional). */
        private String model = "";

        /** Temperatura propia (más alta que la del cliente favorece resúmenes). */
        private double temperature = 0.3;

        /** Timeout propio en segundos; 0 usa el de la configuración principal. */
        private int timeoutSeconds = 0;

        /** Longitud máxima aceptada para el mensaje del administrador. */
        private int maxMessageLength = 600;
    }
}
