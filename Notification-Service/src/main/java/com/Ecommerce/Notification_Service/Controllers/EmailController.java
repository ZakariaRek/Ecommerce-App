package com.Ecommerce.Notification_Service.Controllers;

import com.Ecommerce.Notification_Service.Models.NotificationType;
import com.Ecommerce.Notification_Service.Payload.Response.EmailResponse;
import com.Ecommerce.Notification_Service.Services.EmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/email")
@Slf4j
@RequiredArgsConstructor
@Tag(name = "Email API", description = "Email notification endpoints for testing and operations")
public class EmailController {

    private final EmailService emailService;

    // ==================== SIMPLIFIED TESTING ENDPOINTS ====================

    /**
     * Simple send-to-one test - just provide email address
     */
    @PostMapping("/test/send-to-one")
    @Operation(summary = "Simple send to one user", description = "Send test email to one user - just provide email address")
    public ResponseEntity<EmailResponse> testSendToOne(
            @Parameter(description = "Recipient email address", example = "user@example.com")
            @RequestParam String email,
            @Parameter(description = "Notification type for testing", example = "ACCOUNT_ACTIVITY")
            @RequestParam(defaultValue = "ACCOUNT_ACTIVITY") String type) {

        log.info("Testing send-to-one email for: {} with type: {}", email, type);

        try {
            NotificationType notificationType = NotificationType.valueOf(type.toUpperCase());

            // Predefined test data based on notification type
            String subject = getTestSubject(notificationType);
            String content = getTestContent(notificationType);

            emailService.sendEmailToUser(email, subject, content, notificationType);

            return ResponseEntity.ok(EmailResponse.success("Test email sent successfully to " + email));

        } catch (IllegalArgumentException e) {
            log.error("Invalid notification type: {}", type);
            return ResponseEntity.badRequest().body(EmailResponse.failure("Invalid notification type: " + type));
        } catch (Exception e) {
            log.error("Failed to send test email to: {}", email, e);
            return ResponseEntity.status(500).body(EmailResponse.failure("Failed to send email: " + e.getMessage()));
        }
    }

    /**
     * Simple send-to-all test - just provide comma-separated email addresses
     */
    @PostMapping("/test/send-to-all")
    @Operation(summary = "Simple send to multiple users", description = "Send test email to multiple users - provide comma-separated emails")
    public ResponseEntity<EmailResponse> testSendToAll(
            @Parameter(description = "Comma-separated email addresses", example = "user1@example.com,user2@example.com")
            @RequestParam String emails,
            @Parameter(description = "Notification type for testing", example = "PROMOTION")
            @RequestParam(defaultValue = "PROMOTION") String type) {

        log.info("Testing send-to-all email for emails: {} with type: {}", emails, type);

        try {
            NotificationType notificationType = NotificationType.valueOf(type.toUpperCase());

            // Parse comma-separated emails
            List<String> emailList = Arrays.asList(emails.split(","))
                    .stream()
                    .map(String::trim)
                    .filter(email -> !email.isEmpty())
                    .toList();

            if (emailList.isEmpty()) {
                return ResponseEntity.badRequest().body(EmailResponse.failure("No valid email addresses provided"));
            }

            // Predefined test data based on notification type
            String subject = getTestSubject(notificationType);
            String content = getTestContent(notificationType);

            // Send to each email individually (since we don't have bulk method)
            CompletableFuture.runAsync(() -> {
                int successCount = 0;
                int failureCount = 0;

                for (String emailAddr : emailList) {
                    try {
                        emailService.sendEmailToUser(emailAddr, subject, content, notificationType);
                        successCount++;
                        Thread.sleep(100); // Small delay to avoid overwhelming
                    } catch (Exception e) {
                        failureCount++;
                        log.error("Failed to send email to: {}", emailAddr, e);
                    }
                }

                log.info("Bulk email completed. Success: {}, Failures: {}", successCount, failureCount);
            });

            return ResponseEntity.ok(EmailResponse.success(
                    "Bulk test email started for " + emailList.size() + " recipients"
            ));

        } catch (IllegalArgumentException e) {
            log.error("Invalid notification type: {}", type);
            return ResponseEntity.badRequest().body(EmailResponse.failure("Invalid notification type: " + type));
        } catch (Exception e) {
            log.error("Failed to start bulk test email", e);
            return ResponseEntity.status(500).body(EmailResponse.failure("Failed to send bulk email: " + e.getMessage()));
        }
    }

