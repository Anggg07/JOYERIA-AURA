package com.joyeria.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreateRequest {
    @NotBlank
    private String shippingAddress;
    @NotBlank
    private String shippingCity;
    private String shippingDepartment;
    private String recipientName;
    private String phone;
    @NotBlank
    private String paymentMethod;
    private String couponCode;
    private String notes;
}
