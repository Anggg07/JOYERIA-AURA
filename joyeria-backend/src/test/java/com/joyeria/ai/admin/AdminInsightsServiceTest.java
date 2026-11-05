package com.joyeria.ai.admin;

import com.joyeria.dto.DashboardStatsDTO;
import com.joyeria.dto.TopProductDTO;
import com.joyeria.model.Order;
import com.joyeria.model.Product;
import com.joyeria.model.Review.ReviewStatus;
import com.joyeria.model.SupportTicket.TicketStatus;
import com.joyeria.repository.OrderRepository;
import com.joyeria.repository.ProductRepository;
import com.joyeria.repository.ReviewRepository;
import com.joyeria.repository.SupportTicketRepository;
import com.joyeria.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminInsightsServiceTest {

    @Mock
    private OrderService orderService;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private SupportTicketRepository supportTicketRepository;

    private AdminInsightsService service;

    @BeforeEach
    void setUp() {
        service = new AdminInsightsService(orderService, productRepository, orderRepository,
                reviewRepository, supportTicketRepository);
        ReflectionTestUtils.setField(service, "lowStockThreshold", 5);
    }

    @Test
    void gatherCollectedAllSources() {
        when(orderService.getDashboardStats()).thenReturn(DashboardStatsDTO.builder()
                .totalOrders(120L)
                .totalRevenue(new BigDecimal("15000000"))
                .todayRevenue(new BigDecimal("1200000"))
                .monthRevenue(new BigDecimal("8000000"))
                .totalProducts(45L)
                .totalCustomers(300L)
                .pendingOrders(7L)
                .lowStockProducts(3L)
                .outOfStockProducts(1L)
                .topSellingProducts(List.of(TopProductDTO.builder()
                        .name("Anillo Aurora").soldCount(12L).build()))
                .ordersByStatus(Map.of("PENDING", 7L, "PAID", 20L))
                .build());

        Product product = new Product();
        product.setName("Collar Luna");
        product.setStock(2);
        when(productRepository.findTop10ByActiveTrueAndStockLessThanEqualOrderByStockAsc(5))
                .thenReturn(List.of(product));

        when(orderRepository.sumTotalByPaymentStatusSince(eq(Order.PaymentStatus.COMPLETED), any(LocalDateTime.class)))
                .thenReturn(new BigDecimal("4500000"));

        Order recent = new Order();
        recent.setId(5L);
        recent.setOrderNumber("ORD-5");
        recent.setStatus(Order.OrderStatus.PENDING);
        recent.setTotal(new BigDecimal("250000"));
        when(orderRepository.findTop5ByOrderByCreatedAtDesc()).thenReturn(List.of(recent));

        when(reviewRepository.countByStatus(ReviewStatus.PENDING)).thenReturn(4L);
        when(supportTicketRepository.countByStatusIn(List.of(TicketStatus.OPEN, TicketStatus.IN_PROGRESS)))
                .thenReturn(2L);

        AdminInsights insights = service.gather();

        assertEquals(120L, insights.totalOrders());
        assertEquals(new BigDecimal("1200000"), insights.todayRevenue());
        assertEquals(new BigDecimal("4500000"), insights.weekRevenue());
        assertEquals(new BigDecimal("8000000"), insights.monthRevenue());
        assertEquals(4L, insights.pendingReviews());
        assertEquals(2L, insights.openTickets());
        assertEquals("Anillo Aurora", insights.topProducts().get(0).name());
        assertEquals("Collar Luna", insights.lowStockList().get(0).name());
        assertNotNull(insights.recentOrders());
        assertEquals(5L, insights.recentOrders().get(0).id());
        assertEquals("PENDING", insights.recentOrders().get(0).status());
    }
}
