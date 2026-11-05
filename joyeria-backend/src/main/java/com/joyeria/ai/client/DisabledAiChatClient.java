package com.joyeria.ai.client;

import com.joyeria.ai.SearchIntent;
import com.joyeria.ai.config.AiProperties;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Implementación inactiva: se usa cuando no hay proveedor de IA configurado.
 * El asistente sigue operando con el parser determinista.
 */
public class DisabledAiChatClient implements AiChatClient {

    private final AiProperties properties;

    public DisabledAiChatClient(AiProperties properties) {
        this.properties = properties;
    }

    public AiProperties getProperties() {
        return properties;
    }

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public Optional<SearchIntent> interpretIntent(String message, List<String> categories, List<String> materials) {
        return Optional.empty();
    }

    @Override
    public Optional<String> composeMessage(String message, SearchIntent intent, List<Map<String, Object>> products) {
        return Optional.empty();
    }
}
