package com.joyeria.gift;

import com.joyeria.ai.TextNormalizer;
import com.joyeria.dto.ProductDTO;
import com.joyeria.gift.dto.GiftFinderRequest;
import com.joyeria.gift.dto.GiftFinderResponse;
import com.joyeria.gift.dto.GiftRecommendation;
import com.joyeria.model.Material;
import com.joyeria.model.Product;
import com.joyeria.repository.MaterialRepository;
import com.joyeria.repository.ProductRepository;
import com.joyeria.service.ProductService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Motor de recomendación del Smart Gift Finder.
 *
 * <p>Nunca inventa productos: consulta el catálogo real
 * ({@link ProductRepository#searchFiltered}) y puntúa cada candidato según la
 * coincidencia con los criterios del usuario. Si faltan datos, omite los
 * criterios no informados y recalcula la relevancia; el stock siempre se tiene
 * en cuenta (un producto agotado nunca se ignora, solo se penaliza su orden).</p>
 *
 * <pre>
 * +25 dentro del presupuesto
 * +20 coincidencia de material
 * +20 coincidencia de ocasión
 * +15 coincidencia de estilo
 * +15 afinidad con el destinatario
 * +10 disponibilidad
 * </pre>
 */
@Service
public class GiftFinderService {

    private static final int MAX_CANDIDATES = 200;
    private static final int MAX_RECOMMENDATIONS = 8;

    private static final int W_BUDGET = 25;
    private static final int W_MATERIAL = 20;
    private static final int W_OCCASION = 20;
    private static final int W_STYLE = 15;
    private static final int W_RECIPIENT = 15;
    private static final int W_AVAILABILITY = 10;

    private static final Map<String, List<String>> RECIPIENT_CATEGORIES = Map.of(
            "pareja", List.of("anillos", "collares", "sets", "joyeria personalizada"),
            "madre", List.of("collares", "aretes", "pulseras", "joyeria personalizada"),
            "padre", List.of("pulseras", "dijes", "joyeria personalizada"),
            "amigo", List.of("dijes", "pulseras", "aretes"),
            "amiga", List.of("dijes", "pulseras", "aretes", "collares"),
            "familia", List.of("sets", "dijes", "regalos")
    );

    private static final Map<String, List<String>> OCCASION_KEYWORDS = Map.of(
            "aniversario", List.of("aniversario", "amor", "pareja", "romantico"),
            "cumpleanos", List.of("cumpleanos", "cumple", "fiesta"),
            "boda", List.of("boda", "matrimonio", "compromiso", "novia"),
            "graduacion", List.of("graduacion", "logro", "exito", "meta"),
            "navidad", List.of("navidad", "navidena", "invierno"),
            "san valentin", List.of("san valentin", "valentin", "amor", "romantico"),
            "dia de la madre", List.of("madre", "mama"),
            "dia del padre", List.of("padre", "papa")
    );

    private static final Map<String, List<String>> OCCASION_CATEGORIES = Map.of(
            "aniversario", List.of("anillos", "sets"),
            "cumpleanos", List.of("aretes", "collares", "pulseras"),
            "boda", List.of("anillos", "sets"),
            "graduacion", List.of("dijes", "pulseras"),
            "navidad", List.of("dijes", "regalos"),
            "san valentin", List.of("anillos", "collares"),
            "dia de la madre", List.of("collares", "aretes"),
            "dia del padre", List.of("pulseras", "dijes")
    );

    private static final Map<String, List<String>> STYLE_KEYWORDS = Map.of(
            "elegante", List.of("elegante", "elegantes", "elegancia", "sofisticad", "lujo"),
            "casual", List.of("casual", "diario", "dia a dia", "versatil", "sencill"),
            "moderno", List.of("moderno", "moderna", "contemporan", "actual", "vanguard"),
            "clasico", List.of("clasico", "clasica", "atemporal", "clasic", "tradicional"),
            "minimalista", List.of("minimalista", "minimal", "sobrio", "discreto", "ligero")
    );

    private final ProductRepository productRepository;
    private final MaterialRepository materialRepository;
    private final ProductService productService;

    public GiftFinderService(ProductRepository productRepository,
                             MaterialRepository materialRepository,
                             ProductService productService) {
        this.productRepository = productRepository;
        this.materialRepository = materialRepository;
        this.productService = productService;
    }

    @Transactional(readOnly = true)
    public GiftFinderResponse recommend(GiftFinderRequest request) {
        validate(request);

        String recipient = normalize(request.getRecipient());
        String occasion = normalize(request.getOccasion());
        String style = normalize(request.getStyle());
        String material = normalize(request.getMaterial());
        BigDecimal min = request.getBudgetMin();
        BigDecimal max = request.getBudgetMax();
        boolean hasBudget = min != null || max != null;

        Long materialId = resolveMaterialId(material);
        List<ProductDTO> candidates = productService.mapProducts(fetch(materialId, min, max));

        boolean hasUserCriteria = !recipient.isEmpty() || !occasion.isEmpty() || !style.isEmpty()
                || !material.isEmpty() || hasBudget;

        List<Scored> scored = new ArrayList<>(candidates.size());
        for (ProductDTO candidate : candidates) {
            scored.add(score(candidate, recipient, occasion, style, material, min, max, hasBudget, hasUserCriteria));
        }

        Comparator<Scored> byScore = Comparator.comparingInt(Scored::score).reversed();
        Comparator<Scored> byAvailability = Comparator
                .comparing((Scored item) -> item.recommendation().isAvailable())
                .reversed();
        Comparator<Scored> byPopularity = Comparator.comparingInt(Scored::soldCount).reversed();
        scored.sort(byScore.thenComparing(byAvailability).thenComparing(byPopularity));

        List<GiftRecommendation> recommendations = scored.stream()
                .limit(MAX_RECOMMENDATIONS)
                .map(Scored::recommendation)
                .toList();

        return GiftFinderResponse.builder().recommendations(recommendations).build();
    }

    private void validate(GiftFinderRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("La solicitud es obligatoria");
        }
        if (request.getBudgetMin() != null && request.getBudgetMin().signum() < 0) {
            throw new IllegalArgumentException("El presupuesto mínimo no puede ser negativo");
        }
        if (request.getBudgetMax() != null && request.getBudgetMax().signum() < 0) {
            throw new IllegalArgumentException("El presupuesto máximo no puede ser negativo");
        }
        if (request.getBudgetMin() != null && request.getBudgetMax() != null
                && request.getBudgetMin().compareTo(request.getBudgetMax()) > 0) {
            throw new IllegalArgumentException("El presupuesto mínimo no puede ser mayor que el máximo");
        }
    }

    private List<Product> fetch(Long materialId, BigDecimal min, BigDecimal max) {
        Pageable pageable = PageRequest.of(0, MAX_CANDIDATES, Sort.by(Sort.Direction.DESC, "soldCount"));
        List<Product> products = productRepository
                .searchFiltered("", null, materialId, min, max, "", false, pageable)
                .getContent();
        if (products.isEmpty() && materialId != null) {
            products = productRepository
                    .searchFiltered("", null, null, min, max, "", false, pageable)
                    .getContent();
        }
        return products;
    }

    private Scored score(ProductDTO product, String recipient, String occasion, String style, String material,
                         BigDecimal min, BigDecimal max, boolean hasBudget, boolean hasUserCriteria) {
        int score = 0;
        int maxScore = 0;
        List<String> reasons = new ArrayList<>();
        String haystack = haystack(product);
        String category = normalize(product.getCategoryName());
        boolean available = product.getStock() != null && product.getStock() > 0;

        if (!recipient.isEmpty()) {
            maxScore += W_RECIPIENT;
            if (categoryMatches(RECIPIENT_CATEGORIES.get(recipient), category)) {
                score += W_RECIPIENT;
                reasons.add("es un acierto para esa persona");
            }
        }

        if (!occasion.isEmpty()) {
            maxScore += W_OCCASION;
            boolean keywordHit = containsAny(haystack, OCCASION_KEYWORDS.get(occasion));
            boolean categoryHit = categoryMatches(OCCASION_CATEGORIES.get(occasion), category);
            if (keywordHit || categoryHit) {
                score += W_OCCASION;
                reasons.add("encaja con esa ocasión");
            }
        }

        if (!style.isEmpty()) {
            maxScore += W_STYLE;
            if (containsAny(haystack, STYLE_KEYWORDS.getOrDefault(style, List.of(style)))) {
                score += W_STYLE;
                reasons.add("tiene el estilo que buscas");
            }
        }

        if (!material.isEmpty()) {
            maxScore += W_MATERIAL;
            if (containsMaterial(product, material)) {
                score += W_MATERIAL;
                reasons.add("está elaborada en el material que quieres");
            }
        }

        if (hasBudget) {
            maxScore += W_BUDGET;
            if (withinBudget(product.getPrice(), min, max)) {
                score += W_BUDGET;
                reasons.add("se ajusta a tu presupuesto");
            }
        }

        maxScore += W_AVAILABILITY;
        if (available) {
            score += W_AVAILABILITY;
        }

        Integer relevance = hasUserCriteria && maxScore > 0
                ? (int) Math.round(100.0 * score / maxScore)
                : null;

        GiftRecommendation recommendation = GiftRecommendation.builder()
                .id(product.getId())
                .name(product.getName())
                .slug(product.getSlug())
                .price(product.getPrice())
                .image(primaryImage(product))
                .material(firstMaterial(product))
                .materialNames(product.getMaterialNames())
                .categoryName(product.getCategoryName())
                .available(available)
                .relevance(relevance)
                .explanation(buildExplanation(reasons, available))
                .build();

        return new Scored(recommendation, score, soldCount(product));
    }

    private String buildExplanation(List<String> reasons, boolean available) {
        if (reasons.isEmpty()) {
            return available
                    ? "Una pieza destacada de nuestro catálogo, seleccionada por su popularidad."
                    : "Ahora mismo está agotada, pero encaja con lo que buscas.";
        }
        String body = joinNatural(reasons);
        String suffix = available ? "" : " (actualmente agotada)";
        return "Lo recomendamos porque " + body + suffix + ".";
    }

    private String joinNatural(List<String> reasons) {
        if (reasons.size() == 1) {
            return reasons.get(0);
        }
        return String.join(", ", reasons.subList(0, reasons.size() - 1)) + " y " + reasons.get(reasons.size() - 1);
    }

    private Long resolveMaterialId(String material) {
        if (material.isEmpty()) {
            return null;
        }
        return materialRepository.findAll().stream()
                .filter(m -> Boolean.TRUE.equals(m.getActive()))
                .filter(m -> contains(material, normalize(m.getName())) || contains(material, normalize(m.getSlug())))
                .map(Material::getId)
                .findFirst()
                .orElse(null);
    }

    private boolean categoryMatches(List<String> categories, String normalizedCategory) {
        if (categories == null || normalizedCategory.isEmpty()) {
            return false;
        }
        for (String category : categories) {
            if (contains(normalizedCategory, normalize(category))) {
                return true;
            }
        }
        return false;
    }

    private boolean containsAny(String haystack, List<String> needles) {
        if (needles == null || haystack.isEmpty()) {
            return false;
        }
        return needles.stream().anyMatch(needle -> contains(haystack, needle));
    }

    private boolean containsMaterial(ProductDTO product, String material) {
        if (product.getMaterialNames() == null || product.getMaterialNames().isEmpty()) {
            return false;
        }
        return product.getMaterialNames().stream().anyMatch(name -> contains(normalize(name), material));
    }

    private boolean withinBudget(BigDecimal price, BigDecimal min, BigDecimal max) {
        if (price == null) {
            return false;
        }
        if (min != null && price.compareTo(min) < 0) {
            return false;
        }
        return max == null || price.compareTo(max) <= 0;
    }

    /** Comprueba coincidencia bidireccional de subcadenas ignorando acentos y mayúsculas. */
    private boolean contains(String haystack, String needle) {
        if (haystack == null || needle == null) {
            return false;
        }
        String normalizedNeedle = normalize(needle);
        return !normalizedNeedle.isEmpty() && haystack.contains(normalizedNeedle);
    }

    private String haystack(ProductDTO product) {
        StringBuilder sb = new StringBuilder();
        append(sb, product.getName());
        append(sb, product.getDescription());
        append(sb, product.getCategoryName());
        append(sb, product.getColor());
        append(sb, product.getFeatures());
        append(sb, product.getCareInstructions());
        if (product.getMaterialNames() != null) {
            product.getMaterialNames().forEach(name -> append(sb, name));
        }
        return normalize(sb.toString());
    }

    private void append(StringBuilder sb, String value) {
        if (value != null && !value.isBlank()) {
            sb.append(value).append(' ');
        }
    }

    private String firstMaterial(ProductDTO product) {
        if (product.getMaterialNames() == null || product.getMaterialNames().isEmpty()) {
            return null;
        }
        return product.getMaterialNames().get(0);
    }

    private String primaryImage(ProductDTO product) {
        if (product.getImages() == null || product.getImages().isEmpty()) {
            return null;
        }
        return product.getImages().stream()
                .filter(image -> Boolean.TRUE.equals(image.getIsPrimary()))
                .map(image -> image.getUrl())
                .findFirst()
                .orElseGet(() -> product.getImages().get(0).getUrl());
    }

    private int soldCount(ProductDTO product) {
        return product.getSoldCount() == null ? 0 : product.getSoldCount();
    }

    private String normalize(String value) {
        return TextNormalizer.normalize(value);
    }

    private record Scored(GiftRecommendation recommendation, int score, int soldCount) {
    }
}
