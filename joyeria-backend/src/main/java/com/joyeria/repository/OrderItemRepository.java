package com.joyeria.repository;

import com.joyeria.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findByOrderId(Long orderId);
    
    @Query("SELECT oi.product.id, SUM(oi.quantity) as totalSold FROM OrderItem oi " +
           "WHERE oi.order.status = 'PAID' OR oi.order.status = 'DELIVERED' OR oi.order.status = 'SHIPPED' " +
           "GROUP BY oi.product.id ORDER BY totalSold DESC")
    List<Object[]> findTopSellingProducts();
}
