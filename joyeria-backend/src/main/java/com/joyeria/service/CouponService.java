package com.joyeria.service;

import com.joyeria.dto.*;
import com.joyeria.exception.ResourceNotFoundException;
import com.joyeria.model.Coupon;
import com.joyeria.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;

    public List<CouponDTO> getAllCoupons() {
        return couponRepository.findAll().stream().map(this::mapToDTO).toList();
    }

    public CouponDTO getCouponById(Long id) {
        return mapToDTO(couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cupón no encontrado")));
    }

    public CouponDTO validateCoupon(String code) {
        return mapToDTO(getValidCoupon(code));
    }

    public Coupon getValidCoupon(String code) {
        Coupon coupon = couponRepository.findByCodeAndActiveTrue(code.trim().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Cupón no válido o inactivo"));

        LocalDateTime now = LocalDateTime.now();
        if (coupon.getValidFrom() != null && now.isBefore(coupon.getValidFrom())) {
            throw new IllegalStateException("El cupón aún no está activo");
        }
        if (coupon.getValidUntil() != null && now.isAfter(coupon.getValidUntil())) {
            throw new IllegalStateException("El cupón ha expirado");
        }
        if (coupon.getMaxUses() != null && coupon.getUsesCount() >= coupon.getMaxUses()) {
            throw new IllegalStateException("El cupón ha alcanzado su límite de uso");
        }

        return coupon;
    }

    public CouponDTO createCoupon(CouponCreateRequest request) {
        if (couponRepository.existsByCode(request.getCode().toUpperCase())) {
            throw new IllegalArgumentException("El código del cupón ya existe");
        }
        Coupon coupon = new Coupon();
        coupon.setCode(request.getCode().toUpperCase());
        coupon.setDescription(request.getDescription());
        coupon.setDiscountType(request.getDiscountType());
        coupon.setDiscountValue(request.getDiscountValue());
        coupon.setMinAmount(request.getMinAmount());
        coupon.setMaxUses(request.getMaxUses());
        coupon.setValidFrom(request.getValidFrom());
        coupon.setValidUntil(request.getValidUntil());
        coupon.setActive(true);
        return mapToDTO(couponRepository.save(coupon));
    }

    public CouponDTO updateCoupon(Long id, CouponCreateRequest request) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cupón no encontrado"));
        if (request.getDescription() != null) coupon.setDescription(request.getDescription());
        if (request.getDiscountType() != null) coupon.setDiscountType(request.getDiscountType());
        if (request.getDiscountValue() != null) coupon.setDiscountValue(request.getDiscountValue());
        if (request.getMinAmount() != null) coupon.setMinAmount(request.getMinAmount());
        if (request.getMaxUses() != null) coupon.setMaxUses(request.getMaxUses());
        if (request.getValidFrom() != null) coupon.setValidFrom(request.getValidFrom());
        if (request.getValidUntil() != null) coupon.setValidUntil(request.getValidUntil());
        if (request.getActive() != null) coupon.setActive(request.getActive());
        return mapToDTO(couponRepository.save(coupon));
    }

    public void deleteCoupon(Long id) {
        couponRepository.deleteById(id);
    }

    private CouponDTO mapToDTO(Coupon coupon) {
        return CouponDTO.builder()
                .id(coupon.getId())
                .code(coupon.getCode())
                .description(coupon.getDescription())
                .discountType(coupon.getDiscountType().name())
                .discountValue(coupon.getDiscountValue())
                .minAmount(coupon.getMinAmount())
                .maxUses(coupon.getMaxUses())
                .usesCount(coupon.getUsesCount())
                .validFrom(coupon.getValidFrom())
                .validUntil(coupon.getValidUntil())
                .active(coupon.getActive())
                .build();
    }
}
