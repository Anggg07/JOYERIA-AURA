package com.joyeria.gift.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Respuesta del Gift Finder: lista de recomendaciones reales, ordenadas por
 * relevancia. Puede ir vacía si el catálogo no tiene coincidencias.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GiftFinderResponse {
    private List<GiftRecommendation> recommendations;
}
