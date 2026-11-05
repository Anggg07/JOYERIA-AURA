package com.joyeria.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponDTO {
    private Long id;
    private String code;
    private String description;
    private String discountType;
    private BigDecimal discountValue;
    private BigDecimal minAmount;
    private Integer maxUses;
    private Integer usesCount;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private Boolean active;
}
