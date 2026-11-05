package com.joyeria.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IntentClassifierTest {

    private final IntentClassifier classifier = new IntentClassifier();
    private final SearchIntentParser parser = new SearchIntentParser();

    private AiIntent classify(String message) {
        return classifier.classify(message, parser.parse(message));
    }

    @Test
    void greetingsAreDetected() {
        assertEquals(AiIntent.GREETING, classify("Hola"));
        assertEquals(AiIntent.GREETING, classify("¡Hola! ¿Cómo estás?"));
        assertEquals(AiIntent.GREETING, classify("buenas tardes"));
        assertEquals(AiIntent.GREETING, classify("hey"));
    }

    @Test
    void smallTalkAndThanksAreDetected() {
        assertEquals(AiIntent.SMALL_TALK, classify("¿Cómo estás?"));
        assertEquals(AiIntent.SMALL_TALK, classify("qué tal"));
        assertEquals(AiIntent.SMALL_TALK, classify("Gracias"));
        assertEquals(AiIntent.SMALL_TALK, classify("muchas gracias"));
        assertTrue(classifier.isThankYou("Gracias"));
        assertFalse(classifier.isThankYou("Hola"));
    }

    @Test
    void productSearchIsDetected() {
        assertEquals(AiIntent.PRODUCT_SEARCH, classify("quiero un anillo de plata"));
        assertEquals(AiIntent.PRODUCT_SEARCH, classify("algo con grabado personalizado"));
    }

    @Test
    void giftAdviceIsDetected() {
        assertEquals(AiIntent.GIFT_ADVICE, classify("busco un regalo para mi novia"));
    }

    @Test
    void budgetSearchIsDetected() {
        assertEquals(AiIntent.BUDGET_SEARCH, classify("algo de plata por menos de 300000"));
    }

    @Test
    void catalogQuestionIsDetected() {
        assertEquals(AiIntent.CATALOG_QUESTION, classify("¿qué categorías tienen?"));
    }

    @Test
    void outOfScopeIsDetected() {
        assertEquals(AiIntent.OUT_OF_SCOPE, classify("cuéntame un chiste"));
    }

    @Test
    void greetingWithProductSignalStillCountsAsSearch() {
        assertEquals(AiIntent.PRODUCT_SEARCH, classify("hola, busco un anillo"));
    }

    @Test
    void conversationalIntentsAreFlagged() {
        assertTrue(classifier.isConversational(AiIntent.GREETING));
        assertTrue(classifier.isConversational(AiIntent.SMALL_TALK));
        assertFalse(classifier.isConversational(AiIntent.PRODUCT_SEARCH));
        assertFalse(classifier.isConversational(AiIntent.GIFT_ADVICE));
    }
}
