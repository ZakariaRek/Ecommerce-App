package com.Ecommerce.Loyalty_Service.Mappers;


import com.Ecommerce.Loyalty_Service.Entities.Coupon;
import com.Ecommerce.Loyalty_Service.Payload.Request.Coupon.CouponGenerateRequestDto;
import com.Ecommerce.Loyalty_Service.Payload.Response.Coupon.CouponResponseDto;
import org.springframework.stereotype.Component;

@Component
public class CouponMapper {

    public CouponResponseDto toResponseDto(Coupon coupon) {
        return CouponResponseDto.builder()
                .id(coupon.getId())
                .code(coupon.getCode())
                .discountType(coupon.getDiscountType())
                .discountValue(coupon.getDiscountValue())
                .minPurchaseAmount(coupon.getMinPurchaseAmount())
                .maxDiscountAmount(coupon.getMaxDiscountAmount())
                .expirationDate(coupon.getExpirationDate())
                .userId(coupon.getUserId())
                .isUsed(coupon.isUsed())
                .usageLimit(coupon.getUsageLimit())
                .stackable(coupon.getStackable())
                .priorityLevel(coupon.getPriorityLevel())
                .createdAt(coupon.getCreatedAt())
                .build();
    }

    public Coupon toEntity(CouponGenerateRequestDto dto) {
        Coupon coupon = new Coupon();
        coupon.setUserId(dto.getUserId());
        coupon.setDiscountType(dto.getDiscountType());
        coupon.setDiscountValue(dto.getDiscountValue());
        coupon.setMinPurchaseAmount(dto.getMinPurchaseAmount());
        coupon.setMaxDiscountAmount(dto.getMaxDiscountAmount());
        coupon.setExpirationDate(dto.getExpirationDate());
        coupon.setUsageLimit(dto.getUsageLimit());
        coupon.setUsed(false);
        coupon.setStackable(true);
        coupon.setPriorityLevel(1);
        return coupon;
    }
}