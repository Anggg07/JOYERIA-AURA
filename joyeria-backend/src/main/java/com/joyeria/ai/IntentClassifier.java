package com.joyeria.ai;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Clasificador determinista de intención. Funciona <strong>sin IA</strong> y
 * usa la {@link SearchIntent} ya extraída como señal principal.
 *
 * <p>Regla de precedencia:</p>
 * <ol>
 *   <li>Si el mensaje contiene una señal comercial estructurada (categoría,
 *       material, color, estilo, ocasión, regalo o precio) se considera una
 *       intención de búsqueda, aunque empiece con un saludo.</li>
 *   <li>Si no hay señal comercial, se detectan saludos, small talk, preguntas de
 *       catálogo y peticiones fuera de alcance.</li>
 *   <li>En cualquier otro caso se asume búsqueda de producto, para no dejar al
 *       usuario sin resultados cuando escribe términos libres.</li>
 * </ol>
 */
@Component
public class IntentClassifier {

    private static final List<String> GREETINGS = List.of(
            "hola", "holaa", "buenas", "buenos dias", "buenas tardes", "buenas noches",
            "muy buenas", "hey", "hi", "hello", "que hubo", "quiubo", "que mas", "hola buenas");

    private static final List<String> SMALL_TALK = List.of(
            "como estas", "como esta", "como te va", "como te encuentras", "como andas",
            "que tal", "que tal todo", "como va tu dia", "como va todo", "como estas hoy",
            "quien eres", "que eres", "eres un bot", "eres una bot", "eres humana", "eres humano",
            "que puedes hacer", "en que puedes ayudar", "como funcionas");

    private static final List<String> THANKS = List.of(
            "gracias", "muchas gracias", "mil gracias", "te lo agradezco", "gracias totales", "muy amable");

    private static final List<String> CATALOG = List.of(
            "catalogo", "categorias", "categoria", "materiales", "material", "que venden",
            "que manejan", "que tienes", "que tienen", "que ofrecen", "que ofreces",
            "tipos de joya", "tipos de joyas", "opciones tienen", "opciones hay");

    private static final List<String> OUT_OF_SCOPE = List.of(
            "chiste", "cuentame un chiste", "clima", "el tiempo hoy", "futbol", "politica",
            "receta", "traduce", "traducir", "quien gano", "noticias");

    /**
     * Clasifica el mensaje. Nunca devuelve {@code null}.
     *
     * @param message      mensaje original del usuario
     * @param searchIntent intención estructurada ya extraída (puede ser vacía)
     */
    public AiIntent classify(String message, SearchIntent searchIntent) {
        if (hasCommercialSignal(searchIntent)) {
            return classifySearch(searchIntent);
        }

        String norm = TextNormalizer.normalize(message == null ? "" : message);
        if (matchesAny(norm, OUT_OF_SCOPE)) {
            return AiIntent.OUT_OF_SCOPE;
        }
        if (matchesAny(norm, CATALOG)) {
            return AiIntent.CATALOG_QUESTION;
        }
        if (matchesAny(norm, GREETINGS)) {
            return AiIntent.GREETING;
        }
        if (matchesAny(norm, SMALL_TALK) || matchesAny(norm, THANKS)) {
            return AiIntent.SMALL_TALK;
        }
        return AiIntent.PRODUCT_SEARCH;
    }

    /** Indica si el mensaje es un agradecimiento (para elegir la respuesta social). */
    public boolean isThankYou(String message) {
        String norm = TextNormalizer.normalize(message == null ? "" : message);
        return matchesAny(norm, THANKS);
    }

    /** Indica si la intención es puramente conversacional (no debe consultar el catálogo). */
    public boolean isConversational(AiIntent intent) {
        return intent == AiIntent.GREETING
                || intent == AiIntent.SMALL_TALK
                || intent == AiIntent.CATALOG_QUESTION
                || intent == AiIntent.OUT_OF_SCOPE;
    }

    private AiIntent classifySearch(SearchIntent intent) {
        if (intent.isGift() || intent.getOccasion() != null) {
            return AiIntent.GIFT_ADVICE;
        }
        if (intent.getMinPrice() != null || intent.getMaxPrice() != null) {
            return AiIntent.BUDGET_SEARCH;
        }
        return AiIntent.PRODUCT_SEARCH;
    }

    private boolean hasCommercialSignal(SearchIntent intent) {
        if (intent == null) {
            return false;
        }
        return intent.getCategory() != null
                || intent.getMaterial() != null
                || intent.getColor() != null
                || intent.getStyle() != null
                || intent.getOccasion() != null
                || intent.isGift()
                || intent.getMinPrice() != null
                || intent.getMaxPrice() != null;
    }

    private boolean matchesAny(String text, List<String> phrases) {
        for (String phrase : phrases) {
            if (matchesWord(text, phrase)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesWord(String text, String phrase) {
        return Pattern.compile("(?:^|[^a-z0-9])" + Pattern.quote(phrase) + "(?:$|[^a-z0-9])")
                .matcher(text)
                .find();
    }
}
