package com.joyeria.ai;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Construye la pregunta de seguimiento y las sugerencias de próximas consultas.
 * Es totalmente determinista: no depende del proveedor de IA ni genera datos
 * comerciales, solo frases de navegación basadas en la intención interpretada.
 */
@Component
public class AssistantSuggestions {

    private static final int MAX_SUGGESTIONS = 3;

    /**
     * Pregunta de seguimiento para guiar al usuario. Devuelve {@code null}
     * cuando no aporta valor (ya se cubrieron material, presupuesto y stock).
     */
    public String followUpQuestion(AiIntent intent, SearchIntent search, boolean hasProducts) {
        if (isOnboarding(intent)) {
            return "¿Buscas algo en particular? Cuéntame el tipo de joya o la ocasión.";
        }
        if (!hasProducts) {
            return "¿Quieres probar otra categoría, otro material o subir un poco el presupuesto?";
        }
        if (search.getMaxPrice() == null && search.getMinPrice() == null) {
            return "¿Tienes un presupuesto en mente?";
        }
        if (search.getMaterial() == null) {
            return "¿La prefieres en plata, oro u otro material?";
        }
        if (!search.isInStockOnly()) {
            return "¿Quieres ver solo las disponibles?";
        }
        return null;
    }

    /**
     * Sugerencias de próximas consultas. Son mensajes que el usuario puede
     * enviar tal cual, por lo que el analizador de intención los entiende.
     */
    public List<String> suggestions(AiIntent intent, SearchIntent search, boolean hasProducts) {
        List<String> suggestions = new ArrayList<>();
        if (isOnboarding(intent)) {
            suggestions.add("Busco un anillo de plata");
            suggestions.add("Un regalo para mi novia");
            suggestions.add("Algo por menos de 300000");
            return suggestions;
        }
        if (!hasProducts) {
            suggestions.add("Anillos");
            suggestions.add("Collares");
            suggestions.add("Pulseras");
            return suggestions;
        }
        if (search.getMaxPrice() != null) {
            suggestions.add("Más barato");
        }
        if (search.getMaterial() == null) {
            suggestions.add("Solo en plata");
        }
        if (!search.isInStockOnly()) {
            suggestions.add("Solo disponibles");
        }
        suggestions.add("Muéstrame más");
        return suggestions.stream().distinct().limit(3).toList();
    }

    private boolean isOnboarding(AiIntent intent) {
        return intent == AiIntent.GREETING
                || intent == AiIntent.SMALL_TALK
                || intent == AiIntent.CATALOG_QUESTION;
    }
}
