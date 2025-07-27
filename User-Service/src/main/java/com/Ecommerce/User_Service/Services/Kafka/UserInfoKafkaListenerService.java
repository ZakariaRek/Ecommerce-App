//package com.Ecommerce.User_Service.Services.Kafka;
//
//import com.Ecommerce.User_Service.Config.KafkaConfig;
//import com.Ecommerce.User_Service.Models.User;
//import com.Ecommerce.User_Service.Models.UserAddress;
//import com.Ecommerce.User_Service.Payload.Kafka.*;
//import com.Ecommerce.User_Service.Services.UserAddressService;
//import com.Ecommerce.User_Service.Services.UserService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.kafka.annotation.KafkaListener;
//import org.springframework.kafka.core.KafkaTemplate;
//import org.springframework.kafka.support.Acknowledgment;
//import org.springframework.kafka.support.KafkaHeaders;
//import org.springframework.messaging.handler.annotation.Header;
//import org.springframework.messaging.handler.annotation.Payload;
//import org.springframework.stereotype.Service;
//
//import java.util.*;
//import java.util.stream.Collectors;
//
///**
// * Kafka listener service to handle user information requests from Notification Service
// */
//@Service
//@Slf4j
//@RequiredArgsConstructor
//public class UserInfoKafkaListenerService {
//
//    private final UserService userService;
//    private final UserAddressService userAddressService;
//    private final KafkaTemplate<String, Object> kafkaTemplate;
//
//    /**
//     * Handle single user email requests
//     */
//    @KafkaListener(topics = KafkaConfig.TOPIC_USER_EMAIL_REQUEST, groupId = "user-service-email-requests")
//    public void handleUserEmailRequest(@Payload UserEmailRequest request,
//                                       @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
//                                       Acknowledgment acknowledgment) {
//        log.info("Received user email request: {} for user: {}", request.getRequestId(), request.getUserId());
//
//        try {
//            UserEmailResponse response = processUserEmailRequest(request);
//
//            // Send response back to notification service
//            kafkaTemplate.send(KafkaConfig.TOPIC_USER_EMAIL_RESPONSE, request.getUserId().toString(), response);
//            log.info("Sent user email response: {} for user: {}", request.getRequestId(), request.getUserId());
//
//            acknowledgment.acknowledge();
//        } catch (Exception e) {
//            log.error("Error processing user email request: {}", request.getRequestId(), e);
//
//            // Send error response
//            UserEmailResponse errorResponse = UserEmailResponse.error(
//                    request.getRequestId(),
//                    request.getUserId(),
//                    "Error processing request: " + e.getMessage()
//            );
//            kafkaTemplate.send(KafkaConfig.TOPIC_USER_EMAIL_RESPONSE, request.getUserId().toString(), errorResponse);
//            acknowledgment.acknowledge();
//        }
//    }
//
//    /**
//     * Handle bulk user email requests
//     */
//    @KafkaListener(topics = KafkaConfig.TOPIC_BULK_USER_EMAIL_REQUEST, groupId = "user-service-bulk-email-requests")
//    public void handleBulkUserEmailRequest(@Payload BulkUserEmailRequest request,
//                                           @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
//                                           Acknowledgment acknowledgment) {
//        log.info("Received bulk user email request: {} for {} users", request.getRequestId(), request.getUserIds().size());
//
//        try {
//            List<UserEmailResponse> responses = processBulkUserEmailRequest(request);
//            BulkUserEmailResponse bulkResponse = BulkUserEmailResponse.create(request.getRequestId(), responses);
//
//            // Send response back to notification service
//            kafkaTemplate.send(KafkaConfig.TOPIC_BULK_USER_EMAIL_RESPONSE, "bulk-" + request.getRequestId(), bulkResponse);
//            log.info("Sent bulk user email response: {} for {} users", request.getRequestId(), request.getUserIds().size());
//
//            acknowledgment.acknowledge();
//        } catch (Exception e) {
//            log.error("Error processing bulk user email request: {}", request.getRequestId(), e);
//
//            // Send error response
//            List<UserEmailResponse> errorResponses = request.getUserIds().stream()
//                    .map(userId -> UserEmailResponse.error(request.getRequestId(), String.valueOf(userId), "Error processing request: " + e.getMessage()))
//                    .collect(Collectors.toList());
//
//            BulkUserEmailResponse errorBulkResponse = BulkUserEmailResponse.create(request.getRequestId(), errorResponses);
//            kafkaTemplate.send(KafkaConfig.TOPIC_BULK_USER_EMAIL_RESPONSE, "bulk-" + request.getRequestId(), errorBulkResponse);
//            acknowledgment.acknowledge();
//        }
//    }
//
//    /**
//     * Handle single user information requests
//     */
//    @KafkaListener(topics = KafkaConfig.TOPIC_USER_INFO_REQUEST, groupId = "user-service-info-requests")
//    public void handleUserInfoRequest(@Payload UserInfoRequest request,
//                                      @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
//                                      Acknowledgment acknowledgment) {
//        log.info("Received user info request: {} for user: {}", request.getRequestId(), request.getUserId());
//
//        try {
//            UserInfoResponse response = processUserInfoRequest(request);
//
//            // Send response back to notification service
//            kafkaTemplate.send(KafkaConfig.TOPIC_USER_INFO_RESPONSE, request.getUserId().toString(), response);
//            log.info("Sent user info response: {} for user: {}", request.getRequestId(), request.getUserId());
//
//            acknowledgment.acknowledge();
//        } catch (Exception e) {
//            log.error("Error processing user info request: {}", request.getRequestId(), e);
//
//            // Send error response
//            UserInfoResponse errorResponse = UserInfoResponse.error(
//                    request.getRequestId(),
//                    request.getUserId(),
//                    "Error processing request: " + e.getMessage()
//            );
//            kafkaTemplate.send(KafkaConfig.TOPIC_USER_INFO_RESPONSE, request.getUserId().toString(), errorResponse);
//            acknowledgment.acknowledge();
//        }
//    }
//
//    /**
//     * Handle bulk user information requests
//     */
//    @KafkaListener(topics = KafkaConfig.TOPIC_BULK_USER_INFO_REQUEST, groupId = "user-service-bulk-info-requests")
//    public void handleBulkUserInfoRequest(@Payload BulkUserInfoRequest request,
//                                          @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
//                                          Acknowledgment acknowledgment) {
//        log.info("Received bulk user info request: {} for {} users", request.getRequestId(), request.getUserIds().size());
//
//        try {
//            List<UserInfoResponse> responses = processBulkUserInfoRequest(request);
//            BulkUserInfoResponse bulkResponse = BulkUserInfoResponse.create(request.getRequestId(), responses);
//
//            // Send response back to notification service
//            kafkaTemplate.send(KafkaConfig.TOPIC_BULK_USER_INFO_RESPONSE, "bulk-" + request.getRequestId(), bulkResponse);
//            log.info("Sent bulk user info response: {} for {} users", request.getRequestId(), request.getUserIds().size());
//
//            acknowledgment.acknowledge();
//        } catch (Exception e) {
//            log.error("Error processing bulk user info request: {}", request.getRequestId(), e);
//
//            // Send error response
//            List<UserInfoResponse> errorResponses = request.getUserIds().stream()
//                    .map(userId -> UserInfoResponse.error(request.getRequestId(), userId, "Error processing request: " + e.getMessage()))
//                    .collect(Collectors.toList());
//
//            BulkUserInfoResponse errorBulkResponse = BulkUserInfoResponse.create(request.getRequestId(), errorResponses);
//            kafkaTemplate.send(KafkaConfig.TOPIC_BULK_USER_INFO_RESPONSE, "bulk-" + request.getRequestId(), errorBulkResponse);
//            acknowledgment.acknowledge();
//        }
//    }
//
//    // ===================== PRIVATE HELPER METHODS =====================
//
//    /**
//     * Process single user email request
//     */
//    private UserEmailResponse processUserEmailRequest(UserEmailRequest request) {
//        Optional<User> userOptional = userService.getUserById(request.getUserId().toString());
//
//        if (userOptional.isEmpty()) {
//            return UserEmailResponse.notFound(request.getRequestId(), request.getUserId());
//        }
//
//        User user = userOptional.get();
//        return UserEmailResponse.builder()
//                .requestId(request.getRequestId())
//                .userId(request.getUserId())
//                .email(user.getEmail())
//                .firstName(extractFirstName(user.getUsername()))
//                .lastName(extractLastName(user.getUsername()))
//                .preferredLanguage("en")
//                .emailVerified(true)
//                .marketingOptIn(true)
//                .responseTime(java.time.LocalDateTime.now())
//                .status("SUCCESS")
//                .build();
//    }
//
//    /**
//     * Process bulk user email requests
//     */
//    private List<UserEmailResponse> processBulkUserEmailRequest(BulkUserEmailRequest request) {
//        List<UserEmailResponse> responses = new ArrayList<>();
//
//        for (UUID userId : request.getUserIds()) {
//            UserEmailRequest singleRequest = UserEmailRequest.builder()
//                    .requestId(request.getRequestId())
//                    .userId(String.valueOf(userId))
//                    .requestingService(request.getRequestingService())
//                    .requestTime(request.getRequestTime())
//                    .purpose(request.getPurpose())
//                    .build();
//
//            responses.add(processUserEmailRequest(singleRequest));
//        }
//
//        return responses;
//    }
//
//    /**
//     * Process single user information request
//     */
//    private UserInfoResponse processUserInfoRequest(UserInfoRequest request) {
//        Optional<User> userOptional = userService.getUserById(request.getUserId().toString());
//
//        if (userOptional.isEmpty()) {
//            return UserInfoResponse.notFound(request.getRequestId(), request.getUserId());
//        }
//
//        User user = userOptional.get();
//        UserInfoResponse.UserInfoResponseBuilder responseBuilder = UserInfoResponse.builder()
//                .requestId(request.getRequestId())
//                .userId(request.getUserId())
//                .username(user.getUsername())
//                .email(user.getEmail())
//                .firstName(extractFirstName(user.getUsername()))
//                .lastName(extractLastName(user.getUsername()))
//                .status(user.getStatus())
//                .preferredLanguage("en")
//                .emailVerified(true)
//                .marketingOptIn(true)
//                .responseTime(java.time.LocalDateTime.now())
//                .status_response("SUCCESS");
//
//        // Include roles if requested
//        if (request.isIncludeRoles() && user.getRoles() != null) {
//            Set<String> roleNames = user.getRoles().stream()
//                    .map(role -> role.getName().name())
//                    .collect(Collectors.toSet());
//            responseBuilder.roles(roleNames);
//        }
//
//        // Include addresses if requested
//        if (request.isIncludeAddresses()) {
//            List<UserAddress> addresses = userAddressService.getAddressesByUserId(user.getId());
//            List<UserInfoResponse.UserAddressDTO> addressDTOs = addresses.stream()
//                    .map(UserInfoResponse.UserAddressDTO::fromUserAddress)
//                    .collect(Collectors.toList());
//            responseBuilder.addresses(addressDTOs);
//
//            // Find default address
//            Optional<UserAddress> defaultAddress = userAddressService.getDefaultAddress(user.getId());
//            if (defaultAddress.isPresent()) {
//                responseBuilder.defaultAddress(UserInfoResponse.UserAddressDTO.fromUserAddress(defaultAddress.get()));
//            }
//        }
//
//        return responseBuilder.build();
//    }
//
//    /**
//     * Process bulk user information requests
//     */
//    private List<UserInfoResponse> processBulkUserInfoRequest(BulkUserInfoRequest request) {
//        List<UserInfoResponse> responses = new ArrayList<>();
//
//        for (UUID userId : request.getUserIds()) {
//            UserInfoRequest singleRequest = UserInfoRequest.builder()
//                    .requestId(request.getRequestId())
//                    .userId(userId)
//                    .requestingService(request.getRequestingService())
//                    .requestTime(request.getRequestTime())
//                    .purpose(request.getPurpose())
//                    .includeAddresses(request.isIncludeAddresses())
//                    .includeRoles(request.isIncludeRoles())
//                    .build();
//
//            responses.add(processUserInfoRequest(singleRequest));
//        }
//
//        return responses;
//    }
//
//    // ===================== UTILITY METHODS =====================
//
//    /**
//     * Extract first name from username (simple implementation)
//     */
//    private String extractFirstName(String username) {
//        if (username == null || username.trim().isEmpty()) {
//            return "";
//        }
//        String[] parts = username.split("\\s+");
//        return parts[0];
//    }
//
//    /**
//     * Extract last name from username (simple implementation)
//     */
//    private String extractLastName(String username) {
//        if (username == null || username.trim().isEmpty()) {
//            return "";
//        }
//        String[] parts = username.split("\\s+");
//        return parts.length > 1 ? parts[parts.length - 1] : "";
//    }
//}