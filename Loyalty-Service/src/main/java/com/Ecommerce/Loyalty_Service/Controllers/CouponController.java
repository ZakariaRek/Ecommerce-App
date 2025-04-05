package com.Ecommerce.Loyalty_Service.Controllers;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.Ecommerce.Loyalty_Service.Entities.Coupon;
import com.Ecommerce.Loyalty_Service.Entities.DiscountType;
import com.Ecommerce.Loyalty_Service.Services.CouponService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;



@RestController
@RequestMapping("/api/coupons")
public class CouponController {
    @Autowired
    private CouponService couponService;

    @PostMapping
    public ResponseEntity<Coupon> generateCoupon(
            @RequestParam UUID userId,
            @RequestParam DiscountType discountType,
            @RequestParam BigDecimal discountValue,
            @RequestParam BigDecimal minPurchaseAmount,
            @RequestParam(required = false) BigDecimal maxDiscountAmount,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime expirationDate,
            @RequestParam int usageLimit) {
        return ResponseEntity.ok(couponService.generateCoupon(
                userId, discountType, discountValue, minPurchaseAmount,
                maxDiscountAmount, expirationDate, usageLimit));
    }

    @GetMapping("/validate")
    public ResponseEntity<Boolean> validateCoupon(
            @RequestParam String couponCode,
            @RequestParam BigDecimal purchaseAmount) {
        return ResponseEntity.ok(couponService.validateCoupon(couponCode, purchaseAmount));
    }

    @PostMapping("/apply")
    public ResponseEntity<BigDecimal> applyCoupon(
            @RequestParam String couponCode,
            @RequestParam BigDecimal purchaseAmount) {
        return ResponseEntity.ok(couponService.applyCoupon(couponCode, purchaseAmount));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<List<Coupon>> getUserCoupons(@PathVariable UUID userId) {
        return ResponseEntity.ok(couponService.getUserActiveCoupons(userId));
    }
}