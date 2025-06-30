package com.Ecommerce.Product_Service.Payload.Discont;


import com.Ecommerce.Product_Service.Entities.Discount;
import org.springframework.stereotype.Component;

@Component
public class DiscountMapper {

    public Discount toEntity(DiscountRequestDTO dto) {
        if (dto == null) return null;

        Discount discount = new Discount();
        discount.setDiscountType(dto.getDiscountType());
        discount.setDiscountValue(dto.getDiscountValue());
        discount.setStartDate(dto.getStartDate());
        discount.setEndDate(dto.getEndDate());
        discount.setMinPurchaseAmount(dto.getMinPurchaseAmount());
        discount.setMaxDiscountAmount(dto.getMaxDiscountAmount());

        return discount;
    }

    public DiscountResponseDTO toResponseDTO(Discount entity) {
        if (entity == null) return null;

        DiscountResponseDTO dto = new DiscountResponseDTO();
        dto.setId(entity.getId());
        dto.setProductId(entity.getProduct() != null ? entity.getProduct().getId() : null);
        dto.setProductName(entity.getProduct() != null ? entity.getProduct().getName() : null);
        dto.setDiscountType(entity.getDiscountType());
        dto.setDiscountValue(entity.getDiscountValue());
        dto.setStartDate(entity.getStartDate());
        dto.setEndDate(entity.getEndDate());
        dto.setMinPurchaseAmount(entity.getMinPurchaseAmount());
        dto.setMaxDiscountAmount(entity.getMaxDiscountAmount());
        dto.setActive(entity.isActive());

        return dto;
    }

    public DiscountSummaryDTO toSummaryDTO(Discount entity) {
        if (entity == null) return null;

        DiscountSummaryDTO dto = new DiscountSummaryDTO();
        dto.setId(entity.getId());
        dto.setProductId(entity.getProduct() != null ? entity.getProduct().getId() : null);
        dto.setDiscountType(entity.getDiscountType());
        dto.setDiscountValue(entity.getDiscountValue());
        dto.setEndDate(entity.getEndDate());
        dto.setActive(entity.isActive());

        return dto;
    }

    public void updateEntityFromDTO(Discount entity, DiscountRequestDTO dto) {
        if (entity == null || dto == null) return;

        entity.setDiscountType(dto.getDiscountType());
        entity.setDiscountValue(dto.getDiscountValue());
        entity.setStartDate(dto.getStartDate());
        entity.setEndDate(dto.getEndDate());
        entity.setMinPurchaseAmount(dto.getMinPurchaseAmount());
        entity.setMaxDiscountAmount(dto.getMaxDiscountAmount());
    }
}