package com.joyeria.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SocialLinkCreateRequest {
    @NotBlank
    private String name;
    @NotBlank
    private String url;
    private String icon;
    private Boolean active = true;
    private Integer sortOrder = 0;
}
