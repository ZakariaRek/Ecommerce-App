// Payment-Service/internal/handler/transaction_handler.go
package handler

import (
	"encoding/json"
	"net/http"
	"time"

	"github.com/ZakariaRek/Ecommerce-App/Payment-Service/internal/models"
	"github.com/ZakariaRek/Ecommerce-App/Payment-Service/internal/service"
	"github.com/go-chi/chi/v5"
	"github.com/google/uuid"
)

// TransactionHandler handles transaction-related HTTP requests
type TransactionHandler struct {
	transactionService service.TransactionService
	paymentService     service.PaymentService
}

// NewTransactionHandler creates a new transaction handler
func NewTransactionHandler(
	transactionService service.TransactionService,
	paymentService service.PaymentService,
) *TransactionHandler {
	return &TransactionHandler{
		transactionService: transactionService,
		paymentService:     paymentService,
	}
}

// RegisterRoutes registers transaction routes
func (h *TransactionHandler) RegisterRoutes(r chi.Router) {
	r.Route("/transactions", func(r chi.Router) {
		// Basic transaction operations
		r.Post("/", h.CreateTransaction)
		r.Get("/{id}", h.GetTransaction)
		r.Put("/{id}", h.UpdateTransaction)
		r.Post("/{id}/verify", h.VerifyTransaction)
		r.Post("/{id}/retry", h.RetryTransaction)

		// Transaction queries
		r.Get("/", h.GetTransactions)
		r.Get("/payment/{paymentID}", h.GetTransactionsByPayment)
		r.Get("/gateway/{gateway}", h.GetTransactionsByGateway)
		r.Get("/failed", h.GetFailedTransactions)

		// Transaction analytics
		r.Get("/analytics", h.GetTransactionAnalytics)
		r.Get("/analytics/gateways", h.GetGatewayAnalytics)
		r.Get("/analytics/hourly", h.GetHourlyAnalytics)

		// Bulk operations
		r.Post("/bulk/retry", h.BulkRetryTransactions)
		r.Post("/bulk/verify", h.BulkVerifyTransactions)
	})
}

// CreateTransaction creates a new transaction
func (h *TransactionHandler) CreateTransaction(w http.ResponseWriter, r *http.Request) {
	var transaction models.PaymentTransaction
	if err := json.NewDecoder(r.Body).Decode(&transaction); err != nil {
		http.Error(w, "Invalid request body", http.StatusBadRequest)
		return
	}

	// Validate required fields
	if transaction.PaymentID == uuid.Nil {
		http.Error(w, "Payment ID is required", http.StatusBadRequest)
		return
	}

	if transaction.PaymentGateway == "" {
		http.Error(w, "Payment gateway is required", http.StatusBadRequest)
		return
	}

	if err := h.transactionService.CreateTransaction(&transaction); err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusCreated)
	json.NewEncoder(w).Encode(transaction)
}

