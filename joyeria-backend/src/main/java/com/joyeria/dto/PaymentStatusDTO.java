package com.joyeria.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentStatusDTO {
    private Long orderId;
    private String orderNumber;
    private String orderStatus;
    private String paymentStatus;
    private String provider;
    private String paymentMethod;
    private BigDecimal amount;
    private String currency;
}
