// Payment-Service/internal/handler/order_payment_handler.go
package handler

import (
	"encoding/json"
	"net/http"
	"time"

	"github.com/ZakariaRek/Ecommerce-App/Payment-Service/internal/models"
	"github.com/ZakariaRek/Ecommerce-App/Payment-Service/internal/service"
	"github.com/ZakariaRek/Ecommerce-App/Payment-Service/internal/service/kafka"
	"github.com/go-chi/chi/v5"
	"github.com/google/uuid"
)

// OrderPaymentRequest represents a payment request for an order
type OrderPaymentRequest struct {
	OrderID       string               `json:"order_id"`
	Amount        float64              `json:"amount"`
	PaymentMethod models.PaymentMethod `json:"payment_method"`
	Currency      string               `json:"currency,omitempty"`
}

// OrderPaymentResponse represents the response for order payment
type OrderPaymentResponse struct {
	PaymentID     string               `json:"payment_id"`
	OrderID       string               `json:"order_id"`
	Amount        float64              `json:"amount"`
	Status        models.PaymentStatus `json:"status"`
	PaymentMethod models.PaymentMethod `json:"payment_method"`
	TransactionID string               `json:"transaction_id,omitempty"`
	CreatedAt     time.Time            `json:"created_at"`
	Message       string               `json:"message,omitempty"`
}

// OrderRefundRequest represents a refund request for an order
type OrderRefundRequest struct {
	OrderID string  `json:"order_id"`
	Amount  float64 `json:"amount"`
	Reason  string  `json:"reason,omitempty"`
}

// OrderPaymentHandler handles order payment-related HTTP requests
type OrderPaymentHandler struct {
	orderPaymentService service.OrderPaymentService
	kafkaService        *kafka.PaymentKafkaService
}

// NewOrderPaymentHandler creates a new order payment handler
func NewOrderPaymentHandler(
	orderPaymentService service.OrderPaymentService,
	kafkaService *kafka.PaymentKafkaService,
) *OrderPaymentHandler {
	return &OrderPaymentHandler{
		orderPaymentService: orderPaymentService,
		kafkaService:        kafkaService,
	}
}

// RegisterRoutes registers routes for order payment handler
func (h *OrderPaymentHandler) RegisterRoutes(r chi.Router) {
	r.Route("/orders", func(r chi.Router) {
		r.Post("/{orderID}/payments", h.ProcessOrderPayment)
		r.Get("/{orderID}/payments", h.GetOrderPayments)
		r.Post("/{orderID}/refund", h.RefundOrderPayment)
		r.Get("/{orderID}/payments/status", h.GetOrderPaymentStatus)
	})
}

// ProcessOrderPayment processes payment for an order
func (h *OrderPaymentHandler) ProcessOrderPayment(w http.ResponseWriter, r *http.Request) {
	orderIDStr := chi.URLParam(r, "orderID")
	orderID, err := uuid.Parse(orderIDStr)
	if err != nil {
		http.Error(w, "Invalid order ID", http.StatusBadRequest)
		return
	}

	var req OrderPaymentRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		http.Error(w, "Invalid request body", http.StatusBadRequest)
		return
	}

	// Validate payment method
	if !isValidPaymentMethod(req.PaymentMethod) {
		http.Error(w, "Invalid payment method", http.StatusBadRequest)
		return
	}

	// Validate amount
	if req.Amount <= 0 {
		http.Error(w, "Amount must be greater than zero", http.StatusBadRequest)
		return
	}

	// Process the payment
	payment, err := h.orderPaymentService.ProcessOrderPayment(
		orderID,
		req.Amount,
		req.PaymentMethod,
	)

	// Create response
	response := &OrderPaymentResponse{
		PaymentID:     payment.ID.String(),
		OrderID:       payment.OrderID.String(),
		Amount:        payment.Amount,
		Status:        payment.Status,
		PaymentMethod: payment.Method,
		CreatedAt:     payment.CreatedAt,
	}

	if err != nil {
		response.Message = err.Error()

		// Publish payment failed event
		h.publishPaymentEvent(payment, false, err.Error())

		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(response)
		return
	}

	// Payment successful
	response.Message = "Payment processed successfully"

	// Publish payment confirmed event
	h.publishPaymentEvent(payment, true, "Payment processed successfully")

	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)
	json.NewEncoder(w).Encode(response)
}

