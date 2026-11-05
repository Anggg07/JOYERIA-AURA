package com.joyeria.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Un turno previo de la conversación con el asistente. El cliente lo reenvía
 * para dar contexto; el backend lo usa solo para interpretar mensajes de
 * seguimiento y nunca lo persiste ni lo expone.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiConversationTurn {

    /** "user" o "assistant". */
    private String role;

    /** Texto del turno. */
    private String content;
}
