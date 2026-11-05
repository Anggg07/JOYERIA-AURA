package com.joyeria.dto;

import com.joyeria.model.Coupon.DiscountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CouponCreateRequest {
    @NotBlank
    private String code;
    private String description;
    @NotNull
    private DiscountType discountType;
    @NotNull
    private BigDecimal discountValue;
    private BigDecimal minAmount;
    private Integer maxUses;
    private Boolean active;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
}
