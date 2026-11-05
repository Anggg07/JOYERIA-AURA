package com.joyeria.gift.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Producto recomendado por el Gift Finder. Todos los datos comerciales
 * (nombre, precio, materiales, disponibilidad) provienen del catálogo real.
 * La relevancia es un porcentaje orientativo (nulos si el usuario no eligió
 * ningún criterio) y la explicación es un texto corto, sin puntuaciones internas.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GiftRecommendation {
    private Long id;
    private String name;
    private String slug;
    private BigDecimal price;
    private String image;
    private String material;
    private List<String> materialNames;
    private String categoryName;
    private boolean available;
    private Integer relevance;
    private String explanation;
}