    /**
     * Test all email templates for one user
     */
    @PostMapping("/test/all-templates")
    @Operation(summary = "Test all email templates", description = "Send all email template types to one user for testing")
    public ResponseEntity<EmailResponse> testAllTemplates(
            @Parameter(description = "Recipient email address", example = "test@example.com")
            @RequestParam String email) {

        log.info("Testing all email templates for: {}", email);

        try {
            // Test all notification types
            NotificationType[] types = {
                    NotificationType.PAYMENT_CONFIRMATION,
                    NotificationType.SHIPPING_UPDATE,
                    NotificationType.PROMOTION,
                    NotificationType.ACCOUNT_ACTIVITY
            };

            CompletableFuture.runAsync(() -> {
                int successCount = 0;
                int failureCount = 0;

                for (NotificationType type : types) {
                    try {
                        String subject = getTestSubject(type);
                        String content = getTestContent(type);

                        emailService.sendEmailToUser(email, subject, content, type);
                        successCount++;

                        // Small delay between emails
                        Thread.sleep(2000);
                    } catch (Exception e) {
                        failureCount++;
                        log.error("Failed to send {} template to {}", type, email, e);
                    }
                }

                log.info("All templates test completed for {}. Success: {}, Failures: {}",
                        email, successCount, failureCount);
            });

            return ResponseEntity.ok(EmailResponse.success(
                    "All templates test started for " + email + " (" + types.length + " templates)"
            ));

        } catch (Exception e) {
            log.error("Failed to test all templates for: {}", email, e);
            return ResponseEntity.status(500).body(EmailResponse.failure("Failed to test templates: " + e.getMessage()));
        }
    }

    /**
     * Quick promotional email test - uses generic template
     */
    @PostMapping("/test/promotion")
    @Operation(summary = "Quick promotional email test", description = "Send promotional email test - provide comma-separated emails")
    public ResponseEntity<EmailResponse> testPromotionalEmail(
            @Parameter(description = "Comma-separated email addresses", example = "user1@example.com,user2@example.com")
            @RequestParam String emails) {

        log.info("Testing promotional email for: {}", emails);

        try {
            List<String> emailList = Arrays.asList(emails.split(","))
                    .stream()
                    .map(String::trim)
                    .filter(email -> !email.isEmpty())
                    .toList();

            if (emailList.isEmpty()) {
                return ResponseEntity.badRequest().body(EmailResponse.failure("No valid email addresses provided"));
            }

            // Predefined promotional data
            String subject = "🎉 Special Test Promotion - Limited Time Offer!";
            String content = "This is a test promotional email with special offers and discounts! Use code TEST50 for 50% off. Valid until 2024-12-31.";

            // Send promotional emails
            CompletableFuture.runAsync(() -> {
                int successCount = 0;
                int failureCount = 0;

                for (String emailAddr : emailList) {
                    try {
                        emailService.sendEmailToUser(emailAddr, subject, content, NotificationType.PROMOTION);
                        successCount++;
                        Thread.sleep(200); // Small delay
                    } catch (Exception e) {
                        failureCount++;
                        log.error("Failed to send promotional email to: {}", emailAddr, e);
                    }
                }

                log.info("Promotional email completed. Success: {}, Failures: {}", successCount, failureCount);
            });

            return ResponseEntity.ok(EmailResponse.success(
                    "Promotional test email started for " + emailList.size() + " recipients"
            ));

        } catch (Exception e) {
            log.error("Failed to send promotional test email", e);
            return ResponseEntity.status(500).body(EmailResponse.failure("Failed to send promotional email: " + e.getMessage()));
        }
    }

    // ==================== SPECIFIC EMAIL TYPE TESTS ====================

    /**
     * Test order email with sample data
     */
    @PostMapping("/test/order")
    @Operation(summary = "Test order email", description = "Send order email with sample data")
    public ResponseEntity<EmailResponse> testOrderEmail(
            @Parameter(description = "Recipient email address") @RequestParam String email,
            @Parameter(description = "Order status", example = "SHIPPED") @RequestParam(defaultValue = "SHIPPED") String status) {

        try {
            String orderId = "ORD-TEST-" + System.currentTimeMillis();
            String subject = "📦 Order Update - " + status;
            String content = String.format("Your order %s has been updated to %s status. Items: 2x Test Product, 1x Sample Item", orderId, status);

            emailService.sendEmailToUser(email, subject, content, NotificationType.ACCOUNT_ACTIVITY);
            return ResponseEntity.ok(EmailResponse.success("Order test email sent successfully"));
        } catch (Exception e) {
            log.error("Failed to send order test email", e);
            return ResponseEntity.status(500).body(EmailResponse.failure("Failed to send order email: " + e.getMessage()));
        }
    }

