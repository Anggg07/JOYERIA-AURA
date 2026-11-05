package com.joyeria.controller;

import com.joyeria.dto.*;
import com.joyeria.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<Object>>> getFavorites(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success((PagedResponse<Object>) (PagedResponse<?>) favoriteService.getUserFavorites(authentication.getName(), page, size)));
    }

    @PostMapping("/toggle/{productId}")
    public ResponseEntity<ApiResponse<Boolean>> toggleFavorite(
            Authentication authentication, @PathVariable Long productId) {
        boolean isFavorite = favoriteService.toggleFavorite(authentication.getName(), productId);
        return ResponseEntity.ok(ApiResponse.success(isFavorite ? "Agregado a favoritos" : "Eliminado de favoritos", isFavorite));
    }

    @GetMapping("/check/{productId}")
    public ResponseEntity<ApiResponse<Boolean>> checkFavorite(
            Authentication authentication, @PathVariable Long productId) {
        return ResponseEntity.ok(ApiResponse.success(favoriteService.isFavorite(authentication.getName(), productId)));
    }
}
