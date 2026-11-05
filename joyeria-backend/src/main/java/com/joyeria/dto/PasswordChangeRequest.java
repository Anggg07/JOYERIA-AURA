package com.joyeria.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasswordChangeRequest {
    @NotBlank
    private String currentPassword;
    @NotBlank @Size(min = 8, max = 128)
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
            message = "Debe contener al menos una letra y un número")
    private String newPassword;
    @NotBlank
    private String confirmPassword;
}
