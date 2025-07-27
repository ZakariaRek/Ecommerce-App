package com.Ecommerce.User_Service.Services.Kafka;

import com.Ecommerce.User_Service.Config.KafkaConfig;
import com.Ecommerce.User_Service.Models.User;
import com.Ecommerce.User_Service.Models.UserAddress;
import com.Ecommerce.User_Service.Payload.Kafka.*;
import com.Ecommerce.User_Service.Services.UserAddressService;
import com.Ecommerce.User_Service.Services.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Kafka listener service to handle user information requests from Notification Service
 * Uses String-based deserialization for reliable cross-service communication
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserInfoKafkaListenerService {

    private final UserService userService;
    private final UserAddressService userAddressService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Handle single user email requests
     */



    /**
     * Handle bulk user email requests
     */
    @KafkaListener(topics = KafkaConfig.TOPIC_BULK_USER_EMAIL_REQUEST, groupId = "user-service-bulk-email-requests")
    public void handleBulkUserEmailRequest(@Payload String payload,
                                           @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                           Acknowledgment acknowledgment) {

        BulkUserEmailRequest request = null;
        try {
            // Parse JSON string to object
            request = objectMapper.readValue(payload, BulkUserEmailRequest.class);
            log.info("Received bulk user email request: {} for {} users", request.getRequestId(), request.getUserIds().size());

            List<UserEmailResponse> responses = processBulkUserEmailRequest(request);
            BulkUserEmailResponse bulkResponse = BulkUserEmailResponse.create(request.getRequestId(), responses);

            // Send response back to notification service
            kafkaTemplate.send(KafkaConfig.TOPIC_BULK_USER_EMAIL_RESPONSE, "bulk-" + request.getRequestId(), bulkResponse);
            log.info("Sent bulk user email response: {} for {} users", request.getRequestId(), request.getUserIds().size());

            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error processing bulk user email request: {}", request != null ? request.getRequestId() : "unknown", e);

            // Send error response if we have request info
            if (request != null) {
                BulkUserEmailRequest finalRequest = request;
                List<UserEmailResponse> errorResponses = request.getUserIds().stream()
                        .map(userId -> UserEmailResponse.error(finalRequest.getRequestId(), String.valueOf(userId), "Error processing request: " + e.getMessage()))
                        .collect(Collectors.toList());

                BulkUserEmailResponse errorBulkResponse = BulkUserEmailResponse.create(request.getRequestId(), errorResponses);
                kafkaTemplate.send(KafkaConfig.TOPIC_BULK_USER_EMAIL_RESPONSE, "bulk-" + request.getRequestId(), errorBulkResponse);
            }
            acknowledgment.acknowledge();
        }
    }


    /**
     * Handle bulk user information requests
     */
    @KafkaListener(topics = KafkaConfig.TOPIC_BULK_USER_INFO_REQUEST, groupId = "user-service-bulk-info-requests")
    public void handleBulkUserInfoRequest(@Payload String payload,
                                          @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                          Acknowledgment acknowledgment) {

        BulkUserInfoRequest request = null;
        try {
            // Parse JSON string to object
            request = objectMapper.readValue(payload, BulkUserInfoRequest.class);
            log.info("Received bulk user info request: {} for {} users", request.getRequestId(), request.getUserIds().size());

            List<UserInfoResponse> responses = processBulkUserInfoRequest(request);
            BulkUserInfoResponse bulkResponse = BulkUserInfoResponse.create(request.getRequestId(), responses);

            // Send response back to notification service
            kafkaTemplate.send(KafkaConfig.TOPIC_BULK_USER_INFO_RESPONSE, "bulk-" + request.getRequestId(), bulkResponse);
            log.info("Sent bulk user info response: {} for {} users", request.getRequestId(), request.getUserIds().size());

            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error processing bulk user info request: {}", request != null ? request.getRequestId() : "unknown", e);

            // Send error response if we have request info
            if (request != null) {
                BulkUserInfoRequest finalRequest = request;
                List<UserInfoResponse> errorResponses = request.getUserIds().stream()
                        .map(userId -> UserInfoResponse.error(finalRequest.getRequestId(), userId, "Error processing request: " + e.getMessage()))
                        .collect(Collectors.toList());

                BulkUserInfoResponse errorBulkResponse = BulkUserInfoResponse.create(request.getRequestId(), errorResponses);
                kafkaTemplate.send(KafkaConfig.TOPIC_BULK_USER_INFO_RESPONSE, "bulk-" + request.getRequestId(), errorBulkResponse);
            }
            acknowledgment.acknowledge();
        }
    }


    /**
     * Process bulk user email requests
     */
    private List<UserEmailResponse> processBulkUserEmailRequest(BulkUserEmailRequest request) {
        List<UserEmailResponse> responses = new ArrayList<>();

        for (UUID userId : request.getUserIds()) {
            UserEmailRequest singleRequest = UserEmailRequest.builder()
                    .requestId(request.getRequestId())
                    .userId(String.valueOf(userId))
                    .requestingService(request.getRequestingService())
                    .requestTime(request.getRequestTime())
                    .purpose(request.getPurpose())
                    .build();

            responses.add(processUserEmailRequest(singleRequest));
        }

        return responses;
    }


    /**
     * Process bulk user information requests
     */
    private List<UserInfoResponse> processBulkUserInfoRequest(BulkUserInfoRequest request) {
        List<UserInfoResponse> responses = new ArrayList<>();

        for (UUID userId : request.getUserIds()) {
            UserInfoRequest singleRequest = UserInfoRequest.builder()
                    .requestId(request.getRequestId())
                    .userId(userId)
                    .requestingService(request.getRequestingService())
                    .requestTime(request.getRequestTime())
                    .purpose(request.getPurpose())
                    .includeAddresses(request.isIncludeAddresses())
                    .includeRoles(request.isIncludeRoles())
                    .build();

            responses.add(processUserInfoRequest(singleRequest));
        }

        return responses;
    }

    // ===================== UTILITY METHODS =====================

    /**
     * Extract first name from username (simple implementation)
     */



    /**
     * Health check method to verify service connectivity
     */
    public boolean isServiceHealthy() {
        try {
            // Simple health check - verify we can access the user repository
            long userCount = userService.getAllUsers().size();
            log.debug("User service health check - total users: {}", userCount);
            return true;
        } catch (Exception e) {
            log.error("User service health check failed", e);
            return false;
        }
    }

    /**
     * Get service statistics for monitoring
     */
    public Map<String, Object> getServiceStats() {
        Map<String, Object> stats = new HashMap<>();
        try {
            List<User> allUsers = userService.getAllUsers();
            stats.put("totalUsers", allUsers.size());
            stats.put("serviceStatus", "UP");
            stats.put("lastChecked", java.time.LocalDateTime.now());

            // Count users by status
            Map<String, Long> statusCounts = allUsers.stream()
                    .collect(Collectors.groupingBy(
                            user -> user.getStatus().name(),
                            Collectors.counting()
                    ));
            stats.put("usersByStatus", statusCounts);

        } catch (Exception e) {
            stats.put("serviceStatus", "DOWN");
            stats.put("error", e.getMessage());
            log.error("Error getting service stats", e);
        }
        return stats;
    }

    /**
     * Convert UUID back to original ObjectId (REVERSE of EmailKafkaConsumer conversion)
     * This reverses the convertObjectIdToUuidReversible method
     */
    private String convertUuidToObjectId(UUID uuid) {
        try {
            // Extract bytes from UUID
            byte[] uuidBytes = uuidToBytes(uuid);

            // Check if this looks like a converted ObjectId (last 4 bytes should be zero)
            boolean isConvertedObjectId = true;
            for (int i = 12; i < 16; i++) {
                if (uuidBytes[i] != 0) {
                    isConvertedObjectId = false;
                    break;
                }
            }

            if (isConvertedObjectId) {
                // Extract the first 12 bytes (original ObjectId)
                byte[] objectIdBytes = new byte[12];
                System.arraycopy(uuidBytes, 0, objectIdBytes, 0, 12);

                // Convert back to hex string
                String objectId = bytesToHexString(objectIdBytes);
                log.debug("Converted UUID {} back to ObjectId {}", uuid, objectId);
                return objectId;
            } else {
                // This UUID was not created from ObjectId conversion
                log.debug("UUID {} does not appear to be converted from ObjectId", uuid);
                return uuid.toString();
            }

        } catch (Exception e) {
            log.warn("Failed to convert UUID {} to ObjectId: {}", uuid, e.getMessage());
            return uuid.toString();
        }
    }

    /**
     * Smart user lookup that tries multiple approaches
     */
    private Optional<User> findUserById(String userId) {
        try {
            // Try 1: Direct lookup with the provided ID
            Optional<User> user = userService.getUserById(userId);
            if (user.isPresent()) {
                log.debug("Found user by direct lookup: {}", userId);
                return user;
            }

            // Try 2: If it's a UUID, try converting to ObjectId
            if (isUUID(userId)) {
                UUID uuid = UUID.fromString(userId);
                String possibleObjectId = convertUuidToObjectId(uuid);

                if (!possibleObjectId.equals(userId)) {
                    user = userService.getUserById(possibleObjectId);
                    if (user.isPresent()) {
                        log.debug("Found user by UUID->ObjectId conversion: {} -> {}", userId, possibleObjectId);
                        return user;
                    }
                }
            }

            // Try 3: If it looks like ObjectId, try converting to UUID
            if (isObjectId(userId)) {
                UUID convertedUuid = convertObjectIdToUuid(userId);
                user = userService.getUserById(convertedUuid.toString());
                if (user.isPresent()) {
                    log.debug("Found user by ObjectId->UUID conversion: {} -> {}", userId, convertedUuid);
                    return user;
                }
            }

            log.warn("User not found with any lookup method for ID: {}", userId);
            return Optional.empty();

        } catch (Exception e) {
            log.error("Error during user lookup for ID {}: {}", userId, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Convert ObjectId to UUID (same as EmailKafkaConsumer)
     */
    private UUID convertObjectIdToUuid(String objectId) {
        try {
            if (objectId == null || !objectId.matches("^[0-9a-fA-F]{24}$")) {
                throw new IllegalArgumentException("Invalid ObjectId format: " + objectId);
            }

            byte[] objectIdBytes = hexStringToBytes(objectId);
            byte[] uuidBytes = new byte[16];
            System.arraycopy(objectIdBytes, 0, uuidBytes, 0, 12);

            long mostSigBits = 0;
            long leastSigBits = 0;

            for (int i = 0; i < 8; i++) {
                mostSigBits = (mostSigBits << 8) | (uuidBytes[i] & 0xff);
            }
            for (int i = 8; i < 16; i++) {
                leastSigBits = (leastSigBits << 8) | (uuidBytes[i] & 0xff);
            }

            return new UUID(mostSigBits, leastSigBits);

        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid ObjectId format: " + objectId, e);
        }
    }

    // ================ UTILITY METHODS ================

    /**
     * Convert UUID to byte array
     */
    private byte[] uuidToBytes(UUID uuid) {
        byte[] bytes = new byte[16];
        long msb = uuid.getMostSignificantBits();
        long lsb = uuid.getLeastSignificantBits();

        for (int i = 0; i < 8; i++) {
            bytes[i] = (byte) (msb >>> (8 * (7 - i)));
        }
        for (int i = 8; i < 16; i++) {
            bytes[i] = (byte) (lsb >>> (8 * (7 - i)));
        }

        return bytes;
    }

    /**
     * Convert bytes to hex string
     */
    private String bytesToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * Convert hex string to bytes
     */
    private byte[] hexStringToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }

    /**
     * Check if string is a valid UUID format
     */
    private boolean isUUID(String str) {
        try {
            UUID.fromString(str);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Check if string is a valid ObjectId format
     */
    private boolean isObjectId(String str) {
        return str != null && str.matches("^[0-9a-fA-F]{24}$");
    }

    // ================ KAFKA LISTENERS ================

    @KafkaListener(topics = KafkaConfig.TOPIC_USER_EMAIL_REQUEST, groupId = "user-service-email-requests")
    public void handleUserEmailRequest(@Payload String payload,
                                       @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                       Acknowledgment acknowledgment) {

        UserEmailRequest request = null;
        try {
            request = objectMapper.readValue(payload, UserEmailRequest.class);
            log.info("Received user email request: {} for user: {}", request.getRequestId(), request.getUserId());

            UserEmailResponse response = processUserEmailRequest(request);

            kafkaTemplate.send(KafkaConfig.TOPIC_USER_EMAIL_RESPONSE, request.getUserId(), response);
            log.info("Sent user email response: {} for user: {}", request.getRequestId(), request.getUserId());

            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error processing user email request: {}", request != null ? request.getRequestId() : "unknown", e);

            if (request != null) {
                UserEmailResponse errorResponse = UserEmailResponse.error(
                        request.getRequestId(),
                        request.getUserId(),
                        "Error processing request: " + e.getMessage()
                );
                kafkaTemplate.send(KafkaConfig.TOPIC_USER_EMAIL_RESPONSE, request.getUserId(), errorResponse);
            }
            acknowledgment.acknowledge();
        }
    }

    @KafkaListener(topics = KafkaConfig.TOPIC_USER_INFO_REQUEST, groupId = "user-service-info-requests")
    public void handleUserInfoRequest(@Payload String payload,
                                      @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                      Acknowledgment acknowledgment) {

        UserInfoRequest request = null;
        try {
            request = objectMapper.readValue(payload, UserInfoRequest.class);
            log.info("Received user info request: {} for user: {}", request.getRequestId(), request.getUserId());

            UserInfoResponse response = processUserInfoRequest(request);

            kafkaTemplate.send(KafkaConfig.TOPIC_USER_INFO_RESPONSE, request.getUserId().toString(), response);
            log.info("Sent user info response: {} for user: {}", request.getRequestId(), request.getUserId());

            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error processing user info request: {}", request != null ? request.getRequestId() : "unknown", e);

            if (request != null) {
                UserInfoResponse errorResponse = UserInfoResponse.error(
                        request.getRequestId(),
                        request.getUserId(),
                        "Error processing request: " + e.getMessage()
                );
                kafkaTemplate.send(KafkaConfig.TOPIC_USER_INFO_RESPONSE, request.getUserId().toString(), errorResponse);
            }
            acknowledgment.acknowledge();
        }
    }

    // ================ PROCESSING METHODS ================

    private UserEmailResponse processUserEmailRequest(UserEmailRequest request) {
        Optional<User> userOptional = findUserById(request.getUserId());

        if (userOptional.isEmpty()) {
            log.warn("User not found for email request: {}", request.getUserId());
            return UserEmailResponse.notFound(request.getRequestId(), request.getUserId());
        }

        User user = userOptional.get();
        return UserEmailResponse.builder()
                .requestId(request.getRequestId())
                .userId(request.getUserId())
                .email(user.getEmail())
                .firstName(extractFirstName(user.getUsername()))
                .lastName(extractLastName(user.getUsername()))
                .preferredLanguage("en")
                .emailVerified(true)
                .marketingOptIn(true)
                .responseTime(java.time.LocalDateTime.now())
                .status("SUCCESS")
                .build();
    }

    private UserInfoResponse processUserInfoRequest(UserInfoRequest request) {
        Optional<User> userOptional = findUserById(request.getUserId().toString());

        if (userOptional.isEmpty()) {
            log.warn("User not found for info request: {}", request.getUserId());
            return UserInfoResponse.notFound(request.getRequestId(), request.getUserId());
        }

        User user = userOptional.get();
        UserInfoResponse.UserInfoResponseBuilder responseBuilder = UserInfoResponse.builder()
                .requestId(request.getRequestId())
                .userId(request.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(extractFirstName(user.getUsername()))
                .lastName(extractLastName(user.getUsername()))
                .status(user.getStatus())
                .preferredLanguage("en")
                .emailVerified(true)
                .marketingOptIn(true)
                .responseTime(java.time.LocalDateTime.now())
                .status_response("SUCCESS");

        // Include roles if requested
        if (request.isIncludeRoles() && user.getRoles() != null) {
            Set<String> roleNames = user.getRoles().stream()
                    .map(role -> role.getName().name())
                    .collect(Collectors.toSet());
            responseBuilder.roles(roleNames);
        }

        // Include addresses if requested
        if (request.isIncludeAddresses()) {
            List<UserAddress> addresses = userAddressService.getAddressesByUserId(user.getId());
            List<UserInfoResponse.UserAddressDTO> addressDTOs = addresses.stream()
                    .map(UserInfoResponse.UserAddressDTO::fromUserAddress)
                    .collect(Collectors.toList());
            responseBuilder.addresses(addressDTOs);

            // Find default address
            Optional<UserAddress> defaultAddress = userAddressService.getDefaultAddress(user.getId());
            if (defaultAddress.isPresent()) {
                responseBuilder.defaultAddress(UserInfoResponse.UserAddressDTO.fromUserAddress(defaultAddress.get()));
            }
        }

        return responseBuilder.build();
    }

    // ================ UTILITY METHODS =====================

    private String extractFirstName(String username) {
        if (username == null || username.trim().isEmpty()) {
            return "";
        }
        String[] parts = username.split("\\s+");
        return parts[0];
    }

    private String extractLastName(String username) {
        if (username == null || username.trim().isEmpty()) {
            return "";
        }
        String[] parts = username.split("\\s+");
        return parts.length > 1 ? parts[parts.length - 1] : "";
    }
}