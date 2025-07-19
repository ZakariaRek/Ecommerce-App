package com.Ecommerce.Loyalty_Service.Controllers;

import com.Ecommerce.Loyalty_Service.Entities.Coupon;
import com.Ecommerce.Loyalty_Service.Mappers.CouponMapper;
import com.Ecommerce.Loyalty_Service.Payload.Request.Coupon.CouponApplyRequestDto;
import com.Ecommerce.Loyalty_Service.Payload.Request.Coupon.CouponGenerateRequestDto;
import com.Ecommerce.Loyalty_Service.Payload.Request.Coupon.CouponValidationRequestDto;
import com.Ecommerce.Loyalty_Service.Payload.Response.Coupon.CouponApplyResponseDto;
import com.Ecommerce.Loyalty_Service.Payload.Response.Coupon.CouponResponseDto;
import com.Ecommerce.Loyalty_Service.Payload.Response.Coupon.CouponValidationResponseDto;
import com.Ecommerce.Loyalty_Service.Services.CouponService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/coupons")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Coupon Management", description = "Coupon generation, validation, and redemption operations")
public class CouponController {

    private final CouponService couponService;
    private final CouponMapper couponMapper;

    @Operation(
            summary = "Generate a new coupon",
            description = "Create a new discount coupon for a user"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Coupon generated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping
    public ResponseEntity<CouponResponseDto> generateCoupon(
            @Valid @RequestBody CouponGenerateRequestDto request) {
        log.info("Generating coupon for user: {} with discount: {}",
                request.getUserId(), request.getDiscountValue());

        Coupon coupon = couponService.generateCoupon(
                request.getUserId(),
                request.getDiscountType(),
                request.getDiscountValue(),
                request.getMinPurchaseAmount(),
                request.getMaxDiscountAmount(),
                request.getExpirationDate(),
                request.getUsageLimit()
        );

        CouponResponseDto responseDto = couponMapper.toResponseDto(coupon);
        return ResponseEntity.ok(responseDto);
    }

    @Operation(
            summary = "Validate a coupon",
            description = "Check if a coupon is valid for a given purchase amount"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Coupon validation completed"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "404", description = "Coupon not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/validate")
    public ResponseEntity<CouponValidationResponseDto> validateCoupon(
            @Valid @RequestBody CouponValidationRequestDto request) {
        log.info("Validating coupon: {} for amount: {}",
                request.getCouponCode(), request.getPurchaseAmount());

        boolean isValid = couponService.validateCoupon(
                request.getCouponCode(),
                request.getPurchaseAmount()
        );

        // Calculate expected discount if valid
        BigDecimal expectedDiscount = BigDecimal.ZERO;
        String message = isValid ? "Coupon is valid and ready to use" : "Coupon is not valid";

        CouponValidationResponseDto responseDto = CouponValidationResponseDto.builder()
                .isValid(isValid)
                .couponCode(request.getCouponCode())
                .message(message)
                .expectedDiscount(expectedDiscount)
                .build();

        return ResponseEntity.ok(responseDto);
    }

    @Operation(
            summary = "Apply a coupon",
            description = "Apply a coupon to a purchase and calculate the final amount"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Coupon applied successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid coupon or request data"),
            @ApiResponse(responseCode = "404", description = "Coupon not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/apply")
    public ResponseEntity<CouponApplyResponseDto> applyCoupon(
            @Valid @RequestBody CouponApplyRequestDto request) {
        log.info("Applying coupon: {} to amount: {}",
                request.getCouponCode(), request.getPurchaseAmount());

        BigDecimal finalAmount = couponService.applyCoupon(
                request.getCouponCode(),
                request.getPurchaseAmount()
        );

        BigDecimal discountAmount = request.getPurchaseAmount().subtract(finalAmount);

        CouponApplyResponseDto responseDto = CouponApplyResponseDto.builder()
                .originalAmount(request.getPurchaseAmount())
                .discountAmount(discountAmount)
                .finalAmount(finalAmount)
                .couponCode(request.getCouponCode())
                .message("Coupon applied successfully")
                .build();

        return ResponseEntity.ok(responseDto);
    }

    @Operation(
            summary = "Get user's active coupons",
            description = "Retrieve all active (unused and not expired) coupons for a user"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved user coupons"),
            @ApiResponse(responseCode = "400", description = "Invalid user ID format"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{userId}")
    public ResponseEntity<List<CouponResponseDto>> getUserCoupons(
            @Parameter(description = "User ID", example = "123e4567-e89b-12d3-a456-426614174001")
            @PathVariable UUID userId) {
        log.info("Retrieving active coupons for user: {}", userId);
        List<Coupon> coupons = couponService.getUserActiveCoupons(userId);
        List<CouponResponseDto> couponDtos = coupons.stream()
                .map(couponMapper::toResponseDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(couponDtos);
    }
}
