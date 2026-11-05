package com.joyeria.controller;

import com.joyeria.dto.*;
import com.joyeria.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/product/{productId}")
    public ResponseEntity<ApiResponse<PagedResponse<ReviewDTO>>> getProductReviews(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(reviewService.getProductReviews(productId, page, size)));
    }

    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<ReviewDTO>>> getRecentReviews() {
        return ResponseEntity.ok(ApiResponse.success(reviewService.getRecentApproved(10)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ReviewDTO>> createReview(
            Authentication authentication, @Valid @RequestBody ReviewCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Reseña enviada. Será visible tras aprobación",
                reviewService.createReview(authentication.getName(), request)));
    }

    @GetMapping("/check/{productId}")
    public ResponseEntity<ApiResponse<Boolean>> hasReviewed(
            Authentication authentication, @PathVariable Long productId) {
        if (authentication == null) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("No autenticado"));
        }
        return ResponseEntity.ok(ApiResponse.success(reviewService.hasUserReviewed(authentication.getName(), productId)));
    }
}
