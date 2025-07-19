package com.Ecommerce.Loyalty_Service.Mappers;


import com.Ecommerce.Loyalty_Service.Entities.PointTransaction;
import com.Ecommerce.Loyalty_Service.Payload.Request.Transaction.TransactionCreateRequestDto;
import com.Ecommerce.Loyalty_Service.Payload.Response.Transaction.TransactionResponseDto;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {

    public TransactionResponseDto toResponseDto(PointTransaction transaction) {
        return TransactionResponseDto.builder()
                .id(transaction.getId())
                .userId(transaction.getUserId())
                .type(transaction.getType())
                .points(transaction.getPoints())
                .transactionDate(transaction.getTransactionDate())
                .source(transaction.getSource())
                .balance(transaction.getBalance())
                .relatedOrderId(transaction.getRelatedOrderId())
                .relatedCouponId(transaction.getRelatedCouponId())
                .expirationDate(transaction.getExpirationDate())
                .orderAmount(transaction.getOrderAmount())
                .idempotencyKey(transaction.getIdempotencyKey())
                .build();
    }

    public PointTransaction toEntity(TransactionCreateRequestDto dto) {
        PointTransaction transaction = new PointTransaction();
        transaction.setUserId(dto.getUserId());
        transaction.setType(dto.getType());
        transaction.setPoints(dto.getPoints());
        transaction.setSource(dto.getSource());
        transaction.setRelatedOrderId(dto.getRelatedOrderId());
        transaction.setRelatedCouponId(dto.getRelatedCouponId());
        transaction.setOrderAmount(dto.getOrderAmount());
        transaction.setIdempotencyKey(dto.getIdempotencyKey());
        return transaction;
    }
}