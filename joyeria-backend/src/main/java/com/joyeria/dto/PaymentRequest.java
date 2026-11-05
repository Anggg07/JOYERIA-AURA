package com.joyeria.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {
    @NotNull
    private Long orderId;
    private String paymentMethod;
    private String guestToken;
}
