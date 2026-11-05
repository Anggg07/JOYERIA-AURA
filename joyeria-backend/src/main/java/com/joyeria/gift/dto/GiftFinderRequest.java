package com.joyeria.gift.dto;

import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Criterios del Smart Gift Finder. Todos los campos son opcionales: si faltan
 * datos, el motor de recomendación sigue funcionando con lo que haya.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GiftFinderRequest {
    /** Para quién es el regalo (pareja, madre, padre, amigo, familia…). */
    private String recipient;
    /** Ocasión (aniversario, cumpleaños, boda, graduación…). */
    private String occasion;
    /** Estilo deseado (elegante, casual, moderno, clásico, minimalista). */
    private String style;
    /** Material preferido (oro, plata, acero…). Se compara con el catálogo real. */
    private String material;

    @PositiveOrZero(message = "El presupuesto mínimo no puede ser negativo")
    private BigDecimal budgetMin;

    @PositiveOrZero(message = "El presupuesto máximo no puede ser negativo")
    private BigDecimal budgetMax;
}
