package com.joyeria.ai;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssistantSuggestionsTest {

    private final AssistantSuggestions suggestions = new AssistantSuggestions();

    @Test
    void onboardingReturnsStarterSuggestionsAndQuestion() {
        List<String> result = suggestions.suggestions(AiIntent.GREETING, new SearchIntent(), false);

        assertEquals(3, result.size());
        assertNotNull(suggestions.followUpQuestion(AiIntent.GREETING, new SearchIntent(), false));
    }

    @Test
    void noProductsSuggestsCategories() {
        List<String> result = suggestions.suggestions(AiIntent.PRODUCT_SEARCH, SearchIntent.builder().build(), false);

        assertEquals(List.of("Anillos", "Collares", "Pulseras"), result);
        assertNotNull(suggestions.followUpQuestion(AiIntent.PRODUCT_SEARCH, new SearchIntent(), false));
    }

    @Test
    void budgetPresentAddsCheaperSuggestion() {
        SearchIntent intent = SearchIntent.builder().maxPrice(new BigDecimal("300000")).build();

        assertTrue(suggestions.suggestions(AiIntent.BUDGET_SEARCH, intent, true).contains("Más barato"));
    }

    @Test
    void missingMaterialSuggestsMaterial() {
        SearchIntent intent = SearchIntent.builder()
                .category("anillo")
                .maxPrice(new BigDecimal("300000"))
                .build();

        List<String> result = suggestions.suggestions(AiIntent.BUDGET_SEARCH, intent, true);
        assertTrue(result.contains("Solo en plata"));
        assertTrue(suggestions.followUpQuestion(AiIntent.BUDGET_SEARCH, intent, true).contains("material"));
    }

    @Test
    void budgetPresentProducesBudgetFollowUp() {
        SearchIntent intent = SearchIntent.builder().category("anillo").build();

        assertTrue(suggestions.followUpQuestion(AiIntent.PRODUCT_SEARCH, intent, true)
                .contains("presupuesto"));
    }

    @Test
    void inStockOnlyRemovesAvailabilitySuggestion() {
        SearchIntent intent = SearchIntent.builder()
                .material("plata")
                .maxPrice(new BigDecimal("300000"))
                .inStockOnly(true)
                .build();

        List<String> result = suggestions.suggestions(AiIntent.BUDGET_SEARCH, intent, true);

        assertTrue(result.contains("Más barato"));
        assertTrue(result.stream().noneMatch(value -> value.equalsIgnoreCase("solo disponibles")));
    }

    @Test
    void completeConstraintsProduceNoFollowUp() {
        SearchIntent intent = SearchIntent.builder()
                .material("plata")
                .maxPrice(new BigDecimal("300000"))
                .inStockOnly(true)
                .build();

        assertNull(suggestions.followUpQuestion(AiIntent.BUDGET_SEARCH, intent, true));
    }
}
