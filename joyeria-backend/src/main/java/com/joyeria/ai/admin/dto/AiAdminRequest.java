package com.joyeria.ai.admin.dto;

import com.joyeria.ai.dto.AiConversationTurn;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Petición del asistente interno del panel de administración. El límite de
 * tamaño y el historial acotado son barreras de seguridad.
 */
@Data
@NoArgsConstructor
public class AiAdminRequest {

    @NotBlank(message = "El mensaje no puede estar vacío")
    @Size(max = 600, message = "El mensaje es demasiado largo (máximo 600 caracteres)")
    private String message;

    /** Turnos recientes de la conversación (opcional), máximo 20. */
    @Size(max = 20, message = "El historial es demasiado largo (máximo 20 turnos)")
    private List<AiConversationTurn> history;
}
