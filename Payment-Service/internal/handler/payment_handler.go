// Payment-Service/internal/handler/payment_handler.go - FIXED VERSION
package handler

import (
	"encoding/json"
	"net/http"
	"strconv"
	"time"

	"github.com/ZakariaRek/Ecommerce-App/Payment-Service/internal/models"
	"github.com/ZakariaRek/Ecommerce-App/Payment-Service/internal/repository"
	"github.com/ZakariaRek/Ecommerce-App/Payment-Service/internal/service"
	"github.com/go-chi/chi/v5"
	"github.com/google/uuid"
)

// PaymentHandler handles payment-related HTTP requests
type PaymentHandler struct {
	paymentService      service.PaymentService
	enhancedRepo        repository.EnhancedPaymentRepository // 🎯 FIXED: Add enhanced repo
	orderPaymentService service.OrderPaymentService
	invoiceService      service.InvoiceService
}

// NewPaymentHandler creates a new payment handler
func NewPaymentHandler(
	paymentService service.PaymentService,
	enhancedRepo repository.EnhancedPaymentRepository, // 🎯 FIXED: Add enhanced repo parameter
	orderPaymentService service.OrderPaymentService,
	invoiceService service.InvoiceService,
) *PaymentHandler {
	return &PaymentHandler{
		paymentService:      paymentService,
		enhancedRepo:        enhancedRepo, // 🎯 FIXED: Store enhanced repo
		orderPaymentService: orderPaymentService,
		invoiceService:      invoiceService,
	}
}

// RegisterRoutes registers routes for payment handler
func (h *PaymentHandler) RegisterRoutes(r chi.Router) {
	r.Route("/payments", func(r chi.Router) {
		// Basic payment operations
		r.Post("/", h.CreatePayment)
		r.Get("/{id}", h.GetPayment)
		r.Put("/{id}", h.UpdatePayment)
		r.Delete("/{id}", h.DeletePayment)
		r.Get("/", h.GetAllPayments) // 🎯 FIXED: This is the correct endpoint
		r.Get("/order/{orderID}", h.GetPaymentsByOrder)
		r.Post("/{id}/process", h.ProcessPayment)
		r.Post("/{id}/refund", h.RefundPayment)
		r.Get("/{id}/status", h.GetPaymentStatus)

		// Invoice routes for payments
		r.Get("/{id}/invoice", h.GetPaymentInvoice)
		r.Get("/{id}/invoice/pdf", h.GetPaymentInvoicePDF)
		r.Post("/{id}/invoice/email", h.EmailPaymentInvoice)

		// Transaction routes
		r.Get("/{id}/transactions", h.GetPaymentTransactions)
	})
}

