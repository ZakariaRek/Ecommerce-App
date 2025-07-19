package com.Ecommerce.Loyalty_Service.Controllers;


import com.Ecommerce.Loyalty_Service.Entities.PointTransaction;
import com.Ecommerce.Loyalty_Service.Mappers.TransactionMapper;
import com.Ecommerce.Loyalty_Service.Payload.Request.Transaction.TransactionCreateRequestDto;
import com.Ecommerce.Loyalty_Service.Payload.Response.Transaction.TransactionResponseDto;
import com.Ecommerce.Loyalty_Service.Services.PointTransactionService;
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

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Transaction Management", description = "Point transaction operations")
public class TransactionController {

    private final PointTransactionService transactionService;
    private final TransactionMapper transactionMapper;

    @Operation(
            summary = "Create a new point transaction",
            description = "Record a new point transaction (earn, redeem, adjust, or expire)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transaction created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping
    public ResponseEntity<TransactionResponseDto> createTransaction(
            @Valid @RequestBody TransactionCreateRequestDto request) {
        log.info("Creating transaction for user: {} with {} points",
                request.getUserId(), request.getPoints());

        PointTransaction transaction = transactionService.recordTransactionWithIdempotency(
                request.getUserId(),
                request.getType(),
                request.getPoints(),
                request.getSource(),
                request.getIdempotencyKey()
        );

        TransactionResponseDto responseDto = transactionMapper.toResponseDto(transaction);
        return ResponseEntity.ok(responseDto);
    }

    @Operation(
            summary = "Get transaction history",
            description = "Retrieve transaction history for a specific user"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved transaction history"),
            @ApiResponse(responseCode = "400", description = "Invalid user ID format"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{userId}")
    public ResponseEntity<List<TransactionResponseDto>> getTransactionHistory(
            @Parameter(description = "User ID", example = "123e4567-e89b-12d3-a456-426614174001")
            @PathVariable UUID userId) {
        log.info("Retrieving transaction history for user: {}", userId);
        List<PointTransaction> transactions = transactionService.getTransactionHistory(userId);
        List<TransactionResponseDto> transactionDtos = transactions.stream()
                .map(transactionMapper::toResponseDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(transactionDtos);
    }

    @Operation(
            summary = "Check transaction status",
            description = "Check if a transaction with the given idempotency key already exists"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transaction found"),
            @ApiResponse(responseCode = "404", description = "Transaction not found"),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{userId}/check")
    public ResponseEntity<TransactionResponseDto> checkTransactionStatus(
            @Parameter(description = "User ID", example = "123e4567-e89b-12d3-a456-426614174001")
            @PathVariable UUID userId,
            @Parameter(description = "Idempotency key", example = "order-123e4567-e89b-12d3-a456-426614174002")
            @RequestParam String idempotencyKey) {
        log.info("Checking transaction status for user: {} with key: {}", userId, idempotencyKey);

        if (transactionService.isTransactionProcessed(userId, idempotencyKey)) {
            PointTransaction existingTransaction = transactionService
                    .getTransactionByIdempotencyKey(userId, idempotencyKey)
                    .orElseThrow(() -> new RuntimeException("Transaction not found"));

            TransactionResponseDto responseDto = transactionMapper.toResponseDto(existingTransaction);
            return ResponseEntity.ok(responseDto);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
