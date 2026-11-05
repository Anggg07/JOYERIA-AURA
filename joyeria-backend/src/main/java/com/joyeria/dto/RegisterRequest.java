package com.joyeria.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    @NotBlank @Size(max = 60)
    private String firstName;
    @NotBlank @Size(max = 60)
    private String lastName;
    @NotBlank @Email
    private String email;
    @NotBlank @Size(max = 20)
    private String phone;
    @NotBlank @Size(min = 8, max = 128)
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
            message = "Debe contener al menos una letra y un número")
    private String password;
    @NotBlank
    private String confirmPassword;
}
