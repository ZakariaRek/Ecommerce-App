package com.Ecommerce.Loyalty_Service.Controllers;


import java.util.List;
import java.util.UUID;

import com.Ecommerce.Loyalty_Service.Entities.PointTransaction;
import com.Ecommerce.Loyalty_Service.Entities.TransactionType;
import com.Ecommerce.Loyalty_Service.Services.PointTransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {
    @Autowired
    private PointTransactionService transactionService;

    @PostMapping
    public ResponseEntity<PointTransaction> createTransaction(
            @RequestParam UUID userId,
            @RequestParam TransactionType type,
            @RequestParam int points,
            @RequestParam String source) {
        return ResponseEntity.ok(transactionService.recordTransaction(userId, type, points, source));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<List<PointTransaction>> getTransactionHistory(@PathVariable UUID userId) {
        return ResponseEntity.ok(transactionService.getTransactionHistory(userId));
    }
}
