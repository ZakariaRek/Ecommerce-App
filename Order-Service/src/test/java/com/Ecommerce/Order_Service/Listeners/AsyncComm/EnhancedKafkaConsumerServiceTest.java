package com.Ecommerce.Order_Service.Listeners.AsyncComm;

import com.Ecommerce.Order_Service.Entities.Order;
import com.Ecommerce.Order_Service.Entities.OrderStatus;
import com.Ecommerce.Order_Service.Services.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnhancedKafkaConsumerServiceTest {

    @Mock
    private OrderService orderService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private EnhancedKafkaConsumerService kafkaConsumerService;

    private UUID testOrderId;
    private Order testOrder;

    @BeforeEach
    void setUp() {
        testOrderId = UUID.randomUUID();

        testOrder = new Order();
        testOrder.setId(testOrderId);
        testOrder.setStatus(OrderStatus.PENDING);
    }

    @Test
    void listenPaymentConfirmed_WithSuccessfulPayment_UpdatesOrderToPaid() {
        // Given
        Map<String, Object> paymentEvent = new HashMap<>();
        paymentEvent.put("orderId", testOrderId.toString());
        paymentEvent.put("success", true);
        paymentEvent.put("status", "COMPLETED");
        paymentEvent.put("paymentId", "pay_123");
        paymentEvent.put("amount", 100.0);
        paymentEvent.put("paymentMethod", "CREDIT_CARD");

        when(orderService.updateOrderStatus(testOrderId, OrderStatus.PAID)).thenReturn(testOrder);

        // When
        kafkaConsumerService.listenPaymentConfirmed(paymentEvent);

        // Then
        verify(orderService).updateOrderStatus(testOrderId, OrderStatus.PAID);
    }

    @Test
    void listenPaymentConfirmed_WithSuccessfulPaymentUsingStatus_UpdatesOrderToPaid() {
        // Given
        Map<String, Object> paymentEvent = new HashMap<>();
        paymentEvent.put("orderId", testOrderId.toString());
        paymentEvent.put("status", "COMPLETED");
        paymentEvent.put("paymentId", "pay_456");

        when(orderService.updateOrderStatus(testOrderId, OrderStatus.PAID)).thenReturn(testOrder);

        // When
        kafkaConsumerService.listenPaymentConfirmed(paymentEvent);

        // Then
        verify(orderService).updateOrderStatus(testOrderId, OrderStatus.PAID);
    }

    @Test
    void listenPaymentConfirmed_WithFailedPayment_UpdatesOrderToPaymentFailed() {
        // Given
        Map<String, Object> paymentEvent = new HashMap<>();
        paymentEvent.put("orderId", testOrderId.toString());
        paymentEvent.put("success", false);
        paymentEvent.put("status", "FAILED");

        when(orderService.updateOrderStatus(testOrderId, OrderStatus.PAYMENT_FAILED)).thenReturn(testOrder);

        // When
        kafkaConsumerService.listenPaymentConfirmed(paymentEvent);

        // Then
        verify(orderService).updateOrderStatus(testOrderId, OrderStatus.PAYMENT_FAILED);
    }

    @Test
    void listenPaymentConfirmed_WithMissingOrderId_HandlesGracefully() {
        // Given
        Map<String, Object> paymentEvent = new HashMap<>();
        paymentEvent.put("success", true);
        paymentEvent.put("status", "COMPLETED");

        // When
        kafkaConsumerService.listenPaymentConfirmed(paymentEvent);

        // Then
        verifyNoInteractions(orderService);
    }

    @Test
    void listenPaymentConfirmed_WithInvalidOrderId_HandlesGracefully() {
        // Given
        Map<String, Object> paymentEvent = new HashMap<>();
        paymentEvent.put("orderId", "invalid-uuid");
        paymentEvent.put("success", true);

        // When
        kafkaConsumerService.listenPaymentConfirmed(paymentEvent);

        // Then
        verifyNoInteractions(orderService);
    }

    @Test
    void listenPaymentConfirmed_WithEntityNotFoundException_HandlesGracefully() {
        // Given
        Map<String, Object> paymentEvent = new HashMap<>();
        paymentEvent.put("orderId", testOrderId.toString());
        paymentEvent.put("success", true);

        when(orderService.updateOrderStatus(testOrderId, OrderStatus.PAID))
                .thenThrow(new EntityNotFoundException("Order not found"));

        // When
        kafkaConsumerService.listenPaymentConfirmed(paymentEvent);

        // Then
        verify(orderService).updateOrderStatus(testOrderId, OrderStatus.PAID);
        // Should handle exception gracefully without throwing
    }

    @Test
    void listenPaymentConfirmed_WithAlternativeFieldNames_ExtractsOrderIdCorrectly() {
        // Given
        Map<String, Object> paymentEvent = new HashMap<>();
        paymentEvent.put("order_id", testOrderId.toString()); // Alternative field name
        paymentEvent.put("success", true);

        when(orderService.updateOrderStatus(testOrderId, OrderStatus.PAID)).thenReturn(testOrder);

        // When
        kafkaConsumerService.listenPaymentConfirmed(paymentEvent);

        // Then
        verify(orderService).updateOrderStatus(testOrderId, OrderStatus.PAID);
    }

    @Test
    void listenPaymentFailed_WithValidOrderId_UpdatesOrderToPaymentFailed() {
        // Given
        Map<String, Object> paymentEvent = new HashMap<>();
        paymentEvent.put("orderId", testOrderId.toString());
        paymentEvent.put("error", "Payment processing failed");

        when(orderService.updateOrderStatus(testOrderId, OrderStatus.PAYMENT_FAILED)).thenReturn(testOrder);

        // When
        kafkaConsumerService.listenPaymentFailed(paymentEvent);

        // Then
        verify(orderService).updateOrderStatus(testOrderId, OrderStatus.PAYMENT_FAILED);
    }

    @Test
    void listenPaymentFailed_WithMissingOrderId_HandlesGracefully() {
        // Given
        Map<String, Object> paymentEvent = new HashMap<>();
        paymentEvent.put("error", "Payment failed");

        // When
        kafkaConsumerService.listenPaymentFailed(paymentEvent);

        // Then
        verifyNoInteractions(orderService);
    }

    @Test
    void listenPaymentFailed_WithEntityNotFoundException_HandlesGracefully() {
        // Given
        Map<String, Object> paymentEvent = new HashMap<>();
        paymentEvent.put("orderId", testOrderId.toString());

        when(orderService.updateOrderStatus(testOrderId, OrderStatus.PAYMENT_FAILED))
                .thenThrow(new EntityNotFoundException("Order not found"));

        // When
        kafkaConsumerService.listenPaymentFailed(paymentEvent);

        // Then
        verify(orderService).updateOrderStatus(testOrderId, OrderStatus.PAYMENT_FAILED);
        // Should handle exception gracefully
    }

    @Test
    void listenShippingUpdate_WithShippedStatus_UpdatesOrderToShipped() {
        // Given
        Map<String, Object> shippingUpdate = new HashMap<>();
        shippingUpdate.put("orderId", testOrderId.toString());
        shippingUpdate.put("status", "SHIPPED");
        shippingUpdate.put("shippingId", "ship_123");
        shippingUpdate.put("trackingNumber", "TRK123456");
        shippingUpdate.put("carrier", "UPS");

        when(orderService.updateOrderStatus(testOrderId, OrderStatus.SHIPPED)).thenReturn(testOrder);

        // When
        kafkaConsumerService.listenShippingUpdate(shippingUpdate);

        // Then
        verify(orderService).updateOrderStatus(testOrderId, OrderStatus.SHIPPED);
    }

    @Test
    void listenShippingUpdate_WithDeliveredStatus_UpdatesOrderToDelivered() {
        // Given
        Map<String, Object> shippingUpdate = new HashMap<>();
        shippingUpdate.put("orderId", testOrderId.toString());
        shippingUpdate.put("status", "DELIVERED");
        shippingUpdate.put("shippingId", "ship_456");

        when(orderService.updateOrderStatus(testOrderId, OrderStatus.DELIVERED)).thenReturn(testOrder);

        // When
        kafkaConsumerService.listenShippingUpdate(shippingUpdate);

        // Then
        verify(orderService).updateOrderStatus(testOrderId, OrderStatus.DELIVERED);
    }

    @Test
    void listenShippingUpdate_WithPreparingStatus_UpdatesOrderToProcessing() {
        // Given
        Map<String, Object> shippingUpdate = new HashMap<>();
        shippingUpdate.put("orderId", testOrderId.toString());
        shippingUpdate.put("status", "PREPARING");

        when(orderService.updateOrderStatus(testOrderId, OrderStatus.PROCESSING)).thenReturn(testOrder);

        // When
        kafkaConsumerService.listenShippingUpdate(shippingUpdate);

        // Then
        verify(orderService).updateOrderStatus(testOrderId, OrderStatus.PROCESSING);
    }

    @Test
    void listenShippingUpdate_WithFailedStatus_UpdatesOrderToCanceled() {
        // Given
        Map<String, Object> shippingUpdate = new HashMap<>();
        shippingUpdate.put("orderId", testOrderId.toString());
        shippingUpdate.put("status", "FAILED");

        when(orderService.updateOrderStatus(testOrderId, OrderStatus.CANCELED)).thenReturn(testOrder);

        // When
        kafkaConsumerService.listenShippingUpdate(shippingUpdate);

        // Then
        verify(orderService).updateOrderStatus(testOrderId, OrderStatus.CANCELED);
    }

    @Test
    void listenShippingUpdate_WithUnknownStatus_HandlesGracefully() {
        // Given
        Map<String, Object> shippingUpdate = new HashMap<>();
        shippingUpdate.put("orderId", testOrderId.toString());
        shippingUpdate.put("status", "UNKNOWN_STATUS");

        // When
        kafkaConsumerService.listenShippingUpdate(shippingUpdate);

        // Then
        verifyNoInteractions(orderService);
    }

    @Test
    void listenShippingUpdate_WithMissingOrderId_HandlesGracefully() {
        // Given
        Map<String, Object> shippingUpdate = new HashMap<>();
        shippingUpdate.put("status", "SHIPPED");
        shippingUpdate.put("shippingId", "ship_789");

        // When
        kafkaConsumerService.listenShippingUpdate(shippingUpdate);

        // Then
        verifyNoInteractions(orderService);
    }

    @Test
    void listenShippingUpdate_WithEntityNotFoundException_HandlesGracefully() {
        // Given
        Map<String, Object> shippingUpdate = new HashMap<>();
        shippingUpdate.put("orderId", testOrderId.toString());
        shippingUpdate.put("status", "SHIPPED");

        when(orderService.updateOrderStatus(testOrderId, OrderStatus.SHIPPED))
                .thenThrow(new EntityNotFoundException("Order not found"));

        // When
        kafkaConsumerService.listenShippingUpdate(shippingUpdate);

        // Then
        verify(orderService).updateOrderStatus(testOrderId, OrderStatus.SHIPPED);
        // Should handle exception gracefully
    }

    @Test
    void listenShippingUpdate_WithNullOrEmptyStatus_HandlesGracefully() {
        // Given - Test with null status
        Map<String, Object> shippingUpdate1 = new HashMap<>();
        shippingUpdate1.put("orderId", testOrderId.toString());
        shippingUpdate1.put("status", null);

        // When
        kafkaConsumerService.listenShippingUpdate(shippingUpdate1);

        // Then
        verifyNoInteractions(orderService);

        // Given - Test with empty status
        Map<String, Object> shippingUpdate2 = new HashMap<>();
        shippingUpdate2.put("orderId", testOrderId.toString());
        shippingUpdate2.put("status", "");

        // When
        kafkaConsumerService.listenShippingUpdate(shippingUpdate2);

        // Then
        verifyNoInteractions(orderService);
    }

    @Test
    void listenShippingUpdate_WithCaseInsensitiveStatus_WorksCorrectly() {
        // Given
        Map<String, Object> shippingUpdate = new HashMap<>();
        shippingUpdate.put("orderId", testOrderId.toString());
        shippingUpdate.put("status", "shipped"); // lowercase

        when(orderService.updateOrderStatus(testOrderId, OrderStatus.SHIPPED)).thenReturn(testOrder);

        // When
        kafkaConsumerService.listenShippingUpdate(shippingUpdate);

        // Then
        verify(orderService).updateOrderStatus(testOrderId, OrderStatus.SHIPPED);
    }

    @Test
    void extractPaymentSuccess_WithBooleanTrue_ReturnsTrue() {
        // Given
        Map<String, Object> paymentEvent = new HashMap<>();
        paymentEvent.put("orderId", testOrderId.toString());
        paymentEvent.put("success", true);

        when(orderService.updateOrderStatus(testOrderId, OrderStatus.PAID)).thenReturn(testOrder);

        // When
        kafkaConsumerService.listenPaymentConfirmed(paymentEvent);

        // Then
        verify(orderService).updateOrderStatus(testOrderId, OrderStatus.PAID);
    }

    @Test
    void extractPaymentSuccess_WithStringTrue_ReturnsTrue() {
        // Given
        Map<String, Object> paymentEvent = new HashMap<>();
        paymentEvent.put("orderId", testOrderId.toString());
        paymentEvent.put("successful", "true");

        when(orderService.updateOrderStatus(testOrderId, OrderStatus.PAID)).thenReturn(testOrder);

        // When
        kafkaConsumerService.listenPaymentConfirmed(paymentEvent);

        // Then
        verify(orderService).updateOrderStatus(testOrderId, OrderStatus.PAID);
    }

    @Test
    void extractPaymentSuccess_WithNumericOne_ReturnsTrue() {
        // Given
        Map<String, Object> paymentEvent = new HashMap<>();
        paymentEvent.put("orderId", testOrderId.toString());
        paymentEvent.put("Success", "1");

        when(orderService.updateOrderStatus(testOrderId, OrderStatus.PAID)).thenReturn(testOrder);

        // When
        kafkaConsumerService.listenPaymentConfirmed(paymentEvent);

        // Then
        verify(orderService).updateOrderStatus(testOrderId, OrderStatus.PAID);
    }

    @Test
    void getStringValue_WithValidValue_ReturnsString() {
        // This method is private, but we can test it indirectly through other methods
        // Given
        Map<String, Object> shippingUpdate = new HashMap<>();
        shippingUpdate.put("orderId", testOrderId.toString());
        shippingUpdate.put("status", "SHIPPED");
        shippingUpdate.put("trackingNumber", 123456); // Non-string value

        when(orderService.updateOrderStatus(testOrderId, OrderStatus.SHIPPED)).thenReturn(testOrder);

        // When
        kafkaConsumerService.listenShippingUpdate(shippingUpdate);

        // Then
        verify(orderService).updateOrderStatus(testOrderId, OrderStatus.SHIPPED);
        // The tracking number should be converted to string internally
    }

    @Test
    void getDoubleValue_WithValidNumber_ReturnsDouble() {
        // This method is private, but we can test it indirectly
        // Given
        Map<String, Object> paymentEvent = new HashMap<>();
        paymentEvent.put("orderId", testOrderId.toString());
        paymentEvent.put("success", true);
        paymentEvent.put("amount", "100.50"); // String number

        when(orderService.updateOrderStatus(testOrderId, OrderStatus.PAID)).thenReturn(testOrder);

        // When
        kafkaConsumerService.listenPaymentConfirmed(paymentEvent);

        // Then
        verify(orderService).updateOrderStatus(testOrderId, OrderStatus.PAID);
        // The amount should be parsed correctly internally
    }
}