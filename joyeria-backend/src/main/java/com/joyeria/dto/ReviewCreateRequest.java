package com.joyeria.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewCreateRequest {
    @NotNull
    private Long productId;
    @NotNull @Min(1) @Max(5)
    private Integer rating;
    private String title;
    private String comment;
}
