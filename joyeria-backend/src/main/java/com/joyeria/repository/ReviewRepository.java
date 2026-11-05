package com.joyeria.repository;

import com.joyeria.model.Review;
import com.joyeria.model.Review.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    Page<Review> findByProductIdAndStatusOrderByCreatedAtDesc(Long productId, ReviewStatus status, Pageable pageable);
    Optional<Review> findByUserIdAndProductId(Long userId, Long productId);
    boolean existsByUserIdAndProductId(Long userId, Long productId);
    Page<Review> findByStatus(ReviewStatus status, Pageable pageable);
    Page<Review> findAllByOrderByCreatedAtDesc(Pageable pageable);
    long countByStatus(ReviewStatus status);

    @Query("SELECT r.product.id, COALESCE(AVG(r.rating), 0.0), COUNT(r) " +
           "FROM Review r WHERE r.product.id IN :productIds AND r.status = :status GROUP BY r.product.id")
    List<Object[]> aggregateRatings(
            @Param("productIds") Collection<Long> productIds,
            @Param("status") ReviewStatus status);
}
