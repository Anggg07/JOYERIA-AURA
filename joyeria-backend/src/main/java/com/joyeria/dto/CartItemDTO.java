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
public class CartItemDTO {
    private Long id;
    private Long productId;
    private String productName;
    private String productSlug;
    private String productImage;
    private BigDecimal productPrice;
    private Integer productStock;
    private Integer quantity;
    private BigDecimal subtotal;
}
