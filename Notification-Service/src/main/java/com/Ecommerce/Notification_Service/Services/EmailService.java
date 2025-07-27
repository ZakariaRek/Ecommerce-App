package com.Ecommerce.Notification_Service.Services;

import com.Ecommerce.Notification_Service.Models.Notification;
import com.Ecommerce.Notification_Service.Models.NotificationType;
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
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${notification.email.from}")
    private String fromEmail;

    @Value("${notification.email.from-name}")
    private String fromName;

    @Value("${notification.email.templates.base-url}")
    private String baseUrl;

    /**
     * Send email to a specific user with automatic template selection
     */
    @Retryable(retryFor = {MailException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public void sendEmailToUser(String toEmail, String subject, String content, NotificationType type) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject(subject);

            // Generate HTML content based on notification type
            String htmlContent = generateEmailContent(content, type, null);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Email sent successfully to: {} for type: {}", toEmail, type);

        } catch (Exception e) {
            log.error("Failed to send email to: {} for type: {}", toEmail, type, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    /**
     * Send automatic email based on notification - determines template automatically
     */
    public void sendNotificationEmail(Notification notification, String userEmail) {
        String subject = generateSubject(notification.getType());

        // Create context data for the specific notification type
        Map<String, Object> templateData = createTemplateData(notification);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(userEmail);
            helper.setSubject(subject);

            // Generate HTML content with notification data
            String htmlContent = generateEmailContent(notification.getContent(), notification.getType(), templateData);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Automatic notification email sent to: {} for type: {}", userEmail, notification.getType());

        } catch (Exception e) {
            log.error("Failed to send notification email to: {} for type: {}", userEmail, notification.getType(), e);
            throw new RuntimeException("Failed to send notification email", e);
        }
    }

    /**
     * Send bulk emails to multiple users (async)
     */
    public CompletableFuture<Void> sendBulkEmails(List<String> emails, String subject, String content, NotificationType type) {
        return CompletableFuture.runAsync(() -> {
            log.info("Starting bulk email send to {} recipients for type: {}", emails.size(), type);

            int successCount = 0;
            int failureCount = 0;

            for (String email : emails) {
                try {
                    sendEmailToUser(email, subject, content, type);
                    successCount++;
                } catch (Exception e) {
                    failureCount++;
                    log.error("Failed to send bulk email to: {}", email, e);
                }
            }

            log.info("Bulk email completed. Success: {}, Failures: {}", successCount, failureCount);
        });
    }

    /**
     * Send promotional email to all users
     */
    public CompletableFuture<Void> sendPromotionalEmail(List<String> emails, String title, String message,
                                                        String promoCode, String validUntil) {
        return CompletableFuture.runAsync(() -> {
            String subject = "🎉 " + title + " - Special Offer Just for You!";

            for (String email : emails) {
                try {
                    MimeMessage mimeMessage = mailSender.createMimeMessage();
                    MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

                    helper.setFrom(fromEmail, fromName);
                    helper.setTo(email);
                    helper.setSubject(subject);

                    String htmlContent = generatePromotionalEmailTemplate(title, message, promoCode, validUntil, email);
                    helper.setText(htmlContent, true);

                    mailSender.send(mimeMessage);
                    log.debug("Promotional email sent to: {}", email);

                } catch (Exception e) {
                    log.error("Failed to send promotional email to: {}", email, e);
                }
            }
            log.info("Promotional email campaign completed for {} recipients", emails.size());
        });
    }

    /**
     * Send order-related emails
     */
    public void sendOrderEmail(String userEmail, String orderId, String status, String orderDetails) {
        try {
            String subject = generateOrderSubject(status);
            String htmlContent = generateOrderEmailTemplate(orderId, status, orderDetails);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(userEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Order email sent to: {} for order: {} with status: {}", userEmail, orderId, status);

        } catch (Exception e) {
            log.error("Failed to send order email to: {} for order: {}", userEmail, orderId, e);
            throw new RuntimeException("Failed to send order email", e);
        }
    }

    /**
     * Send payment confirmation email
     */
    public void sendPaymentConfirmationEmail(String userEmail, String orderId, String amount, String paymentMethod) {
        try {
            String subject = "💳 Payment Confirmed - Order #" + orderId;
            String htmlContent = generatePaymentConfirmationTemplate(orderId, amount, paymentMethod);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(userEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Payment confirmation email sent to: {} for order: {}", userEmail, orderId);

        } catch (Exception e) {
            log.error("Failed to send payment confirmation email to: {} for order: {}", userEmail, orderId, e);
            throw new RuntimeException("Failed to send payment confirmation email", e);
        }
    }

    /**
     * Send loyalty points email
     */
    public void sendLoyaltyPointsEmail(String userEmail, int pointsEarned, int totalPoints, String reason) {
        try {
            String subject = "🌟 You've Earned " + pointsEarned + " Loyalty Points!";
            String htmlContent = generateLoyaltyPointsTemplate(pointsEarned, totalPoints, reason);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(userEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Loyalty points email sent to: {} - {} points earned", userEmail, pointsEarned);

        } catch (Exception e) {
            log.error("Failed to send loyalty points email to: {}", userEmail, e);
            throw new RuntimeException("Failed to send loyalty points email", e);
        }
    }

    /**
     * Send shipping update email
     */
    public void sendShippingUpdateEmail(String userEmail, String orderId, String status, String trackingNumber) {
        try {
            String subject = "🚚 Shipping Update - Order #" + orderId;
            String htmlContent = generateShippingUpdateTemplate(orderId, status, trackingNumber);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(userEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Shipping update email sent to: {} for order: {}", userEmail, orderId);

        } catch (Exception e) {
            log.error("Failed to send shipping update email to: {} for order: {}", userEmail, orderId, e);
            throw new RuntimeException("Failed to send shipping update email", e);
        }
    }

    /**
     * Send account activity email
     */
    public void sendAccountActivityEmail(String userEmail, String activityType, String description) {
        try {
            String subject = "👤 Account Activity - " + activityType;
            String htmlContent = generateAccountActivityTemplate(activityType, description);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(userEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Account activity email sent to: {} for activity: {}", userEmail, activityType);

        } catch (Exception e) {
            log.error("Failed to send account activity email to: {}", userEmail, e);
            throw new RuntimeException("Failed to send account activity email", e);
        }
    }

    /**
     * Test email functionality
     */
    public void sendTestEmail(String toEmail, String testType) {
        try {
            String subject = "🧪 Test Email - " + testType;
            String content = "This is a test email from the Notification Service. Test type: " + testType;

            sendEmailToUser(toEmail, subject, content, NotificationType.SYSTEM_ALERT);
            log.info("Test email sent successfully to: {}", toEmail);

        } catch (Exception e) {
            log.error("Failed to send test email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send test email", e);
        }
    }

    // ==================== TEMPLATE GENERATION METHODS ====================

    /**
     * Generate email content based on notification type
     */
    private String generateEmailContent(String content, NotificationType type, Map<String, Object> variables) {
        return switch (type) {
            case ORDER_STATUS -> generateOrderEmailTemplate(
                    getFromVariables(variables, "orderId", "N/A"),
                    getFromVariables(variables, "status", "Updated"),
                    getFromVariables(variables, "orderDetails", content)
            );
            case PAYMENT_CONFIRMATION -> generatePaymentConfirmationTemplate(
                    getFromVariables(variables, "orderId", "N/A"),
                    getFromVariables(variables, "amount", "N/A"),
                    getFromVariables(variables, "paymentMethod", "N/A")
            );
            case SHIPPING_UPDATE -> generateShippingUpdateTemplate(
                    getFromVariables(variables, "orderId", "N/A"),
                    getFromVariables(variables, "status", "Updated"),
                    getFromVariables(variables, "trackingNumber", "")
            );
            case PROMOTION -> generatePromotionalEmailTemplate(
                    getFromVariables(variables, "title", "Special Offer"),
                    content,
                    getFromVariables(variables, "promoCode", ""),
                    getFromVariables(variables, "validUntil", ""),
                    getFromVariables(variables, "userEmail", "")
            );
            case ACCOUNT_ACTIVITY -> generateAccountActivityTemplate(
                    getFromVariables(variables, "activityType", "Activity"),
                    content
            );
            default -> generateGenericEmailTemplate(content, type.name());
        };
    }

    /**
     * Create template data based on notification content
     */
    private Map<String, Object> createTemplateData(Notification notification) {
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("notificationId", notification.getId());
        data.put("userId", notification.getUserId().toString());
        data.put("createdAt", notification.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        data.put("expiresAt", notification.getExpiresAt() != null ?
                notification.getExpiresAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "");

        // Parse content for specific data based on type
        String content = notification.getContent();
        switch (notification.getType()) {
            case ORDER_STATUS -> {
                if (content.contains("#")) {
                    String orderId = extractOrderId(content);
                    data.put("orderId", orderId);
                }
                data.put("status", extractStatus(content));
            }
            case PAYMENT_CONFIRMATION -> {
                data.put("orderId", extractOrderId(content));
                data.put("amount", extractAmount(content));
                data.put("paymentMethod", "Payment Method");
            }
            case SHIPPING_UPDATE -> {
                data.put("orderId", extractOrderId(content));
                data.put("status", extractStatus(content));
                data.put("trackingNumber", extractTrackingNumber(content));
            }
        }

        return data;
    }

    // ==================== EMAIL TEMPLATES ====================

    /**
     * Generate Order Status Email Template
     */
    private String generateOrderEmailTemplate(String orderId, String status, String orderDetails) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Order Update</title>
                <style>
                    body { font-family: Arial, sans-serif; margin: 0; padding: 20px; background-color: #f4f4f4; }
                    .container { max-width: 600px; margin: 0 auto; background-color: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
                    .header { text-align: center; border-bottom: 2px solid #3498db; padding-bottom: 20px; margin-bottom: 20px; }
                    .status { background-color: #3498db; color: white; padding: 10px; border-radius: 5px; text-align: center; margin: 15px 0; }
                    .order-details { background-color: #f8f9fa; padding: 15px; border-radius: 5px; margin: 15px 0; }
                    .footer { text-align: center; margin-top: 30px; padding-top: 20px; border-top: 1px solid #eee; color: #666; font-size: 12px; }
                    .button { display: inline-block; background-color: #3498db; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px; margin: 10px 0; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>📦 Order Update</h1>
                    </div>
                    <div class="content">
                        <p>Hello!</p>
                        <p>Your order has been updated:</p>
                        
                        <div class="order-details">
                            <p><strong>Order ID:</strong> %s</p>
                            <div class="status">
                                <h3>%s</h3>
                            </div>
                            <p><strong>Order Details:</strong></p>
                            <p>%s</p>
                        </div>
                        
                        <p>Thank you for choosing our service!</p>
                        
                        <a href="%s/orders/%s" class="button">View Order Details</a>
                    </div>
                    <div class="footer">
                        <p>This is an automated message from the ECommerce System.</p>
                        <p>If you have any questions, please contact our support team.</p>
                        <p>Generated at: %s</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(orderId, status, orderDetails, baseUrl, orderId, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
    }

    /**
     * Generate Payment Confirmation Email Template
     */
    private String generatePaymentConfirmationTemplate(String orderId, String amount, String paymentMethod) {
        return """
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
                        
                        <p>Hello!</p>
                        <p>Your payment has been successfully processed:</p>
                        
                        <div class="payment-details">
                            <p><strong>Order ID:</strong> %s</p>
                            <p><strong>Amount Paid:</strong> %s</p>
                            <p><strong>Payment Method:</strong> %s</p>
                        </div>
                        
                        <p>Thank you for your purchase! Your order will be processed shortly.</p>
                        
                        <a href="%s/orders/%s" class="button">View Order</a>
                    </div>
                    <div class="footer">
                        <p>This is an automated message from the ECommerce System.</p>
                        <p>Keep this email for your records.</p>
                        <p>Generated at: %s</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(orderId, amount, paymentMethod, baseUrl, orderId, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
    }

    /**
     * Generate Promotional Email Template
     */
    private String generatePromotionalEmailTemplate(String title, String message, String promoCode, String validUntil, String userEmail) {
        return """
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
                        <div class="promo-banner">
                            <h2>%s</h2>
                        </div>
                        
                        <p>Hello Valued Customer!</p>
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
            """.formatted(
                title,
                message,
                promoCode != null && !promoCode.isEmpty() ? "<div class=\"promo-code\">Use Code: " + promoCode + "</div>" : "",
                validUntil != null && !validUntil.isEmpty() ? "<p><strong>Valid Until:</strong> " + validUntil + "</p>" : "",
                baseUrl,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        );
    }

    /**
     * Generate Loyalty Points Email Template
     */
    private String generateLoyaltyPointsTemplate(int pointsEarned, int totalPoints, String reason) {
        return """
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
                        <div class="points-earned">
                            <h2>You've Earned %d Points!</h2>
                        </div>
                        
                        <p>Hello Loyal Customer!</p>
                        <p>%s</p>
                        
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
            """.formatted(pointsEarned, reason, totalPoints, baseUrl, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
    }

    /**
     * Generate Shipping Update Email Template
     */
    private String generateShippingUpdateTemplate(String orderId, String status, String trackingNumber) {
        return """
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
                        <p>Hello!</p>
                        <p>We have an update on your shipment:</p>
                        
                        <div class="tracking-info">
                            <p><strong>Order ID:</strong> %s</p>
                            <div class="shipping-status">
                                <h3>%s</h3>
                            </div>
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
            """.formatted(
                orderId,
                status,
                trackingNumber != null && !trackingNumber.isEmpty() ? "<p><strong>Tracking Number:</strong> " + trackingNumber + "</p>" : "",
                baseUrl,
                orderId,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        );
    }

    /**
     * Generate Account Activity Email Template
     */
    private String generateAccountActivityTemplate(String activityType, String description) {
        return """
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
                        <div class="activity-info">
                            <h3>%s</h3>
                        </div>
                        
                        <p>Hello!</p>
                        <p>We wanted to notify you about recent activity on your account:</p>
                        
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
            """.formatted(activityType, description, baseUrl, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
    }

    /**
     * Generate Generic Email Template
     */
    private String generateGenericEmailTemplate(String content, String type) {
        return """
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
            """.formatted(type, content, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
    }

    // ==================== HELPER METHODS ====================

    private String getFromVariables(Map<String, Object> variables, String key, String defaultValue) {
        if (variables == null) return defaultValue;
        Object value = variables.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    private String extractOrderId(String content) {
        // Extract order ID from content (assuming format like "Order #12345")
        if (content.contains("#")) {
            int startIndex = content.indexOf("#") + 1;
            int endIndex = content.indexOf(" ", startIndex);
            if (endIndex == -1) endIndex = content.length();
            return content.substring(startIndex, endIndex);
        }
        return "N/A";
    }

    private String extractStatus(String content) {
        // Extract status from content
        String[] statusKeywords = {"confirmed", "processing", "shipped", "delivered", "cancelled"};
        for (String status : statusKeywords) {
            if (content.toLowerCase().contains(status)) {
                return status.toUpperCase();
            }
        }
        return "UPDATED";
    }

    private String extractAmount(String content) {
        // Extract amount from content (assuming format like "$99.99")
        if (content.contains("$")) {
            int startIndex = content.indexOf("$");
            int endIndex = content.indexOf(" ", startIndex);
            if (endIndex == -1) endIndex = content.length();
            return content.substring(startIndex, endIndex);
        }
        return "N/A";
    }

    private String extractTrackingNumber(String content) {
        // Extract tracking number from content
        if (content.toLowerCase().contains("tracking")) {
            String[] words = content.split(" ");
            for (int i = 0; i < words.length; i++) {
                if (words[i].toLowerCase().contains("tracking") && i + 1 < words.length) {
                    return words[i + 1];
                }
            }
        }
        return "";
    }

    /**
     * Generate subject based on notification type
     */
    private String generateSubject(NotificationType type) {
        return switch (type) {
            case ORDER_STATUS -> "📦 Order Status Update";
            case PAYMENT_CONFIRMATION -> "💳 Payment Confirmation";
            case SHIPPING_UPDATE -> "🚚 Shipping Update";
            case PROMOTION -> "🎉 Special Promotion";
            case ACCOUNT_ACTIVITY -> "👤 Account Activity";
            case SYSTEM_ALERT -> "⚠️ System Alert";
            default -> "📧 Notification";
        };
    }

    /**
     * Generate order-specific subject
     */
    private String generateOrderSubject(String status) {
        return switch (status.toUpperCase()) {
            case "CONFIRMED" -> "✅ Order Confirmed";
            case "PROCESSING" -> "⚙️ Order Processing";
            case "SHIPPED" -> "🚚 Order Shipped";
            case "DELIVERED" -> "📦 Order Delivered";
            case "CANCELLED" -> "❌ Order Cancelled";
            default -> "📦 Order Update";
        };
    }
}