// GetOrderPayments retrieves all payments for an order
func (h *OrderPaymentHandler) GetOrderPayments(w http.ResponseWriter, r *http.Request) {
	orderIDStr := chi.URLParam(r, "orderID")
	orderID, err := uuid.Parse(orderIDStr)
	if err != nil {
		http.Error(w, "Invalid order ID", http.StatusBadRequest)
		return
	}

	payments, err := h.orderPaymentService.GetOrderPayments(orderID)
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	// Convert to response format
	responses := make([]OrderPaymentResponse, len(payments))
	for i, payment := range payments {
		responses[i] = OrderPaymentResponse{
			PaymentID:     payment.ID.String(),
			OrderID:       payment.OrderID.String(),
			Amount:        payment.Amount,
			Status:        payment.Status,
			PaymentMethod: payment.Method,
			CreatedAt:     payment.CreatedAt,
		}
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(responses)
}

// RefundOrderPayment processes a refund for an order
func (h *OrderPaymentHandler) RefundOrderPayment(w http.ResponseWriter, r *http.Request) {
	orderIDStr := chi.URLParam(r, "orderID")
	orderID, err := uuid.Parse(orderIDStr)
	if err != nil {
		http.Error(w, "Invalid order ID", http.StatusBadRequest)
		return
	}

	var req OrderRefundRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		http.Error(w, "Invalid request body", http.StatusBadRequest)
		return
	}

	if req.Amount <= 0 {
		http.Error(w, "Refund amount must be greater than zero", http.StatusBadRequest)
		return
	}

	// Process the refund
	refundPayment, err := h.orderPaymentService.RefundOrderPayment(orderID, req.Amount)
	if err != nil {
		http.Error(w, err.Error(), http.StatusBadRequest)
		return
	}

	response := &OrderPaymentResponse{
		PaymentID:     refundPayment.ID.String(),
		OrderID:       refundPayment.OrderID.String(),
		Amount:        refundPayment.Amount,
		Status:        refundPayment.Status,
		PaymentMethod: refundPayment.Method,
		CreatedAt:     refundPayment.CreatedAt,
		Message:       "Refund processed successfully",
	}

	// Publish refund event (could be handled by Order Service for status updates)
	h.publishPaymentEvent(refundPayment, true, "Refund processed successfully")

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
}

// GetOrderPaymentStatus gets the payment status for an order
func (h *OrderPaymentHandler) GetOrderPaymentStatus(w http.ResponseWriter, r *http.Request) {
	orderIDStr := chi.URLParam(r, "orderID")
	orderID, err := uuid.Parse(orderIDStr)
	if err != nil {
		http.Error(w, "Invalid order ID", http.StatusBadRequest)
		return
	}

	payments, err := h.orderPaymentService.GetOrderPayments(orderID)
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	if len(payments) == 0 {
		http.Error(w, "No payments found for order", http.StatusNotFound)
		return
	}

	// Find the latest payment
	latestPayment := payments[0]
	for _, payment := range payments {
		if payment.CreatedAt.After(latestPayment.CreatedAt) {
			latestPayment = payment
		}
	}

	status := map[string]interface{}{
		"order_id":       orderID.String(),
		"payment_status": latestPayment.Status,
		"total_amount":   calculateTotalAmount(payments),
		"last_updated":   latestPayment.UpdatedAt,
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(status)
}

// publishPaymentEvent publishes payment events to Kafka for Order Service consumption
func (h *OrderPaymentHandler) publishPaymentEvent(payment *models.Payment, success bool, message string) {
	if h.kafkaService == nil {
		return // Kafka not available
	}

	//eventData := map[string]interface{}{
	//	"orderId":       payment.OrderID.String(),
	//	"paymentId":     payment.ID.String(),
	//	"amount":        payment.Amount,
	//	"paymentMethod": payment.Method,
	//	"status":        payment.Status,
	//	"success":       success,
	//	"message":       message,
	//	"processedAt":   time.Now(),
	//}

	if success {
		// Create payment confirmed event for Order Service
		h.kafkaService.PublishPaymentStatusChanged(payment, models.Pending)
		//h.kafkaService.PublishPaymentEvent(eventData)

	} else {
		// Create payment failed event for Order Service
		h.kafkaService.PublishPaymentStatusChanged(payment, models.Pending)
	}
}

// Helper functions
func isValidPaymentMethod(method models.PaymentMethod) bool {
	switch method {
	case models.CreditCard, models.DebitCard, models.PayPal,
		models.BankTransfer, models.Crypto, models.Points, models.GiftCard:
		return true
	default:
		return false
	}
}

func calculateTotalAmount(payments []*models.Payment) float64 {
	total := 0.0
	for _, payment := range payments {
		if payment.Status == models.Completed || payment.Status == models.Refunded {
			total += payment.Amount
		}
	}
	return total
}
