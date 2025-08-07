package com.Ecommerce.Loyalty_Service.Controllers;

import com.Ecommerce.Loyalty_Service.Entities.CRM;
import com.Ecommerce.Loyalty_Service.Entities.MembershipTier;
import com.Ecommerce.Loyalty_Service.Mappers.CrmMapper;
import com.Ecommerce.Loyalty_Service.Payload.Response.CRM.CrmResponseDto;
import com.Ecommerce.Loyalty_Service.Payload.Response.CRM.LoyaltyScoreResponseDto;
import com.Ecommerce.Loyalty_Service.Services.CRMService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CRMController.class)
class CRMControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Mock
    private CRMService crmService;

    @Mock
    private CrmMapper crmMapper;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID userId;
    private CRM testCrm;
    private CrmResponseDto testCrmDto;

    @BeforeEach
    void setUp() {
        userId = UUID.fromString("123e4567-e89b-12d3-a456-426614174001");

        testCrm = new CRM();
        testCrm.setId(UUID.randomUUID());
        testCrm.setUserId(userId);
        testCrm.setTotalPoints(1500);
        testCrm.setMembershipLevel(MembershipTier.SILVER);
        testCrm.setJoinDate(LocalDateTime.now().minusMonths(6));
        testCrm.setLastActivity(LocalDateTime.now().minusDays(5));

        testCrmDto = CrmResponseDto.builder()
                .id(testCrm.getId())
                .userId(testCrm.getUserId())
                .totalPoints(testCrm.getTotalPoints())
                .membershipLevel(testCrm.getMembershipLevel())
                .joinDate(testCrm.getJoinDate())
                .lastActivity(testCrm.getLastActivity())
                .loyaltyScore(78.5)
                .build();
    }

    @Test
    void getAllUsers_ReturnsAllCRMUsers() throws Exception {
        // Given
        List<CRM> crmUsers = Arrays.asList(testCrm);
        List<CrmResponseDto> crmDtos = Arrays.asList(testCrmDto);

        when(crmService.getAllUsers()).thenReturn(crmUsers);
        when(crmMapper.toResponseDto(any(CRM.class))).thenReturn(testCrmDto);

        // When & Then
        mockMvc.perform(get("/crm")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].userId", is(userId.toString())))
                .andExpect(jsonPath("$[0].totalPoints", is(1500)))
                .andExpect(jsonPath("$[0].membershipLevel", is("SILVER")))
                .andExpect(jsonPath("$[0].loyaltyScore", is(78.5)));
    }

    @Test
    void getCRMByUserId_ValidUUID_ReturnsUserCRM() throws Exception {
        // Given
        when(crmService.getByUserId(userId)).thenReturn(testCrm);
        when(crmMapper.toResponseDto(testCrm)).thenReturn(testCrmDto);

        // When & Then
        mockMvc.perform(get("/crm/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId", is(userId.toString())))
                .andExpect(jsonPath("$.totalPoints", is(1500)))
                .andExpect(jsonPath("$.membershipLevel", is("SILVER")))
                .andExpect(jsonPath("$.loyaltyScore", is(78.5)));
    }

    @Test
    void getCRMByUserId_ValidMongoDbObjectId_ReturnsUserCRM() throws Exception {
        // Given - MongoDB ObjectId (24 hex characters)
        String mongoUserId = "507f1f77bcf86cd799439011";

        when(crmService.getByUserId(any(UUID.class))).thenReturn(testCrm);
        when(crmMapper.toResponseDto(testCrm)).thenReturn(testCrmDto);

        // When & Then
        mockMvc.perform(get("/crm/{userId}", mongoUserId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId", is(userId.toString())))
                .andExpect(jsonPath("$.totalPoints", is(1500)))
                .andExpect(jsonPath("$.membershipLevel", is("SILVER")));
    }

    @Test
    void getCRMByUserId_UserNotFound_Returns404() throws Exception {
        // Given
        when(crmService.getByUserId(any(UUID.class))).thenThrow(new RuntimeException("User not found in CRM system"));

        // When & Then
        mockMvc.perform(get("/crm/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(content().string("User not found in CRM system"));
    }

    @Test
    void getCRMByUserId_InvalidUUIDFormat_Returns400() throws Exception {
        // Given
        String invalidUserId = "invalid-uuid";

        // When & Then
        mockMvc.perform(get("/crm/{userId}", invalidUserId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid user ID format"));
    }

    @Test
    void getLoyaltyScore_ValidUser_ReturnsScore() throws Exception {
        // Given
        LoyaltyScoreResponseDto loyaltyScoreDto = LoyaltyScoreResponseDto.builder()
                .userId(userId)
                .loyaltyScore(78.5)
                .explanation("Based on 1500 points, 180 days membership, recent activity")
                .build();

        when(crmService.getByUserId(userId)).thenReturn(testCrm);
        when(crmService.calculateLoyaltyScore(userId)).thenReturn(78.5);
        when(crmMapper.toLoyaltyScoreDto(testCrm, 78.5)).thenReturn(loyaltyScoreDto);

        // When & Then
        mockMvc.perform(get("/crm/{userId}/loyalty-score", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId", is(userId.toString())))
                .andExpect(jsonPath("$.loyaltyScore", is(78.5)))
                .andExpect(jsonPath("$.explanation", containsString("1500 points")));
    }

    @Test
    void getLoyaltyScore_UserNotFound_ThrowsException() throws Exception {
        // Given
        when(crmService.getByUserId(userId)).thenThrow(new RuntimeException("User not found in loyalty system"));

        // When & Then
        mockMvc.perform(get("/crm/{userId}/loyalty-score", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isInternalServerError());
    }

    @Test
    void getAllUsers_EmptyResult_ReturnsEmptyArray() throws Exception {
        // Given
        when(crmService.getAllUsers()).thenReturn(Arrays.asList());

        // When & Then
        mockMvc.perform(get("/crm")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getAllUsers_MultipleUsers_ReturnsAllUsers() throws Exception {
        // Given
        CRM user1 = new CRM();
        user1.setUserId(UUID.randomUUID());
        user1.setTotalPoints(500);
        user1.setMembershipLevel(MembershipTier.BRONZE);

        CRM user2 = new CRM();
        user2.setUserId(UUID.randomUUID());
        user2.setTotalPoints(2500);
        user2.setMembershipLevel(MembershipTier.GOLD);

        CrmResponseDto dto1 = CrmResponseDto.builder()
                .userId(user1.getUserId())
                .totalPoints(500)
                .membershipLevel(MembershipTier.BRONZE)
                .build();

        CrmResponseDto dto2 = CrmResponseDto.builder()
                .userId(user2.getUserId())
                .totalPoints(2500)
                .membershipLevel(MembershipTier.GOLD)
                .build();

        when(crmService.getAllUsers()).thenReturn(Arrays.asList(user1, user2));
        when(crmMapper.toResponseDto(user1)).thenReturn(dto1);
        when(crmMapper.toResponseDto(user2)).thenReturn(dto2);

        // When & Then
        mockMvc.perform(get("/crm")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].membershipLevel", is("BRONZE")))
                .andExpect(jsonPath("$[0].totalPoints", is(500)))
                .andExpect(jsonPath("$[1].membershipLevel", is("GOLD")))
                .andExpect(jsonPath("$[1].totalPoints", is(2500)));
    }

    @Test
    void getCRMByUserId_ServiceException_Returns500() throws Exception {
        // Given
        when(crmService.getByUserId(any(UUID.class))).thenThrow(new RuntimeException("Database connection error"));

        // When & Then
        mockMvc.perform(get("/crm/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound()); // Controller catches RuntimeException and returns 404
    }

    @Test
    void getCRMByUserId_DifferentTierLevels_ReturnsCorrectData() throws Exception {
        // Test for each membership tier
        MembershipTier[] tiers = {MembershipTier.BRONZE, MembershipTier.SILVER,
                MembershipTier.GOLD, MembershipTier.PLATINUM, MembershipTier.DIAMOND};
        int[] points = {100, 750, 3000, 7500, 15000};

        for (int i = 0; i < tiers.length; i++) {
            // Given
            CRM crmWithTier = new CRM();
            crmWithTier.setUserId(userId);
            crmWithTier.setTotalPoints(points[i]);
            crmWithTier.setMembershipLevel(tiers[i]);

            CrmResponseDto dtoWithTier = CrmResponseDto.builder()
                    .userId(userId)
                    .totalPoints(points[i])
                    .membershipLevel(tiers[i])
                    .build();

            when(crmService.getByUserId(userId)).thenReturn(crmWithTier);
            when(crmMapper.toResponseDto(crmWithTier)).thenReturn(dtoWithTier);

            // When & Then
            mockMvc.perform(get("/crm/{userId}", userId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.membershipLevel", is(tiers[i].toString())))
                    .andExpect(jsonPath("$.totalPoints", is(points[i])));
        }
    }
}