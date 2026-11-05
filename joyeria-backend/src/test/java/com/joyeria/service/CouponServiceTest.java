package com.joyeria.service;

import com.joyeria.dto.CouponDTO;
import com.joyeria.exception.ResourceNotFoundException;
import com.joyeria.model.Coupon;
import com.joyeria.repository.CouponRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @Mock
    private CouponRepository couponRepository;

    @InjectMocks
    private CouponService couponService;

    private Coupon activeCoupon;
    private Coupon expiredCoupon;
    private Coupon maxUsesCoupon;

    @BeforeEach
    void setUp() {
        activeCoupon = new Coupon();
        activeCoupon.setId(1L);
        activeCoupon.setCode("BIENVENIDA10");
        activeCoupon.setDiscountType(Coupon.DiscountType.PERCENTAGE);
        activeCoupon.setDiscountValue(new BigDecimal("10"));
        activeCoupon.setActive(true);
        activeCoupon.setUsesCount(0);
        activeCoupon.setMaxUses(100);

        expiredCoupon = new Coupon();
        expiredCoupon.setId(2L);
        expiredCoupon.setCode("EXPIRADO");
        expiredCoupon.setDiscountType(Coupon.DiscountType.PERCENTAGE);
        expiredCoupon.setDiscountValue(new BigDecimal("10"));
        expiredCoupon.setActive(true);
        expiredCoupon.setValidUntil(LocalDateTime.now().minusDays(1));

        maxUsesCoupon = new Coupon();
        maxUsesCoupon.setId(3L);
        maxUsesCoupon.setCode("MAXADO");
        maxUsesCoupon.setDiscountType(Coupon.DiscountType.FIXED);
        maxUsesCoupon.setDiscountValue(new BigDecimal("5000"));
        maxUsesCoupon.setActive(true);
        maxUsesCoupon.setUsesCount(100);
        maxUsesCoupon.setMaxUses(100);
    }

    @Test
    void validateCoupon_WithActiveCoupon_ReturnsCoupon() {
        when(couponRepository.findByCodeAndActiveTrue("BIENVENIDA10"))
                .thenReturn(Optional.of(activeCoupon));

        CouponDTO result = couponService.validateCoupon("BIENVENIDA10");

        assertNotNull(result);
        assertEquals("BIENVENIDA10", result.getCode());
        assertEquals("PERCENTAGE", result.getDiscountType());
    }

    @Test
    void validateCoupon_WithExpiredCoupon_ThrowsException() {
        when(couponRepository.findByCodeAndActiveTrue("EXPIRADO"))
                .thenReturn(Optional.of(expiredCoupon));

        assertThrows(IllegalStateException.class, () -> couponService.validateCoupon("EXPIRADO"));
    }

    @Test
    void validateCoupon_WithMaxUsesReached_ThrowsException() {
        when(couponRepository.findByCodeAndActiveTrue("MAXADO"))
                .thenReturn(Optional.of(maxUsesCoupon));

        assertThrows(IllegalStateException.class, () -> couponService.validateCoupon("MAXADO"));
    }

    @Test
    void validateCoupon_WithNonExistentCoupon_ThrowsException() {
        when(couponRepository.findByCodeAndActiveTrue(anyString()))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> couponService.validateCoupon("NOEXISTE"));
    }
}
