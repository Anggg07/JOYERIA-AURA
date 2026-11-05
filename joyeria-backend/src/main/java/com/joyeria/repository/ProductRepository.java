package com.joyeria.repository;

import com.joyeria.model.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findBySlug(String slug);
    Optional<Product> findBySku(String sku);
    boolean existsBySlug(String slug);
    boolean existsBySku(String sku);
    Page<Product> findByActiveTrue(Pageable pageable);
    Page<Product> findByCategoryIdAndActiveTrue(Long categoryId, Pageable pageable);
    Page<Product> findByFeaturedTrueAndActiveTrue(Pageable pageable);
    Page<Product> findByIsNewTrueAndActiveTrue(Pageable pageable);
    Page<Product> findByBestSellerTrueAndActiveTrue(Pageable pageable);
    List<Product> findTop10ByActiveTrueOrderBySoldCountDesc();

    @Query("SELECT p.category.id, COUNT(p) FROM Product p WHERE p.active = true AND p.category IS NOT NULL GROUP BY p.category.id")
    List<Object[]> countActiveGroupedByCategory();

    long countByActiveTrueAndStockLessThanEqual(int stock);

    long countByActiveTrueAndStockGreaterThanAndStockLessThanEqual(int minExclusive, int maxInclusive);

    List<Product> findTop10ByActiveTrueAndStockLessThanEqualOrderByStockAsc(int stock);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);
    
    @Query("SELECT p FROM Product p WHERE p.active = true AND (" +
           "LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.sku) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.color) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Product> searchProducts(@Param("query") String query, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.active = true AND " +
           "(:categoryId IS NULL OR p.category.id = :categoryId) AND " +
           "(:materialId IS NULL OR EXISTS (SELECT 1 FROM p.materials m WHERE m.id = :materialId)) AND " +
           "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
           "(:maxPrice IS NULL OR p.price <= :maxPrice) AND " +
           "(:color = '' OR LOWER(p.color) = LOWER(:color)) AND " +
           "(:inStock = false OR p.stock > 0)")
    Page<Product> findFiltered(
            @Param("categoryId") Long categoryId,
            @Param("materialId") Long materialId,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("color") String color,
            @Param("inStock") boolean inStock,
            Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.active = true AND " +
           "(:query IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.sku) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.color) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
           "(:categoryId IS NULL OR p.category.id = :categoryId) AND " +
           "(:materialId IS NULL OR EXISTS (SELECT 1 FROM p.materials m WHERE m.id = :materialId)) AND " +
           "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
           "(:maxPrice IS NULL OR p.price <= :maxPrice) AND " +
           "(:color = '' OR LOWER(p.color) = LOWER(:color)) AND " +
           "(:inStock = false OR p.stock > 0)")
    Page<Product> searchFiltered(
            @Param("query") String query,
            @Param("categoryId") Long categoryId,
            @Param("materialId") Long materialId,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("color") String color,
            @Param("inStock") boolean inStock,
            Pageable pageable);
}
