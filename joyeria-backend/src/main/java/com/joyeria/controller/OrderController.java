package com.joyeria.controller;

import com.joyeria.dto.*;
import com.joyeria.model.User;
import com.joyeria.service.AuthService;
import com.joyeria.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final AuthService authService;

    @PostMapping
    public ResponseEntity<ApiResponse<OrderDTO>> createOrder(
            Authentication authentication, @Valid @RequestBody OrderCreateRequest request) {
        User user = authService.getCurrentUser(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Pedido creado", orderService.createOrder(user.getId(), request)));
    }

    @PostMapping("/guest")
    public ResponseEntity<ApiResponse<OrderDTO>> createGuestOrder(
            @Valid @RequestBody GuestOrderRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Pedido creado", orderService.createGuestOrder(request)));
    }

    @GetMapping("/guest/{orderNumber}")
    public ResponseEntity<ApiResponse<OrderDTO>> getGuestOrder(
            @PathVariable String orderNumber, @RequestParam String token) {
        return ResponseEntity.ok(ApiResponse.success(orderService.getGuestOrder(orderNumber, token)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<OrderDTO>>> getUserOrders(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        User user = authService.getCurrentUser(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(orderService.getUserOrders(user.getId(), page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderDTO>> getOrder(
            Authentication authentication, @PathVariable Long id) {
        User user = authService.getCurrentUser(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(
                orderService.getOrderByIdForUser(id, user.getId(), isAdmin(authentication))));
    }

    @GetMapping("/number/{orderNumber}")
    public ResponseEntity<ApiResponse<OrderDTO>> getByOrderNumber(
            Authentication authentication, @PathVariable String orderNumber) {
        User user = authService.getCurrentUser(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(
                orderService.getOrderByIdentifierForUser(orderNumber, user.getId(), isAdmin(authentication))));
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }
}