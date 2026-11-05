package com.joyeria.ai.admin;

import com.joyeria.dto.DashboardStatsDTO;
import com.joyeria.dto.TopProductDTO;
import com.joyeria.model.Order;
import com.joyeria.model.Review.ReviewStatus;
import com.joyeria.model.SupportTicket.TicketStatus;
import com.joyeria.repository.OrderRepository;
import com.joyeria.repository.ProductRepository;
import com.joyeria.repository.ReviewRepository;
import com.joyeria.repository.SupportTicketRepository;
import com.joyeria.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Recopila una fotografía de solo lectura del estado de la tienda para el
 * asistente de administración: ventas, pedidos, inventario, reseñas y soporte.
 * Solo entrega métricas agregadas y unos pocos pedidos recientes (con su ID
 * real) para que la IA no tenga que inventar identificadores. Todos los valores
 * provienen de PostgreSQL.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminInsightsService {

    private final OrderService orderService;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final ReviewRepository reviewRepository;
    private final SupportTicketRepository supportTicketRepository;

    @Value("${app.low-stock-threshold:5}")
    private int lowStockThreshold;

    public AdminInsights gather() {
        DashboardStatsDTO stats = orderService.getDashboardStats();

        List<AdminInsights.TopProduct> topProducts = stats.getTopSellingProducts() == null
                ? List.of()
                : stats.getTopSellingProducts().stream()
                        .map(this::toTopProduct)
                        .toList();

        List<AdminInsights.LowStockProduct> lowStock = productRepository
                .findTop10ByActiveTrueAndStockLessThanEqualOrderByStockAsc(lowStockThreshold)
                .stream()
                .map(product -> new AdminInsights.LowStockProduct(
                        product.getName(),
                        product.getStock() == null ? 0 : product.getStock()))
                .toList();

        List<AdminInsights.RecentOrder> recentOrders = orderRepository.findTop5ByOrderByCreatedAtDesc()
                .stream()
                .map(this::toRecentOrder)
                .toList();

        return new AdminInsights(
                stats.getTotalOrders(),
                stats.getTotalRevenue(),
                stats.getTodayRevenue(),
                weekRevenue(),
                stats.getMonthRevenue(),
                stats.getTotalProducts(),
                stats.getTotalCustomers(),
                stats.getPendingOrders(),
                reviewRepository.countByStatus(ReviewStatus.PENDING),
                supportTicketRepository.countByStatusIn(List.of(TicketStatus.OPEN, TicketStatus.IN_PROGRESS)),
                stats.getLowStockProducts(),
                stats.getOutOfStockProducts(),
                stats.getOrdersByStatus(),
                topProducts,
                lowStock,
                recentOrders);
    }

    private java.math.BigDecimal weekRevenue() {
        LocalDateTime startOfWeek = LocalDateTime.now().minusDays(7);
        return orderRepository.sumTotalByPaymentStatusSince(Order.PaymentStatus.COMPLETED, startOfWeek);
    }

    private AdminInsights.TopProduct toTopProduct(TopProductDTO product) {
        return new AdminInsights.TopProduct(
                product.getName(),
                product.getSoldCount() == null ? 0L : product.getSoldCount());
    }

    private AdminInsights.RecentOrder toRecentOrder(Order order) {
        return new AdminInsights.RecentOrder(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus() == null ? null : order.getStatus().name(),
                order.getTotal());
    }
}
