package com.joyeria.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SocialLinkDTO {
    private Long id;
    private String name;
    private String url;
    private String icon;
    private Boolean active;
    private Integer sortOrder;
}
