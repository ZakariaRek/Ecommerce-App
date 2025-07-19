package com.Ecommerce.Loyalty_Service.Controllers;

import com.Ecommerce.Loyalty_Service.Entities.BenefitType;
import com.Ecommerce.Loyalty_Service.Entities.MembershipTier;
import com.Ecommerce.Loyalty_Service.Entities.TierBenefit;
import com.Ecommerce.Loyalty_Service.Mappers.TierBenefitMapper;
import com.Ecommerce.Loyalty_Service.Payload.Request.TierBenefit.TierBenefitCreateRequestDto;
import com.Ecommerce.Loyalty_Service.Payload.Request.TierBenefit.TierBenefitUpdateRequestDto;
import com.Ecommerce.Loyalty_Service.Payload.Response.TierBenefit.TierBenefitResponseDto;
import com.Ecommerce.Loyalty_Service.Payload.Response.TierBenefit.TierBenefitSummaryDto;
import com.Ecommerce.Loyalty_Service.Services.TierBenefitService;
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
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/tier-benefits")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Tier Benefit Management", description = "Membership tier benefit operations")
public class TierBenefitController {

    private final TierBenefitService tierBenefitService;
    private final TierBenefitMapper tierBenefitMapper;

    @Operation(
            summary = "Get all tier benefits",
            description = "Retrieve all tier benefits in the system"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved all tier benefits"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping
    public ResponseEntity<List<TierBenefitResponseDto>> getAllTierBenefits() {
        log.info("Retrieving all tier benefits");
        List<TierBenefit> tierBenefits = tierBenefitService.getAllTierBenefits();
        List<TierBenefitResponseDto> responseDtos = tierBenefits.stream()
                .map(tierBenefitMapper::toResponseDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responseDtos);
    }

    @Operation(
            summary = "Get active tier benefits",
            description = "Retrieve all active tier benefits in the system"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved active tier benefits"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/active")
    public ResponseEntity<List<TierBenefitResponseDto>> getActiveTierBenefits() {
        log.info("Retrieving all active tier benefits");
        List<TierBenefit> tierBenefits = tierBenefitService.getActiveTierBenefits();
        List<TierBenefitResponseDto> responseDtos = tierBenefits.stream()
                .map(tierBenefitMapper::toResponseDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responseDtos);
    }

    @Operation(
            summary = "Get tier benefits summary",
            description = "Retrieve a summary of all tier benefits ordered by tier and type"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved tier benefits summary"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/summary")
    public ResponseEntity<List<TierBenefitSummaryDto>> getTierBenefitsSummary() {
        log.info("Retrieving tier benefits summary");
        List<TierBenefit> tierBenefits = tierBenefitService.getTierBenefitsSummary();
        List<TierBenefitSummaryDto> summaryDtos = tierBenefits.stream()
                .map(tierBenefitMapper::toSummaryDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(summaryDtos);
    }

    @Operation(
            summary = "Get tier benefit by ID",
            description = "Retrieve a specific tier benefit by its ID"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved tier benefit"),
            @ApiResponse(responseCode = "404", description = "Tier benefit not found"),
            @ApiResponse(responseCode = "400", description = "Invalid ID format"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{id}")
    public ResponseEntity<TierBenefitResponseDto> getTierBenefitById(
            @Parameter(description = "Tier benefit ID", example = "123e4567-e89b-12d3-a456-426614174001")
            @PathVariable UUID id) {
        log.info("Retrieving tier benefit by ID: {}", id);
        TierBenefit tierBenefit = tierBenefitService.getTierBenefitById(id);
        TierBenefitResponseDto responseDto = tierBenefitMapper.toResponseDto(tierBenefit);

        return ResponseEntity.ok(responseDto);
    }

    @Operation(
            summary = "Get tier benefits by membership tier",
            description = "Retrieve all benefits for a specific membership tier"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved tier benefits"),
            @ApiResponse(responseCode = "400", description = "Invalid membership tier"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/tier/{tier}")
    public ResponseEntity<List<TierBenefitResponseDto>> getTierBenefitsByTier(
            @Parameter(description = "Membership tier", example = "GOLD")
            @PathVariable MembershipTier tier) {
        log.info("Retrieving tier benefits for tier: {}", tier);
        List<TierBenefit> tierBenefits = tierBenefitService.getTierBenefitsByTier(tier);
        List<TierBenefitResponseDto> responseDtos = tierBenefits.stream()
                .map(tierBenefitMapper::toResponseDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responseDtos);
    }

    @Operation(
            summary = "Get tier benefits by benefit type",
            description = "Retrieve all benefits of a specific type across all tiers"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved tier benefits"),
            @ApiResponse(responseCode = "400", description = "Invalid benefit type"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/type/{benefitType}")
    public ResponseEntity<List<TierBenefitResponseDto>> getTierBenefitsByType(
            @Parameter(description = "Benefit type", example = "DISCOUNT")
            @PathVariable BenefitType benefitType) {
        log.info("Retrieving tier benefits for benefit type: {}", benefitType);
        List<TierBenefit> tierBenefits = tierBenefitService.getTierBenefitsByType(benefitType);
        List<TierBenefitResponseDto> responseDtos = tierBenefits.stream()
                .map(tierBenefitMapper::toResponseDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responseDtos);
    }

    @Operation(
            summary = "Get specific tier benefit",
            description = "Retrieve a specific benefit for a tier and benefit type combination"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved tier benefit"),
            @ApiResponse(responseCode = "404", description = "Tier benefit not found"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/tier/{tier}/type/{benefitType}")
    public ResponseEntity<TierBenefitResponseDto> getSpecificTierBenefit(
            @Parameter(description = "Membership tier", example = "GOLD")
            @PathVariable MembershipTier tier,
            @Parameter(description = "Benefit type", example = "DISCOUNT")
            @PathVariable BenefitType benefitType) {
        log.info("Retrieving tier benefit for tier: {} and type: {}", tier, benefitType);

        Optional<TierBenefit> tierBenefitOpt = tierBenefitService.getTierBenefit(tier, benefitType);
        if (tierBenefitOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        TierBenefitResponseDto responseDto = tierBenefitMapper.toResponseDto(tierBenefitOpt.get());
        return ResponseEntity.ok(responseDto);
    }

    @Operation(
            summary = "Create a new tier benefit",
            description = "Create a new benefit for a specific membership tier"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tier benefit created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data or benefit already exists"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping
    public ResponseEntity<TierBenefitResponseDto> createTierBenefit(
            @Valid @RequestBody TierBenefitCreateRequestDto request) {
        log.info("Creating tier benefit for tier: {} and type: {}",
                request.getTier(), request.getBenefitType());

        // Validate benefit configuration
        boolean isValid = tierBenefitService.validateBenefitConfiguration(
                request.getBenefitType(),
                request.getDiscountPercentage(),
                request.getMaxDiscountAmount(),
                request.getMinOrderAmount()
        );

        if (!isValid) {
            throw new RuntimeException("Invalid benefit configuration for benefit type: " + request.getBenefitType());
        }

        TierBenefit tierBenefit = tierBenefitService.createTierBenefit(
                request.getTier(),
                request.getBenefitType(),
                request.getBenefitConfig(),
                request.getDiscountPercentage(),
                request.getMaxDiscountAmount(),
                request.getMinOrderAmount()
        );

        TierBenefitResponseDto responseDto = tierBenefitMapper.toResponseDto(tierBenefit);
        return ResponseEntity.ok(responseDto);
    }

    @Operation(
            summary = "Update an existing tier benefit",
            description = "Update the configuration of an existing tier benefit"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tier benefit updated successfully"),
            @ApiResponse(responseCode = "404", description = "Tier benefit not found"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/{id}")
    public ResponseEntity<TierBenefitResponseDto> updateTierBenefit(
            @Parameter(description = "Tier benefit ID", example = "123e4567-e89b-12d3-a456-426614174001")
            @PathVariable UUID id,
            @Valid @RequestBody TierBenefitUpdateRequestDto request) {
        log.info("Updating tier benefit with ID: {}", id);

        TierBenefit updatedTierBenefit = tierBenefitService.updateTierBenefit(
                id,
                request.getBenefitConfig(),
                request.getDiscountPercentage(),
                request.getMaxDiscountAmount(),
                request.getMinOrderAmount(),
                request.getActive()
        );

        TierBenefitResponseDto responseDto = tierBenefitMapper.toResponseDto(updatedTierBenefit);
        return ResponseEntity.ok(responseDto);
    }

    @Operation(
            summary = "Activate a tier benefit",
            description = "Activate a previously deactivated tier benefit"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tier benefit activated successfully"),
            @ApiResponse(responseCode = "404", description = "Tier benefit not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PatchMapping("/{id}/activate")
    public ResponseEntity<TierBenefitResponseDto> activateTierBenefit(
            @Parameter(description = "Tier benefit ID", example = "123e4567-e89b-12d3-a456-426614174001")
            @PathVariable UUID id) {
        log.info("Activating tier benefit with ID: {}", id);

        TierBenefit activatedTierBenefit = tierBenefitService.activateTierBenefit(id);
        TierBenefitResponseDto responseDto = tierBenefitMapper.toResponseDto(activatedTierBenefit);

        return ResponseEntity.ok(responseDto);
    }

    @Operation(
            summary = "Deactivate a tier benefit",
            description = "Deactivate an active tier benefit"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tier benefit deactivated successfully"),
            @ApiResponse(responseCode = "404", description = "Tier benefit not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<TierBenefitResponseDto> deactivateTierBenefit(
            @Parameter(description = "Tier benefit ID", example = "123e4567-e89b-12d3-a456-426614174001")
            @PathVariable UUID id) {
        log.info("Deactivating tier benefit with ID: {}", id);

        TierBenefit deactivatedTierBenefit = tierBenefitService.deactivateTierBenefit(id);
        TierBenefitResponseDto responseDto = tierBenefitMapper.toResponseDto(deactivatedTierBenefit);

        return ResponseEntity.ok(responseDto);
    }

    @Operation(
            summary = "Delete a tier benefit",
            description = "Permanently delete a tier benefit from the system"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Tier benefit deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Tier benefit not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTierBenefit(
            @Parameter(description = "Tier benefit ID", example = "123e4567-e89b-12d3-a456-426614174001")
            @PathVariable UUID id) {
        log.info("Deleting tier benefit with ID: {}", id);

        tierBenefitService.deleteTierBenefit(id);
        return ResponseEntity.noContent().build();
    }
}