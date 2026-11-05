package com.joyeria.ai;

import com.joyeria.ai.dto.AiConversationTurn;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Da continuidad a la conversación. El backend es sin estado: el cliente
 * reenvía los turnos recientes y este componente reconstruye el contexto de
 * búsqueda para interpretar mensajes de seguimiento ("y en oro?", "más barato",
 * "muéstrame más") sin repetir toda la consulta.
 *
 * <p>Cuando el mensaje no es un seguimiento (cambio de tema), se usa la
 * intención nueva sin arrastrar filtros antiguos.</p>
 */
@Component
@RequiredArgsConstructor
public class ConversationContextResolver {

    private final SearchIntentParser intentParser;

    /** Frases que indican que el usuario sigue hablando de la búsqueda previa. */
    private static final List<String> CONTINUATIONS = List.of(
            "mas barato", "mas baratos", "mas barata", "mas baratas",
            "mas economico", "mas economica", "mas economicos", "mas economicas",
            "mas caro", "mas caros", "mas cara", "mas caras",
            "mas costoso", "mas costosa", "mas costosos", "mas costosas",
            "otro", "otros", "otra", "otras", "muestrame mas", "muestrame otros",
            "muestrame otras", "algo mas", "alguna otra", "algun otro",
            "tambien", "ese", "esa", "esos", "esas", "aquel", "aquella",
            "el primero", "el segundo", "el tercero", "ahora", "pero", "entonces");

    private static final List<String> CHEAPER = List.of(
            "mas barato", "mas baratos", "mas barata", "mas baratas",
            "mas economico", "mas economica", "mas economicos", "mas economicas",
            "menos costoso", "menos costosa");

    private static final List<String> PRICIER = List.of(
            "mas caro", "mas caros", "mas cara", "mas caras",
            "mas costoso", "mas costosa", "mas costosos", "mas costosas");

    private static final int MAX_CONTEXT_TURNS = 10;
    private static final int FOLLOW_UP_MAX_TOKENS = 3;
    private static final BigDecimal CHEAPER_FACTOR = new BigDecimal("0.7");

    /**
     * Devuelve la intención efectiva del mensaje actual, combinándola con el
     * contexto previo cuando corresponde.
     */
    public SearchIntent enrich(SearchIntent current, String message, List<AiConversationTurn> history) {
        SearchIntent prior = priorContext(history);
        if (prior == null || !isFollowUp(current, message)) {
            return current;
        }
        SearchIntent merged = overlay(prior, current);
        applyRelativeBudget(message, merged, prior);
        return merged;
    }

    /**
     * Reconstruye la intención acumulada de los turnos del usuario. Un mensaje
     * que cambia de tema reinicia el contexto.
     */
    private SearchIntent priorContext(List<AiConversationTurn> history) {
        if (history == null || history.isEmpty()) {
            return null;
        }
        List<AiConversationTurn> turns = history.size() > MAX_CONTEXT_TURNS
                ? history.subList(history.size() - MAX_CONTEXT_TURNS, history.size())
                : history;
        SearchIntent accumulated = null;
        for (AiConversationTurn turn : turns) {
            if (turn == null || turn.getContent() == null || turn.getContent().isBlank()) {
                continue;
            }
            if (turn.getRole() != null && !turn.getRole().equalsIgnoreCase("user")) {
                continue;
            }
            SearchIntent parsed = intentParser.parse(turn.getContent());
            if (accumulated != null && isFollowUp(parsed, turn.getContent())) {
                accumulated = overlay(accumulated, parsed);
            } else {
                accumulated = parsed;
            }
        }
        return accumulated;
    }

    private boolean isFollowUp(SearchIntent current, String message) {
        String norm = TextNormalizer.normalize(message == null ? "" : message);
        if (norm.equals("y") || norm.startsWith("y ")) {
            return true;
        }
        if (matchesAny(norm, CONTINUATIONS)) {
            return true;
        }
        boolean hasCategory = current != null && current.getCategory() != null;
        if (!hasCategory && current != null
                && (current.getMaxPrice() != null || current.getMinPrice() != null)) {
            return true;
        }
        int tokens = countTokens(norm);
        return !hasCategory && tokens > 0 && tokens <= FOLLOW_UP_MAX_TOKENS;
    }

    private void applyRelativeBudget(String message, SearchIntent merged, SearchIntent prior) {
        String norm = TextNormalizer.normalize(message == null ? "" : message);
        if (matchesAny(norm, CHEAPER)) {
            if (prior.getMaxPrice() != null) {
                merged.setMaxPrice(prior.getMaxPrice().multiply(CHEAPER_FACTOR)
                        .setScale(0, RoundingMode.HALF_UP));
            }
        } else if (matchesAny(norm, PRICIER)) {
            if (prior.getMaxPrice() != null) {
                merged.setMinPrice(prior.getMaxPrice());
                merged.setMaxPrice(null);
            }
        }
    }

    /** Superpone la intención nueva sobre la previa: los campos nuevos ganan. */
    private SearchIntent overlay(SearchIntent base, SearchIntent over) {
        if (base == null) {
            return over;
        }
        if (over == null) {
            return base;
        }
        SearchIntent out = new SearchIntent();
        out.setCategory(firstNonNull(over.getCategory(), base.getCategory()));
        out.setMaterial(firstNonNull(over.getMaterial(), base.getMaterial()));
        out.setColor(firstNonNull(over.getColor(), base.getColor()));
        out.setStyle(firstNonNull(over.getStyle(), base.getStyle()));
        out.setOccasion(firstNonNull(over.getOccasion(), base.getOccasion()));
        out.setGender(firstNonNull(over.getGender(), base.getGender()));
        out.setMinPrice(firstNonNull(over.getMinPrice(), base.getMinPrice()));
        out.setMaxPrice(firstNonNull(over.getMaxPrice(), base.getMaxPrice()));
        out.setGift(over.isGift() || base.isGift());
        out.setInStockOnly(over.isInStockOnly() || base.isInStockOnly());
        Set<String> keywords = new LinkedHashSet<>();
        if (base.getKeywords() != null) {
            keywords.addAll(base.getKeywords());
        }
        if (over.getKeywords() != null) {
            keywords.addAll(over.getKeywords());
        }
        out.setKeywords(keywords);
        return out;
    }

    private int countTokens(String norm) {
        int count = 0;
        for (String token : norm.split("[^a-z0-9]+")) {
            if (!token.isBlank()) {
                count++;
            }
        }
        return count;
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

    private String firstNonNull(String first, String second) {
        return first != null ? first : second;
    }

    private BigDecimal firstNonNull(BigDecimal first, BigDecimal second) {
        return first != null ? first : second;
    }
}
