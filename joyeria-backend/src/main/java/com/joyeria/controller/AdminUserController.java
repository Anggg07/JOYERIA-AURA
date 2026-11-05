package com.joyeria.controller;

import com.joyeria.dto.*;
import com.joyeria.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<UserDTO>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(userService.getAllUsers(page, size)));
    }

    @PutMapping("/{id}/role")
    public ResponseEntity<ApiResponse<UserDTO>> updateRole(
            @PathVariable Long id, @RequestParam String role) {
        return ResponseEntity.ok(ApiResponse.success("Rol actualizado", userService.updateUserRole(id, role)));
    }

    @PutMapping("/{id}/toggle")
    public ResponseEntity<ApiResponse<UserDTO>> toggleActive(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(userService.toggleUserActive(id)));
    }
}
