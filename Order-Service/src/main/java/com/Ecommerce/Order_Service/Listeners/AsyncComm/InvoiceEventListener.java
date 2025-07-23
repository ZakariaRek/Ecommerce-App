// Order-Service/src/main/java/com/Ecommerce/Order_Service/Listeners/AsyncComm/InvoiceEventListener.java
package com.Ecommerce.Order_Service.Listeners.AsyncComm;

import com.Ecommerce.Order_Service.Entities.Order;
import com.Ecommerce.Order_Service.Services.OrderService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Listener for invoice-related events from Payment Service
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InvoiceEventListener {

    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    /**
     * Listen for invoice created events from Payment Service
     */
    @KafkaListener(topics = "invoice-created", groupId = "${spring.kafka.consumer.group-id}")
    public void handleInvoiceCreated(ConsumerRecord<String, Object> record) {
        try {
            log.info("📄 ORDER SERVICE: Received invoice created event: {}", record.value());

            // Handle both null payload and actual response
            if (record.value() == null || "KafkaNull".equals(record.value().getClass().getSimpleName())) {
                log.warn("📄 ORDER SERVICE: Received null payload for invoice created event");
                return;
            }

            // Parse the invoice event
            JsonNode eventNode;
            if (record.value() instanceof String) {
                eventNode = objectMapper.readTree((String) record.value());
            } else {
                eventNode = objectMapper.convertValue(record.value(), JsonNode.class);
            }

            // Extract invoice information
            String orderIdStr = extractValue(eventNode, "orderId");
            String paymentIdStr = extractValue(eventNode, "paymentId");
            String invoiceIdStr = extractValue(eventNode, "invoiceId");
            String invoiceNumber = extractValue(eventNode, "invoiceNumber");
            String downloadUrl = extractValue(eventNode, "downloadUrl");

            if (orderIdStr == null || orderIdStr.isEmpty()) {
                log.warn("📄 ORDER SERVICE: No order ID found in invoice created event");
                return;
            }

            UUID orderId = UUID.fromString(orderIdStr);

            log.info("📄 ORDER SERVICE: Invoice created for order {} - Invoice: {}", orderId, invoiceNumber);

            // Get the order to update
            Order order = orderService.getOrderById(orderId);
            if (order == null) {
                log.warn("📄 ORDER SERVICE: Order not found for invoice event: {}", orderId);
                return;
            }

            // Store invoice information in order metadata (if you have a metadata field)
            // or create a separate invoice tracking mechanism
            log.info("📄 ORDER SERVICE: Invoice {} successfully created for order {} (Payment: {})",
                    invoiceNumber, orderId, paymentIdStr);
            log.info("📄 ORDER SERVICE: Invoice download URL: {}", downloadUrl);

            // You can add additional logic here such as:
            // 1. Storing invoice reference in order
            // 2. Sending notification to customer
            // 3. Updating order status if needed
            // 4. Triggering fulfillment process

        } catch (Exception e) {
            log.error("📄 ORDER SERVICE: Error processing invoice created event", e);
        }
    }

    /**
     * Listen for invoice updated events from Payment Service
     */
    @KafkaListener(topics = "invoice-updated", groupId = "${spring.kafka.consumer.group-id}")
    public void handleInvoiceUpdated(ConsumerRecord<String, Object> record) {
        try {
            log.info("📄 ORDER SERVICE: Received invoice updated event: {}", record.value());

            if (record.value() == null) {
                log.warn("📄 ORDER SERVICE: Received null payload for invoice updated event");
                return;
            }

            // Parse the invoice event
            JsonNode eventNode;
            if (record.value() instanceof String) {
                eventNode = objectMapper.readTree((String) record.value());
            } else {
                eventNode = objectMapper.convertValue(record.value(), JsonNode.class);
            }

            String orderIdStr = extractValue(eventNode, "orderId");
            String invoiceNumber = extractValue(eventNode, "invoiceNumber");

            if (orderIdStr != null) {
                UUID orderId = UUID.fromString(orderIdStr);
                log.info("📄 ORDER SERVICE: Invoice {} updated for order {}", invoiceNumber, orderId);
            }

        } catch (Exception e) {
            log.error("📄 ORDER SERVICE: Error processing invoice updated event", e);
        }
    }

    /**
     * Listen for invoice due date changed events from Payment Service
     */
    @KafkaListener(topics = "invoice-due-date-changed", groupId = "${spring.kafka.consumer.group-id}")
    public void handleInvoiceDueDateChanged(ConsumerRecord<String, Object> record) {
        try {
            log.info("📄 ORDER SERVICE: Received invoice due date changed event: {}", record.value());

            if (record.value() == null) {
                log.warn("📄 ORDER SERVICE: Received null payload for invoice due date changed event");
                return;
            }

            // Parse the invoice event
            JsonNode eventNode;
            if (record.value() instanceof String) {
                eventNode = objectMapper.readTree((String) record.value());
            } else {
                eventNode = objectMapper.convertValue(record.value(), JsonNode.class);
            }

            String orderIdStr = extractValue(eventNode, "orderId");
            String invoiceNumber = extractValue(eventNode, "invoiceNumber");
            String newDueDate = extractValue(eventNode, "dueDate");

            if (orderIdStr != null) {
                UUID orderId = UUID.fromString(orderIdStr);
                log.info("📄 ORDER SERVICE: Invoice {} due date changed for order {} - New due date: {}",
                        invoiceNumber, orderId, newDueDate);
            }

        } catch (Exception e) {
            log.error("📄 ORDER SERVICE: Error processing invoice due date changed event", e);
        }
    }

    /**
     * Listen for invoice deleted events from Payment Service
     */
    @KafkaListener(topics = "invoice-deleted", groupId = "${spring.kafka.consumer.group-id}")
    public void handleInvoiceDeleted(ConsumerRecord<String, Object> record) {
        try {
            log.info("📄 ORDER SERVICE: Received invoice deleted event: {}", record.value());

            if (record.value() == null) {
                log.warn("📄 ORDER SERVICE: Received null payload for invoice deleted event");
                return;
            }

            // Parse the invoice event
            JsonNode eventNode;
            if (record.value() instanceof String) {
                eventNode = objectMapper.readTree((String) record.value());
            } else {
                eventNode = objectMapper.convertValue(record.value(), JsonNode.class);
            }

            String orderIdStr = extractValue(eventNode, "orderId");
            String invoiceNumber = extractValue(eventNode, "invoiceNumber");

            if (orderIdStr != null) {
                UUID orderId = UUID.fromString(orderIdStr);
                log.info("📄 ORDER SERVICE: Invoice {} deleted for order {}", invoiceNumber, orderId);
            }

        } catch (Exception e) {
            log.error("📄 ORDER SERVICE: Error processing invoice deleted event", e);
        }
    }

    /**
     * Helper method to safely extract string values from JsonNode
     */
    private String extractValue(JsonNode node, String fieldName) {
        if (node == null || !node.has(fieldName)) {
            return null;
        }

        JsonNode fieldNode = node.get(fieldName);
        if (fieldNode == null || fieldNode.isNull()) {
            return null;
        }

        return fieldNode.asText();
    }
}