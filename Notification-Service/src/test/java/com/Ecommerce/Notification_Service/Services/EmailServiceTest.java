package com.Ecommerce.Notification_Service.Services;

import com.Ecommerce.Notification_Service.Models.NotificationType;
import com.Ecommerce.Notification_Service.Payload.Kafka.UserInfoResponse;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    private MimeMessage mimeMessage;
    private UserInfoResponse testUserInfo;

    @BeforeEach
    void setUp() {
        // Set up email configuration properties
        ReflectionTestUtils.setField(emailService, "fromEmail", "test@example.com");
        ReflectionTestUtils.setField(emailService, "fromName", "Test Service");
        ReflectionTestUtils.setField(emailService, "baseUrl", "http://localhost:3000");

        // Create mock MimeMessage
        mimeMessage = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // Create test user info
        testUserInfo = UserInfoResponse.builder()
                .userId(UUID.randomUUID())
                .email("testuser@example.com")
                .firstName("John")
                .lastName("Doe")
                .emailVerified(true)
                .marketingOptIn(true)
                .status_response("SUCCESS")
                .build();
    }

    @Test
    void sendEmailToUser_WithValidData_ShouldSendEmail() {
        // Given
        String toEmail = "test@example.com";
        String subject = "Test Subject";
        String content = "Test content";
        NotificationType type = NotificationType.ORDER_STATUS;

        // When
        emailService.sendEmailToUser(toEmail, subject, content, type);

        // Then
        verify(mailSender).createMimeMessage();
        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendEmailToUser_WithMailException_ShouldThrowRuntimeException() {
        // Given
        String toEmail = "test@example.com";
        String subject = "Test Subject";
        String content = "Test content";
        NotificationType type = NotificationType.ORDER_STATUS;

        doThrow(new MailException("SMTP error") {}).when(mailSender).send(any(MimeMessage.class));

        // When & Then
        assertThatThrownBy(() -> emailService.sendEmailToUser(toEmail, subject, content, type))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Failed to send email");
    }

    @Test
    void sendEmailToUserWithInfo_WithValidUserInfo_ShouldSendEnhancedEmail() {
        // Given
        String subject = "Test Subject";
        String content = "Test content";
        NotificationType type = NotificationType.PAYMENT_CONFIRMATION;
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("orderId", "ORDER-123");

        // When
        emailService.sendEmailToUserWithInfo(testUserInfo, subject, content, type, templateData);

        // Then
        verify(mailSender).createMimeMessage();
        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendEmailToUserWithInfo_WithNullUserInfo_ShouldNotSendEmail() {
        // Given
        String subject = "Test Subject";
        String content = "Test content";
        NotificationType type = NotificationType.ORDER_STATUS;
        Map<String, Object> templateData = new HashMap<>();

        // When
        emailService.sendEmailToUserWithInfo(null, subject, content, type, templateData);

        // Then
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void sendPaymentConfirmationEmailWithUserInfo_WithValidData_ShouldSendEmail() {
        // Given
        String orderId = "ORDER-123";
        String amount = "$99.99";
        String paymentMethod = "Credit Card";
        Map<String, Object> templateData = new HashMap<>();

        // When
        emailService.sendPaymentConfirmationEmailWithUserInfo(testUserInfo, orderId, amount, paymentMethod, templateData);

        // Then
        verify(mailSender).createMimeMessage();
        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendShippingUpdateEmailWithUserInfo_WithValidData_ShouldSendEmail() {
        // Given
        String orderId = "ORDER-123";
        String status = "SHIPPED";
        String trackingNumber = "1Z999AA1234567890";
        Map<String, Object> templateData = new HashMap<>();

        // When
        emailService.sendShippingUpdateEmailWithUserInfo(testUserInfo, orderId, status, trackingNumber, templateData);

        // Then
        verify(mailSender).createMimeMessage();
        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendDeliveryConfirmationEmailWithUserInfo_WithValidData_ShouldSendEmail() {
        // Given
        String orderId = "ORDER-123";
        String deliveryDate = "2024-01-15";
        Map<String, Object> templateData = new HashMap<>();

        // When
        emailService.sendDeliveryConfirmationEmailWithUserInfo(testUserInfo, orderId, deliveryDate, templateData);

        // Then
        verify(mailSender).createMimeMessage();
        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendLoyaltyPointsEmailWithUserInfo_WithValidData_ShouldSendEmail() {
        // Given
        int pointsEarned = 100;
        int totalPoints = 1500;
        String reason = "Purchase reward";
        Map<String, Object> templateData = new HashMap<>();

        // When
        emailService.sendLoyaltyPointsEmailWithUserInfo(testUserInfo, pointsEarned, totalPoints, reason, templateData);

        // Then
        verify(mailSender).createMimeMessage();
        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendTierUpgradeEmailWithUserInfo_WithValidData_ShouldSendEmail() {
        // Given
        String newTier = "GOLD";
        String oldTier = "SILVER";
        Map<String, Object> templateData = new HashMap<>();

        // When
        emailService.sendTierUpgradeEmailWithUserInfo(testUserInfo, newTier, oldTier, templateData);

        // Then
        verify(mailSender).createMimeMessage();
        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendBulkPromotionalEmailWithUserInfo_WithValidData_ShouldSendEmailsAsync() {
        // Given
        Map<UUID, UserInfoResponse> userInfoMap = new HashMap<>();
        userInfoMap.put(testUserInfo.getUserId(), testUserInfo);

        String title = "Special Promotion";
        String message = "Limited time offer!";
        String promoCode = "SAVE20";
        String validUntil = "2024-12-31";

        // When
        CompletableFuture<Void> future = emailService.sendBulkPromotionalEmailWithUserInfo(
                userInfoMap, title, message, promoCode, validUntil);

        // Wait for completion
        future.join();

        // Then
        verify(mailSender, times(1)).createMimeMessage();
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    void sendBulkPromotionalEmailWithUserInfo_WithNonOptInUser_ShouldSkipEmail() {
        // Given
        UserInfoResponse nonOptInUser = testUserInfo.toBuilder()
                .marketingOptIn(false)
                .build();

        Map<UUID, UserInfoResponse> userInfoMap = new HashMap<>();
        userInfoMap.put(nonOptInUser.getUserId(), nonOptInUser);

        String title = "Special Promotion";
        String message = "Limited time offer!";
        String promoCode = "SAVE20";
        String validUntil = "2024-12-31";

        // When
        CompletableFuture<Void> future = emailService.sendBulkPromotionalEmailWithUserInfo(
                userInfoMap, title, message, promoCode, validUntil);

        // Wait for completion
        future.join();

        // Then
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void sendTestEmail_WithValidEmail_ShouldSendTestEmail() {
        // Given
        String testEmail = "test@example.com";

        // When
        emailService.sendTestEmail(testEmail);

        // Then
        verify(mailSender).createMimeMessage();
        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendTestEmail_WithMailException_ShouldThrowRuntimeException() {
        // Given
        String testEmail = "test@example.com";
        doThrow(new MailException("SMTP error") {}).when(mailSender).send(any(MimeMessage.class));

        // When & Then
        assertThatThrownBy(() -> emailService.sendTestEmail(testEmail))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Failed to send test email: SMTP error");
    }

    @Test
    void init_ShouldValidateEmailConfiguration() {
        // When
        emailService.init();

        // Then
        verify(mailSender).createMimeMessage();
    }

    @Test
    void sendEmailToUserWithInfo_WithUserInfoWithAddress_ShouldIncludeAddressInEmail() {
        // Given
        UserInfoResponse.UserAddressDTO address = UserInfoResponse.UserAddressDTO.builder()
                .street("123 Main St")
                .city("New York")
                .state("NY")
                .zipCode("10001")
                .country("USA")
                .isDefault(true)
                .build();

        UserInfoResponse userWithAddress = testUserInfo.toBuilder()
                .defaultAddress(address)
                .build();

        String subject = "Test Subject";
        String content = "Test content";
        NotificationType type = NotificationType.SHIPPING_UPDATE;
        Map<String, Object> templateData = new HashMap<>();

        // When
        emailService.sendEmailToUserWithInfo(userWithAddress, subject, content, type, templateData);

        // Then
        verify(mailSender).createMimeMessage();
        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendEmailToUserWithInfo_WithEmptyEmail_ShouldNotSendEmail() {
        // Given
        UserInfoResponse userWithoutEmail = testUserInfo.toBuilder()
                .email("")
                .build();

        String subject = "Test Subject";
        String content = "Test content";
        NotificationType type = NotificationType.ORDER_STATUS;
        Map<String, Object> templateData = new HashMap<>();

        // When
        emailService.sendEmailToUserWithInfo(userWithoutEmail, subject, content, type, templateData);

        // Then
        verify(mailSender, never()).send(any(MimeMessage.class));
    }
}