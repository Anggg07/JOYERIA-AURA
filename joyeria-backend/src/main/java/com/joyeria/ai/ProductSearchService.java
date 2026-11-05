package com.joyeria.ai;

import com.joyeria.dto.ProductDTO;
import com.joyeria.model.Category;
import com.joyeria.model.Material;
import com.joyeria.model.Product;
import com.joyeria.repository.CategoryRepository;
import com.joyeria.repository.MaterialRepository;
import com.joyeria.repository.ProductRepository;
import com.joyeria.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Consulta productos <strong>reales</strong> de PostgreSQL a partir de una
 * intención. Es la única fuente de datos comerciales del asistente.
 * Si el filtro inicial no devuelve nada, se relaja progresivamente para no
 * dejar al usuario sin resultados.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductSearchService {

    private static final int MAX_CANDIDATES = 50;
    private static final int MAX_KEYWORD_QUERIES = 6;

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final MaterialRepository materialRepository;
    private final ProductService productService;

    public List<ProductDTO> findCandidates(SearchIntent intent) {
        Long categoryId = resolveCategoryId(intent.getCategory());
        Long materialId = resolveMaterialId(intent.getMaterial());

        List<Product> pool = collect(categoryId, materialId, intent);
        if (pool.isEmpty() && materialId != null) {
            pool = collect(categoryId, null, intent);
        }
        if (pool.isEmpty() && categoryId != null) {
            pool = collect(null, materialId, intent);
        }
        if (pool.isEmpty()) {
            pool = collect(null, null, intent);
        }
        return productService.mapProducts(pool);
    }

    /**
     * Recupera candidatos combinando la consulta estructurada (categoría,
     * material, precio y disponibilidad) con búsquedas por cada término
     * distintivo del mensaje (color y palabras clave). Así un producto que
     * coincide con lo pedido aparece aunque no esté entre los más vendidos.
     * Los resultados se deduplican por id preservando el orden de inserción.
     */
    private List<Product> collect(Long categoryId, Long materialId, SearchIntent intent) {
        Map<Long, Product> merged = new LinkedHashMap<>();
        addAll(merged, query(categoryId, materialId, intent, null));
        for (String term : distinctiveTerms(intent)) {
            if (merged.size() >= MAX_CANDIDATES) {
                break;
            }
            addAll(merged, query(categoryId, materialId, intent, term));
        }
        return new ArrayList<>(merged.values());
    }

    private List<String> distinctiveTerms(SearchIntent intent) {
        Set<String> terms = new LinkedHashSet<>();
        if (intent.getColor() != null && !intent.getColor().isBlank()) {
            terms.add(intent.getColor());
        }
        if (intent.getKeywords() != null) {
            intent.getKeywords().stream()
                    .filter(keyword -> keyword != null && !keyword.isBlank())
                    .forEach(terms::add);
        }
        return terms.stream().limit(MAX_KEYWORD_QUERIES).toList();
    }

    private void addAll(Map<Long, Product> target, List<Product> products) {
        for (Product product : products) {
            target.putIfAbsent(product.getId(), product);
        }
    }

    private List<Product> query(Long categoryId, Long materialId, SearchIntent intent, String term) {
        Pageable pageable = PageRequest.of(0, MAX_CANDIDATES,
                Sort.by(Sort.Direction.DESC, "soldCount"));
        return productRepository.searchFiltered(
                term == null ? "" : term,
                categoryId,
                materialId,
                intent.getMinPrice(),
                intent.getMaxPrice(),
                "",
                intent.isInStockOnly(),
                pageable).getContent();
    }

    Long resolveCategoryId(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String needle = TextNormalizer.normalize(name);
        return categoryRepository.findByActiveTrueOrderByDisplayOrderAsc().stream()
                .filter(category -> matches(category.getName(), needle) || matches(category.getSlug(), needle))
                .map(Category::getId)
                .findFirst()
                .orElse(null);
    }

    Long resolveMaterialId(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String needle = TextNormalizer.normalize(name);
        return materialRepository.findByActiveTrue().stream()
                .filter(material -> matches(material.getName(), needle) || matches(material.getSlug(), needle))
                .map(Material::getId)
                .findFirst()
                .orElse(null);
    }

    private boolean matches(String candidate, String needle) {
        if (candidate == null) {
            return false;
        }
        String normalized = TextNormalizer.normalize(candidate);
        return normalized.equals(needle) || normalized.contains(needle) || needle.contains(normalized);
    }
}
