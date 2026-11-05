package com.joyeria.dto;

import com.joyeria.model.Address.AddressType;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddressCreateRequest {
    private String firstName;
    private String lastName;
    private String phone;
    @NotBlank
    private String addressLine1;
    private String addressLine2;
    @NotBlank
    private String city;
    private String department;
    private String postalCode;
    private AddressType addressType = AddressType.SHIPPING;
    private Boolean isDefault = false;
}
