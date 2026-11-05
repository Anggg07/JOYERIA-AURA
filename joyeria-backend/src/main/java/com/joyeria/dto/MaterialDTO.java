package com.joyeria.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaterialDTO {
    private Long id;
    private String name;
    private String slug;
    private String description;
    private String image;
    private Boolean active;
    private LocalDateTime createdAt;
}
