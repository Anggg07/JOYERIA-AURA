package com.joyeria.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductCreateRequest {
    @NotBlank
    private String name;
    private String description;
    @NotNull @Positive
    private BigDecimal price;
    private BigDecimal comparePrice;
    private String sku;
    @NotNull
    private Integer stock;
    private String weight;
    private String dimensions;
    private String size;
    private String color;
    private String careInstructions;
    private String features;
    private String deliveryTime;
    private Boolean featured = false;
    private Boolean isNew = false;
    private Boolean bestSeller = false;
    private Boolean active = true;
    @NotNull
    private Long categoryId;
    private List<Long> materialIds;
    private List<String> imageUrls;
}