// GetAllPayments gets all payments with proper pagination and filters
func (h *PaymentHandler) GetAllPayments(w http.ResponseWriter, r *http.Request) {
	// Parse pagination parameters
	page := 1
	limit := 50

	if pageStr := r.URL.Query().Get("page"); pageStr != "" {
		if p, err := strconv.Atoi(pageStr); err == nil && p > 0 {
			page = p
		}
	}

	if limitStr := r.URL.Query().Get("limit"); limitStr != "" {
		if l, err := strconv.Atoi(limitStr); err == nil && l > 0 && l <= 100 {
			limit = l
		}
	}

	// 🎯 FIXED: Parse and validate filters
	filters := repository.PaymentFilters{}

	// Parse status filter
	if statusStr := r.URL.Query().Get("status"); statusStr != "" {
		status := models.PaymentStatus(statusStr)
		// Validate status
		if h.isValidStatus(status) {
			filters.Status = &status
		} else {
			http.Error(w, "Invalid status value. Valid values: PENDING, COMPLETED, FAILED, REFUNDED, PARTIALLY_REFUNDED", http.StatusBadRequest)
			return
		}
	}

	// Parse method filter
	if methodStr := r.URL.Query().Get("method"); methodStr != "" {
		method := models.PaymentMethod(methodStr)
		// Validate method
		if h.isValidMethod(method) {
			filters.Method = &method
		} else {
			http.Error(w, "Invalid method value. Valid values: CREDIT_CARD, DEBIT_CARD, PAYPAL, BANK_TRANSFER, CRYPTO, POINTS, GIFT_CARD", http.StatusBadRequest)
			return
		}
	}

	// Parse date filters
	if dateFromStr := r.URL.Query().Get("dateFrom"); dateFromStr != "" {
		if dateFrom, err := time.Parse("2006-01-02", dateFromStr); err == nil {
			filters.StartDate = &dateFrom
		} else {
			http.Error(w, "Invalid dateFrom format. Use YYYY-MM-DD", http.StatusBadRequest)
			return
		}
	}

	if dateToStr := r.URL.Query().Get("dateTo"); dateToStr != "" {
		if dateTo, err := time.Parse("2006-01-02", dateToStr); err == nil {
			// Set to end of day
			dateTo = dateTo.Add(23*time.Hour + 59*time.Minute + 59*time.Second)
			filters.EndDate = &dateTo
		} else {
			http.Error(w, "Invalid dateTo format. Use YYYY-MM-DD", http.StatusBadRequest)
			return
		}
	}

	// Parse amount filters
	if amountMinStr := r.URL.Query().Get("amountMin"); amountMinStr != "" {
		if amountMin, err := strconv.ParseFloat(amountMinStr, 64); err == nil && amountMin >= 0 {
			filters.AmountMin = &amountMin
		} else {
			http.Error(w, "Invalid amountMin value", http.StatusBadRequest)
			return
		}
	}

	if amountMaxStr := r.URL.Query().Get("amountMax"); amountMaxStr != "" {
		if amountMax, err := strconv.ParseFloat(amountMaxStr, 64); err == nil && amountMax >= 0 {
			filters.AmountMax = &amountMax
		} else {
			http.Error(w, "Invalid amountMax value", http.StatusBadRequest)
			return
		}
	}

	// Parse orderID filter
	if orderIDStr := r.URL.Query().Get("orderID"); orderIDStr != "" {
		if orderID, err := uuid.Parse(orderIDStr); err == nil {
			filters.OrderID = &orderID
		} else {
			http.Error(w, "Invalid orderID format", http.StatusBadRequest)
			return
		}
	}

	// 🎯 FIXED: Use enhanced repository with proper filtering and pagination
	paginatedResult, err := h.enhancedRepo.FindWithPagination(page, limit, filters)
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	// 🎯 FIXED: Return properly structured response
	response := map[string]interface{}{
		"payments": paginatedResult.Payments,
		"pagination": map[string]interface{}{
			"page":       paginatedResult.Page,
			"limit":      paginatedResult.Limit,
			"total":      paginatedResult.Total,
			"totalPages": paginatedResult.TotalPages,
		},
		"filters": map[string]interface{}{
			"status":    r.URL.Query().Get("status"),
			"method":    r.URL.Query().Get("method"),
			"dateFrom":  r.URL.Query().Get("dateFrom"),
			"dateTo":    r.URL.Query().Get("dateTo"),
			"amountMin": r.URL.Query().Get("amountMin"),
			"amountMax": r.URL.Query().Get("amountMax"),
			"orderID":   r.URL.Query().Get("orderID"),
		},
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
}

// 🎯 FIXED: Add validation helper methods
func (h *PaymentHandler) isValidStatus(status models.PaymentStatus) bool {
	validStatuses := []models.PaymentStatus{
		models.Pending,
		models.Completed,
		models.Failed,
		models.Refunded,
		models.PartiallyRefunded,
	}

	for _, validStatus := range validStatuses {
		if status == validStatus {
			return true
		}
	}
	return false
}

func (h *PaymentHandler) isValidMethod(method models.PaymentMethod) bool {
	validMethods := []models.PaymentMethod{
		models.CreditCard,
		models.DebitCard,
		models.PayPal,
		models.BankTransfer,
		models.Crypto,
		models.Points,
		models.GiftCard,
	}

	for _, validMethod := range validMethods {
		if method == validMethod {
			return true
		}
	}
	return false
}

// CreatePayment creates a new payment
func (h *PaymentHandler) CreatePayment(w http.ResponseWriter, r *http.Request) {
	var payment models.Payment
	if err := json.NewDecoder(r.Body).Decode(&payment); err != nil {
		http.Error(w, err.Error(), http.StatusBadRequest)
		return
	}

	if err := h.paymentService.CreatePayment(&payment); err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusCreated)
	json.NewEncoder(w).Encode(payment)
}

