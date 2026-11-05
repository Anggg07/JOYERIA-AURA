package com.joyeria.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Respuesta del asistente: un mensaje, la intención reconocida y una lista de
 * productos reales (vacía cuando la intención es conversacional o no hay
 * coincidencias).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiAssistantResponse {
    private String message;
    private String intent;
    private List<AiProductSuggestion> products;

    /** Pregunta de seguimiento opcional para guiar al usuario. */
    private String followUpQuestion;

    /** Sugerencias de próximas consultas (mensajes listos para enviar). */
    private List<String> suggestions;
}

