package com.Ecommerce.Loyalty_Service.Payload.Request.Transaction;

import com.Ecommerce.Loyalty_Service.Entities.MembershipTier;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TierProgressInfo {
    private MembershipTier currentTier;
    private int currentPoints;
    private int pointsInCurrentTier;
    private int pointsNeededForNextTier;
    private MembershipTier nextTier;
    private double progressPercentage;
    private int tierStartPoints;
    private int nextTierPoints;
}