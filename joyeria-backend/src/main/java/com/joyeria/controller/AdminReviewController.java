package com.joyeria.controller;

import com.joyeria.dto.*;
import com.joyeria.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/reviews")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminReviewController {

    private final ReviewService reviewService;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<ReviewDTO>>> getAllReviews(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(reviewService.getReviewsForAdmin(status, page, size)));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<ReviewDTO>> moderate(
            @PathVariable Long id, @RequestParam String status) {
        return ResponseEntity.ok(ApiResponse.success("Reseña moderada", reviewService.moderateReview(id, status)));
    }
}
