package com.joyeria.dto;

import com.joyeria.model.Order.OrderStatus;
import com.joyeria.model.Order.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDTO {
    private Long id;
    private String orderNumber;
    private OrderStatus status;
    private PaymentStatus paymentStatus;
    private BigDecimal subtotal;
    private BigDecimal discount;
    private BigDecimal shippingCost;
    private BigDecimal total;
    private String shippingAddress;
    private String shippingCity;
    private String shippingDepartment;
    private String recipientName;
    private String phone;
    private String paymentMethod;
    private String couponCode;
    private String trackingNumber;
    private String customerName;
    private String customerEmail;
    private String guestEmail;
    private String guestToken;
    private List<OrderItemDTO> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
