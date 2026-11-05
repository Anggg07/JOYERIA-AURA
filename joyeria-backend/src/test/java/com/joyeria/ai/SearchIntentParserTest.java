package com.joyeria.ai;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchIntentParserTest {

    private final SearchIntentParser parser = new SearchIntentParser();

    @Test
    void parsesMaterialStyleAndMaxPriceFromMilExpression() {
        SearchIntent intent = parser.parse("Quiero algo elegante de plata por menos de 300 mil.");

        assertEquals("plata", intent.getMaterial());
        assertEquals("elegante", intent.getStyle());
        assertEquals(new BigDecimal("300000"), intent.getMaxPrice());
    }

    @Test
    void parsesMaxPriceFromDottedAmountAndGiftContext() {
        SearchIntent intent = parser.parse("Busco un regalo elegante para mi novia de máximo 400.000");

        assertEquals(new BigDecimal("400000"), intent.getMaxPrice());
        assertTrue(intent.isGift());
        assertEquals("mujer", intent.getGender());
    }

    @Test
    void parsesBudgetPhrase() {
        SearchIntent intent = parser.parse("Con un presupuesto de 250000 pesos");

        assertEquals(new BigDecimal("250000"), intent.getMaxPrice());
    }

    @Test
    void parsesMinPrice() {
        SearchIntent intent = parser.parse("Algo de más de 100000");

        assertEquals(new BigDecimal("100000"), intent.getMinPrice());
        assertNull(intent.getMaxPrice());
    }

    @Test
    void parsesBetweenRange() {
        SearchIntent intent = parser.parse("entre 100000 y 300000");

        assertEquals(new BigDecimal("100000"), intent.getMinPrice());
        assertEquals(new BigDecimal("300000"), intent.getMaxPrice());
    }

    @Test
    void detectsCategoryColorAndAvailability() {
        SearchIntent intent = parser.parse("Muéstrame un collar dorado disponible");

        assertEquals("collar", intent.getCategory());
        assertEquals("dorado", intent.getColor());
        assertTrue(intent.isInStockOnly());
    }

    @Test
    void detectsMultiwordMaterial() {
        SearchIntent intent = parser.parse("Un anillo de oro blanco");

        assertEquals("oro blanco", intent.getMaterial());
        assertEquals("anillo", intent.getCategory());
    }

    @Test
    void keywordsExcludeStructuredTermsToAvoidDoubleScoring() {
        SearchIntent intent = parser.parse("quiero un anillo de oro con grabado personalizado");

        assertEquals("anillo", intent.getCategory());
        assertEquals("oro", intent.getMaterial());
        assertTrue(intent.getKeywords().contains("grabado"));
        assertTrue(intent.getKeywords().contains("personalizado"));
        assertFalse(intent.getKeywords().contains("anillo"));
        assertFalse(intent.getKeywords().contains("oro"));
    }

    @Test
    void emptyMessageProducesEmptyIntent() {
        SearchIntent intent = parser.parse("   ");

        assertNull(intent.getMaterial());
        assertNull(intent.getCategory());
        assertNull(intent.getMaxPrice());
        assertTrue(intent.getKeywords().isEmpty());
    }

    @Test
    void bareSmallNumberIsNotTreatedAsBudget() {
        SearchIntent intent = parser.parse("un anillo de plata 925");

        assertNull(intent.getMaxPrice());
        assertEquals("plata", intent.getMaterial());
        assertEquals("anillo", intent.getCategory());
    }

    @Test
    void bareLargeNumberIsTreatedAsBudget() {
        SearchIntent intent = parser.parse("busco algo de 300000");

        assertEquals(new BigDecimal("300000"), intent.getMaxPrice());
    }

    @Test
    void mergeIgnoresNonPositiveAiPrice() {
        SearchIntent base = parser.parse("plata");
        SearchIntent ai = new SearchIntent();
        ai.setMaxPrice(BigDecimal.ZERO);

        SearchIntent merged = parser.merge(base, ai, Set.of("plata"), Set.of("plata"));

        assertNull(merged.getMaxPrice());
    }

    @Test
    void mergeDiscardsUnknownAiValuesAndKeepsDeterministic() {
        SearchIntent base = parser.parse("plata");
        SearchIntent ai = new SearchIntent();
        ai.setCategory("Una categoría inventada");
        ai.setMaterial("material-inexistente");
        ai.setMaxPrice(new BigDecimal("500000"));

        SearchIntent merged = parser.merge(base, ai, Set.of("plata"), Set.of("plata"));

        assertNull(merged.getCategory());
        assertEquals("plata", merged.getMaterial());
        assertEquals(new BigDecimal("500000"), merged.getMaxPrice());
    }
}
