package com.joyeria.ai.admin;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Fotografía de solo lectura del estado de la tienda que se entrega al asistente
 * de administración. Todos los valores provienen de PostgreSQL; la IA nunca los
 * inventa ni los modifica.
 */
public record AdminInsights(
        Long totalOrders,
        BigDecimal totalRevenue,
        BigDecimal todayRevenue,
        BigDecimal weekRevenue,
        BigDecimal monthRevenue,
        Long totalProducts,
        Long totalCustomers,
        Long pendingOrders,
        Long pendingReviews,
        Long openTickets,
        Long lowStockProducts,
        Long outOfStockProducts,
        Map<String, Long> ordersByStatus,
        List<TopProduct> topProducts,
        List<LowStockProduct> lowStockList,
        List<RecentOrder> recentOrders
) {

    /** Producto más vendido (solo nombre y unidades). */
    public record TopProduct(String name, long soldCount) {
    }

    /** Producto con inventario bajo (solo nombre y existencias). */
    public record LowStockProduct(String name, int stock) {
    }

    /**
     * Pedido reciente con su identificador real, para que el asistente pueda
     * referenciarlo y proponer acciones con el ID correcto (nunca lo inventa).
     */
    public record RecentOrder(Long id, String orderNumber, String status, BigDecimal total) {
    }
}
