package com.joyeria.repository;

import com.joyeria.model.Order;
import com.joyeria.model.Order.OrderStatus;
import com.joyeria.model.Order.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderNumber(String orderNumber);
    Page<Order> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    Page<Order> findAllByOrderByCreatedAtDesc(Pageable pageable);
    List<Order> findTop5ByOrderByCreatedAtDesc();
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);
    boolean existsByUserIdAndItemsProductIdAndStatusIn(Long userId, Long productId, java.util.Collection<OrderStatus> statuses);
    
    @Query("SELECT COALESCE(SUM(o.total), 0) FROM Order o WHERE o.status = :status")
    java.math.BigDecimal sumTotalByStatus(@Param("status") OrderStatus status);

    @Query("SELECT COALESCE(SUM(o.total), 0) FROM Order o WHERE o.paymentStatus = :status")
    java.math.BigDecimal sumTotalByPaymentStatus(@Param("status") PaymentStatus status);

    @Query("SELECT COALESCE(SUM(o.total), 0) FROM Order o WHERE o.paymentStatus = :status AND o.createdAt >= :start")
    java.math.BigDecimal sumTotalByPaymentStatusSince(@Param("status") PaymentStatus status,
                                                      @Param("start") java.time.LocalDateTime start);

    @Query("SELECT o.status, COUNT(o) FROM Order o GROUP BY o.status")
    List<Object[]> countGroupByStatus();
}
