package com.joyeria.service;

import com.joyeria.dto.CheckoutSummaryDTO;
import com.joyeria.dto.DashboardStatsDTO;
import com.joyeria.dto.OrderCreateRequest;
import com.joyeria.dto.OrderDTO;
import com.joyeria.exception.ResourceNotFoundException;
import com.joyeria.model.*;
import com.joyeria.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderItemRepository orderItemRepository;
    @Mock
    private CartRepository cartRepository;
    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CouponRepository couponRepository;
    @Mock
    private CouponService couponService;
    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;
    @Mock
    private PushNotificationService pushNotificationService;
    @InjectMocks
    private OrderService orderService;

    private User testUser;
    private Cart testCart;
    private Product testProduct;
    private CartItem testCartItem;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setEmail("test@example.com");

        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setName("Test Product");
        testProduct.setPrice(new BigDecimal("100000"));
        testProduct.setStock(10);
        testProduct.setSoldCount(0);
        testProduct.setSku("TEST-001");

        testCart = new Cart();
        testCart.setId(1L);
        testCart.setUser(testUser);

        testCartItem = new CartItem();
        testCartItem.setId(1L);
        testCartItem.setCart(testCart);
        testCartItem.setProduct(testProduct);
        testCartItem.setQuantity(2);

        List<CartItem> items = new ArrayList<>();
        items.add(testCartItem);
        testCart.setItems(items);
    }

    @Test
    void createOrder_WithEmptyCart_ThrowsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(testCart));
        testCart.setItems(new ArrayList<>());

        OrderCreateRequest request = new OrderCreateRequest();
        request.setShippingAddress("Test Address");
        request.setShippingCity("Bogota");
        request.setPaymentMethod("card");

        assertThrows(IllegalStateException.class, () -> orderService.createOrder(1L, request));
    }

    @Test
    void createOrder_WithInsufficientStock_ThrowsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(testCart));
        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(testProduct));
        testProduct.setStock(1);

        OrderCreateRequest request = new OrderCreateRequest();
        request.setShippingAddress("Test Address");
        request.setShippingCity("Bogota");
        request.setPaymentMethod("card");

        assertThrows(IllegalStateException.class, () -> orderService.createOrder(1L, request));
    }

    @Test
    void getOrderById_WithNonExistentOrder_ThrowsException() {
        when(orderRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> orderService.getOrderById(999L));
    }

    @Test
    void updateOrderStatus_WithInvalidTransition_ThrowsException() {
        Order order = new Order();
        order.setId(1L);
        order.setStatus(Order.OrderStatus.DELIVERED);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThrows(IllegalStateException.class, () -> orderService.updateOrderStatus(1L, "PENDING"));
    }

    private OrderCreateRequest baseRequest() {
        OrderCreateRequest request = new OrderCreateRequest();
        request.setShippingAddress("Test Address");
        request.setShippingCity("Bogota");
        request.setPaymentMethod("card");
        return request;
    }

    @Test
    void createOrder_WithValidCoupon_AppliesDiscount() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(testCart));
        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(testProduct));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Coupon coupon = new Coupon();
        coupon.setCode("SAVE10");
        coupon.setDiscountType(Coupon.DiscountType.PERCENTAGE);
        coupon.setDiscountValue(new BigDecimal("10"));
        coupon.setUsesCount(0);
        when(couponService.getValidCoupon("SAVE10")).thenReturn(coupon);

        OrderCreateRequest request = baseRequest();
        request.setCouponCode("save10");

        OrderDTO order = orderService.createOrder(1L, request);

        assertEquals(0, new BigDecimal("200000").compareTo(order.getSubtotal()));
        assertEquals(0, new BigDecimal("20000").compareTo(order.getDiscount()));
        assertEquals(0, new BigDecimal("195000").compareTo(order.getTotal()));
        assertEquals("SAVE10", order.getCouponCode());
        assertEquals(1, coupon.getUsesCount());
    }

    @Test
    void createOrder_WithInvalidCoupon_ThrowsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(testCart));
        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(testProduct));
        when(couponService.getValidCoupon("NOPE"))
                .thenThrow(new IllegalStateException("El cupón ha expirado"));

        OrderCreateRequest request = baseRequest();
        request.setCouponCode("NOPE");

        assertThrows(IllegalStateException.class, () -> orderService.createOrder(1L, request));
    }

    @Test
    void createOrder_WithMinAmountNotReached_ThrowsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(testCart));
        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(testProduct));

        Coupon coupon = new Coupon();
        coupon.setCode("BIG");
        coupon.setDiscountType(Coupon.DiscountType.FIXED);
        coupon.setDiscountValue(new BigDecimal("50000"));
        coupon.setMinAmount(new BigDecimal("500000"));
        coupon.setUsesCount(0);
        when(couponService.getValidCoupon("BIG")).thenReturn(coupon);

        OrderCreateRequest request = baseRequest();
        request.setCouponCode("BIG");

        assertThrows(IllegalStateException.class, () -> orderService.createOrder(1L, request));
    }

    @Test
    void summarizeCart_ComputesTotalsFromBackendPrices() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(testCart));

        CheckoutSummaryDTO summary = orderService.summarizeCart(1L, null);

        assertEquals(0, new BigDecimal("200000").compareTo(summary.getSubtotal()));
        assertEquals(0, BigDecimal.ZERO.compareTo(summary.getDiscount()));
        assertEquals(0, new BigDecimal("15000").compareTo(summary.getShippingCost()));
        assertEquals(0, new BigDecimal("215000").compareTo(summary.getTotal()));
        assertNull(summary.getCouponCode());
    }

    @Test
    void summarizeCart_WithCoupon_AppliesDiscountWithoutConsumingUses() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(testCart));

        Coupon coupon = new Coupon();
        coupon.setCode("SAVE10");
        coupon.setDiscountType(Coupon.DiscountType.PERCENTAGE);
        coupon.setDiscountValue(new BigDecimal("10"));
        coupon.setUsesCount(0);
        when(couponService.getValidCoupon("SAVE10")).thenReturn(coupon);

        CheckoutSummaryDTO summary = orderService.summarizeCart(1L, "save10");

        assertEquals(0, new BigDecimal("20000").compareTo(summary.getDiscount()));
        assertEquals(0, new BigDecimal("195000").compareTo(summary.getTotal()));
        assertEquals("SAVE10", summary.getCouponCode());
        assertEquals(0, coupon.getUsesCount());
    }

    @Test
    void summarizeCart_WithFreeShippingThreshold_NoShippingCost() {
        testProduct.setPrice(new BigDecimal("600000"));
        testCartItem.setQuantity(1);
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(testCart));

        CheckoutSummaryDTO summary = orderService.summarizeCart(1L, null);

        assertEquals(0, BigDecimal.ZERO.compareTo(summary.getShippingCost()));
        assertEquals(0, new BigDecimal("600000").compareTo(summary.getTotal()));
    }

    @Test
    void getDashboardStats_RevenueCountsOnlyCompletedPayments() {
        when(orderRepository.count()).thenReturn(10L);
        when(orderRepository.sumTotalByPaymentStatus(Order.PaymentStatus.COMPLETED))
                .thenReturn(new BigDecimal("750000"));
        when(productRepository.count()).thenReturn(5L);
        when(userRepository.count()).thenReturn(3L);
        when(orderRepository.findByStatus(eq(Order.OrderStatus.PENDING), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        DashboardStatsDTO stats = orderService.getDashboardStats();

        assertEquals(0, new BigDecimal("750000").compareTo(stats.getTotalRevenue()));
        assertEquals(10L, stats.getTotalOrders());
        verify(orderRepository).sumTotalByPaymentStatus(Order.PaymentStatus.COMPLETED);
    }

    @Test
    void getDashboardStats_ComputesFullMetrics() {
        Product top = new Product();
        top.setId(7L);
        top.setName("Anillo de oro");
        top.setPrice(new BigDecimal("50000"));
        top.setSoldCount(9);

        when(orderRepository.count()).thenReturn(10L);
        when(orderRepository.sumTotalByPaymentStatus(Order.PaymentStatus.COMPLETED)).thenReturn(new BigDecimal("750000"));
        when(orderRepository.sumTotalByPaymentStatusSince(eq(Order.PaymentStatus.COMPLETED), any()))
                .thenReturn(new BigDecimal("100000"));
        when(productRepository.count()).thenReturn(5L);
        when(userRepository.count()).thenReturn(4L);
        when(userRepository.countByRole(UserRole.USER)).thenReturn(3L);
        when(orderRepository.findByStatus(eq(Order.OrderStatus.PENDING), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(productRepository.countByActiveTrueAndStockGreaterThanAndStockLessThanEqual(eq(0), anyInt())).thenReturn(2L);
        when(productRepository.countByActiveTrueAndStockLessThanEqual(0)).thenReturn(1L);
        when(productRepository.findTop10ByActiveTrueOrderBySoldCountDesc()).thenReturn(List.of(top));
        when(orderRepository.countGroupByStatus())
                .thenReturn(Collections.singletonList(new Object[]{Order.OrderStatus.PAID, 2L}));

        DashboardStatsDTO stats = orderService.getDashboardStats();

        assertEquals(3L, stats.getTotalCustomers());
        assertEquals(2L, stats.getLowStockProducts());
        assertEquals(1L, stats.getOutOfStockProducts());
        assertEquals(2L, stats.getOrdersByStatus().get("PAID"));
        assertEquals(0L, stats.getOrdersByStatus().get("PENDING"));
        assertEquals(1, stats.getTopSellingProducts().size());
        assertEquals(9L, stats.getTopSellingProducts().get(0).getSoldCount());
        assertNotNull(stats.getTodayRevenue());
        assertNotNull(stats.getMonthRevenue());
    }
}

