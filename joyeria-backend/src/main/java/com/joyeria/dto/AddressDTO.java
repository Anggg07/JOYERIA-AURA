package com.joyeria.dto;

import com.joyeria.model.Address.AddressType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressDTO {
    private Long id;
    private String firstName;
    private String lastName;
    private String phone;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String department;
    private String postalCode;
    private AddressType addressType;
    private Boolean isDefault;
}
