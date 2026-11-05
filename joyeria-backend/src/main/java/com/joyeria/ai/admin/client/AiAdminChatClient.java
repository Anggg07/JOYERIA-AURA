package com.joyeria.ai.admin.client;

import com.joyeria.ai.dto.AiConversationTurn;

import java.util.List;
import java.util.Optional;

/**
 * Abstracción del proveedor de IA usado exclusivamente por el asistente interno
 * del panel de administración. Es independiente del asistente de compras para
 * poder usar otro proveedor, modelo o clave de API.
 */
public interface AiAdminChatClient {

    /** Indica si hay un proveedor externo habilitado y configurado. */
    boolean isEnabled();

    /**
     * Envía un sistema de instrucciones y una secuencia de mensajes y devuelve
     * la respuesta en texto. Devuelve {@link Optional#empty()} si no hay contenido
     * utilizable. Nunca debe recibir datos personales innecesarios.
     */
    Optional<String> chat(String systemPrompt, List<AiConversationTurn> messages);
}
