// Payment-Service/internal/handler/payment_handler.go - FIXED VERSION
package handler

import (
	"encoding/json"
	"net/http"
	"strconv"

	"github.com/ZakariaRek/Ecommerce-App/Payment-Service/internal/models"
	"github.com/ZakariaRek/Ecommerce-App/Payment-Service/internal/service"
	"github.com/go-chi/chi/v5"
	"github.com/google/uuid"
)

// PaymentHandler handles payment-related HTTP requests
type PaymentHandler struct {
	paymentService      service.PaymentService
	orderPaymentService service.OrderPaymentService
	invoiceService      service.InvoiceService
}

// NewPaymentHandler creates a new payment handler
func NewPaymentHandler(
	paymentService service.PaymentService,
	orderPaymentService service.OrderPaymentService,
	invoiceService service.InvoiceService,
) *PaymentHandler {
	return &PaymentHandler{
		paymentService:      paymentService,
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
		r.Get("/", h.GetAllPayments)
		r.Get("/order/{orderID}", h.GetPaymentsByOrder)
		r.Post("/{id}/process", h.ProcessPayment)
		r.Post("/{id}/refund", h.RefundPayment)
		r.Get("/{id}/status", h.GetPaymentStatus)

		// 🎯 FIXED: Add invoice routes for payments
		r.Get("/{id}/invoice", h.GetPaymentInvoice)
		r.Get("/{id}/invoice/pdf", h.GetPaymentInvoicePDF)
		r.Post("/{id}/invoice/email", h.EmailPaymentInvoice)

		// 🎯 NEW: Add transaction routes
		r.Get("/{id}/transactions", h.GetPaymentTransactions)
	})
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

// GetAllPayments gets all payments with pagination and filters
func (h *PaymentHandler) GetAllPayments(w http.ResponseWriter, r *http.Request) {
	// 🎯 NEW: Add pagination and filtering
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

	// Get filters
	status := r.URL.Query().Get("status")
	method := r.URL.Query().Get("method")
	dateFrom := r.URL.Query().Get("dateFrom")
	dateTo := r.URL.Query().Get("dateTo")

	// For now, get all payments (you should implement filtered repository method)
	payments, err := h.paymentService.GetAllPayments()
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	// 🎯 TODO: Implement actual filtering and pagination in repository
	// This is a simplified version - you should add these methods to your repository

	response := map[string]interface{}{
		"payments": payments,
		"pagination": map[string]interface{}{
			"page":       page,
			"limit":      limit,
			"total":      len(payments),
			"totalPages": (len(payments) + limit - 1) / limit,
		},
		"filters": map[string]interface{}{
			"status":   status,
			"method":   method,
			"dateFrom": dateFrom,
			"dateTo":   dateTo,
		},
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
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
		// 🎯 TODO: Implement partial refund in service
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

// 🎯 NEW: GetPaymentInvoice gets the invoice for a payment
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

// 🎯 NEW: GetPaymentInvoicePDF generates and returns the invoice PDF
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

// 🎯 NEW: EmailPaymentInvoice sends the invoice via email
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

// 🎯 NEW: GetPaymentTransactions gets all transactions for a payment
func (h *PaymentHandler) GetPaymentTransactions(w http.ResponseWriter, r *http.Request) {
	paymentIDStr := chi.URLParam(r, "id")
	paymentID, err := uuid.Parse(paymentIDStr)
	if err != nil {
		http.Error(w, "Invalid payment ID", http.StatusBadRequest)
		return
	}

	// 🎯 TODO: You'll need to add this method to your service
	// For now, return empty array
	response := map[string]interface{}{
		"paymentID":    paymentID.String(),
		"transactions": []interface{}{},
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
}
