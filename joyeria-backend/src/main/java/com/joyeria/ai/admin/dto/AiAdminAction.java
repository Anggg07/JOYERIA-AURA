package com.joyeria.ai.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Acción propuesta por el asistente. El backend NUNCA la ejecuta: solo la
 * valida contra una lista blanca y la devuelve al panel, donde el administrador
 * debe confirmarla explícitamente. La ejecución ocurre después mediante los
 * endpoints de administración ya existentes.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiAdminAction {

    /** Tipo de acción (lista blanca). Ej. UPDATE_ORDER_STATUS. */
    private String type;

    /** Resumen legible de lo que se haría. */
    private String summary;

    /** Parámetros de la acción (todos como texto). */
    private Map<String, String> parameters;
}
