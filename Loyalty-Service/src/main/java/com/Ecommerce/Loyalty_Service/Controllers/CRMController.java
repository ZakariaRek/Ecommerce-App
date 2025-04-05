package com.Ecommerce.Loyalty_Service.Controllers;

import com.Ecommerce.Loyalty_Service.Entities.CRM;
import com.Ecommerce.Loyalty_Service.Services.CRMService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/crm")
public class CRMController {
    @Autowired
    private CRMService crmService;

    @GetMapping("/{userId}")
    public ResponseEntity<CRM> getCRMByUserId(@PathVariable UUID userId) {
        return ResponseEntity.ok(crmService.getByUserId(userId));
    }

    @GetMapping("/{userId}/loyalty-score")
    public ResponseEntity<Double> getLoyaltyScore(@PathVariable UUID userId) {
        return ResponseEntity.ok(crmService.calculateLoyaltyScore(userId));
    }
}
