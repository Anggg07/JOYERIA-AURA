package com.joyeria.service;

import com.joyeria.dto.CategoryCreateRequest;
import com.joyeria.dto.CategoryDTO;
import com.joyeria.exception.ResourceNotFoundException;
import com.joyeria.model.Category;
import com.joyeria.repository.CategoryRepository;
import com.joyeria.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public List<CategoryDTO> getAllCategories() {
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : productRepository.countActiveGroupedByCategory()) {
            counts.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
        }
        return categoryRepository.findByActiveTrueOrderByDisplayOrderAsc()
                .stream().map(c -> mapToDTO(c, counts.getOrDefault(c.getId(), 0L))).toList();
    }

    public CategoryDTO getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada"));
        return mapToDTO(category);
    }

    public CategoryDTO getCategoryBySlug(String slug) {
        Category category = categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada"));
        return mapToDTO(category);
    }

    @Transactional
    public CategoryDTO createCategory(CategoryCreateRequest request) {
        if (categoryRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("La categoría ya existe");
        }
        Category category = new Category();
        category.setName(request.getName());
        category.setSlug(generateSlug(request.getName()));
        category.setDescription(request.getDescription());
        category.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0);
        category.setImage(request.getImage());
        category.setActive(request.getActive() != null ? request.getActive() : true);
        return mapToDTO(categoryRepository.save(category));
    }

    @Transactional
    public CategoryDTO updateCategory(Long id, CategoryCreateRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada"));
        if (request.getName() != null) category.setName(request.getName());
        if (request.getDescription() != null) category.setDescription(request.getDescription());
        if (request.getDisplayOrder() != null) category.setDisplayOrder(request.getDisplayOrder());
        if (request.getImage() != null) category.setImage(request.getImage());
        if (request.getActive() != null) category.setActive(request.getActive());
        return mapToDTO(categoryRepository.save(category));
    }

    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada"));
        category.setActive(false);
        categoryRepository.save(category);
    }

    private CategoryDTO mapToDTO(Category category) {
        long productCount = 0;
        try {
            productCount = productRepository.findByCategoryIdAndActiveTrue(category.getId(),
                    org.springframework.data.domain.PageRequest.of(0, 1)).getTotalElements();
        } catch (Exception e) { /* ignore */ }

        return mapToDTO(category, productCount);
    }

    private CategoryDTO mapToDTO(Category category, long productCount) {
        return CategoryDTO.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .displayOrder(category.getDisplayOrder())
                .image(category.getImage())
                .active(category.getActive())
                .productCount(productCount)
                .createdAt(category.getCreatedAt())
                .build();
    }

    private String generateSlug(String name) {
        String slug = name.toLowerCase()
                .replaceAll("[áàäâ]", "a").replaceAll("[éèëê]", "e")
                .replaceAll("[íìïî]", "i").replaceAll("[óòöô]", "o")
                .replaceAll("[úùüû]", "u").replaceAll("[ñ]", "n")
                .replaceAll("[^a-z0-9\\s-]", "").replaceAll("\\s+", "-");
        if (categoryRepository.existsBySlug(slug)) {
            slug = slug + "-" + UUID.randomUUID().toString().substring(0, 6);
        }
        return slug;
    }
}
