// Payment-Service/internal/handler/order_payment_handler.go - ENHANCED WITH INVOICE ENDPOINTS
package handler

import (
	"encoding/json"
	"fmt"
	"log"
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
	OrderID       string               `json:"orderId"`
	Amount        float64              `json:"amount"`
	PaymentMethod models.PaymentMethod `json:"paymentMethod"`
	Currency      string               `json:"currency,omitempty"`
}

// OrderPaymentResponse represents the response for order payment
type OrderPaymentResponse struct {
	PaymentID     string               `json:"paymentId"`
	OrderID       string               `json:"orderId"`
	Amount        float64              `json:"amount"`
	Status        models.PaymentStatus `json:"status"`
	PaymentMethod models.PaymentMethod `json:"paymentMethod"`
	TransactionID string               `json:"transaction_id,omitempty"`
	CreatedAt     time.Time            `json:"created_at"`
	Message       string               `json:"message,omitempty"`
	InvoiceNumber string               `json:"invoiceNumber,omitempty"` // 🎯 NEW: Include invoice info
}

// 🎯 NEW: InvoiceResponse represents the response for invoice data
type InvoiceResponse struct {
	ID            string    `json:"id"`
	OrderID       string    `json:"orderID"`
	PaymentID     string    `json:"paymentID"`
	InvoiceNumber string    `json:"invoiceNumber"`
	IssueDate     time.Time `json:"issueDate"`
	DueDate       time.Time `json:"dueDate"`
	Amount        float64   `json:"amount"`
	PaymentMethod string    `json:"paymentMethod"`
	Status        string    `json:"status"`
}

// OrderRefundRequest represents a refund request for an order
type OrderRefundRequest struct {
	OrderID string  `json:"orderId"`
	Amount  float64 `json:"amount"`
	Reason  string  `json:"reason,omitempty"`
}

// OrderPaymentHandler handles order payment-related HTTP requests
type OrderPaymentHandler struct {
	orderPaymentService service.OrderPaymentService
	invoiceService      service.InvoiceService // 🎯 NEW: Add invoice service
	kafkaService        *kafka.PaymentKafkaService
}

// NewOrderPaymentHandler creates a new order payment handler
func NewOrderPaymentHandler(
	orderPaymentService service.OrderPaymentService,
	invoiceService service.InvoiceService, // 🎯 NEW: Add invoice service parameter
	kafkaService *kafka.PaymentKafkaService,
) *OrderPaymentHandler {
	return &OrderPaymentHandler{
		orderPaymentService: orderPaymentService,
		invoiceService:      invoiceService,
		kafkaService:        kafkaService,
	}
}

