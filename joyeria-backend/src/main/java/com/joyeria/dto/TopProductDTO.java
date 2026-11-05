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
public class TopProductDTO {
    private Long id;
    private String name;
    private Long soldCount;
    private BigDecimal price;
    private String imageUrl;
}
