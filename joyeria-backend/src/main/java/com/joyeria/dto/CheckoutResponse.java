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
public class CheckoutResponse {
    private Long orderId;
    private String orderNumber;
    private String provider;
    private String preferenceId;
    private String checkoutUrl;
    private BigDecimal amount;
    private String currency;
    private String paymentStatus;
}
