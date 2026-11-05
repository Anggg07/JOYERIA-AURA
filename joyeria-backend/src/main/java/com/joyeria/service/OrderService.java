package com.joyeria.service;

import com.joyeria.dto.*;
import com.joyeria.event.OrderCreatedEvent;
import com.joyeria.exception.ResourceNotFoundException;
import com.joyeria.model.*;
import com.joyeria.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private static final BigDecimal FREE_SHIPPING_THRESHOLD = BigDecimal.valueOf(500000);
    private static final BigDecimal SHIPPING_COST = BigDecimal.valueOf(15000);

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CouponRepository couponRepository;
    private final CouponService couponService;
    private final ApplicationEventPublisher eventPublisher;
    private final PushNotificationService pushNotificationService;

    @Value("${app.low-stock-threshold:5}")
    private int lowStockThreshold;

    @Transactional
    public OrderDTO createOrder(Long userId, OrderCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Carrito no encontrado"));

        if (cart.getItems().isEmpty()) {
            throw new IllegalStateException("El carrito está vacío");
        }

        String orderNumber = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Order order = new Order();
        order.setOrderNumber(orderNumber);
        order.setUser(user);
        order.setShippingAddress(request.getShippingAddress());
        order.setShippingCity(request.getShippingCity());
        order.setShippingDepartment(request.getShippingDepartment());
        order.setRecipientName(request.getRecipientName());
        order.setPhone(request.getPhone());
        order.setPaymentMethod(request.getPaymentMethod());
        order.setNotes(request.getNotes());

        BigDecimal subtotal = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (CartItem cartItem : cart.getItems()) {
            Product product = productRepository.findByIdForUpdate(cartItem.getProduct().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
            
            if (product.getStock() < cartItem.getQuantity()) {
                throw new IllegalStateException("Stock insuficiente para: " + product.getName());
            }

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setName(product.getName());
            orderItem.setPrice(product.getPrice());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setSubtotal(product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
            orderItem.setSku(product.getSku());
            orderItems.add(orderItem);

            product.setStock(product.getStock() - cartItem.getQuantity());
            product.setSoldCount(product.getSoldCount() + cartItem.getQuantity());
            productRepository.save(product);

            subtotal = subtotal.add(orderItem.getSubtotal());
        }

        BigDecimal discount = BigDecimal.ZERO;
        Coupon coupon = resolveCoupon(subtotal, request.getCouponCode());
        if (coupon != null) {
            discount = calculateDiscount(subtotal, coupon);
            coupon.setUsesCount(coupon.getUsesCount() + 1);
            couponRepository.save(coupon);
            order.setCouponCode(coupon.getCode());
        }

        BigDecimal shippingCost = calculateShipping(subtotal);

        order.setSubtotal(subtotal);
        order.setDiscount(discount);
        order.setShippingCost(shippingCost);
        order.setTotal(subtotal.subtract(discount).add(shippingCost));
        order.setPaymentStatus(Order.PaymentStatus.PENDING);
        order.setStatus(Order.OrderStatus.PENDING);

        order = orderRepository.save(order);
        orderItemRepository.saveAll(orderItems);

        cart.getItems().clear();
        cartRepository.save(cart);

        OrderDTO dto = mapToDTO(order);
        eventPublisher.publishEvent(new OrderCreatedEvent(
                user.getEmail(), user.getFirstName(), dto.getOrderNumber(), dto.getTotal()));
        pushNotificationService.sendToUser(user.getId(), "Pedido creado",
                "Tu pedido " + dto.getOrderNumber() + " fue creado correctamente.", "/cuenta/pedidos");
        return dto;
    }

    @Transactional
    public OrderDTO createGuestOrder(GuestOrderRequest request) {
        String orderNumber = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String guestToken = UUID.randomUUID().toString().replace("-", "");

        Order order = new Order();
        order.setOrderNumber(orderNumber);
        order.setUser(null);
        order.setGuestEmail(request.getEmail());
        order.setGuestToken(guestToken);
        order.setShippingAddress(request.getShippingAddress());
        order.setShippingCity(request.getShippingCity());
        order.setShippingDepartment(request.getShippingDepartment());
        order.setRecipientName(request.getRecipientName());
        order.setPhone(request.getPhone());
        order.setPaymentMethod(request.getPaymentMethod());
        order.setNotes(request.getNotes());

        BigDecimal subtotal = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (GuestOrderItemRequest itemReq : request.getItems()) {
            Product product = productRepository.findByIdForUpdate(itemReq.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
            if (product.getStock() < itemReq.getQuantity()) {
                throw new IllegalStateException("Stock insuficiente para: " + product.getName());
            }
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setName(product.getName());
            orderItem.setPrice(product.getPrice());
            orderItem.setQuantity(itemReq.getQuantity());
            orderItem.setSubtotal(product.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity())));
            orderItem.setSku(product.getSku());
            orderItems.add(orderItem);

            product.setStock(product.getStock() - itemReq.getQuantity());
            product.setSoldCount(product.getSoldCount() + itemReq.getQuantity());
            productRepository.save(product);

            subtotal = subtotal.add(orderItem.getSubtotal());
        }

        BigDecimal discount = BigDecimal.ZERO;
        Coupon coupon = resolveCoupon(subtotal, request.getCouponCode());
        if (coupon != null) {
            discount = calculateDiscount(subtotal, coupon);
            coupon.setUsesCount(coupon.getUsesCount() + 1);
            couponRepository.save(coupon);
            order.setCouponCode(coupon.getCode());
        }

        BigDecimal shippingCost = calculateShipping(subtotal);
        order.setSubtotal(subtotal);
        order.setDiscount(discount);
        order.setShippingCost(shippingCost);
        order.setTotal(subtotal.subtract(discount).add(shippingCost));
        order.setPaymentStatus(Order.PaymentStatus.PENDING);
        order.setStatus(Order.OrderStatus.PENDING);

        order = orderRepository.save(order);
        orderItemRepository.saveAll(orderItems);

        OrderDTO dto = mapToDTO(order);
        dto.setGuestEmail(request.getEmail());
        dto.setGuestToken(guestToken);
        return dto;
    }

    public CheckoutSummaryDTO summarizeCart(Long userId, String couponCode) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Carrito no encontrado"));

        BigDecimal subtotal = cart.getItems().stream()
                .map(item -> item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Coupon coupon = resolveCoupon(subtotal, couponCode);
        BigDecimal discount = coupon != null ? calculateDiscount(subtotal, coupon) : BigDecimal.ZERO;
        BigDecimal shippingCost = calculateShipping(subtotal);

        return CheckoutSummaryDTO.builder()
                .subtotal(subtotal)
                .discount(discount)
                .shippingCost(shippingCost)
                .total(subtotal.subtract(discount).add(shippingCost))
                .couponCode(coupon != null ? coupon.getCode() : null)
                .build();
    }

    private BigDecimal calculateShipping(BigDecimal subtotal) {
        return subtotal.compareTo(FREE_SHIPPING_THRESHOLD) >= 0 ? BigDecimal.ZERO : SHIPPING_COST;
    }

    private BigDecimal calculateDiscount(BigDecimal subtotal, Coupon coupon) {
        if (coupon.getDiscountType() == Coupon.DiscountType.PERCENTAGE) {
            return subtotal.multiply(coupon.getDiscountValue()).divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
        }
        return coupon.getDiscountValue().min(subtotal);
    }

    private Coupon resolveCoupon(BigDecimal subtotal, String couponCode) {
        if (couponCode == null || couponCode.isBlank()) {
            return null;
        }
        Coupon coupon = couponService.getValidCoupon(couponCode.trim().toUpperCase());
        if (coupon.getMinAmount() != null && subtotal.compareTo(coupon.getMinAmount()) < 0) {
            throw new IllegalStateException("El cupón requiere una compra mínima de " + coupon.getMinAmount());
        }
        return coupon;
    }

    public PagedResponse<OrderDTO> getUserOrders(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return mapToPagedResponse(orders);
    }

    public PagedResponse<OrderDTO> getAllOrders(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Order> orders = orderRepository.findAllByOrderByCreatedAtDesc(pageable);
        return mapToPagedResponse(orders);
    }

    public PagedResponse<OrderDTO> getOrdersByStatus(String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Order> orders = orderRepository.findByStatus(Order.OrderStatus.valueOf(status), pageable);
        return mapToPagedResponse(orders);
    }

    public OrderDTO getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado"));
        return mapToDTO(order);
    }

    public OrderDTO getOrderByIdentifier(String identifier) {
        Order order = orderRepository.findByOrderNumber(identifier).orElse(null);
        if (order == null) {
            try {
                order = orderRepository.findById(Long.parseLong(identifier)).orElse(null);
            } catch (NumberFormatException ignored) {}
        }
        if (order == null) throw new ResourceNotFoundException("Pedido no encontrado");
        return mapToDTO(order);
    }

    public OrderDTO getOrderByIdForUser(Long id, Long userId, boolean isAdmin) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado"));
        if (!isAdmin && !order.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Pedido no encontrado");
        }
        return mapToDTO(order);
    }

    public OrderDTO getOrderByIdentifierForUser(String identifier, Long userId, boolean isAdmin) {
        Order order = orderRepository.findByOrderNumber(identifier).orElse(null);
        if (order == null) {
            try {
                order = orderRepository.findById(Long.parseLong(identifier)).orElse(null);
            } catch (NumberFormatException ignored) {}
        }
        if (order == null) throw new ResourceNotFoundException("Pedido no encontrado");
        if (order.getUser() != null && !isAdmin && !order.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Pedido no encontrado");
        }
        if (order.getUser() == null) {
            throw new ResourceNotFoundException("Pedido no encontrado");
        }
        return mapToDTO(order);
    }

    public OrderDTO getGuestOrder(String orderNumber, String guestToken) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado"));
        if (order.getUser() != null || order.getGuestToken() == null || !order.getGuestToken().equals(guestToken)) {
            throw new ResourceNotFoundException("Pedido no encontrado");
        }
        return mapToDTO(order);
    }

    @Transactional
    public OrderDTO updateOrderStatus(Long id, String status) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado"));
        
        Order.OrderStatus newStatus = Order.OrderStatus.valueOf(status);
        Order.OrderStatus currentStatus = order.getStatus();
        
        // Validar transiciones de estado permitidas
        boolean validTransition = switch (currentStatus) {
            case PENDING -> newStatus == Order.OrderStatus.PAID || newStatus == Order.OrderStatus.CANCELLED;
            case PAID -> newStatus == Order.OrderStatus.PREPARING || newStatus == Order.OrderStatus.CANCELLED;
            case PREPARING -> newStatus == Order.OrderStatus.SHIPPED || newStatus == Order.OrderStatus.CANCELLED;
            case SHIPPED -> newStatus == Order.OrderStatus.DELIVERED;
            case DELIVERED, CANCELLED -> false;
        };
        
        if (!validTransition) {
            throw new IllegalStateException("Transición de estado no permitida: " + currentStatus + " -> " + newStatus);
        }
        
        order.setStatus(newStatus);
        if (newStatus == Order.OrderStatus.PAID) {
            order.setPaymentStatus(Order.PaymentStatus.COMPLETED);
        }
        if (newStatus == Order.OrderStatus.CANCELLED) {
            for (OrderItem item : order.getItems()) {
                Product product = item.getProduct();
                if (product != null) {
                    product.setStock(product.getStock() + item.getQuantity());
                    product.setSoldCount(Math.max(0, product.getSoldCount() - item.getQuantity()));
                    productRepository.save(product);
                }
            }
        }
        Order saved = orderRepository.save(order);
        if (saved.getUser() != null) {
            pushNotificationService.sendToUser(saved.getUser().getId(), "Pedido actualizado",
                    "Tu pedido " + saved.getOrderNumber() + " ahora está " + newStatus.name() + ".", "/cuenta/pedidos");
        }
        return mapToDTO(saved);
    }

    public DashboardStatsDTO getDashboardStats() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();

        Map<String, Long> ordersByStatus = new LinkedHashMap<>();
        for (Order.OrderStatus status : Order.OrderStatus.values()) {
            ordersByStatus.put(status.name(), 0L);
        }
        for (Object[] row : orderRepository.countGroupByStatus()) {
            ordersByStatus.put(((Order.OrderStatus) row[0]).name(), ((Number) row[1]).longValue());
        }

        List<TopProductDTO> topProducts = productRepository.findTop10ByActiveTrueOrderBySoldCountDesc()
                .stream()
                .limit(5)
                .map(p -> TopProductDTO.builder()
                        .id(p.getId())
                        .name(p.getName())
                        .soldCount(p.getSoldCount() != null ? p.getSoldCount().longValue() : 0L)
                        .price(p.getPrice())
                        .imageUrl(p.getImages().isEmpty() ? null : p.getImages().get(0).getUrl())
                        .build())
                .toList();

        return DashboardStatsDTO.builder()
                .totalOrders(orderRepository.count())
                .totalRevenue(orderRepository.sumTotalByPaymentStatus(Order.PaymentStatus.COMPLETED))
                .todayRevenue(orderRepository.sumTotalByPaymentStatusSince(Order.PaymentStatus.COMPLETED, startOfDay))
                .monthRevenue(orderRepository.sumTotalByPaymentStatusSince(Order.PaymentStatus.COMPLETED, startOfMonth))
                .totalProducts(productRepository.count())
                .totalUsers(userRepository.count())
                .totalCustomers(userRepository.countByRole(UserRole.USER))
                .pendingOrders(orderRepository.findByStatus(Order.OrderStatus.PENDING,
                        PageRequest.of(0, 1)).getTotalElements())
                .lowStockProducts(productRepository.countByActiveTrueAndStockGreaterThanAndStockLessThanEqual(0, lowStockThreshold))
                .outOfStockProducts(productRepository.countByActiveTrueAndStockLessThanEqual(0))
                .topSellingProducts(topProducts)
                .ordersByStatus(ordersByStatus)
                .build();
    }

    private OrderDTO mapToDTO(Order order) {
        List<OrderItemDTO> items = order.getItems().stream()
                .map(item -> OrderItemDTO.builder()
                        .id(item.getId())
                        .productId(item.getProduct() != null ? item.getProduct().getId() : null)
                        .name(item.getName())
                        .price(item.getPrice())
                        .quantity(item.getQuantity())
                        .subtotal(item.getSubtotal())
                        .sku(item.getSku())
                        .image(item.getProduct() != null && !item.getProduct().getImages().isEmpty() ?
                                item.getProduct().getImages().get(0).getUrl() : null)
                        .build()
                ).toList();

        String customerName = order.getUser() != null ? order.getUser().getFullName() : order.getRecipientName();
        String customerEmail = order.getUser() != null ? order.getUser().getEmail() : order.getGuestEmail();
        return OrderDTO.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus())
                .paymentStatus(order.getPaymentStatus())
                .subtotal(order.getSubtotal())
                .discount(order.getDiscount())
                .shippingCost(order.getShippingCost())
                .total(order.getTotal())
                .shippingAddress(order.getShippingAddress())
                .shippingCity(order.getShippingCity())
                .shippingDepartment(order.getShippingDepartment())
                .recipientName(order.getRecipientName())
                .phone(order.getPhone())
                .paymentMethod(order.getPaymentMethod())
                .couponCode(order.getCouponCode())
                .trackingNumber(order.getTrackingNumber())
                .customerName(customerName)
                .customerEmail(customerEmail)
                .guestEmail(order.getGuestEmail())
                .guestToken(order.getGuestToken())
                .items(items)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    private PagedResponse<OrderDTO> mapToPagedResponse(Page<Order> orders) {
        return PagedResponse.<OrderDTO>builder()
                .content(orders.getContent().stream().map(this::mapToDTO).toList())
                .page(orders.getNumber())
                .size(orders.getSize())
                .totalElements(orders.getTotalElements())
                .totalPages(orders.getTotalPages())
                .first(orders.isFirst())
                .last(orders.isLast())
                .build();
    }
}
