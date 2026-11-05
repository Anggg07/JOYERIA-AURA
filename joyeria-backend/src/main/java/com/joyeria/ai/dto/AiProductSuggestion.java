package com.joyeria.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Producto recomendado. Todos los campos comerciales provienen de la base de
 * datos; la IA solo aporta el motivo (texto), nunca los datos.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiProductSuggestion {
    private Long id;
    private String name;
    private String slug;
    private java.math.BigDecimal price;
    private String image;
    private String material;
    private boolean available;
    private String reason;
}
