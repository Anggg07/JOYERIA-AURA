package com.joyeria.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Intención de búsqueda extraída del mensaje del usuario.
 * Solo describe <em>qué busca</em> el usuario; nunca contiene datos comerciales
 * (productos, precios o stock) que deban provenir de la base de datos.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchIntent {

    /** Nombre o término de categoría detectado (ej. "anillo", "collar"). */
    private String category;

    /** Nombre o término de material detectado (ej. "plata", "oro blanco"). */
    private String material;

    /** Precio mínimo (COP), si el usuario lo indicó. */
    private BigDecimal minPrice;

    /** Precio máximo/presupuesto (COP), si el usuario lo indicó. */
    private BigDecimal maxPrice;

    /** Color detectado (ej. "dorado", "plateado"). */
    private String color;

    /** Estilo detectado (ej. "elegante", "minimalista"). */
    private String style;

    /** Ocasión detectada (ej. "regalo", "aniversario"). */
    private String occasion;

    /** Género detectado: "mujer", "hombre" o "unisex". */
    private String gender;

    /** Indica intención de regalo. */
    private boolean gift;

    /** El usuario pidió explícitamente productos disponibles. */
    @Builder.Default
    private boolean inStockOnly = false;

    /** Palabras clave adicionales para puntuación por relevancia. */
    @Builder.Default
    private Set<String> keywords = new LinkedHashSet<>();
}
