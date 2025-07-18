package com.Ecommerce.Loyalty_Service.Payload.Kafka;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponUsageNotification {
    private List<String> couponCodes;
    private UUID orderId;
    private UUID userId;
    private LocalDateTime timestamp;
}