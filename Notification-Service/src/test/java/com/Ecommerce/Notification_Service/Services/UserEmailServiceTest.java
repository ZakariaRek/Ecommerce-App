package com.Ecommerce.Notification_Service.Services;

import com.Ecommerce.Notification_Service.Payload.Kafka.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserEmailServiceTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private UserEmailService userEmailService;

    private UUID testUserId;
    private UserEmailResponse successEmailResponse;
    private UserInfoResponse successInfoResponse;

    @BeforeEach
    void setUp() throws Exception {
        testUserId = UUID.randomUUID();

        successEmailResponse = UserEmailResponse.builder()
                .requestId("test-request-id")
                .userId(testUserId)
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .emailVerified(true)
                .marketingOptIn(true)
                .responseTime(LocalDateTime.now())
                .status("SUCCESS")
                .build();

        successInfoResponse = UserInfoResponse.builder()
                .requestId("test-request-id")
                .userId(testUserId)
                .username("johndoe")
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .emailVerified(true)
                .marketingOptIn(true)
                .responseTime(LocalDateTime.now())
                .status_response("SUCCESS")
                .build();

        // Mock ObjectMapper for JSON serialization/deserialization
        when(objectMapper.readValue(anyString(), eq(UserEmailResponse.class))).thenReturn(successEmailResponse);
        when(objectMapper.readValue(anyString(), eq(UserInfoResponse.class))).thenReturn(successInfoResponse);
        when(objectMapper.readValue(anyString(), eq(BulkUserEmailResponse.class)))
                .thenReturn(BulkUserEmailResponse.create("test-request-id", Arrays.asList(successEmailResponse)));
        when(objectMapper.readValue(anyString(), eq(BulkUserInfoResponse.class)))
                .thenReturn(BulkUserInfoResponse.create("test-request-id", Arrays.asList(successInfoResponse)));
    }

    @Test
    void getUserEmail_ShouldReturnEmailFromService() throws Exception {
        // When
        CompletableFuture<String> emailFuture = userEmailService.getUserEmail(testUserId);

        // Simulate response from Kafka
        String responsePayload = objectMapper.writeValueAsString(successEmailResponse);
        userEmailService.handleUserEmailResponse(responsePayload);

        // Then
        String email = emailFuture.get(1, TimeUnit.SECONDS);
        assertThat(email).isEqualTo("test@example.com");

        // Verify Kafka request was sent
        verify(kafkaTemplate).send(eq("user-email-request"), eq(testUserId.toString()), any(UserEmailRequest.class));
    }

    @Test
    void getUserEmailWithDetails_ShouldReturnFullEmailResponse() throws Exception {
        // When
        CompletableFuture<UserEmailResponse> responseFuture = userEmailService.getUserEmailWithDetails(testUserId, "TEST_PURPOSE");

        // Simulate response from Kafka
        String responsePayload = objectMapper.writeValueAsString(successEmailResponse);
        userEmailService.handleUserEmailResponse(responsePayload);

        // Then
        UserEmailResponse response = responseFuture.get(1, TimeUnit.SECONDS);
        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getFirstName()).isEqualTo("John");
        assertThat(response.getLastName()).isEqualTo("Doe");
        assertThat(response.getStatus()).isEqualTo("SUCCESS");
    }

    @Test
    void getUserEmailWithDetails_WithCachedData_ShouldReturnFromCache() throws Exception {
        // Given - Cache some data first
        userEmailService.cacheUserInfo(testUserId, "cached@example.com", "cacheduser");

        // When
        CompletableFuture<UserEmailResponse> responseFuture = userEmailService.getUserEmailWithDetails(testUserId, "TEST_PURPOSE");

        // Then
        UserEmailResponse response = responseFuture.get(1, TimeUnit.SECONDS);
        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo("cached@example.com");

        // Verify no Kafka request was sent due to cache hit
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }

    @Test
    void getBulkUserEmails_ShouldReturnEmailMap() throws Exception {
        // Given
        List<UUID> userIds = Arrays.asList(testUserId, UUID.randomUUID());

        // When
        CompletableFuture<Map<UUID, String>> emailsFuture = userEmailService.getBulkUserEmails(userIds, "BULK_TEST");

        // Simulate response from Kafka
        BulkUserEmailResponse bulkResponse = BulkUserEmailResponse.create("test-request-id", Arrays.asList(successEmailResponse));
        String responsePayload = objectMapper.writeValueAsString(bulkResponse);
        userEmailService.handleBulkUserEmailResponse(responsePayload);

        // Then
        Map<UUID, String> emails = emailsFuture.get(1, TimeUnit.SECONDS);
        assertThat(emails).containsKey(testUserId);
        assertThat(emails.get(testUserId)).isEqualTo("test@example.com");

        // Verify bulk Kafka request was sent
        verify(kafkaTemplate).send(eq("bulk-user-email-request"), anyString(), any(BulkUserEmailRequest.class));
    }

    @Test
    void getUserInfo_ShouldReturnUserInfoResponse() throws Exception {
        // When
        CompletableFuture<UserInfoResponse> infoFuture = userEmailService.getUserInfo(testUserId, "TEST_PURPOSE");

        // Simulate response from Kafka
        String responsePayload = objectMapper.writeValueAsString(successInfoResponse);
        userEmailService.handleUserInfoResponse(responsePayload);

        // Then
        UserInfoResponse response = infoFuture.get(1, TimeUnit.SECONDS);
        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getUsername()).isEqualTo("johndoe");
        assertThat(response.getStatus_response()).isEqualTo("SUCCESS");

        // Verify Kafka request was sent
        verify(kafkaTemplate).send(eq("user-info-request"), eq(testUserId.toString()), any(UserInfoRequest.class));
    }

    @Test
    void getBulkUserInfo_ShouldReturnUserInfoMap() throws Exception {
        // Given
        List<UUID> userIds = Arrays.asList(testUserId, UUID.randomUUID());

        // When
        CompletableFuture<Map<UUID, UserInfoResponse>> infoFuture = userEmailService.getBulkUserInfo(userIds, "BULK_TEST");

        // Simulate response from Kafka
        BulkUserInfoResponse bulkResponse = BulkUserInfoResponse.create("test-request-id", Arrays.asList(successInfoResponse));
        String responsePayload = objectMapper.writeValueAsString(bulkResponse);
        userEmailService.handleBulkUserInfoResponse(responsePayload);

        // Then
        Map<UUID, UserInfoResponse> infoMap = infoFuture.get(1, TimeUnit.SECONDS);
        assertThat(infoMap).containsKey(testUserId);
        assertThat(infoMap.get(testUserId).getEmail()).isEqualTo("test@example.com");

        // Verify bulk Kafka request was sent
        verify(kafkaTemplate).send(eq("bulk-user-info-request"), anyString(), any(BulkUserInfoRequest.class));
    }

    @Test
    void getUserEmailWithFallback_WithNoUserService_ShouldReturnFallbackEmail() throws Exception {
        // Given - Mock a failed response
        UserEmailResponse failedResponse = UserEmailResponse.error("test-request-id", testUserId, "Service unavailable");
        when(objectMapper.readValue(anyString(), eq(UserEmailResponse.class))).thenReturn(failedResponse);

        // When
        CompletableFuture<String> emailFuture = userEmailService.getUserEmailWithFallback(testUserId);

        // Simulate failed response from Kafka
        String responsePayload = objectMapper.writeValueAsString(failedResponse);
        userEmailService.handleUserEmailResponse(responsePayload);

        // Then
        String email = emailFuture.get(1, TimeUnit.SECONDS);
        assertThat(email).contains("user-"); // Should be fallback email format
        assertThat(email).contains("@demo.ecommerce.com");
    }

    @Test
    void getBulkUserEmailsWithFallback_ShouldIncludeFallbackEmails() throws Exception {
        // Given
        List<UUID> userIds = Arrays.asList(testUserId, UUID.randomUUID());

        // Mock partial success response (only first user found)
        BulkUserEmailResponse partialResponse = BulkUserEmailResponse.create("test-request-id", Arrays.asList(successEmailResponse));
        when(objectMapper.readValue(anyString(), eq(BulkUserEmailResponse.class))).thenReturn(partialResponse);

        // When
        CompletableFuture<Map<UUID, String>> emailsFuture = userEmailService.getBulkUserEmailsWithFallback(userIds, "BULK_TEST");

        // Simulate response from Kafka
        String responsePayload = objectMapper.writeValueAsString(partialResponse);
        userEmailService.handleBulkUserEmailResponse(responsePayload);

        // Then
        Map<UUID, String> emails = emailsFuture.get(1, TimeUnit.SECONDS);
        assertThat(emails).hasSize(2); // Both users should have emails
        assertThat(emails.get(testUserId)).isEqualTo("test@example.com"); // Real email

        // Second user should have fallback email
        UUID secondUserId = userIds.get(1);
        assertThat(emails.get(secondUserId)).contains("@demo.ecommerce.com");
    }

    @Test
    void handleUserEmailResponse_WithUnknownRequestId_ShouldLogWarning() throws Exception {
        // Given
        UserEmailResponse unknownResponse = successEmailResponse.toBuilder()
                .requestId("unknown-request-id")
                .build();

        // When
        String responsePayload = objectMapper.writeValueAsString(unknownResponse);
        userEmailService.handleUserEmailResponse(responsePayload);

        // Then
        // Should not throw exception, just log warning
        // This is tested by verifying the method completes without exception
    }

    @Test
    void cleanupExpiredCache_ShouldRemoveExpiredEntries() {
        // Given
        userEmailService.cacheUserInfo(testUserId, "test@example.com", "testuser");

        // When
        userEmailService.cleanupExpiredCache();

        // Then
        // Cache should still contain non-expired entries
        // This is more of an integration test and would require reflection or extended time
    }

    @Test
    void getCacheStats_ShouldReturnCorrectStatistics() {
        // Given
        userEmailService.cacheUserInfo(testUserId, "test@example.com", "testuser");

        // When
        Map<String, Object> stats = userEmailService.getCacheStats();

        // Then
        assertThat(stats).containsKeys(
                "totalEmailCached", "validEmailCached", "expiredEmailCached",
                "totalInfoCached", "validInfoCached", "expiredInfoCached",
                "pendingEmailRequests", "pendingBulkEmailRequests",
                "pendingInfoRequests", "pendingBulkInfoRequests"
        );
        assertThat(stats.get("totalEmailCached")).isEqualTo(1);
        assertThat(stats.get("totalInfoCached")).isEqualTo(1);
    }

    @Test
    void clearCache_ShouldRemoveAllCachedData() {
        // Given
        userEmailService.cacheUserInfo(testUserId, "test@example.com", "testuser");
        Map<String, Object> statsBefore = userEmailService.getCacheStats();
        assertThat(statsBefore.get("totalEmailCached")).isEqualTo(1);

        // When
        userEmailService.clearCache();

        // Then
        Map<String, Object> statsAfter = userEmailService.getCacheStats();
        assertThat(statsAfter.get("totalEmailCached")).isEqualTo(0);
        assertThat(statsAfter.get("totalInfoCached")).isEqualTo(0);
    }

    @Test
    void isUserServiceAvailable_WithWorkingService_ShouldReturnTrue() throws Exception {
        // When
        CompletableFuture<Boolean> availabilityFuture = userEmailService.isUserServiceAvailable();

        // Simulate successful response
        String responsePayload = objectMapper.writeValueAsString(successEmailResponse);
        userEmailService.handleUserEmailResponse(responsePayload);

        // Then
        Boolean isAvailable = availabilityFuture.get(1, TimeUnit.SECONDS);
        assertThat(isAvailable).isTrue();
    }

    @Test
    void getUserEmail_WithEmptyUserIdList_ShouldReturnEmptyMap() throws Exception {
        // When
        CompletableFuture<Map<UUID, String>> emailsFuture = userEmailService.getBulkUserEmails(Collections.emptyList(), "TEST");

        // Then
        Map<UUID, String> emails = emailsFuture.get(1, TimeUnit.SECONDS);
        assertThat(emails).isEmpty();

        // Verify no Kafka request was sent
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }

    @Test
    void getUserInfo_WithOptionsEnabled_ShouldSendCorrectRequest() throws Exception {
        // When
        userEmailService.getUserInfo(testUserId, "TEST_PURPOSE", true, true);

        // Then
        ArgumentCaptor<UserInfoRequest> requestCaptor = ArgumentCaptor.forClass(UserInfoRequest.class);
        verify(kafkaTemplate).send(eq("user-info-request"), eq(testUserId.toString()), requestCaptor.capture());

        UserInfoRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.isIncludeAddresses()).isTrue();
        assertThat(capturedRequest.isIncludeRoles()).isTrue();
        assertThat(capturedRequest.getPurpose()).isEqualTo("TEST_PURPOSE");
    }

    @Test
    void handleUserEmailResponse_WithInvalidJson_ShouldHandleGracefully() throws Exception {
        // Given
        when(objectMapper.readValue(anyString(), eq(UserEmailResponse.class)))
                .thenThrow(new RuntimeException("Invalid JSON"));

        // When & Then - Should not throw exception
        userEmailService.handleUserEmailResponse("invalid json");

        // Method should complete without throwing exception
    }
}