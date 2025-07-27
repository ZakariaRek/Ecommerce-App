package com.Ecommerce.Notification_Service.Services;

import com.Ecommerce.Notification_Service.Models.Notification;
import com.Ecommerce.Notification_Service.Models.NotificationType;
import com.Ecommerce.Notification_Service.Payload.Kafka.UserInfoResponse;
import jakarta.annotation.PostConstruct;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${notification.email.from:noreply@yourdomain.com}")
    private String fromEmail;

    @Value("${notification.email.from-name:YourCompany Notifications}")
    private String fromName;

    @Value("${notification.email.templates.base-url:http://localhost:3000}")
    private String baseUrl;

    @PostConstruct
    public void init() {
        log.info("EmailService initialized with:");
        log.info("From Email: {}", fromEmail);
        log.info("From Name: {}", fromName);
        log.info("Base URL: {}", baseUrl);

        // Test email configuration
        try {
            mailSender.createMimeMessage();
            log.info("Email configuration is valid");
        } catch (Exception e) {
            log.error("Email configuration error: {}", e.getMessage());
        }
    }

    /**
     * Send basic email without user info (fallback method)
     */
    @Retryable(retryFor = {MailException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public void sendEmailToUser(String toEmail, String subject, String content, NotificationType type) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject(subject);

            // Generate basic HTML content
            String htmlContent = generateBasicEmailContent(content, type);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Basic email sent successfully to: {} for type: {}", toEmail, type);

        } catch (Exception e) {
            log.error("Failed to send basic email to: {} for type: {}", toEmail, type, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    /**
     * Send email with enhanced user information and template data
     */
    @Retryable(retryFor = {MailException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public void sendEmailToUserWithInfo(UserInfoResponse userInfo, String subject, String content,
                                        NotificationType type, Map<String, Object> templateData) {
        if (userInfo == null || userInfo.getEmail() == null || userInfo.getEmail().isEmpty()) {
            log.warn("Cannot send email - user info or email is null/empty");
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // Validate and set sender
            if (fromEmail == null || fromEmail.isEmpty()) {
                throw new IllegalStateException("From email is not configured");
            }

            helper.setFrom(fromEmail, fromName);
            helper.setTo(userInfo.getEmail());
            helper.setSubject(subject);

            // Generate HTML content with enhanced user information
            String htmlContent = generateEnhancedEmailContent(content, type, userInfo, templateData);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Enhanced email sent successfully to: {} ({}) for type: {}",
                    userInfo.getEmail(), userInfo.getFullName(), type);

        } catch (Exception e) {
            log.error("Failed to send enhanced email to: {} for type: {}", userInfo.getEmail(), type, e);
            throw new RuntimeException("Failed to send enhanced email", e);
        }
    }

    /**
     * Send payment confirmation email with user information
     */
    @Retryable(retryFor = {MailException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public void sendPaymentConfirmationEmailWithUserInfo(UserInfoResponse userInfo, String orderId,
                                                         String amount, String paymentMethod,
                                                         Map<String, Object> templateData) {
        if (userInfo == null || userInfo.getEmail() == null) {
            log.warn("Cannot send payment confirmation email - user info is null");
            return;
        }

        try {
            String subject = "💳 Payment Confirmed - Order #" + orderId;
            String htmlContent = generateEnhancedPaymentConfirmationTemplate(userInfo, orderId, amount, paymentMethod, templateData);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(userInfo.getEmail());
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Enhanced payment confirmation email sent to: {} for order: {}", userInfo.getEmail(), orderId);

        } catch (Exception e) {
            log.error("Failed to send enhanced payment confirmation email to: {} for order: {}", userInfo.getEmail(), orderId, e);
            throw new RuntimeException("Failed to send enhanced payment confirmation email", e);
        }
    }

    /**
     * Send shipping update email with user information and address
     */
    @Retryable(retryFor = {MailException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public void sendShippingUpdateEmailWithUserInfo(UserInfoResponse userInfo, String orderId,
                                                    String status, String trackingNumber,
                                                    Map<String, Object> templateData) {
        if (userInfo == null || userInfo.getEmail() == null) {
            log.warn("Cannot send shipping update email - user info is null");
            return;
        }

        try {
            String subject = "🚚 Shipping Update - Order #" + orderId;
            String htmlContent = generateEnhancedShippingUpdateTemplate(userInfo, orderId, status, trackingNumber, templateData);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(userInfo.getEmail());
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Enhanced shipping update email sent to: {} for order: {}", userInfo.getEmail(), orderId);

        } catch (Exception e) {
            log.error("Failed to send enhanced shipping update email to: {} for order: {}", userInfo.getEmail(), orderId, e);
            throw new RuntimeException("Failed to send enhanced shipping update email", e);
        }
    }

    /**
     * Send delivery confirmation email with user information
     */
    @Retryable(retryFor = {MailException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public void sendDeliveryConfirmationEmailWithUserInfo(UserInfoResponse userInfo, String orderId,
                                                          String deliveryDate, Map<String, Object> templateData) {
        if (userInfo == null || userInfo.getEmail() == null) {
            log.warn("Cannot send delivery confirmation email - user info is null");
            return;
        }

        try {
            String subject = "📦 Package Delivered - Order #" + orderId;
            String htmlContent = generateEnhancedDeliveryConfirmationTemplate(userInfo, orderId, deliveryDate, templateData);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(userInfo.getEmail());
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Enhanced delivery confirmation email sent to: {} for order: {}", userInfo.getEmail(), orderId);

        } catch (Exception e) {
            log.error("Failed to send enhanced delivery confirmation email to: {} for order: {}", userInfo.getEmail(), orderId, e);
            throw new RuntimeException("Failed to send enhanced delivery confirmation email", e);
        }
    }

    /**
     * Send loyalty points email with user information
     */
    @Retryable(retryFor = {MailException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public void sendLoyaltyPointsEmailWithUserInfo(UserInfoResponse userInfo, int pointsEarned,
                                                   int totalPoints, String reason,
                                                   Map<String, Object> templateData) {
        if (userInfo == null || userInfo.getEmail() == null) {
            log.warn("Cannot send loyalty points email - user info is null");
            return;
        }

        try {
            String subject = "🌟 You've Earned " + pointsEarned + " Loyalty Points!";
            String htmlContent = generateEnhancedLoyaltyPointsTemplate(userInfo, pointsEarned, totalPoints, reason, templateData);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(userInfo.getEmail());
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Enhanced loyalty points email sent to: {} - {} points earned", userInfo.getEmail(), pointsEarned);

        } catch (Exception e) {
            log.error("Failed to send enhanced loyalty points email to: {}", userInfo.getEmail(), e);
            throw new RuntimeException("Failed to send enhanced loyalty points email", e);
        }
    }

    /**
     * Send tier upgrade email with user information
     */
    @Retryable(retryFor = {MailException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public void sendTierUpgradeEmailWithUserInfo(UserInfoResponse userInfo, String newTier,
                                                 String oldTier, Map<String, Object> templateData) {
        if (userInfo == null || userInfo.getEmail() == null) {
            log.warn("Cannot send tier upgrade email - user info is null");
            return;
        }

        try {
            String subject = "🎉 Loyalty Tier Upgraded to " + newTier + "!";
            String htmlContent = generateEnhancedTierUpgradeTemplate(userInfo, newTier, oldTier, templateData);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(userInfo.getEmail());
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Enhanced tier upgrade email sent to: {} - upgraded to {}", userInfo.getEmail(), newTier);

        } catch (Exception e) {
            log.error("Failed to send enhanced tier upgrade email to: {}", userInfo.getEmail(), e);
            throw new RuntimeException("Failed to send enhanced tier upgrade email", e);
        }
    }

    /**
     * Send bulk promotional emails with user information
     */
    public CompletableFuture<Void> sendBulkPromotionalEmailWithUserInfo(Map<UUID, UserInfoResponse> userInfoMap,
                                                                        String title, String message,
                                                                        String promoCode, String validUntil) {
        return CompletableFuture.runAsync(() -> {
            log.info("Starting bulk promotional email send to {} recipients", userInfoMap.size());

            int successCount = 0;
            int failureCount = 0;

            for (Map.Entry<UUID, UserInfoResponse> entry : userInfoMap.entrySet()) {
                UserInfoResponse userInfo = entry.getValue();

                // Skip users who haven't opted in for marketing emails
                if (!userInfo.isMarketingOptIn() || !userInfo.isEmailVerified()) {
                    log.debug("Skipping promotional email for user {} - not opted in or email not verified", userInfo.getUserId());
                    continue;
                }

                try {
                    sendPromotionalEmailWithUserInfo(userInfo, title, message, promoCode, validUntil);
                    successCount++;
                } catch (Exception e) {
                    failureCount++;
                    log.error("Failed to send promotional email to: {}", userInfo.getEmail(), e);
                }
            }

            log.info("Bulk promotional email completed. Success: {}, Failures: {}", successCount, failureCount);
        });
    }

    /**
     * Send individual promotional email with user information
     */
    @Retryable(retryFor = {MailException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    private void sendPromotionalEmailWithUserInfo(UserInfoResponse userInfo, String title, String message,
                                                  String promoCode, String validUntil) {
        try {
            String subject = "🎉 " + title + " - Special Offer for " + userInfo.getFullName() + "!";

            Map<String, Object> templateData = new HashMap<>();
            templateData.put("userName", userInfo.getFullName());
            templateData.put("userEmail", userInfo.getEmail());
            templateData.put("title", title);
            templateData.put("message", message);
            templateData.put("promoCode", promoCode);
            templateData.put("validUntil", validUntil);

            String htmlContent = generateEnhancedPromotionalEmailTemplate(userInfo, title, message, promoCode, validUntil, templateData);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(userInfo.getEmail());
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
            log.debug("Enhanced promotional email sent to: {} ({})", userInfo.getEmail(), userInfo.getFullName());

        } catch (Exception e) {
            log.error("Failed to send enhanced promotional email to: {}", userInfo.getEmail(), e);
            throw new RuntimeException("Failed to send enhanced promotional email", e);
        }
    }

    /**
     * Test email configuration
     */
    public void sendTestEmail(String toEmail) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject("Test Email - Configuration Verification");
            helper.setText("<h1>Email Configuration Test</h1><p>If you receive this email, your configuration is working correctly!</p>", true);

            mailSender.send(message);
            log.info("Test email sent successfully to: {}", toEmail);

        } catch (Exception e) {
            log.error("Failed to send test email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send test email: " + e.getMessage(), e);
        }
    }

    // ==================== TEMPLATE GENERATION METHODS ====================

    /**
     * Generate basic email content (fallback)
     */
    private String generateBasicEmailContent(String content, NotificationType type) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"><title>Notification</title></head>
            <body style="font-family: Arial, sans-serif; margin: 0; padding: 20px; background-color: #f4f4f4;">
                <div style="max-width: 600px; margin: 0 auto; background-color: white; padding: 20px; border-radius: 8px;">
                    <h1>📧 %s</h1>
                    <p>%s</p>
                    <p><small>Generated at: %s</small></p>
                </div>
            </body>
            </html>
            """,
                type.name(),
                content,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        );
    }

    /**
     * Generate enhanced email content with user information
     */
    private String generateEnhancedEmailContent(String content, NotificationType type,
                                                UserInfoResponse userInfo, Map<String, Object> templateData) {
        return switch (type) {
            case PAYMENT_CONFIRMATION -> generateEnhancedPaymentConfirmationTemplate(
                    userInfo,
                    getFromTemplateData(templateData, "orderId", "N/A"),
                    getFromTemplateData(templateData, "amount", "N/A"),
                    getFromTemplateData(templateData, "paymentMethod", "N/A"),
                    templateData
            );
            case SHIPPING_UPDATE -> generateEnhancedShippingUpdateTemplate(
                    userInfo,
                    getFromTemplateData(templateData, "orderId", "N/A"),
                    getFromTemplateData(templateData, "status", "Updated"),
                    getFromTemplateData(templateData, "trackingNumber", ""),
                    templateData
            );
            case PROMOTION -> generateEnhancedPromotionalEmailTemplate(
                    userInfo,
                    getFromTemplateData(templateData, "title", "Special Offer"),
                    content,
                    getFromTemplateData(templateData, "promoCode", ""),
                    getFromTemplateData(templateData, "validUntil", ""),
                    templateData
            );
            case ACCOUNT_ACTIVITY -> generateEnhancedAccountActivityTemplate(
                    userInfo,
                    getFromTemplateData(templateData, "activityType", "Activity"),
                    content,
                    templateData
            );
            default -> generateEnhancedGenericEmailTemplate(userInfo, content, type.name(), templateData);
        };
    }

    /**
     * Generate Enhanced Payment Confirmation Email Template
     */
    private String generateEnhancedPaymentConfirmationTemplate(UserInfoResponse userInfo, String orderId,
                                                               String amount, String paymentMethod,
                                                               Map<String, Object> templateData) {
        String shippingAddress = userInfo.getFormattedDefaultAddress();

        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Payment Confirmation</title>
                <style>
                    body { font-family: Arial, sans-serif; margin: 0; padding: 20px; background-color: #f4f4f4; }
                    .container { max-width: 600px; margin: 0 auto; background-color: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
                    .header { text-align: center; border-bottom: 2px solid #27ae60; padding-bottom: 20px; margin-bottom: 20px; }
                    .success { background-color: #27ae60; color: white; padding: 15px; border-radius: 5px; text-align: center; margin: 15px 0; }
                    .payment-details { background-color: #f8f9fa; padding: 15px; border-radius: 5px; margin: 15px 0; }
                    .user-info { background-color: #e8f4fd; padding: 15px; border-radius: 5px; margin: 15px 0; border-left: 4px solid #3498db; }
                    .footer { text-align: center; margin-top: 30px; padding-top: 20px; border-top: 1px solid #eee; color: #666; font-size: 12px; }
                    .button { display: inline-block; background-color: #27ae60; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px; margin: 10px 0; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>💳 Payment Confirmed</h1>
                    </div>
                    <div class="content">
                        <div class="success">
                            <h2>✅ Payment Successful!</h2>
                        </div>
                        
                        <div class="user-info">
                            <h3>Hello %s!</h3>
                            <p>Thank you for your purchase. We're processing your order now.</p>
                        </div>
                        
                        <div class="payment-details">
                            <p><strong>Order ID:</strong> %s</p>
                            <p><strong>Amount Paid:</strong> %s</p>
                            <p><strong>Payment Method:</strong> %s</p>
                            %s
                        </div>
                        
                        <p>Your order will be processed shortly and you'll receive a shipping notification once it's on its way.</p>
                        
                        <a href="%s/orders/%s" class="button">View Order Details</a>
                    </div>
                    <div class="footer">
                        <p>This is an automated message from the ECommerce System.</p>
                        <p>Keep this email for your records.</p>
                        <p>Generated at: %s</p>
                    </div>
                </div>
            </body>
            </html>
            """,
                userInfo.getFullName(),
                orderId,
                amount,
                paymentMethod,
                shippingAddress != null && !shippingAddress.equals("No address on file") ?
                        "<p><strong>Shipping Address:</strong> " + shippingAddress + "</p>" : "",
                baseUrl,
                orderId,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        );
    }

    /**
     * Generate Enhanced Shipping Update Email Template
     */
    private String generateEnhancedShippingUpdateTemplate(UserInfoResponse userInfo, String orderId,
                                                          String status, String trackingNumber,
                                                          Map<String, Object> templateData) {
        String deliveryAddress = userInfo.getFormattedDefaultAddress();

        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Shipping Update</title>
                <style>
                    body { font-family: Arial, sans-serif; margin: 0; padding: 20px; background-color: #f4f4f4; }
                    .container { max-width: 600px; margin: 0 auto; background-color: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
                    .header { text-align: center; border-bottom: 2px solid #8e44ad; padding-bottom: 20px; margin-bottom: 20px; }
                    .shipping-status { background-color: #8e44ad; color: white; padding: 15px; border-radius: 5px; text-align: center; margin: 15px 0; }
                    .tracking-info { background-color: #f8f9fa; padding: 15px; border-radius: 5px; margin: 15px 0; }
                    .user-info { background-color: #e8f4fd; padding: 15px; border-radius: 5px; margin: 15px 0; border-left: 4px solid #3498db; }
                    .footer { text-align: center; margin-top: 30px; padding-top: 20px; border-top: 1px solid #eee; color: #666; font-size: 12px; }
                    .button { display: inline-block; background-color: #8e44ad; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px; margin: 10px 0; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🚚 Shipping Update</h1>
                    </div>
                    <div class="content">
                        <div class="user-info">
                            <h3>Hello %s!</h3>
                            <p>We have an update on your shipment.</p>
                        </div>
                        
                        <div class="tracking-info">
                            <p><strong>Order ID:</strong> %s</p>
                            <div class="shipping-status">
                                <h3>%s</h3>
                            </div>
                            %s
                            %s
                        </div>
                        
                        <p>Thank you for your patience!</p>
                        
                        <a href="%s/orders/%s" class="button">Track Package</a>
                    </div>
                    <div class="footer">
                        <p>This is an automated message from the ECommerce System.</p>
                        <p>Delivery estimates may vary based on location and carrier.</p>
                        <p>Generated at: %s</p>
                    </div>
                </div>
            </body>
            </html>
            """,
                userInfo.getFullName(),
                orderId,
                status,
                trackingNumber != null && !trackingNumber.isEmpty() ?
                        "<p><strong>Tracking Number:</strong> " + trackingNumber + "</p>" : "",
                deliveryAddress != null && !deliveryAddress.equals("No address on file") ?
                        "<p><strong>Delivery Address:</strong> " + deliveryAddress + "</p>" : "",
                baseUrl,
                orderId,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        );
    }

    /**
     * Generate Enhanced Delivery Confirmation Template
     */
    private String generateEnhancedDeliveryConfirmationTemplate(UserInfoResponse userInfo, String orderId,
                                                                String deliveryDate, Map<String, Object> templateData) {
        String deliveryAddress = userInfo.getFormattedDefaultAddress();

        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Package Delivered</title>
                <style>
                    body { font-family: Arial, sans-serif; margin: 0; padding: 20px; background-color: #f4f4f4; }
                    .container { max-width: 600px; margin: 0 auto; background-color: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
                    .header { text-align: center; border-bottom: 2px solid #27ae60; padding-bottom: 20px; margin-bottom: 20px; }
                    .delivered { background-color: #27ae60; color: white; padding: 15px; border-radius: 5px; text-align: center; margin: 15px 0; }
                    .delivery-info { background-color: #f8f9fa; padding: 15px; border-radius: 5px; margin: 15px 0; }
                    .user-info { background-color: #e8f4fd; padding: 15px; border-radius: 5px; margin: 15px 0; border-left: 4px solid #3498db; }
                    .footer { text-align: center; margin-top: 30px; padding-top: 20px; border-top: 1px solid #eee; color: #666; font-size: 12px; }
                    .button { display: inline-block; background-color: #27ae60; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px; margin: 10px 0; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>📦 Package Delivered</h1>
                    </div>
                    <div class="content">
                        <div class="user-info">
                            <h3>Hello %s!</h3>
                            <p>Great news! Your package has been delivered.</p>
                        </div>
                        
                        <div class="delivered">
                            <h2>✅ Successfully Delivered!</h2>
                        </div>
                        
                        <div class="delivery-info">
                            <p><strong>Order ID:</strong> %s</p>
                            %s
                            %s
                        </div>
                        
                        <p>We hope you enjoy your purchase! If you have any issues with your order, please don't hesitate to contact our customer service team.</p>
                        
                        <a href="%s/orders/%s" class="button">Leave a Review</a>
                    </div>
                    <div class="footer">
                        <p>This is an automated message from the ECommerce System.</p>
                        <p>Thank you for shopping with us!</p>
                        <p>Generated at: %s</p>
                    </div>
                </div>
            </body>
            </html>
            """,
                userInfo.getFullName(),
                orderId,
                deliveryDate != null && !deliveryDate.isEmpty() ?
                        "<p><strong>Delivery Date:</strong> " + deliveryDate + "</p>" : "",
                deliveryAddress != null && !deliveryAddress.equals("No address on file") ?
                        "<p><strong>Delivered To:</strong> " + deliveryAddress + "</p>" : "",
                baseUrl,
                orderId,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        );
    }

    /**
     * Generate Enhanced Loyalty Points Template
     */
    private String generateEnhancedLoyaltyPointsTemplate(UserInfoResponse userInfo, int pointsEarned,
                                                         int totalPoints, String reason,
                                                         Map<String, Object> templateData) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Loyalty Points Earned</title>
                <style>
                    body { font-family: Arial, sans-serif; margin: 0; padding: 20px; background-color: #f4f4f4; }
                    .container { max-width: 600px; margin: 0 auto; background-color: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
                    .header { text-align: center; border-bottom: 2px solid #f39c12; padding-bottom: 20px; margin-bottom: 20px; }
                    .points-earned { background: linear-gradient(45deg, #f39c12, #e67e22); color: white; padding: 20px; border-radius: 8px; text-align: center; margin: 20px 0; }
                    .total-points { background-color: #ecf0f1; padding: 15px; border-radius: 5px; text-align: center; margin: 15px 0; }
                    .user-info { background-color: #e8f4fd; padding: 15px; border-radius: 5px; margin: 15px 0; border-left: 4px solid #3498db; }
                    .footer { text-align: center; margin-top: 30px; padding-top: 20px; border-top: 1px solid #eee; color: #666; font-size: 12px; }
                    .button { display: inline-block; background-color: #f39c12; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px; margin: 10px 0; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🌟 Loyalty Points Earned</h1>
                    </div>
                    <div class="content">
                        <div class="user-info">
                            <h3>Hello %s!</h3>
                            <p>You've earned more loyalty points!</p>
                        </div>
                        
                        <div class="points-earned">
                            <h2>You've Earned %d Points!</h2>
                            <p>%s</p>
                        </div>
                        
                        <div class="total-points">
                            <h3>Total Points: %d</h3>
                        </div>
                        
                        <p>Keep shopping to earn more points and unlock exclusive rewards!</p>
                        
                        <a href="%s/loyalty" class="button">View Rewards</a>
                    </div>
                    <div class="footer">
                        <p>Thank you for being a loyal customer!</p>
                        <p>Points expire after 12 months of inactivity.</p>
                        <p>Generated at: %s</p>
                    </div>
                </div>
            </body>
            </html>
            """,
                userInfo.getFullName(),
                pointsEarned,
                reason,
                totalPoints,
                baseUrl,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        );
    }

    /**
     * Generate Enhanced Tier Upgrade Template
     */
    private String generateEnhancedTierUpgradeTemplate(UserInfoResponse userInfo, String newTier,
                                                       String oldTier, Map<String, Object> templateData) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Loyalty Tier Upgrade</title>
                <style>
                    body { font-family: Arial, sans-serif; margin: 0; padding: 20px; background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); }
                    .container { max-width: 600px; margin: 0 auto; background-color: white; padding: 20px; border-radius: 8px; box-shadow: 0 4px 8px rgba(0,0,0,0.2); }
                    .header { text-align: center; border-bottom: 2px solid #9b59b6; padding-bottom: 20px; margin-bottom: 20px; }
                    .upgrade-banner { background: linear-gradient(45deg, #9b59b6, #8e44ad); color: white; padding: 20px; border-radius: 8px; text-align: center; margin: 20px 0; }
                    .tier-info { background-color: #f8f9fa; padding: 15px; border-radius: 5px; margin: 15px 0; }
                    .user-info { background-color: #e8f4fd; padding: 15px; border-radius: 5px; margin: 15px 0; border-left: 4px solid #3498db; }
                    .footer { text-align: center; margin-top: 30px; padding-top: 20px; border-top: 1px solid #eee; color: #666; font-size: 12px; }
                    .button { display: inline-block; background: linear-gradient(45deg, #9b59b6, #8e44ad); color: white; padding: 15px 30px; text-decoration: none; border-radius: 25px; margin: 15px 0; font-weight: bold; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🎉 Tier Upgrade</h1>
                    </div>
                    <div class="content">
                        <div class="user-info">
                            <h3>Congratulations %s!</h3>
                            <p>You've been upgraded to a new loyalty tier!</p>
                        </div>
                        
                        <div class="upgrade-banner">
                            <h2>Welcome to %s Tier!</h2>
                            <p>Upgraded from %s</p>
                        </div>
                        
                        <div class="tier-info">
                            <p><strong>Your New Benefits Include:</strong></p>
                            <ul>
                                <li>Exclusive member discounts</li>
                                <li>Priority customer service</li>
                                <li>Early access to sales</li>
                                <li>Enhanced reward points</li>
                            </ul>
                        </div>
                        
                        <p>Enjoy your new benefits and thank you for being a valued customer!</p>
                        
                        <a href="%s/loyalty" class="button">Explore Benefits</a>
                    </div>
                    <div class="footer">
                        <p>Thank you for your continued loyalty!</p>
                        <p>Keep shopping to maintain your tier status.</p>
                        <p>Generated at: %s</p>
                    </div>
                </div>
            </body>
            </html>
            """,
                userInfo.getFullName(),
                newTier,
                oldTier,
                baseUrl,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        );
    }

    /**
     * Generate Enhanced Promotional Email Template
     */
    private String generateEnhancedPromotionalEmailTemplate(UserInfoResponse userInfo, String title, String message,
                                                            String promoCode, String validUntil,
                                                            Map<String, Object> templateData) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Special Promotion</title>
                <style>
                    body { font-family: Arial, sans-serif; margin: 0; padding: 20px; background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); }
                    .container { max-width: 600px; margin: 0 auto; background-color: white; padding: 20px; border-radius: 8px; box-shadow: 0 4px 8px rgba(0,0,0,0.2); }
                    .header { text-align: center; border-bottom: 2px solid #e74c3c; padding-bottom: 20px; margin-bottom: 20px; }
                    .promo-banner { background: linear-gradient(45deg, #e74c3c, #f39c12); color: white; padding: 20px; border-radius: 8px; text-align: center; margin: 20px 0; }
                    .promo-code { background-color: #2c3e50; color: #ecf0f1; padding: 15px; border-radius: 5px; text-align: center; font-size: 18px; font-weight: bold; margin: 15px 0; letter-spacing: 2px; }
                    .user-greeting { background-color: #e8f4fd; padding: 15px; border-radius: 5px; margin: 15px 0; border-left: 4px solid #3498db; }
                    .footer { text-align: center; margin-top: 30px; padding-top: 20px; border-top: 1px solid #eee; color: #666; font-size: 12px; }
                    .button { display: inline-block; background: linear-gradient(45deg, #e74c3c, #f39c12); color: white; padding: 15px 30px; text-decoration: none; border-radius: 25px; margin: 15px 0; font-weight: bold; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🎉 Special Promotion</h1>
                    </div>
                    <div class="content">
                        <div class="user-greeting">
                            <h3>Hello %s!</h3>
                            <p>We have a special offer just for you!</p>
                        </div>
                        
                        <div class="promo-banner">
                            <h2>%s</h2>
                        </div>
                        
                        <p>%s</p>
                        
                        %s
                        
                        %s
                        
                        <div style="text-align: center;">
                            <a href="%s/products" class="button">Shop Now</a>
                        </div>
                    </div>
                    <div class="footer">
                        <p>This offer is exclusive to our valued customers.</p>
                        <p>Terms and conditions apply. Valid for one-time use only.</p>
                        <p>Generated at: %s</p>
                    </div>
                </div>
            </body>
            </html>
            """,
                userInfo.getFullName(),
                title,
                message,
                promoCode != null && !promoCode.isEmpty() ? "<div class=\"promo-code\">Use Code: " + promoCode + "</div>" : "",
                validUntil != null && !validUntil.isEmpty() ? "<p><strong>Valid Until:</strong> " + validUntil + "</p>" : "",
                baseUrl,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        );
    }

    /**
     * Generate Enhanced Account Activity Template
     */
    private String generateEnhancedAccountActivityTemplate(UserInfoResponse userInfo, String activityType,
                                                           String description, Map<String, Object> templateData) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Account Activity</title>
                <style>
                    body { font-family: Arial, sans-serif; margin: 0; padding: 20px; background-color: #f4f4f4; }
                    .container { max-width: 600px; margin: 0 auto; background-color: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
                    .header { text-align: center; border-bottom: 2px solid #16a085; padding-bottom: 20px; margin-bottom: 20px; }
                    .activity-info { background-color: #16a085; color: white; padding: 15px; border-radius: 5px; text-align: center; margin: 15px 0; }
                    .description { background-color: #f8f9fa; padding: 15px; border-radius: 5px; margin: 15px 0; }
                    .user-info { background-color: #e8f4fd; padding: 15px; border-radius: 5px; margin: 15px 0; border-left: 4px solid #3498db; }
                    .footer { text-align: center; margin-top: 30px; padding-top: 20px; border-top: 1px solid #eee; color: #666; font-size: 12px; }
                    .button { display: inline-block; background-color: #16a085; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px; margin: 10px 0; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>👤 Account Activity</h1>
                    </div>
                    <div class="content">
                        <div class="user-info">
                            <h3>Hello %s!</h3>
                            <p>We wanted to notify you about recent activity on your account.</p>
                        </div>
                        
                        <div class="activity-info">
                            <h3>%s</h3>
                        </div>
                        
                        <div class="description">
                            <p>%s</p>
                        </div>
                        
                        <p>If you have any questions about this activity, please contact our support team.</p>
                        
                        <a href="%s/account" class="button">View Account</a>
                    </div>
                    <div class="footer">
                        <p>This is an automated message from the ECommerce System.</p>
                        <p>For security reasons, please verify any unexpected activity.</p>
                        <p>Generated at: %s</p>
                    </div>
                </div>
            </body>
            </html>
            """,
                userInfo.getFullName(),
                activityType,
                description,
                baseUrl,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        );
    }

    /**
     * Generate Enhanced Generic Email Template
     */
    private String generateEnhancedGenericEmailTemplate(UserInfoResponse userInfo, String content,
                                                        String type, Map<String, Object> templateData) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Notification</title>
                <style>
                    body { font-family: Arial, sans-serif; margin: 0; padding: 20px; background-color: #f4f4f4; }
                    .container { max-width: 600px; margin: 0 auto; background-color: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
                    .header { text-align: center; border-bottom: 2px solid #3498db; padding-bottom: 20px; margin-bottom: 20px; }
                    .content { line-height: 1.6; color: #333; }
                    .user-info { background-color: #e8f4fd; padding: 15px; border-radius: 5px; margin: 15px 0; border-left: 4px solid #3498db; }
                    .footer { text-align: center; margin-top: 30px; padding-top: 20px; border-top: 1px solid #eee; color: #666; font-size: 12px; }
                    .notification-type { background-color: #3498db; color: white; padding: 10px; border-radius: 5px; text-align: center; margin: 15px 0; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>📧 Notification</h1>
                    </div>
                    <div class="content">
                        <div class="user-info">
                            <h3>Hello %s!</h3>
                            <p>You have a new notification.</p>
                        </div>
                        
                        <div class="notification-type">
                            <h3>%s</h3>
                        </div>
                        <p>%s</p>
                    </div>
                    <div class="footer">
                        <p>This is an automated message from the ECommerce Notification System.</p>
                        <p>Please do not reply to this email.</p>
                        <p>Generated at: %s</p>
                    </div>
                </div>
            </body>
            </html>
            """,
                userInfo.getFullName(),
                type,
                content,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        );
    }

    // Helper method to get data from template variables
    private String getFromTemplateData(Map<String, Object> templateData, String key, String defaultValue) {
        if (templateData == null) return defaultValue;
        Object value = templateData.get(key);
        return value != null ? value.toString() : defaultValue;
    }
}