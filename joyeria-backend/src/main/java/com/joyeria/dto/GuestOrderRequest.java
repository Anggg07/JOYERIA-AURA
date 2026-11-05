package com.joyeria.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class GuestOrderRequest {
    @NotBlank @Email
    private String email;

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

    @NotEmpty
    private List<GuestOrderItemRequest> items;
}