    /**
     * Test payment email with sample data
     */
    @PostMapping("/test/payment")
    @Operation(summary = "Test payment email", description = "Send payment confirmation with sample data")
    public ResponseEntity<EmailResponse> testPaymentEmail(
            @Parameter(description = "Recipient email address") @RequestParam String email) {

        try {
            String orderId = "ORD-TEST-" + System.currentTimeMillis();
            String subject = "💳 Payment Confirmed - Order #" + orderId;
            String content = String.format("Payment of $199.99 has been successfully processed for order #%s using Credit Card (**** 1234)", orderId);

            emailService.sendEmailToUser(email, subject, content, NotificationType.PAYMENT_CONFIRMATION);
            return ResponseEntity.ok(EmailResponse.success("Payment test email sent successfully"));
        } catch (Exception e) {
            log.error("Failed to send payment test email", e);
            return ResponseEntity.status(500).body(EmailResponse.failure("Failed to send payment email: " + e.getMessage()));
        }
    }

    /**
     * Test loyalty email with sample data
     */
    @PostMapping("/test/loyalty")
    @Operation(summary = "Test loyalty email", description = "Send loyalty points email with sample data")
    public ResponseEntity<EmailResponse> testLoyaltyEmail(
            @Parameter(description = "Recipient email address") @RequestParam String email) {

        try {
            String subject = "🌟 You've Earned 250 Loyalty Points!";
            String content = "Congratulations! You've earned 250 loyalty points from your recent purchase (Order #TEST-12345). Your total points: 2,500";

            emailService.sendEmailToUser(email, subject, content, NotificationType.ACCOUNT_ACTIVITY);
            return ResponseEntity.ok(EmailResponse.success("Loyalty test email sent successfully"));
        } catch (Exception e) {
            log.error("Failed to send loyalty test email", e);
            return ResponseEntity.status(500).body(EmailResponse.failure("Failed to send loyalty email: " + e.getMessage()));
        }
    }

    /**
     * Test shipping email with sample data
     */
    @PostMapping("/test/shipping")
    @Operation(summary = "Test shipping email", description = "Send shipping update with sample data")
    public ResponseEntity<EmailResponse> testShippingEmail(
            @Parameter(description = "Recipient email address") @RequestParam String email,
            @Parameter(description = "Shipping status", example = "IN_TRANSIT") @RequestParam(defaultValue = "IN_TRANSIT") String status) {

        try {
            String orderId = "ORD-TEST-" + System.currentTimeMillis();
            String subject = "🚚 Shipping Update - " + status;
            String content = String.format("Your order #%s is now %s. Tracking number: 1Z999AA1234567890", orderId, status);

            emailService.sendEmailToUser(email, subject, content, NotificationType.SHIPPING_UPDATE);
            return ResponseEntity.ok(EmailResponse.success("Shipping test email sent successfully"));
        } catch (Exception e) {
            log.error("Failed to send shipping test email", e);
            return ResponseEntity.status(500).body(EmailResponse.failure("Failed to send shipping email: " + e.getMessage()));
        }
    }

    /**
     * Test account activity email with sample data
     */
    @PostMapping("/test/account")
    @Operation(summary = "Test account activity email", description = "Send account activity email with sample data")
    public ResponseEntity<EmailResponse> testAccountEmail(
            @Parameter(description = "Recipient email address") @RequestParam String email) {

        try {
            String subject = "👤 Account Activity - Profile Updated";
            String content = "Your account profile has been successfully updated with new information. If this wasn't you, please contact support immediately.";

            emailService.sendEmailToUser(email, subject, content, NotificationType.ACCOUNT_ACTIVITY);
            return ResponseEntity.ok(EmailResponse.success("Account activity test email sent successfully"));
        } catch (Exception e) {
            log.error("Failed to send account test email", e);
            return ResponseEntity.status(500).body(EmailResponse.failure("Failed to send account email: " + e.getMessage()));
        }
    }

    /**
     * Test email configuration
     */
    @PostMapping("/test/config")
    @Operation(summary = "Test email configuration", description = "Send a simple test email to verify configuration")
    public ResponseEntity<EmailResponse> testEmailConfig(
            @Parameter(description = "Recipient email address") @RequestParam String email) {

        try {
            emailService.sendTestEmail(email);
            return ResponseEntity.ok(EmailResponse.success("Configuration test email sent successfully"));
        } catch (Exception e) {
            log.error("Failed to send configuration test email", e);
            return ResponseEntity.status(500).body(EmailResponse.failure("Failed to send test email: " + e.getMessage()));
        }
    }

