package com.Ecommerce.Loyalty_Service.Controllers;

import com.Ecommerce.Loyalty_Service.Entities.Coupon;
import com.Ecommerce.Loyalty_Service.Mappers.CouponMapper;
import com.Ecommerce.Loyalty_Service.Payload.Request.Coupon.CouponApplyRequestDto;
import com.Ecommerce.Loyalty_Service.Payload.Request.Coupon.CouponGenerateRequestDto;
import com.Ecommerce.Loyalty_Service.Payload.Request.Coupon.CouponPointsPurchaseRequestDto;
import com.Ecommerce.Loyalty_Service.Payload.Request.Coupon.CouponValidationRequestDto;
import com.Ecommerce.Loyalty_Service.Payload.Response.Coupon.CouponApplyResponseDto;
import com.Ecommerce.Loyalty_Service.Payload.Response.Coupon.CouponPackageResponseDto;
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
@Tag(name = "Coupon Management", description = "Enhanced coupon generation, validation, and redemption operations")
public class CouponController {

    private final CouponService couponService;
    private final CouponMapper couponMapper;

    @GetMapping
    @Operation(
            summary = "Get all coupons",
            description = "Retrieve all available coupons in the system"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved all coupons"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<CouponResponseDto>> getAllCoupons() {
        log.info("Retrieving all coupons");
        List<Coupon> coupons = couponService.getAllCoupons();
        List<CouponResponseDto> couponDtos = coupons.stream()
                .map(couponMapper::toResponseDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(couponDtos);
    }

    @GetMapping("valid/{couponCode}")
    @Operation(
            summary = "Check coupon validity",
            description = "Check if a coupon is valid for use"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Coupon is valid"),
            @ApiResponse(responseCode = "404", description = "Coupon not found or invalid"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<CouponResponseDto> checkCouponValidity(
            @Parameter(description = "Coupon code to validate", example = "COUPON123")
            @PathVariable String couponCode) {
        log.info("Checking validity for coupon: {}", couponCode);

        Coupon coupon = couponService.getCouponByCode(couponCode);
        if (coupon == null) {
            log.warn("Coupon not found or invalid: {}", couponCode);
            return ResponseEntity.notFound().build();
        }

        CouponResponseDto responseDto = couponMapper.toResponseDto(coupon);
        return ResponseEntity.ok(responseDto);
    }


    @Operation(
            summary = "Generate a coupon using points",
            description = "Create a new discount coupon by spending loyalty points"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Coupon generated successfully"),
            @ApiResponse(responseCode = "400", description = "Insufficient points or invalid request data"),
            @ApiResponse(responseCode = "404", description = "User not found in loyalty system"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/purchase")
    public ResponseEntity<CouponResponseDto> purchaseCouponWithPoints(
            @Valid @RequestBody CouponPointsPurchaseRequestDto request) {
        log.info("Purchasing coupon with points for user: {} costing {} points",
                request.getUserId(), request.getPointsCost());

        Coupon coupon = couponService.generateCouponForPoints(
                request.getUserId(),
                request.getDiscountType(),
                request.getDiscountValue(),
                request.getPointsCost(),
                request.getMinPurchaseAmount(),
                request.getMaxDiscountAmount(),
                request.getExpirationDate(),
                request.getUsageLimit()
        );

        CouponResponseDto responseDto = couponMapper.toResponseDto(coupon);
        return ResponseEntity.ok(responseDto);
    }

    @Operation(
            summary = "Generate coupon from predefined package",
            description = "Purchase a coupon from predefined packages with set point costs"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Coupon package purchased successfully"),
            @ApiResponse(responseCode = "400", description = "Insufficient points or invalid package"),
            @ApiResponse(responseCode = "404", description = "User not found in loyalty system"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/purchase-package")
    public ResponseEntity<CouponResponseDto> purchaseCouponPackage(
            @Parameter(description = "User ID", example = "123e4567-e89b-12d3-a456-426614174001")
            @RequestParam UUID userId,
            @Parameter(description = "Package type", example = "STANDARD_10_PERCENT")
            @RequestParam CouponService.CouponPackage packageType) {

        log.info("Purchasing coupon package {} for user: {}", packageType, userId);

        Coupon coupon = couponService.generateCouponFromPackage(userId, packageType);
        CouponResponseDto responseDto = couponMapper.toResponseDto(coupon);

        return ResponseEntity.ok(responseDto);
    }

    @Operation(
            summary = "Get available coupon packages",
            description = "Retrieve coupon packages that the user can afford with their current points"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved available packages"),
            @ApiResponse(responseCode = "404", description = "User not found in loyalty system"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/packages/{userId}")
    public ResponseEntity<List<CouponPackageResponseDto>> getAvailableCouponPackages(
            @Parameter(description = "User ID", example = "123e4567-e89b-12d3-a456-426614174001")
            @PathVariable UUID userId) {

        log.info("Retrieving available coupon packages for user: {}", userId);

        List<CouponService.CouponPackage> availablePackages =
                couponService.getAvailableCouponPackages(userId);

        List<CouponPackageResponseDto> packageDtos = availablePackages.stream()
                .map(this::mapPackageToDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(packageDtos);
    }

    @Operation(
            summary = "Get all coupon packages",
            description = "Retrieve all available coupon packages with their point costs"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved all packages"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/packages")
    public ResponseEntity<List<CouponPackageResponseDto>> getAllCouponPackages() {
        log.info("Retrieving all coupon packages");

        List<CouponPackageResponseDto> packageDtos = java.util.Arrays.stream(CouponService.CouponPackage.values())
                .map(this::mapPackageToDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(packageDtos);
    }

    // Keep existing endpoints for backward compatibility

    @Operation(
            summary = "Generate a promotional coupon (FREE)",
            description = "Create a new discount coupon for promotional purposes (admin use)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Promotional coupon generated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/promotional")
    public ResponseEntity<CouponResponseDto> generatePromotionalCoupon(
            @Valid @RequestBody CouponGenerateRequestDto request) {
        log.info("Generating promotional coupon for user: {} with discount: {}",
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
            @PathVariable String userId) {
        log.info("Retrieving active coupons for user: {}", userId);
//        UUID parsedUserId = (userId);
//        String userId
        UUID parsedUserId = parseUUID(userId);

        List<Coupon> coupons = couponService.getUserActiveCoupons(parsedUserId);
        List<CouponResponseDto> couponDtos = coupons.stream()
                .map(couponMapper::toResponseDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(couponDtos);
    }
    private UUID parseUUID(String uuidString) {
        // Remove any existing hyphens
        String cleanUuid = uuidString.replaceAll("-", "");

        // Handle MongoDB ObjectId (24 characters) by padding to UUID format
        if (cleanUuid.length() == 24 && cleanUuid.matches("[0-9a-fA-F]+")) {
            // Pad with zeros to make it 32 characters
            cleanUuid = cleanUuid + "00000000";
        }

        // Check if it's exactly 32 hex characters
        if (cleanUuid.length() == 32 && cleanUuid.matches("[0-9a-fA-F]+")) {
            // Insert hyphens at correct positions: 8-4-4-4-12
            String formattedUuid = cleanUuid.substring(0, 8) + "-" +
                    cleanUuid.substring(8, 12) + "-" +
                    cleanUuid.substring(12, 16) + "-" +
                    cleanUuid.substring(16, 20) + "-" +
                    cleanUuid.substring(20, 32);
            return UUID.fromString(formattedUuid);
        }

        // Try parsing as-is (in case it's already properly formatted)
        return UUID.fromString(uuidString);
    }


    // Helper method to map package to DTO
    private CouponPackageResponseDto mapPackageToDto(CouponService.CouponPackage packageType) {
        return CouponPackageResponseDto.builder()
                .packageName(packageType.name())
                .pointsCost(packageType.getPointsCost())
                .discountType(packageType.getDiscountType())
                .discountValue(packageType.getDiscountValue())
                .minPurchaseAmount(packageType.getMinPurchaseAmount())
                .maxDiscountAmount(packageType.getMaxDiscountAmount())
                .description(generatePackageDescription(packageType))
                .build();
    }

    private String generatePackageDescription(CouponService.CouponPackage packageType) {
        if (packageType.getDiscountType().name().equals("PERCENTAGE")) {
            return String.format("%.0f%% discount (min purchase: $%.0f, max discount: $%.0f)",
                    packageType.getDiscountValue(),
                    packageType.getMinPurchaseAmount(),
                    packageType.getMaxDiscountAmount());
        } else {
            return String.format("$%.0f off (min purchase: $%.0f)",
                    packageType.getDiscountValue(),
                    packageType.getMinPurchaseAmount());
        }
    }
}

