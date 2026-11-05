package com.joyeria.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Petición del asistente de compras. El límite de tamaño es una primera
 * barrera de seguridad; el backend lo vuelve a validar contra la configuración.
 */
@Data
@NoArgsConstructor
public class AiAssistantRequest {

    @NotBlank(message = "El mensaje no puede estar vacío")
    @Size(max = 500, message = "El mensaje es demasiado largo (máximo 500 caracteres)")
    private String message;

    /**
     * Turnos recientes de la conversación (opcional). Permite interpretar
     * mensajes de seguimiento como "y en oro?" o "más barato" usando el
     * contexto previo. El backend solo conserva una ventana acotada.
     */
    @Size(max = 20, message = "El historial es demasiado largo (máximo 20 turnos)")
    private List<AiConversationTurn> history;
}