// GetTransaction gets a transaction by ID
func (h *TransactionHandler) GetTransaction(w http.ResponseWriter, r *http.Request) {
	idStr := chi.URLParam(r, "id")
	id, err := uuid.Parse(idStr)
	if err != nil {
		http.Error(w, "Invalid transaction ID", http.StatusBadRequest)
		return
	}

	transaction, err := h.transactionService.GetTransactionByID(id)
	if err != nil {
		http.Error(w, "Transaction not found", http.StatusNotFound)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(transaction)
}

// UpdateTransaction updates a transaction
func (h *TransactionHandler) UpdateTransaction(w http.ResponseWriter, r *http.Request) {
	idStr := chi.URLParam(r, "id")
	id, err := uuid.Parse(idStr)
	if err != nil {
		http.Error(w, "Invalid transaction ID", http.StatusBadRequest)
		return
	}

	var transaction models.PaymentTransaction
	if err := json.NewDecoder(r.Body).Decode(&transaction); err != nil {
		http.Error(w, "Invalid request body", http.StatusBadRequest)
		return
	}

	transaction.ID = id
	if err := h.transactionService.UpdateTransaction(&transaction); err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(transaction)
}

// VerifyTransaction verifies a transaction with the payment gateway
func (h *TransactionHandler) VerifyTransaction(w http.ResponseWriter, r *http.Request) {
	idStr := chi.URLParam(r, "id")
	id, err := uuid.Parse(idStr)
	if err != nil {
		http.Error(w, "Invalid transaction ID", http.StatusBadRequest)
		return
	}

	isValid, err := h.transactionService.VerifyTransaction(id)
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	response := map[string]interface{}{
		"transactionID": id,
		"isValid":       isValid,
		"verifiedAt":    time.Now(),
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
}

// RetryTransaction retries a failed transaction
func (h *TransactionHandler) RetryTransaction(w http.ResponseWriter, r *http.Request) {
	idStr := chi.URLParam(r, "id")
	id, err := uuid.Parse(idStr)
	if err != nil {
		http.Error(w, "Invalid transaction ID", http.StatusBadRequest)
		return
	}

	if err := h.transactionService.RetryTransaction(id); err != nil {
		http.Error(w, err.Error(), http.StatusBadRequest)
		return
	}

	response := map[string]interface{}{
		"message":       "Transaction retry initiated",
		"transactionID": id,
		"retriedAt":     time.Now(),
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
}

// GetTransactions gets transactions with filtering
func (h *TransactionHandler) GetTransactions(w http.ResponseWriter, r *http.Request) {
	// Parse query parameters for filtering
	gateway := r.URL.Query().Get("gateway")
	status := r.URL.Query().Get("status")
	dateFrom := r.URL.Query().Get("dateFrom")
	dateTo := r.URL.Query().Get("dateTo")

	var transactions []*models.PaymentTransaction
	var err error

	// Apply filters based on query parameters
	if gateway != "" {
		transactions, err = h.transactionService.GetTransactionsByGateway(gateway)
	} else if dateFrom != "" && dateTo != "" {
		startDate, _ := time.Parse("2006-01-02", dateFrom)
		endDate, _ := time.Parse("2006-01-02", dateTo)
		transactions, err = h.transactionService.GetTransactionsByDateRange(startDate, endDate)
	} else {
		// For now, return empty array since we don't have GetAllTransactions method
		transactions = []*models.PaymentTransaction{}
	}

	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	// Filter by status if provided
	if status != "" {
		var filtered []*models.PaymentTransaction
		for _, tx := range transactions {
			if tx.Status == status {
				filtered = append(filtered, tx)
			}
		}
		transactions = filtered
	}

	response := map[string]interface{}{
		"transactions": transactions,
		"count":        len(transactions),
		"filters": map[string]string{
			"gateway":  gateway,
			"status":   status,
			"dateFrom": dateFrom,
			"dateTo":   dateTo,
		},
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
}

// GetTransactionsByPayment gets all transactions for a specific payment
func (h *TransactionHandler) GetTransactionsByPayment(w http.ResponseWriter, r *http.Request) {
	paymentIDStr := chi.URLParam(r, "paymentID")
	paymentID, err := uuid.Parse(paymentIDStr)
	if err != nil {
		http.Error(w, "Invalid payment ID", http.StatusBadRequest)
		return
	}

	transactions, err := h.transactionService.GetTransactionsByPaymentID(paymentID)
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	response := map[string]interface{}{
		"paymentID":    paymentID,
		"transactions": transactions,
		"count":        len(transactions),
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
}

// GetTransactionsByGateway gets transactions for a specific gateway
func (h *TransactionHandler) GetTransactionsByGateway(w http.ResponseWriter, r *http.Request) {
	gateway := chi.URLParam(r, "gateway")

	transactions, err := h.transactionService.GetTransactionsByGateway(gateway)
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	response := map[string]interface{}{
		"gateway":      gateway,
		"transactions": transactions,
		"count":        len(transactions),
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
}

// GetFailedTransactions gets all failed transactions
func (h *TransactionHandler) GetFailedTransactions(w http.ResponseWriter, r *http.Request) {
	transactions, err := h.transactionService.GetFailedTransactions()
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	response := map[string]interface{}{
		"failedTransactions": transactions,
		"count":              len(transactions),
		"retrievedAt":        time.Now(),
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
}

// GetTransactionAnalytics gets comprehensive transaction analytics
func (h *TransactionHandler) GetTransactionAnalytics(w http.ResponseWriter, r *http.Request) {
	analytics, err := h.transactionService.GetTransactionAnalytics()
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(analytics)
}

// GetGatewayAnalytics gets gateway-specific analytics
func (h *TransactionHandler) GetGatewayAnalytics(w http.ResponseWriter, r *http.Request) {
	analytics, err := h.transactionService.GetTransactionAnalytics()
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	// Extract only gateway breakdown
	response := map[string]interface{}{
		"gatewayBreakdown": analytics.GatewayBreakdown,
		"generatedAt":      time.Now(),
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
}

// GetHourlyAnalytics gets hourly transaction distribution
func (h *TransactionHandler) GetHourlyAnalytics(w http.ResponseWriter, r *http.Request) {
	analytics, err := h.transactionService.GetTransactionAnalytics()
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}

	// Extract only hourly distribution
	response := map[string]interface{}{
		"hourlyDistribution": analytics.HourlyDistribution,
		"generatedAt":        time.Now(),
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
}

// BulkRetryTransactions retries multiple failed transactions
func (h *TransactionHandler) BulkRetryTransactions(w http.ResponseWriter, r *http.Request) {
	var request struct {
		TransactionIDs []string `json:"transactionIds"`
	}

	if err := json.NewDecoder(r.Body).Decode(&request); err != nil {
		http.Error(w, "Invalid request body", http.StatusBadRequest)
		return
	}

	if len(request.TransactionIDs) == 0 {
		http.Error(w, "No transaction IDs provided", http.StatusBadRequest)
		return
	}

	results := make([]map[string]interface{}, 0, len(request.TransactionIDs))

	for _, idStr := range request.TransactionIDs {
		id, err := uuid.Parse(idStr)
		if err != nil {
			results = append(results, map[string]interface{}{
				"transactionID": idStr,
				"success":       false,
				"error":         "Invalid transaction ID",
			})
			continue
		}

		err = h.transactionService.RetryTransaction(id)
		if err != nil {
			results = append(results, map[string]interface{}{
				"transactionID": idStr,
				"success":       false,
				"error":         err.Error(),
			})
		} else {
			results = append(results, map[string]interface{}{
				"transactionID": idStr,
				"success":       true,
				"message":       "Retry initiated",
			})
		}
	}

	response := map[string]interface{}{
		"bulkRetryResults": results,
		"totalProcessed":   len(request.TransactionIDs),
		"processedAt":      time.Now(),
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
}

// BulkVerifyTransactions verifies multiple transactions
func (h *TransactionHandler) BulkVerifyTransactions(w http.ResponseWriter, r *http.Request) {
	var request struct {
		TransactionIDs []string `json:"transactionIds"`
	}

	if err := json.NewDecoder(r.Body).Decode(&request); err != nil {
		http.Error(w, "Invalid request body", http.StatusBadRequest)
		return
	}

	if len(request.TransactionIDs) == 0 {
		http.Error(w, "No transaction IDs provided", http.StatusBadRequest)
		return
	}

	results := make([]map[string]interface{}, 0, len(request.TransactionIDs))

	for _, idStr := range request.TransactionIDs {
		id, err := uuid.Parse(idStr)
		if err != nil {
			results = append(results, map[string]interface{}{
				"transactionID": idStr,
				"success":       false,
				"error":         "Invalid transaction ID",
			})
			continue
		}

		isValid, err := h.transactionService.VerifyTransaction(id)
		if err != nil {
			results = append(results, map[string]interface{}{
				"transactionID": idStr,
				"success":       false,
				"error":         err.Error(),
			})
		} else {
			results = append(results, map[string]interface{}{
				"transactionID": idStr,
				"success":       true,
				"isValid":       isValid,
				"verifiedAt":    time.Now(),
			})
		}
	}

	response := map[string]interface{}{
		"bulkVerifyResults": results,
		"totalProcessed":    len(request.TransactionIDs),
		"processedAt":       time.Now(),
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response)
}