// GetPayment gets a payment by ID
func (h *PaymentHandler) GetPayment(w http.ResponseWriter, r *http.Request) {
	idStr := chi.URLParam(r, "id")
	id, err := uuid.Parse(idStr)
	if err != nil {
		http.Error(w, "Invalid payment ID", http.StatusBadRequest)
		return
	}

	payment, err := h.paymentService.GetPaymentByID(id)
	if err != nil {
		http.Error(w, "Payment not found", http.StatusNotFound)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(payment)
}

// UpdatePayment updates a payment
func (h *PaymentHandler) UpdatePayment(w http.ResponseWriter, r *http.Request) {
	idStr := chi.URLParam(r, "id")
	id, err := uuid.Parse(idStr)
	if err != nil {
		http.Error(w, "Invalid payment ID", http.StatusBadRequest)
		return
	}

	var payment models.Payment
	if err := json.NewDecoder(r.Body).Decode(&payment); err != nil {
		http.Error(w, err.Error(), http.StatusBadRequest)
		return
	}

	payment.ID = id
	if err := h.paymentService.UpdatePayment(&payment); err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(payment)
}

// DeletePayment deletes a payment
func (h *PaymentHandler) DeletePayment(w http.ResponseWriter, r *http.Request) {
	idStr := chi.URLParam(r, "id")
	id, err := uuid.Parse(idStr)
	if err != nil {
		http.Error(w, "Invalid payment ID", http.StatusBadRequest)
		return
	}

	if err := h.paymentService.DeletePayment(id); err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	w.WriteHeader(http.StatusNoContent)
}

// GetPaymentsByOrder gets payments by order ID
func (h *PaymentHandler) GetPaymentsByOrder(w http.ResponseWriter, r *http.Request) {
	orderIDStr := chi.URLParam(r, "orderID")
	orderID, err := uuid.Parse(orderIDStr)
	if err != nil {
		http.Error(w, "Invalid order ID", http.StatusBadRequest)
		return
	}

	payments, err := h.paymentService.GetPaymentsByOrderID(orderID)
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(payments)
}

// ProcessPayment processes a payment
func (h *PaymentHandler) ProcessPayment(w http.ResponseWriter, r *http.Request) {
	idStr := chi.URLParam(r, "id")
	id, err := uuid.Parse(idStr)
	if err != nil {
		http.Error(w, "Invalid payment ID", http.StatusBadRequest)
		return
	}

	if err := h.paymentService.ProcessPayment(id); err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	payment, _ := h.paymentService.GetPaymentByID(id)
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(payment)
}

// RefundPayment refunds a payment
func (h *PaymentHandler) RefundPayment(w http.ResponseWriter, r *http.Request) {
	idStr := chi.URLParam(r, "id")
	id, err := uuid.Parse(idStr)
	if err != nil {
		http.Error(w, "Invalid payment ID", http.StatusBadRequest)
		return
	}

	var refundRequest struct {
		Amount float64 `json:"amount,omitempty"`
		Reason string  `json:"reason,omitempty"`
	}

	if err := json.NewDecoder(r.Body).Decode(&refundRequest); err != nil {
		// If no body provided, refund the full amount
		if err := h.paymentService.RefundPayment(id); err != nil {
			http.Error(w, err.Error(), http.StatusInternalServerError)
			return
		}
	} else {
		// TODO: Implement partial refund in service
		if err := h.paymentService.RefundPayment(id); err != nil {
			http.Error(w, err.Error(), http.StatusInternalServerError)
			return
		}
	}

	payment, _ := h.paymentService.GetPaymentByID(id)
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(payment)
}

// GetPaymentStatus gets payment status
func (h *PaymentHandler) GetPaymentStatus(w http.ResponseWriter, r *http.Request) {
	idStr := chi.URLParam(r, "id")
	id, err := uuid.Parse(idStr)
	if err != nil {
		http.Error(w, "Invalid payment ID", http.StatusBadRequest)
		return
	}

	status, err := h.paymentService.GetPaymentStatus(id)
	if err != nil {
		http.Error(w, err.Error(), http.StatusNotFound)
		return
	}

	response := map[string]interface{}{
		"payment_id": id,
		"status":     status,
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
}

// GetPaymentInvoice gets the invoice for a payment
func (h *PaymentHandler) GetPaymentInvoice(w http.ResponseWriter, r *http.Request) {
	paymentIDStr := chi.URLParam(r, "id")
	paymentID, err := uuid.Parse(paymentIDStr)
	if err != nil {
		http.Error(w, "Invalid payment ID", http.StatusBadRequest)
		return
	}

	invoice, err := h.orderPaymentService.GetPaymentInvoice(paymentID)
	if err != nil {
		http.Error(w, "Invoice not found for payment", http.StatusNotFound)
		return
	}

	response := map[string]interface{}{
		"id":            invoice.ID.String(),
		"orderID":       invoice.OrderID.String(),
		"paymentID":     invoice.PaymentID.String(),
		"invoiceNumber": invoice.InvoiceNumber,
		"issueDate":     invoice.IssueDate.Format("2006-01-02"),
		"dueDate":       invoice.DueDate.Format("2006-01-02"),
		"status":        "issued",
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
}

// GetPaymentInvoicePDF generates and returns the invoice PDF
func (h *PaymentHandler) GetPaymentInvoicePDF(w http.ResponseWriter, r *http.Request) {
	paymentIDStr := chi.URLParam(r, "id")
	paymentID, err := uuid.Parse(paymentIDStr)
	if err != nil {
		http.Error(w, "Invalid payment ID", http.StatusBadRequest)
		return
	}

	invoice, err := h.orderPaymentService.GetPaymentInvoice(paymentID)
	if err != nil {
		http.Error(w, "Invoice not found for payment", http.StatusNotFound)
		return
	}

	pdfData, err := h.invoiceService.GeneratePDF(invoice.ID)
	if err != nil {
		http.Error(w, "Failed to generate PDF", http.StatusInternalServerError)
		return
	}

	filename := "invoice.pdf"
	if invoice != nil {
		filename = "invoice_" + invoice.InvoiceNumber + ".pdf"
	}

	w.Header().Set("Content-Type", "application/pdf")
	w.Header().Set("Content-Disposition", "attachment; filename="+filename)
	w.WriteHeader(http.StatusOK)
	w.Write(pdfData)
}

// EmailPaymentInvoice sends the invoice via email
func (h *PaymentHandler) EmailPaymentInvoice(w http.ResponseWriter, r *http.Request) {
	paymentIDStr := chi.URLParam(r, "id")
	paymentID, err := uuid.Parse(paymentIDStr)
	if err != nil {
		http.Error(w, "Invalid payment ID", http.StatusBadRequest)
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

	invoice, err := h.orderPaymentService.GetPaymentInvoice(paymentID)
	if err != nil {
		http.Error(w, "Invoice not found for payment", http.StatusNotFound)
		return
	}

	if err := h.invoiceService.SendToEmail(invoice.ID, emailRequest.Email); err != nil {
		http.Error(w, "Failed to send email", http.StatusInternalServerError)
		return
	}

	response := map[string]interface{}{
		"message":   "Invoice sent successfully",
		"email":     emailRequest.Email,
		"invoiceID": invoice.ID.String(),
		"paymentID": paymentID.String(),
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
}

// GetPaymentTransactions gets all transactions for a payment
func (h *PaymentHandler) GetPaymentTransactions(w http.ResponseWriter, r *http.Request) {
	paymentIDStr := chi.URLParam(r, "id")
	paymentID, err := uuid.Parse(paymentIDStr)
	if err != nil {
		http.Error(w, "Invalid payment ID", http.StatusBadRequest)
		return
	}

	// TODO: You'll need to add this method to your service
	// For now, return empty array
	response := map[string]interface{}{
		"paymentID":    paymentID.String(),
		"transactions": []interface{}{},
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
}