// RegisterRoutes registers routes for order payment handler
func (h *OrderPaymentHandler) RegisterRoutes(r chi.Router) {
	r.Route("/orders", func(r chi.Router) {
		// Payment routes
		r.Post("/{orderID}/payments", h.ProcessOrderPayment)
		r.Get("/{orderID}/payments", h.GetOrderPayments)
		r.Post("/{orderID}/refund", h.RefundOrderPayment)
		r.Get("/{orderID}/payments/status", h.GetOrderPaymentStatus)

		// 🎯 NEW: Invoice routes
		r.Get("/{orderID}/invoices", h.GetOrderInvoices)
		r.Get("/{orderID}/invoices/{invoiceID}", h.GetInvoiceByID)
		r.Get("/{orderID}/invoices/{invoiceID}/pdf", h.GenerateInvoicePDF)
		r.Post("/{orderID}/invoices/{invoiceID}/email", h.EmailInvoice)
	})

	// 🎯 NEW: Payment-specific invoice routes
	r.Route("/payments", func(r chi.Router) {
		r.Get("/{paymentID}/invoice", h.GetPaymentInvoice)
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

	log.Printf("💳 PAYMENT SERVICE: Processing payment for order %s - Amount: %.2f, Method: %s",
		orderID, req.Amount, req.PaymentMethod)

	// Store the old status before processing
	oldStatus := models.Pending

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
		h.publishPaymentFailedEvent(payment, oldStatus, err.Error())

		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(response)
		return
	}

	// Payment successful
	response.Message = "Payment processed successfully"

	// 🎯 NEW: Try to get the generated invoice info
	if payment.Status == models.Completed {
		if invoice, invoiceErr := h.orderPaymentService.GetPaymentInvoice(payment.ID); invoiceErr == nil && invoice != nil {
			response.InvoiceNumber = invoice.InvoiceNumber
			log.Printf("📋 INVOICE: Payment response includes invoice number: %s", invoice.InvoiceNumber)
		}
	}

	// Publish payment confirmed event with proper format for Order Service
	h.publishPaymentConfirmedEvent(payment, oldStatus, "Payment processed successfully")

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
		response := OrderPaymentResponse{
			PaymentID:     payment.ID.String(),
			OrderID:       payment.OrderID.String(),
			Amount:        payment.Amount,
			Status:        payment.Status,
			PaymentMethod: payment.Method,
			CreatedAt:     payment.CreatedAt,
		}

		// 🎯 NEW: Include invoice info if available
		if invoice, invoiceErr := h.orderPaymentService.GetPaymentInvoice(payment.ID); invoiceErr == nil && invoice != nil {
			response.InvoiceNumber = invoice.InvoiceNumber
		}

		responses[i] = response
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(responses)
}

// 🎯 NEW: GetOrderInvoices retrieves all invoices for an order
func (h *OrderPaymentHandler) GetOrderInvoices(w http.ResponseWriter, r *http.Request) {
	orderIDStr := chi.URLParam(r, "orderID")
	orderID, err := uuid.Parse(orderIDStr)
	if err != nil {
		http.Error(w, "Invalid order ID", http.StatusBadRequest)
		return
	}

	log.Printf("📋 INVOICE: Getting invoices for order: %s", orderID)

	invoices, err := h.orderPaymentService.GetOrderInvoices(orderID)
	if err != nil {
		log.Printf("📋 INVOICE: Error getting invoices for order %s: %v", orderID, err)
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	log.Printf("📋 INVOICE: Found %d invoices for order %s", len(invoices), orderID)

	// Convert to response format
	responses := make([]InvoiceResponse, len(invoices))
	for i, invoice := range invoices {
		// Get payment details for additional info
		payment, _ := h.orderPaymentService.GetPaymentInvoice(invoice.PaymentID)

		responses[i] = InvoiceResponse{
			ID:            invoice.ID.String(),
			OrderID:       invoice.OrderID.String(),
			PaymentID:     invoice.PaymentID.String(),
			InvoiceNumber: invoice.InvoiceNumber,
			IssueDate:     invoice.IssueDate,
			DueDate:       invoice.DueDate,
			Amount:        0, // Will be filled from payment if available
			PaymentMethod: "",
			Status:        "issued",
		}

		// Add payment details if available
		if payment != nil {
			responses[i].Amount = payment.Payment.Amount
			responses[i].PaymentMethod = string(payment.Payment.Method)
		}
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(responses)
}

// 🎯 NEW: GetPaymentInvoice retrieves invoice for a specific payment
func (h *OrderPaymentHandler) GetPaymentInvoice(w http.ResponseWriter, r *http.Request) {
	paymentIDStr := chi.URLParam(r, "paymentID")
	paymentID, err := uuid.Parse(paymentIDStr)
	if err != nil {
		http.Error(w, "Invalid payment ID", http.StatusBadRequest)
		return
	}

	log.Printf("📋 INVOICE: Getting invoice for payment: %s", paymentID)

	invoice, err := h.orderPaymentService.GetPaymentInvoice(paymentID)
	if err != nil {
		log.Printf("📋 INVOICE: Invoice not found for payment %s: %v", paymentID, err)
		http.Error(w, "Invoice not found", http.StatusNotFound)
		return
	}

	log.Printf("📋 INVOICE: Found invoice %s for payment %s", invoice.InvoiceNumber, paymentID)

	response := InvoiceResponse{
		ID:            invoice.ID.String(),
		OrderID:       invoice.OrderID.String(),
		PaymentID:     invoice.PaymentID.String(),
		InvoiceNumber: invoice.InvoiceNumber,
		IssueDate:     invoice.IssueDate,
		DueDate:       invoice.DueDate,
		Status:        "issued",
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
}

// 🎯 NEW: GetInvoiceByID retrieves a specific invoice
func (h *OrderPaymentHandler) GetInvoiceByID(w http.ResponseWriter, r *http.Request) {
	orderIDStr := chi.URLParam(r, "orderID")
	invoiceIDStr := chi.URLParam(r, "invoiceID")

	invoiceID, err := uuid.Parse(invoiceIDStr)
	if err != nil {
		http.Error(w, "Invalid invoice ID", http.StatusBadRequest)
		return
	}

	log.Printf("📋 INVOICE: Getting invoice details: %s for order: %s", invoiceID, orderIDStr)

	invoice, err := h.invoiceService.GetInvoiceByID(invoiceID)
	if err != nil {
		log.Printf("📋 INVOICE: Invoice not found: %s", invoiceID)
		http.Error(w, "Invoice not found", http.StatusNotFound)
		return
	}

	response := InvoiceResponse{
		ID:            invoice.ID.String(),
		OrderID:       invoice.OrderID.String(),
		PaymentID:     invoice.PaymentID.String(),
		InvoiceNumber: invoice.InvoiceNumber,
		IssueDate:     invoice.IssueDate,
		DueDate:       invoice.DueDate,
		Status:        "issued",
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
}

// 🎯 NEW: GenerateInvoicePDF generates and returns invoice PDF
func (h *OrderPaymentHandler) GenerateInvoicePDF(w http.ResponseWriter, r *http.Request) {
	invoiceIDStr := chi.URLParam(r, "invoiceID")
	invoiceID, err := uuid.Parse(invoiceIDStr)
	if err != nil {
		http.Error(w, "Invalid invoice ID", http.StatusBadRequest)
		return
	}

	log.Printf("📋 INVOICE: Generating PDF for invoice: %s", invoiceID)

	pdfData, err := h.invoiceService.GeneratePDF(invoiceID)
	if err != nil {
		log.Printf("📋 INVOICE: Failed to generate PDF for invoice %s: %v", invoiceID, err)
		http.Error(w, "Failed to generate PDF", http.StatusInternalServerError)
		return
	}

	// Get invoice details for filename
	invoice, _ := h.invoiceService.GetInvoiceByID(invoiceID)
	filename := "invoice.pdf"
	if invoice != nil {
		filename = fmt.Sprintf("invoice_%s.pdf", invoice.InvoiceNumber)
	}

	w.Header().Set("Content-Type", "application/pdf")
	w.Header().Set("Content-Disposition", fmt.Sprintf("attachment; filename=%s", filename))
	w.WriteHeader(http.StatusOK)
	w.Write(pdfData)

	log.Printf("📋 INVOICE: PDF generated successfully for invoice: %s", invoiceID)
}

// 🎯 NEW: EmailInvoice sends invoice via email
func (h *OrderPaymentHandler) EmailInvoice(w http.ResponseWriter, r *http.Request) {
	invoiceIDStr := chi.URLParam(r, "invoiceID")
	invoiceID, err := uuid.Parse(invoiceIDStr)
	if err != nil {
		http.Error(w, "Invalid invoice ID", http.StatusBadRequest)
		return
	}

	var emailRequest struct {
		Email string `json:"email"`
	}

	if err := json.NewDecoder(r.Body).Decode(&emailRequest); err != nil {
		http.Error(w, "Invalid request body", http.StatusBadRequest)
		return
	}

	if emailRequest.Email == "" {
		http.Error(w, "Email address is required", http.StatusBadRequest)
		return
	}

	log.Printf("📋 INVOICE: Sending invoice %s to email: %s", invoiceID, emailRequest.Email)

	if err := h.invoiceService.SendToEmail(invoiceID, emailRequest.Email); err != nil {
		log.Printf("📋 INVOICE: Failed to send invoice %s to email %s: %v", invoiceID, emailRequest.Email, err)
		http.Error(w, "Failed to send email", http.StatusInternalServerError)
		return
	}

	response := map[string]interface{}{
		"message":   "Invoice sent successfully",
		"email":     emailRequest.Email,
		"invoiceID": invoiceID.String(),
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)

	log.Printf("📋 INVOICE: Invoice %s sent successfully to email: %s", invoiceID, emailRequest.Email)
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

	log.Printf("💳 PAYMENT SERVICE: Processing refund for order %s - Amount: %.2f", orderID, req.Amount)

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

	// 🎯 NEW: Include refund invoice/credit note info
	if invoice, invoiceErr := h.orderPaymentService.GetPaymentInvoice(refundPayment.ID); invoiceErr == nil && invoice != nil {
		response.InvoiceNumber = invoice.InvoiceNumber
		log.Printf("📋 INVOICE: Refund response includes credit note: %s", invoice.InvoiceNumber)
	}

	// Publish refund event
	h.publishPaymentUpdatedEvent(refundPayment, models.Completed, "Refund processed successfully")

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

	// 🎯 NEW: Include invoice information in status
	invoices, _ := h.orderPaymentService.GetOrderInvoices(orderID)
	invoiceNumbers := make([]string, len(invoices))
	for i, invoice := range invoices {
		invoiceNumbers[i] = invoice.InvoiceNumber
	}

	status := map[string]interface{}{
		"order_id":        orderID.String(),
		"payment_status":  latestPayment.Status,
		"total_amount":    calculateTotalAmount(payments),
		"last_updated":    latestPayment.UpdatedAt,
		"invoice_count":   len(invoices),
		"invoice_numbers": invoiceNumbers,
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(status)
}

// EXISTING METHODS: publishPaymentConfirmedEvent, publishPaymentFailedEvent, publishPaymentUpdatedEvent
// ... (keep the existing methods from the previous version)

// FIXED: publishPaymentConfirmedEvent - sends message in format Order Service expects
func (h *OrderPaymentHandler) publishPaymentConfirmedEvent(payment *models.Payment, oldStatus models.PaymentStatus, message string) {
	if h.kafkaService == nil {
		log.Printf("💳 PAYMENT SERVICE: Kafka service not available")
		return
	}

	// Create the message in the format Order Service expects
	eventMessage := map[string]interface{}{
		"orderId":       payment.OrderID.String(),
		"paymentId":     payment.ID.String(),
		"amount":        payment.Amount,
		"paymentMethod": payment.Method,
		"status":        payment.Status,
		"success":       true,
		"message":       message,
		"processedAt":   time.Now(),
		"oldStatus":     oldStatus,
	}

	log.Printf("💳 PAYMENT SERVICE: Publishing payment confirmed event - OrderId: %s, Status: %s, Message: %s",
		payment.OrderID.String(), payment.Status, message)

	// Send directly to the payment-confirmed topic with the correct format
	if err := h.kafkaService.PublishOrderPaymentEvent("payment-confirmed", eventMessage); err != nil {
		log.Printf("💳 PAYMENT SERVICE: Failed to publish payment confirmed event: %v", err)
		return
	}

	log.Printf("💳 PAYMENT SERVICE: Successfully published payment-confirmed event for order %s", payment.OrderID.String())
}

// FIXED: publishPaymentFailedEvent - sends message in format Order Service expects
func (h *OrderPaymentHandler) publishPaymentFailedEvent(payment *models.Payment, oldStatus models.PaymentStatus, message string) {
	if h.kafkaService == nil {
		log.Printf("💳 PAYMENT SERVICE: Kafka service not available")
		return
	}

	// Create the message in the format Order Service expects
	eventMessage := map[string]interface{}{
		"orderId":       payment.OrderID.String(),
		"paymentId":     payment.ID.String(),
		"amount":        payment.Amount,
		"paymentMethod": payment.Method,
		"status":        payment.Status,
		"success":       false,
		"message":       message,
		"processedAt":   time.Now(),
		"oldStatus":     oldStatus,
	}

	log.Printf("💳 PAYMENT SERVICE: Publishing payment failed event - OrderId: %s, Status: %s, Message: %s",
		payment.OrderID.String(), payment.Status, message)

	// Send to payment-failed topic
	if err := h.kafkaService.PublishOrderPaymentEvent("payment-failed", eventMessage); err != nil {
		log.Printf("💳 PAYMENT SERVICE: Failed to publish payment failed event: %v", err)
		return
	}

	log.Printf("💳 PAYMENT SERVICE: Successfully published payment-failed event for order %s", payment.OrderID.String())
}

// publishPaymentUpdatedEvent - for refunds and other updates
func (h *OrderPaymentHandler) publishPaymentUpdatedEvent(payment *models.Payment, oldStatus models.PaymentStatus, message string) {
	if h.kafkaService == nil {
		return
	}

	eventMessage := map[string]interface{}{
		"orderId":       payment.OrderID.String(),
		"paymentId":     payment.ID.String(),
		"amount":        payment.Amount,
		"paymentMethod": payment.Method,
		"status":        payment.Status,
		"success":       true,
		"message":       message,
		"processedAt":   time.Now(),
		"oldStatus":     oldStatus,
	}

	log.Printf("💳 PAYMENT SERVICE: Publishing payment updated event - OrderId: %s, Status: %s",
		payment.OrderID.String(), payment.Status)

	if err := h.kafkaService.PublishOrderPaymentEvent("payment-updated", eventMessage); err != nil {
		log.Printf("💳 PAYMENT SERVICE: Failed to publish payment updated event: %v", err)
		return
	}

	log.Printf("💳 PAYMENT SERVICE: Successfully published payment-updated event for order %s", payment.OrderID.String())
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
