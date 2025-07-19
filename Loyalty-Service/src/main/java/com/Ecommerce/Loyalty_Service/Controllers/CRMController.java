package com.Ecommerce.Loyalty_Service.Controllers;


import com.Ecommerce.Loyalty_Service.Entities.CRM;
import com.Ecommerce.Loyalty_Service.Mappers.CrmMapper;
import com.Ecommerce.Loyalty_Service.Payload.Response.CRM.CrmResponseDto;
import com.Ecommerce.Loyalty_Service.Payload.Response.CRM.LoyaltyScoreResponseDto;
import com.Ecommerce.Loyalty_Service.Services.CRMService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/crm")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "CRM Management", description = "Customer Relationship Management operations")
public class CRMController {

    private final CRMService crmService;
    private final CrmMapper crmMapper;

    @Operation(
            summary = "Get all CRM users",
            description = "Retrieve all users registered in the loyalty CRM system"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved all users"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping
    public ResponseEntity<List<CrmResponseDto>> getAllUsers() {
        log.info("Retrieving all CRM users");
        List<CRM> users = crmService.getAllUsers();
        List<CrmResponseDto> userDtos = users.stream()
                .map(crmMapper::toResponseDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(userDtos);
    }

    @Operation(
            summary = "Get CRM user by ID",
            description = "Retrieve detailed CRM information for a specific user"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved user CRM data"),
            @ApiResponse(responseCode = "404", description = "User not found in CRM system"),
            @ApiResponse(responseCode = "400", description = "Invalid user ID format"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{userId}")
    public ResponseEntity<CrmResponseDto> getCRMByUserId(
            @Parameter(description = "User ID", example = "123e4567-e89b-12d3-a456-426614174001")
            @PathVariable UUID userId) {
        log.info("Retrieving CRM data for user: {}", userId);
        CRM crm = crmService.getByUserId(userId);
        CrmResponseDto responseDto = crmMapper.toResponseDto(crm);

        return ResponseEntity.ok(responseDto);
    }

    @Operation(
            summary = "Get user loyalty score",
            description = "Calculate and retrieve the loyalty score for a specific user"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully calculated loyalty score"),
            @ApiResponse(responseCode = "404", description = "User not found in CRM system"),
            @ApiResponse(responseCode = "400", description = "Invalid user ID format"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{userId}/loyalty-score")
    public ResponseEntity<LoyaltyScoreResponseDto> getLoyaltyScore(
            @Parameter(description = "User ID", example = "123e4567-e89b-12d3-a456-426614174001")
            @PathVariable UUID userId) {
        log.info("Calculating loyalty score for user: {}", userId);
        CRM crm = crmService.getByUserId(userId);
        double loyaltyScore = crmService.calculateLoyaltyScore(userId);
        LoyaltyScoreResponseDto responseDto = crmMapper.toLoyaltyScoreDto(crm, loyaltyScore);

        return ResponseEntity.ok(responseDto);
    }
}