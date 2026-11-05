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
public class CheckoutSummaryDTO {
    private BigDecimal subtotal;
    private BigDecimal discount;
    private BigDecimal shippingCost;
    private BigDecimal total;
    private String couponCode;
}
