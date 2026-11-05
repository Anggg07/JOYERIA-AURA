package com.joyeria.controller;

import com.joyeria.dto.*;
import com.joyeria.model.User;
import com.joyeria.service.AuthService;
import com.joyeria.service.CartService;
import com.joyeria.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final AuthService authService;
    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<ApiResponse<CartDTO>> getCart(Authentication authentication) {
        User user = authService.getCurrentUser(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(cartService.getCart(user.getId())));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<CheckoutSummaryDTO>> getSummary(
            Authentication authentication, @RequestParam(required = false) String coupon) {
        User user = authService.getCurrentUser(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(orderService.summarizeCart(user.getId(), coupon)));
    }

    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartDTO>> addItem(
            Authentication authentication, @Valid @RequestBody CartItemRequest request) {
        User user = authService.getCurrentUser(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Producto agregado", cartService.addItem(user.getId(), request)));
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<CartDTO>> updateItem(
            Authentication authentication, @PathVariable Long itemId, @RequestParam Integer quantity) {
        User user = authService.getCurrentUser(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(cartService.updateItemQuantity(user.getId(), itemId, quantity)));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<CartDTO>> removeItem(
            Authentication authentication, @PathVariable Long itemId) {
        User user = authService.getCurrentUser(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Item eliminado", cartService.removeItem(user.getId(), itemId)));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> clearCart(Authentication authentication) {
        User user = authService.getCurrentUser(authentication.getName());
        cartService.clearCart(user.getId());
        return ResponseEntity.ok(ApiResponse.success("Carrito vaciado", null));
    }
}