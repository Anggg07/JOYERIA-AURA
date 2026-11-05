package com.joyeria.service;

import com.joyeria.dto.*;
import com.joyeria.exception.ResourceNotFoundException;
import com.joyeria.model.*;
import com.joyeria.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final Set<String> SORT_FIELDS =
            Set.of("createdAt", "price", "comparePrice", "name", "soldCount", "stock");

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final MaterialRepository materialRepository;
    private final ReviewRepository reviewRepository;

    private Pageable buildPageable(int page, int size, String sort, String direction) {
        if (page < 0) {
            throw new IllegalArgumentException("Parámetro 'page' no puede ser negativo");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("Parámetro 'size' debe estar entre 1 y " + MAX_PAGE_SIZE);
        }
        if (sort == null || !SORT_FIELDS.contains(sort)) {
            throw new IllegalArgumentException("Parámetro 'sort' no válido");
        }
        String dir = direction == null ? "desc" : direction.toLowerCase();
        if (!dir.equals("asc") && !dir.equals("desc")) {
            throw new IllegalArgumentException("Parámetro 'direction' no válido");
        }
        Sort sortConfig = dir.equals("desc") ? Sort.by(sort).descending() : Sort.by(sort).ascending();
        return PageRequest.of(page, size, sortConfig);
    }

    public PagedResponse<ProductDTO> getAllProducts(int page, int size, String sort, String direction) {
        Page<Product> products = productRepository.findByActiveTrue(buildPageable(page, size, sort, direction));
        return mapToPagedResponse(products);
    }

    public PagedResponse<ProductDTO> getFiltered(Long categoryId, Long materialId, BigDecimal minPrice, BigDecimal maxPrice,
                                                  String color, boolean inStock, int page, int size, String sort, String direction) {
        Page<Product> products = productRepository.findFiltered(categoryId, materialId, minPrice, maxPrice,
                colorOrEmpty(color), inStock, buildPageable(page, size, sort, direction));
        return mapToPagedResponse(products);
    }

    public ProductDTO getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
        return mapToDTO(product);
    }

    public ProductDTO getProductBySlug(String slug) {
        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
        return mapToDTO(product);
    }

    public List<ProductDTO> getFeaturedProducts() {
        return productRepository.findByFeaturedTrueAndActiveTrue(
                PageRequest.of(0, 8, Sort.by("createdAt").descending())
        ).getContent().stream().map(this::mapToDTO).toList();
    }

    public List<ProductDTO> getNewProducts() {
        return productRepository.findByIsNewTrueAndActiveTrue(
                PageRequest.of(0, 8, Sort.by("createdAt").descending())
        ).getContent().stream().map(this::mapToDTO).toList();
    }

    public List<ProductDTO> getBestSellers() {
        return productRepository.findTop10ByActiveTrueOrderBySoldCountDesc()
                .stream().map(this::mapToDTO).toList();
    }

    public PagedResponse<ProductDTO> search(String query, int page, int size, String sort, String direction,
                                            Long categoryId, Long materialId, BigDecimal minPrice, BigDecimal maxPrice,
                                            String color, boolean inStock) {
        Page<Product> products = productRepository.searchFiltered(
                query, categoryId, materialId, minPrice, maxPrice, colorOrEmpty(color), inStock,
                buildPageable(page, size, sort, direction));
        return mapToPagedResponse(products);
    }

    private String colorOrEmpty(String color) {
        return color == null ? "" : color;
    }

    @Transactional
    public ProductDTO createProduct(ProductCreateRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada"));

        String slug = generateSlug(request.getName());
        if (productRepository.existsBySlug(slug)) {
            slug = slug + "-" + System.currentTimeMillis();
        }

        String sku = request.getSku() != null ? request.getSku() : "SKU-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Product product = new Product();
        product.setName(request.getName());
        product.setSlug(slug);
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setComparePrice(request.getComparePrice());
        product.setSku(sku);
        product.setStock(request.getStock() != null ? request.getStock() : 0);
        product.setWeight(request.getWeight());
        product.setDimensions(request.getDimensions());
        product.setSize(request.getSize());
        product.setColor(request.getColor());
        product.setCareInstructions(request.getCareInstructions());
        product.setFeatures(request.getFeatures());
        product.setDeliveryTime(request.getDeliveryTime());
        product.setFeatured(request.getFeatured() != null ? request.getFeatured() : false);
        product.setIsNew(request.getIsNew() != null ? request.getIsNew() : false);
        product.setBestSeller(request.getBestSeller() != null ? request.getBestSeller() : false);
        product.setActive(request.getActive() != null ? request.getActive() : true);
        product.setCategory(category);

        if (request.getMaterialIds() != null && !request.getMaterialIds().isEmpty()) {
            Set<Material> materials = new HashSet<>(materialRepository.findAllById(request.getMaterialIds()));
            product.setMaterials(materials);
        }

        product = productRepository.save(product);

        applyImages(product, request.getImageUrls());

        return mapToDTO(productRepository.save(product));
    }

    @Transactional
    public ProductDTO updateProduct(Long id, ProductCreateRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));

        if (request.getName() != null) product.setName(request.getName());
        if (request.getDescription() != null) product.setDescription(request.getDescription());
        if (request.getPrice() != null) product.setPrice(request.getPrice());
        if (request.getComparePrice() != null) product.setComparePrice(request.getComparePrice());
        if (request.getStock() != null) product.setStock(request.getStock());
        if (request.getWeight() != null) product.setWeight(request.getWeight());
        if (request.getDimensions() != null) product.setDimensions(request.getDimensions());
        if (request.getSize() != null) product.setSize(request.getSize());
        if (request.getColor() != null) product.setColor(request.getColor());
        if (request.getCareInstructions() != null) product.setCareInstructions(request.getCareInstructions());
        if (request.getFeatures() != null) product.setFeatures(request.getFeatures());
        if (request.getDeliveryTime() != null) product.setDeliveryTime(request.getDeliveryTime());
        if (request.getFeatured() != null) product.setFeatured(request.getFeatured());
        if (request.getIsNew() != null) product.setIsNew(request.getIsNew());
        if (request.getBestSeller() != null) product.setBestSeller(request.getBestSeller());
        if (request.getActive() != null) product.setActive(request.getActive());
        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada"));
            product.setCategory(category);
        }
        if (request.getMaterialIds() != null) {
            Set<Material> materials = new HashSet<>(materialRepository.findAllById(request.getMaterialIds()));
            product.setMaterials(materials);
        }
        if (request.getImageUrls() != null) {
            applyImages(product, request.getImageUrls());
        }

        return mapToDTO(productRepository.save(product));
    }

    private void applyImages(Product product, List<String> urls) {
        product.getImages().clear();
        if (urls == null) {
            return;
        }
        int order = 0;
        for (String url : urls) {
            if (url == null || url.isBlank()) {
                continue;
            }
            ProductImage image = new ProductImage();
            image.setProduct(product);
            image.setUrl(url.trim());
            image.setAlt(product.getName() + " - Imagen " + (order + 1));
            image.setIsPrimary(order == 0);
            image.setSortOrder(order);
            product.getImages().add(image);
            order++;
        }
    }

    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
        product.setActive(false);
        productRepository.save(product);
    }

    public List<ProductDTO> getRelatedProducts(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
        if (product.getCategory() != null) {
            return productRepository.findByCategoryIdAndActiveTrue(
                    product.getCategory().getId(),
                    PageRequest.of(0, 4)
            ).getContent().stream()
                    .filter(p -> !p.getId().equals(productId))
                    .map(this::mapToDTO).toList();
        }
        return List.of();
    }

    /**
     * Mapea una colección de entidades {@link Product} a {@link ProductDTO},
     * reutilizando el cálculo de ratings. Expuesto para servicios que trabajan
     * con productos ya cargados (p. ej. el asistente de compras).
     */
    public List<ProductDTO> mapProducts(Collection<Product> products) {
        List<Product> list = List.copyOf(products);
        Map<Long, double[]> ratings = ratingsByProduct(list);
        return list.stream().map(product -> mapToDTO(product, ratings)).toList();
    }

    private ProductDTO mapToDTO(Product product) {
        return mapToDTO(product, ratingsByProduct(List.of(product)));
    }

    private ProductDTO mapToDTO(Product product, Map<Long, double[]> ratings) {
        ProductDTO.ProductDTOBuilder builder = ProductDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .slug(product.getSlug())
                .description(product.getDescription())
                .price(product.getPrice())
                .comparePrice(product.getComparePrice())
                .sku(product.getSku())
                .stock(product.getStock())
                .weight(product.getWeight())
                .dimensions(product.getDimensions())
                .size(product.getSize())
                .color(product.getColor())
                .careInstructions(product.getCareInstructions())
                .features(product.getFeatures())
                .deliveryTime(product.getDeliveryTime())
                .featured(product.getFeatured())
                .isNew(product.getIsNew())
                .bestSeller(product.getBestSeller())
                .active(product.getActive())
                .soldCount(product.getSoldCount())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt());

        if (product.getCategory() != null) {
            builder.categoryId(product.getCategory().getId());
            builder.categoryName(product.getCategory().getName());
        }

        if (product.getMaterials() != null) {
            builder.materialIds(product.getMaterials().stream().map(Material::getId).toList());
            builder.materialNames(product.getMaterials().stream().map(Material::getName).toList());
        }

        if (product.getImages() != null) {
            builder.images(product.getImages().stream().map(img ->
                    ProductImageDTO.builder()
                            .id(img.getId())
                            .url(img.getUrl())
                            .alt(img.getAlt())
                            .isPrimary(img.getIsPrimary())
                            .sortOrder(img.getSortOrder())
                            .build()
            ).toList());
        }

        double[] rating = ratings.get(product.getId());
        if (rating != null) {
            builder.averageRating(rating[0]);
            builder.reviewCount((int) rating[1]);
        }

        if (product.getComparePrice() != null && product.getComparePrice().compareTo(product.getPrice()) > 0) {
            BigDecimal discount = BigDecimal.ONE.subtract(
                    product.getPrice().divide(product.getComparePrice(), 4, RoundingMode.HALF_UP)
            ).multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP);
            builder.discountPercent(discount);
        } else {
            builder.discountPercent(BigDecimal.ZERO);
        }

        return builder.build();
    }

    private Map<Long, double[]> ratingsByProduct(Collection<Product> content) {
        List<Long> ids = content.stream().map(Product::getId).toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<Long, double[]> map = new HashMap<>();
        for (Object[] row : reviewRepository.aggregateRatings(ids, Review.ReviewStatus.APPROVED)) {
            Long productId = ((Number) row[0]).longValue();
            double average = Math.round(((Number) row[1]).doubleValue() * 10.0) / 10.0;
            long count = ((Number) row[2]).longValue();
            map.put(productId, new double[]{average, count});
        }
        return map;
    }

    private PagedResponse<ProductDTO> mapToPagedResponse(Page<Product> products) {
        Map<Long, double[]> ratings = ratingsByProduct(products.getContent());
        return PagedResponse.<ProductDTO>builder()
                .content(products.getContent().stream().map(p -> mapToDTO(p, ratings)).toList())
                .page(products.getNumber())
                .size(products.getSize())
                .totalElements(products.getTotalElements())
                .totalPages(products.getTotalPages())
                .first(products.isFirst())
                .last(products.isLast())
                .build();
    }

    private String generateSlug(String name) {
        return name.toLowerCase()
                .replaceAll("[áàäâ]", "a")
                .replaceAll("[éèëê]", "e")
                .replaceAll("[íìïî]", "i")
                .replaceAll("[óòöô]", "o")
                .replaceAll("[úùüû]", "u")
                .replaceAll("[ñ]", "n")
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }
}
