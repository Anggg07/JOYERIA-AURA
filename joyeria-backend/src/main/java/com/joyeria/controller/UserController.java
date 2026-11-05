package com.joyeria.controller;

import com.joyeria.dto.*;
import com.joyeria.service.AuthService;
import com.joyeria.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AuthService authService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDTO>> getProfile(Authentication authentication) {
        UserDTO user = userService.mapToDTO(authService.getCurrentUser(authentication.getName()));
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserDTO>> updateProfile(
            Authentication authentication, @Valid @RequestBody UserUpdateRequest request) {
        UserDTO updated = userService.updateProfile(authentication.getName(), request);
        return ResponseEntity.ok(ApiResponse.success("Perfil actualizado", updated));
    }

    @PutMapping("/me/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            Authentication authentication, @Valid @RequestBody PasswordChangeRequest request) {
        userService.changePassword(authentication.getName(), request);
        return ResponseEntity.ok(ApiResponse.success("Contraseña actualizada", null));
    }
}
