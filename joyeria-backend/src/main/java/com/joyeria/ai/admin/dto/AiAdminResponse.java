package com.joyeria.ai.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Respuesta del asistente de administración: un mensaje, preguntas sugeridas y,
 * opcionalmente, una acción pendiente de confirmación por parte del admin.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiAdminResponse {

    private String message;

    /** Próximas consultas sugeridas (mensajes listos para enviar). */
    private List<String> suggestedPrompts;

    /** Acción propuesta que el administrador debe confirmar (o {@code null}). */
    private AiAdminAction action;

    /** Origen de la respuesta: "AI" (proveedor externo) o "RULES" (determinista). */
    private String generatedBy;
}
