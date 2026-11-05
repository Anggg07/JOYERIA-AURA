package com.joyeria.ai;

import com.joyeria.dto.ProductDTO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Sistema de recomendación por relevancia. No devuelve simplemente los
 * primeros productos: puntúa cada candidato real según la coincidencia con la
 * intención y los ordena.
 *
 * <pre>
 * +30 coincidencia de categoría
 * +25 coincidencia de material
 * +20 dentro del presupuesto
 * +15 coincidencia de estilo
 * +12 coincidencia de color
 * +10 disponibilidad
 *  +8 por palabra clave coincidente
 *  +6 coincidencia de ocasión
 *  +4 intención de regalo
 * </pre>
 */
@Service
public class ProductRecommendationService {

    private static final int SCORE_CATEGORY = 30;
    private static final int SCORE_MATERIAL = 25;
    private static final int SCORE_BUDGET = 20;
    private static final int SCORE_STYLE = 15;
    private static final int SCORE_COLOR = 12;
    private static final int SCORE_AVAILABILITY = 10;
    private static final int SCORE_KEYWORD = 8;
    private static final int SCORE_OCCASION = 6;
    private static final int SCORE_GIFT = 4;

    public record Recommendation(ProductDTO product, int score, String reason) {
    }

    public List<Recommendation> recommend(List<ProductDTO> candidates, SearchIntent intent, int limit) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }
        List<Recommendation> scored = new ArrayList<>(candidates.size());
        for (ProductDTO product : candidates) {
            if (!satisfiesConstraints(product, intent)) {
                continue;
            }
            scored.add(score(product, intent));
        }
        scored.sort(Comparator
                .comparingInt(Recommendation::score).reversed()
                .thenComparing(recommendation -> soldCount(recommendation.product()), Comparator.reverseOrder()));
        return scored.stream().limit(Math.max(1, limit)).toList();
    }

    /**
     * Aplica los filtros explícitos del cliente como restricciones duras: ningún
     * producto fuera de presupuesto o agotado (cuando se pidió disponibilidad)
     * debe llegar a la respuesta, sin importar su puntuación.
     */
    private boolean satisfiesConstraints(ProductDTO product, SearchIntent intent) {
        if (intent.getMaxPrice() != null) {
            if (product.getPrice() == null || product.getPrice().compareTo(intent.getMaxPrice()) > 0) {
                return false;
            }
        }
        if (intent.getMinPrice() != null) {
            if (product.getPrice() == null || product.getPrice().compareTo(intent.getMinPrice()) < 0) {
                return false;
            }
        }
        if (intent.isInStockOnly()) {
            if (product.getStock() == null || product.getStock() <= 0) {
                return false;
            }
        }
        return true;
    }

    private Recommendation score(ProductDTO product, SearchIntent intent) {
        int score = 0;
        List<String> reasons = new ArrayList<>();
        String haystack = haystack(product);

        if (matches(intent.getCategory(), product.getCategoryName())) {
            score += SCORE_CATEGORY;
        }
        if (intent.getMaterial() != null && containsMaterial(product, intent.getMaterial())) {
            score += SCORE_MATERIAL;
            reasons.add("elaborado en " + intent.getMaterial());
        }
        if (intent.getMaxPrice() != null && product.getPrice() != null
                && product.getPrice().compareTo(intent.getMaxPrice()) <= 0) {
            score += SCORE_BUDGET;
            reasons.add("dentro de tu presupuesto");
        }
        if (intent.getStyle() != null && matches(intent.getStyle(), haystack)) {
            score += SCORE_STYLE;
            reasons.add(intent.getStyle());
        }
        if (intent.getColor() != null && matches(intent.getColor(), haystack)) {
            score += SCORE_COLOR;
            reasons.add("tono " + intent.getColor());
        }
        boolean available = product.getStock() != null && product.getStock() > 0;
        if (available) {
            score += SCORE_AVAILABILITY;
        }
        if (intent.getKeywords() != null) {
            for (String keyword : intent.getKeywords()) {
                if (matches(keyword, haystack)) {
                    score += SCORE_KEYWORD;
                }
            }
        }
        if (intent.getOccasion() != null && matches(intent.getOccasion(), haystack)) {
            score += SCORE_OCCASION;
        }
        if (intent.isGift()) {
            score += SCORE_GIFT;
            reasons.add("ideal para regalar");
        }

        return new Recommendation(product, score, buildReason(reasons, product, available));
    }

    private String buildReason(List<String> reasons, ProductDTO product, boolean available) {
        List<String> parts = new ArrayList<>(reasons);
        if (available && product.getStock() != null && product.getStock() <= 5) {
            parts.add("pocas unidades disponibles");
        } else if (available) {
            parts.add("disponible");
        } else {
            parts.add("actualmente agotado");
        }
        if (parts.isEmpty()) {
            return "Recomendado para ti";
        }
        String joined = String.join(", ", parts);
        return Character.toUpperCase(parts.get(0).charAt(0)) + parts.get(0).substring(1)
                + parts.stream().skip(1).reduce("", (acc, part) -> acc + ", " + part)
                + ".";
    }

    private boolean containsMaterial(ProductDTO product, String material) {
        if (product.getMaterialNames() == null || product.getMaterialNames().isEmpty()) {
            return false;
        }
        String needle = TextNormalizer.normalize(material);
        return product.getMaterialNames().stream()
                .anyMatch(name -> TextNormalizer.normalize(name).contains(needle));
    }

    private boolean matches(String needle, String candidate) {
        if (needle == null || needle.isBlank() || candidate == null) {
            return false;
        }
        return TextNormalizer.normalize(candidate).contains(TextNormalizer.normalize(needle));
    }

    private String haystack(ProductDTO product) {
        StringBuilder sb = new StringBuilder();
        append(sb, product.getName());
        append(sb, product.getDescription());
        append(sb, product.getCategoryName());
        append(sb, product.getColor());
        append(sb, product.getSize());
        append(sb, product.getFeatures());
        append(sb, product.getCareInstructions());
        if (product.getMaterialNames() != null) {
            product.getMaterialNames().forEach(name -> append(sb, name));
        }
        return TextNormalizer.normalize(sb.toString());
    }

    private void append(StringBuilder sb, String value) {
        if (value != null && !value.isBlank()) {
            sb.append(value).append(' ');
        }
    }

    private int soldCount(ProductDTO product) {
        return product.getSoldCount() == null ? 0 : product.getSoldCount();
    }
}
