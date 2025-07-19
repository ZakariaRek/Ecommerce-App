package com.Ecommerce.Loyalty_Service.Mappers;


import com.Ecommerce.Loyalty_Service.Entities.CRM;
import com.Ecommerce.Loyalty_Service.Payload.Response.CRM.CrmResponseDto;
import com.Ecommerce.Loyalty_Service.Payload.Response.CRM.LoyaltyScoreResponseDto;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

@Component
public class CrmMapper {

    public CrmResponseDto toResponseDto(CRM crm) {
        return CrmResponseDto.builder()
                .id(crm.getId())
                .userId(crm.getUserId())
                .totalPoints(crm.getTotalPoints())
                .membershipLevel(crm.getMembershipLevel())
                .joinDate(crm.getJoinDate())
                .lastActivity(crm.getLastActivity())
                .build();
    }

    public CrmResponseDto toResponseDto(CRM crm, Double loyaltyScore) {
        return CrmResponseDto.builder()
                .id(crm.getId())
                .userId(crm.getUserId())
                .totalPoints(crm.getTotalPoints())
                .membershipLevel(crm.getMembershipLevel())
                .joinDate(crm.getJoinDate())
                .lastActivity(crm.getLastActivity())
                .loyaltyScore(loyaltyScore)
                .build();
    }

    public LoyaltyScoreResponseDto toLoyaltyScoreDto(CRM crm, double loyaltyScore) {
        LocalDateTime now = LocalDateTime.now();
        long membershipDays = Duration.between(crm.getJoinDate(), now).toDays();
        long daysSinceLastActivity = Duration.between(crm.getLastActivity(), now).toDays();

        String explanation = String.format(
                "Based on %d points, %d days membership, last activity %d days ago",
                crm.getTotalPoints(), membershipDays, daysSinceLastActivity
        );

        return LoyaltyScoreResponseDto.builder()
                .userId(crm.getUserId())
                .loyaltyScore(loyaltyScore)
                .explanation(explanation)
                .build();
    }
}