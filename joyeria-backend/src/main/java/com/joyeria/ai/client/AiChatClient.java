package com.joyeria.ai.client;

import com.joyeria.ai.SearchIntent;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Abstracción del proveedor de IA. Permite cambiar de proveedor o deshabilitarlo
 * por completo sin afectar al resto del asistente.
 */
public interface AiChatClient {

    /** Indica si hay un proveedor externo habilitado y configurado. */
    boolean isEnabled();

    /**
     * Interpreta el mensaje del usuario y devuelve una intención estructurada.
     * Devuelve {@link Optional#empty()} si no pudo interpretarse.
     */
    Optional<SearchIntent> interpretIntent(String message, List<String> categories, List<String> materials);

    /**
     * Redacta un mensaje natural a partir de productos reales ya seleccionados.
     * Devuelve {@link Optional#empty()} si no hay respuesta utilizable.
     */
    Optional<String> composeMessage(String message, SearchIntent intent, List<Map<String, Object>> products);
}
