package com.joyeria.ai;

import com.joyeria.ai.dto.AiConversationTurn;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ConversationContextResolverTest {

    private final SearchIntentParser parser = new SearchIntentParser();
    private final ConversationContextResolver resolver = new ConversationContextResolver(parser);

    private List<AiConversationTurn> history(String... userMessages) {
        return java.util.Arrays.stream(userMessages)
                .map(content -> new AiConversationTurn("user", content))
                .toList();
    }

    @Test
    void noHistoryReturnsCurrentIntent() {
        SearchIntent current = parser.parse("busco un collar");

        SearchIntent result = resolver.enrich(current, "busco un collar", List.of());

        assertEquals("collar", result.getCategory());
    }

    @Test
    void followUpMaterialKeepsPreviousCategory() {
        SearchIntent current = parser.parse("y en oro?");

        SearchIntent result = resolver.enrich(current, "y en oro?", history("busco un anillo de plata"));

        assertEquals("anillo", result.getCategory());
        assertEquals("oro", result.getMaterial());
    }

    @Test
    void shortFragmentWithoutCategoryIsFollowUp() {
        SearchIntent current = parser.parse("en oro");

        SearchIntent result = resolver.enrich(current, "en oro", history("busco un anillo de plata"));

        assertEquals("anillo", result.getCategory());
        assertEquals("oro", result.getMaterial());
    }

    @Test
    void cheaperFollowUpLowersBudget() {
        SearchIntent current = parser.parse("mas barato");

        SearchIntent result = resolver.enrich(current, "mas barato", history("anillo por menos de 300000"));

        assertEquals(new BigDecimal("210000"), result.getMaxPrice());
    }

    @Test
    void pricierFollowUpRaisesFloor() {
        SearchIntent current = parser.parse("mas caro");

        SearchIntent result = resolver.enrich(current, "mas caro", history("anillo por menos de 300000"));

        assertEquals(new BigDecimal("300000"), result.getMinPrice());
        assertNull(result.getMaxPrice());
    }

    @Test
    void topicChangeResetsPreviousFilters() {
        SearchIntent current = parser.parse("muestrame collares");

        SearchIntent result = resolver.enrich(current, "muestrame collares",
                history("busco un anillo de plata por menos de 300000"));

        assertEquals("collares", result.getCategory());
        assertNull(result.getMaterial());
        assertNull(result.getMaxPrice());
    }

    @Test
    void reconstructsContextAcrossMultipleTurns() {
        SearchIntent current = parser.parse("mas barato");

        SearchIntent result = resolver.enrich(current, "mas barato",
                history("busco un anillo de plata", "y en oro", "por menos de 300000"));

        assertEquals("anillo", result.getCategory());
        assertEquals("oro", result.getMaterial());
        assertEquals(new BigDecimal("210000"), result.getMaxPrice());
    }
}
