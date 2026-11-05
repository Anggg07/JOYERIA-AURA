package com.joyeria.dto;

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
public class ProductDTO {
    private Long id;
    private String name;
    private String slug;
    private String description;
    private BigDecimal price;
    private BigDecimal comparePrice;
    private String sku;
    private Integer stock;
    private String weight;
    private String dimensions;
    private String size;
    private String color;
    private String careInstructions;
    private String features;
    private String deliveryTime;
    private Boolean featured;
    private Boolean isNew;
    private Boolean bestSeller;
    private Boolean active;
    private Integer soldCount;
    private Long categoryId;
    private String categoryName;
    private List<Long> materialIds;
    private List<String> materialNames;
    private List<ProductImageDTO> images;
    private BigDecimal discountPercent;
    private Double averageRating;
    private Integer reviewCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
