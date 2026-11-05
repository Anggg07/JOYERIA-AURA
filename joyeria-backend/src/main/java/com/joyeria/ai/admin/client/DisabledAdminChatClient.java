package com.joyeria.ai.admin.client;

import com.joyeria.ai.dto.AiConversationTurn;

import java.util.List;
import java.util.Optional;

/**
 * Implementación inactiva: se usa cuando no hay proveedor de IA configurado para
 * el panel. El asistente de administración sigue operando en modo determinista
 * con los datos reales de la tienda.
 */
public class DisabledAdminChatClient implements AiAdminChatClient {

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public Optional<String> chat(String systemPrompt, List<AiConversationTurn> messages) {
        return Optional.empty();
    }
}