    // ==================== UTILITY ENDPOINTS ====================

    /**
     * Get available notification types
     */
    @GetMapping("/types")
    @Operation(summary = "Get notification types", description = "Get list of available notification types for testing")
    public ResponseEntity<Map<String, Object>> getNotificationTypes() {
        return ResponseEntity.ok(Map.of(
                "availableTypes", Arrays.asList(NotificationType.values()),
                "message", "Use these types in the 'type' parameter for testing",
                "examples", Map.of(
                        "PAYMENT_CONFIRMATION", "Payment success/failure notifications",
                        "SHIPPING_UPDATE", "Shipping and delivery updates",
                        "PROMOTION", "Marketing and promotional emails",
                        "ACCOUNT_ACTIVITY", "Account and security notifications"
                )
        ));
    }

    /**
     * Get email service status
     */
    @GetMapping("/status")
    @Operation(summary = "Email service status", description = "Get the status of the email service")
    public ResponseEntity<Map<String, Object>> getEmailServiceStatus() {
        return ResponseEntity.ok(Map.of(
                "service", "Email Service",
                "status", "UP",
                "timestamp", java.time.LocalDateTime.now().toString(),
                "simplifiedEndpoints", Arrays.asList(
                        "POST /test/send-to-one?email={email}&type={type}",
                        "POST /test/send-to-all?emails={email1,email2}&type={type}",
                        "POST /test/all-templates?email={email}",
                        "POST /test/promotion?emails={email1,email2}",
                        "POST /test/order?email={email}&status={status}",
                        "POST /test/payment?email={email}",
                        "POST /test/loyalty?email={email}",
                        "POST /test/shipping?email={email}&status={status}",
                        "POST /test/account?email={email}",
                        "POST /test/config?email={email}"
                ),
                "note", "All tests use existing EmailService.sendEmailToUser() method with appropriate templates"
        ));
    }

    /**
     * Get email templates preview
     */
    @GetMapping("/templates")
    @Operation(summary = "Get email templates", description = "Preview all available email templates")
    public ResponseEntity<Map<String, Object>> getEmailTemplates() {
        Map<String, Map<String, String>> templates = new HashMap<>();

        for (NotificationType type : NotificationType.values()) {
            templates.put(type.name(), Map.of(
                    "subject", getTestSubject(type),
                    "content", getTestContent(type),
                    "description", getTypeDescription(type)
            ));
        }

        return ResponseEntity.ok(Map.of(
                "templates", templates,
                "note", "These are sample templates used for testing"
        ));
    }

    // ==================== HELPER METHODS ====================

    /**
     * Get test subject based on notification type
     */
    private String getTestSubject(NotificationType type) {
        return switch (type) {
            case PAYMENT_CONFIRMATION -> "💳 Test Payment Confirmation";
            case SHIPPING_UPDATE -> "🚚 Test Shipping Update";
            case PROMOTION -> "🎉 Test Special Promotion";
            case ACCOUNT_ACTIVITY -> "👤 Test Account Activity";
            default -> "📧 Test Notification";
        };
    }

    /**
     * Get test content based on notification type
     */
    private String getTestContent(NotificationType type) {
        return switch (type) {
            case PAYMENT_CONFIRMATION -> "This is a test payment confirmation. Payment of $199.99 has been successfully processed for order #TEST-12345.";
            case SHIPPING_UPDATE -> "This is a test shipping update. Your order #TEST-12345 is now IN_TRANSIT with tracking number 1Z999AA1234567890.";
            case PROMOTION -> "This is a test promotional notification. Get 50% off on all items with code TEST50! Limited time offer.";
            case ACCOUNT_ACTIVITY -> "This is a test account activity notification. Your profile information has been successfully updated.";
            default -> "This is a test notification from the Email Service.";
        };
    }

    /**
     * Get type description
     */
    private String getTypeDescription(NotificationType type) {
        return switch (type) {
            case PAYMENT_CONFIRMATION -> "Sent when payment is processed (success or failure)";
            case SHIPPING_UPDATE -> "Sent for shipping status changes and delivery updates";
            case PROMOTION -> "Sent for marketing campaigns and special offers";
            case ACCOUNT_ACTIVITY -> "Sent for account changes, security alerts, and general activities";
            default -> "General notification type";
        };
    }
}