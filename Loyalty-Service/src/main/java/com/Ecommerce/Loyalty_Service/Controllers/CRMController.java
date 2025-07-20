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

import java.nio.charset.StandardCharsets;
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
    public ResponseEntity<?> getCRMByUserId(
            @Parameter(description = "User ID", example = "123e4567-e89b-12d3-a456-426614174001")
            @PathVariable String userId) {
        log.info("Retrieving CRM data for user: {}", userId);
        try {
//            UUID uuid = parseUUID(userId);
//
//            log.info(" ID format: {}", uuid);
            UUID userUuid = parseOrConvertToUUID(userId);
            log.info(" ID format1111: {}", userUuid);

//            UUID uuid1 = UUID.fromString(userId);
//            log.info(" ID format1111: {}", uuid1);



            CRM crm = crmService.getByUserId(userUuid);
            CrmResponseDto responseDto = crmMapper.toResponseDto(crm);
            return ResponseEntity.ok(responseDto);
        } catch (IllegalArgumentException e) {
            log.error("Invalid user ID format: {}", userId, e);
            return ResponseEntity.badRequest().body("Invalid user ID format");
        } catch (RuntimeException e) {
            log.error("User not found: {}", userId, e);
            return ResponseEntity.status(404).body("User not found in CRM system");
        }
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
    private UUID parseOrConvertToUUID(String userId) {
        try {
            // Try to parse as UUID
            return UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            // If not a UUID, convert from MongoDB ObjectId
            return convertObjectIdToUuid(userId);
        }
    }
    private UUID convertObjectIdToUuid(String objectId) {
        // Convert MongoDB ObjectId to UUID using a deterministic approach
        // This ensures the same ObjectId always maps to the same UUID
        try {
            // Use a hash-based approach to convert ObjectId to UUID
            byte[] bytes = objectId.getBytes(StandardCharsets.UTF_8);
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(bytes);

            // Create UUID from hash bytes
            long mostSigBits = 0;
            long leastSigBits = 0;

            for (int i = 0; i < 8; i++) {
                mostSigBits = (mostSigBits << 8) | (hash[i] & 0xff);
            }
            for (int i = 8; i < 16; i++) {
                leastSigBits = (leastSigBits << 8) | (hash[i] & 0xff);
            }

            return new UUID(mostSigBits, leastSigBits);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid user ID format: " + objectId, e);
        }
    }




    private UUID parseUUID(String uuidString) {
        // Remove any existing hyphens
        String cleanUuid = uuidString.replaceAll("-", "");

        // Handle MongoDB ObjectId (24 characters) by padding to UUID format
        if (cleanUuid.length() == 24 && cleanUuid.matches("[0-9a-fA-F]+")) {
            // Pad with zeros to make it 32 characters
            cleanUuid = cleanUuid + "00000000";
        }

        // Check if it's exactly 32 hex characters
        if (cleanUuid.length() == 32 && cleanUuid.matches("[0-9a-fA-F]+")) {
            // Insert hyphens at correct positions: 8-4-4-4-12
            String formattedUuid = cleanUuid.substring(0, 8) + "-" +
                    cleanUuid.substring(8, 12) + "-" +
                    cleanUuid.substring(12, 16) + "-" +
                    cleanUuid.substring(16, 20) + "-" +
                    cleanUuid.substring(20, 32);
            return UUID.fromString(formattedUuid);
        }

        // Try parsing as-is (in case it's already properly formatted)
        return UUID.fromString(uuidString);
    }


}