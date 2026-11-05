package com.joyeria.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class VapidPublicKeyResponse {
    private String publicKey;
    private boolean enabled;
}