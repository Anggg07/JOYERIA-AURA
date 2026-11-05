package com.joyeria.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryCreateRequest {
    @NotBlank
    private String name;
    private String description;
    private Integer displayOrder;
    private String image;
    private Boolean active = true;
}
