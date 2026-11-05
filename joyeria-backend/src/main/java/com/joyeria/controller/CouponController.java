package com.joyeria.controller;

import com.joyeria.dto.*;
import com.joyeria.service.CouponService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @PostMapping("/validate")
    public ResponseEntity<ApiResponse<CouponDTO>> validate(@RequestParam String code) {
        return ResponseEntity.ok(ApiResponse.success("Cupón válido", couponService.validateCoupon(code)));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<CouponDTO>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(couponService.getAllCoupons()));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CouponDTO>> create(@Valid @RequestBody CouponCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Cupón creado", couponService.createCoupon(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CouponDTO>> update(
            @PathVariable Long id, @Valid @RequestBody CouponCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Cupón actualizado", couponService.updateCoupon(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        couponService.deleteCoupon(id);
        return ResponseEntity.ok(ApiResponse.success("Cupón eliminado", null));
    }
}