// Payment-Service/internal/service/transaction_service.go
package service

import (
	"fmt"
	"log"
	"time"

	"github.com/ZakariaRek/Ecommerce-App/Payment-Service/internal/models"
	"github.com/ZakariaRek/Ecommerce-App/Payment-Service/internal/repository"
	"github.com/google/uuid"
)

// TransactionService interface for payment transaction operations
type TransactionService interface {
	CreateTransaction(transaction *models.PaymentTransaction) error
	GetTransactionByID(id uuid.UUID) (*models.PaymentTransaction, error)
	GetTransactionsByPaymentID(paymentID uuid.UUID) ([]*models.PaymentTransaction, error)
	UpdateTransaction(transaction *models.PaymentTransaction) error
	VerifyTransaction(id uuid.UUID) (bool, error)
	GetTransactionsByDateRange(startDate, endDate time.Time) ([]*models.PaymentTransaction, error)
	GetTransactionsByGateway(gateway string) ([]*models.PaymentTransaction, error)
	GetFailedTransactions() ([]*models.PaymentTransaction, error)
	RetryTransaction(id uuid.UUID) error
	GetTransactionAnalytics() (*TransactionAnalytics, error)
}

// TransactionAnalytics represents transaction analytics data
type TransactionAnalytics struct {
	TotalTransactions      int64                    `json:"totalTransactions"`
	SuccessfulTransactions int64                    `json:"successfulTransactions"`
	FailedTransactions     int64                    `json:"failedTransactions"`
	SuccessRate            float64                  `json:"successRate"`
	GatewayBreakdown       map[string]*GatewayStats `json:"gatewayBreakdown"`
	StatusBreakdown        map[string]int64         `json:"statusBreakdown"`
	AverageResponseTime    float64                  `json:"averageResponseTime"`
	HourlyDistribution     map[int]int64            `json:"hourlyDistribution"`
}

// GatewayStats represents statistics for a specific payment gateway
type GatewayStats struct {
	TotalTransactions      int64   `json:"totalTransactions"`
	SuccessfulTransactions int64   `json:"successfulTransactions"`
	FailedTransactions     int64   `json:"failedTransactions"`
	SuccessRate            float64 `json:"successRate"`
	AverageResponseTime    float64 `json:"averageResponseTime"`
}

// transactionService implements TransactionService
type transactionService struct {
	repo        repository.PaymentTransactionRepository
	paymentRepo repository.PaymentRepository
}

// NewTransactionService creates a new transaction service
func NewTransactionService(
	repo repository.PaymentTransactionRepository,
	paymentRepo repository.PaymentRepository,
) TransactionService {
	return &transactionService{
		repo:        repo,
		paymentRepo: paymentRepo,
	}
}

// CreateTransaction creates a new payment transaction
func (s *transactionService) CreateTransaction(transaction *models.PaymentTransaction) error {
	// Set timestamp if not provided
	if transaction.Timestamp.IsZero() {
		transaction.Timestamp = time.Now()
	}

	// Generate transaction ID if not provided
	if transaction.TransactionID == "" {
		transaction.TransactionID = s.generateTransactionID(transaction.PaymentGateway)
	}

	// Set default status if not provided
	if transaction.Status == "" {
		transaction.Status = "pending"
	}

	log.Printf("💳 TRANSACTION: Creating transaction %s for payment %s via %s",
		transaction.TransactionID, transaction.PaymentID, transaction.PaymentGateway)

	return s.repo.Create(transaction)
}

// GetTransactionByID gets a transaction by ID
func (s *transactionService) GetTransactionByID(id uuid.UUID) (*models.PaymentTransaction, error) {
	return s.repo.FindByID(id)
}

// GetTransactionsByPaymentID gets all transactions for a payment
func (s *transactionService) GetTransactionsByPaymentID(paymentID uuid.UUID) ([]*models.PaymentTransaction, error) {
	return s.repo.FindByPaymentID(paymentID)
}

// UpdateTransaction updates a transaction
func (s *transactionService) UpdateTransaction(transaction *models.PaymentTransaction) error {
	log.Printf("💳 TRANSACTION: Updating transaction %s - Status: %s",
		transaction.TransactionID, transaction.Status)

	return s.repo.Update(transaction)
}

// VerifyTransaction verifies a transaction with the payment gateway
func (s *transactionService) VerifyTransaction(id uuid.UUID) (bool, error) {
	transaction, err := s.repo.FindByID(id)
	if err != nil {
		return false, fmt.Errorf("transaction not found: %w", err)
	}

	log.Printf("💳 TRANSACTION: Verifying transaction %s with gateway %s",
		transaction.TransactionID, transaction.PaymentGateway)

	// Simulate verification with payment gateway
	isValid := s.simulateGatewayVerification(transaction)

	if isValid {
		// Update transaction status
		transaction.Status = "verified"
		if transaction.ResponseData == nil {
			transaction.ResponseData = make(models.JSON)
		}
		transaction.ResponseData["verified_at"] = time.Now()
		transaction.ResponseData["verification_result"] = "valid"

		err = s.repo.Update(transaction)
		if err != nil {
			log.Printf("💳 TRANSACTION: Failed to update verification status: %v", err)
		}
	} else {
		transaction.Status = "verification_failed"
		if transaction.ResponseData == nil {
			transaction.ResponseData = make(models.JSON)
		}
		transaction.ResponseData["verification_result"] = "invalid"
		transaction.ResponseData["verification_error"] = "Transaction verification failed"

		err = s.repo.Update(transaction)
		if err != nil {
			log.Printf("💳 TRANSACTION: Failed to update verification status: %v", err)
		}
	}

	return isValid, nil
}

// GetTransactionsByDateRange gets transactions within a date range
func (s *transactionService) GetTransactionsByDateRange(startDate, endDate time.Time) ([]*models.PaymentTransaction, error) {
	// This would require adding a method to the repository
	// For now, get all transactions and filter (not efficient for production)
	transactions, err := s.getAllTransactions()
	if err != nil {
		return nil, err
	}

	var filtered []*models.PaymentTransaction
	for _, tx := range transactions {
		if tx.Timestamp.After(startDate) && tx.Timestamp.Before(endDate) {
			filtered = append(filtered, tx)
		}
	}

	return filtered, nil
}

// GetTransactionsByGateway gets transactions by payment gateway
func (s *transactionService) GetTransactionsByGateway(gateway string) ([]*models.PaymentTransaction, error) {
	// This would require adding a method to the repository
	// For now, get all transactions and filter
	transactions, err := s.getAllTransactions()
	if err != nil {
		return nil, err
	}

	var filtered []*models.PaymentTransaction
	for _, tx := range transactions {
		if tx.PaymentGateway == gateway {
			filtered = append(filtered, tx)
		}
	}

	return filtered, nil
}

// GetFailedTransactions gets all failed transactions
func (s *transactionService) GetFailedTransactions() ([]*models.PaymentTransaction, error) {
	// This would require adding a method to the repository
	transactions, err := s.getAllTransactions()
	if err != nil {
		return nil, err
	}

	var failed []*models.PaymentTransaction
	for _, tx := range transactions {
		if s.isFailedStatus(tx.Status) {
			failed = append(failed, tx)
		}
	}

	return failed, nil
}

// RetryTransaction retries a failed transaction
func (s *transactionService) RetryTransaction(id uuid.UUID) error {
	transaction, err := s.repo.FindByID(id)
	if err != nil {
		return fmt.Errorf("transaction not found: %w", err)
	}

	if !s.isFailedStatus(transaction.Status) {
		return fmt.Errorf("transaction %s is not in a failed state", transaction.TransactionID)
	}

	log.Printf("💳 TRANSACTION: Retrying failed transaction %s", transaction.TransactionID)

	// Create a new transaction for the retry
	retryTransaction := &models.PaymentTransaction{
		ID:             uuid.New(),
		PaymentID:      transaction.PaymentID,
		TransactionID:  s.generateTransactionID(transaction.PaymentGateway) + "_retry",
		PaymentGateway: transaction.PaymentGateway,
		Status:         "retrying",
		ResponseData: models.JSON{
			"original_transaction_id": transaction.TransactionID,
			"retry_attempt":           1,
			"retry_reason":            "Manual retry",
			"retry_timestamp":         time.Now(),
		},
		Timestamp: time.Now(),
	}

	err = s.repo.Create(retryTransaction)
	if err != nil {
		return fmt.Errorf("failed to create retry transaction: %w", err)
	}

	// Simulate processing the retry
	success := s.simulateGatewayProcessing(retryTransaction)

	if success {
		retryTransaction.Status = "completed"
		retryTransaction.ResponseData["retry_result"] = "success"

		// Update original payment status if this retry succeeded
		payment, err := s.paymentRepo.FindByID(transaction.PaymentID)
		if err == nil && payment.Status == models.Failed {
			payment.Status = models.Completed
			s.paymentRepo.Update(payment)
			log.Printf("💳 TRANSACTION: Payment %s status updated to completed after successful retry", payment.ID)
		}
	} else {
		retryTransaction.Status = "retry_failed"
		retryTransaction.ResponseData["retry_result"] = "failed"
		retryTransaction.ResponseData["retry_error"] = "Retry attempt failed"
	}

	return s.repo.Update(retryTransaction)
}

// GetTransactionAnalytics generates analytics for transactions
func (s *transactionService) GetTransactionAnalytics() (*TransactionAnalytics, error) {
	transactions, err := s.getAllTransactions()
	if err != nil {
		return nil, err
	}

	analytics := &TransactionAnalytics{
		GatewayBreakdown:   make(map[string]*GatewayStats),
		StatusBreakdown:    make(map[string]int64),
		HourlyDistribution: make(map[int]int64),
	}

	analytics.TotalTransactions = int64(len(transactions))

	// Initialize gateway stats
	gatewayStats := make(map[string]*GatewayStats)

	for _, tx := range transactions {
		// Count by status
		analytics.StatusBreakdown[tx.Status]++

		// Count successful vs failed
		if s.isSuccessfulStatus(tx.Status) {
			analytics.SuccessfulTransactions++
		} else if s.isFailedStatus(tx.Status) {
			analytics.FailedTransactions++
		}

		// Gateway statistics
		if gatewayStats[tx.PaymentGateway] == nil {
			gatewayStats[tx.PaymentGateway] = &GatewayStats{}
		}

		gatewayStats[tx.PaymentGateway].TotalTransactions++
		if s.isSuccessfulStatus(tx.Status) {
			gatewayStats[tx.PaymentGateway].SuccessfulTransactions++
		} else if s.isFailedStatus(tx.Status) {
			gatewayStats[tx.PaymentGateway].FailedTransactions++
		}

		// Hourly distribution
		hour := tx.Timestamp.Hour()
		analytics.HourlyDistribution[hour]++
	}

	// Calculate success rate
	if analytics.TotalTransactions > 0 {
		analytics.SuccessRate = float64(analytics.SuccessfulTransactions) / float64(analytics.TotalTransactions) * 100
	}

	// Calculate gateway success rates
	for gateway, stats := range gatewayStats {
		if stats.TotalTransactions > 0 {
			stats.SuccessRate = float64(stats.SuccessfulTransactions) / float64(stats.TotalTransactions) * 100
		}
		// Mock average response time (in production, calculate from actual data)
		stats.AverageResponseTime = s.getMockResponseTime(gateway)
		analytics.GatewayBreakdown[gateway] = stats
	}

	// Mock overall average response time
	analytics.AverageResponseTime = 285.5

	return analytics, nil
}

// Helper methods

func (s *transactionService) generateTransactionID(gateway string) string {
	timestamp := time.Now().Unix()
	return fmt.Sprintf("%s_%d_%s", gateway, timestamp, uuid.New().String()[:8])
}

func (s *transactionService) simulateGatewayVerification(transaction *models.PaymentTransaction) bool {
	// Simulate verification logic based on gateway and transaction details
	switch transaction.PaymentGateway {
	case "stripe":
		// Stripe has high reliability
		return true
	case "paypal":
		// PayPal verification
		return transaction.Status != "failed"
	case "bank_transfer":
		// Bank transfers are usually reliable once completed
		return transaction.Status == "completed" || transaction.Status == "captured"
	default:
		// Default verification
		return transaction.Status != "failed"
	}
}

func (s *transactionService) simulateGatewayProcessing(transaction *models.PaymentTransaction) bool {
	// Simulate processing with different success rates by gateway
	switch transaction.PaymentGateway {
	case "stripe":
		return true // 95% success rate simulated as always true for simplicity
	case "paypal":
		return time.Now().Unix()%10 < 9 // 90% success rate
	case "bank_transfer":
		return time.Now().Unix()%10 < 8 // 80% success rate
	default:
		return time.Now().Unix()%10 < 7 // 70% success rate
	}
}

func (s *transactionService) isSuccessfulStatus(status string) bool {
	successStatuses := []string{"completed", "captured", "authorized", "verified", "success"}
	for _, s := range successStatuses {
		if status == s {
			return true
		}
	}
	return false
}

func (s *transactionService) isFailedStatus(status string) bool {
	failedStatuses := []string{"failed", "declined", "error", "timeout", "cancelled", "retry_failed", "verification_failed"}
	for _, s := range failedStatuses {
		if status == s {
			return true
		}
	}
	return false
}

func (s *transactionService) getMockResponseTime(gateway string) float64 {
	// Mock response times by gateway (in milliseconds)
	switch gateway {
	case "stripe":
		return 245.3
	case "paypal":
		return 387.8
	case "bank_transfer":
		return 1250.0
	case "crypto_gateway":
		return 450.2
	default:
		return 300.0
	}
}

// getAllTransactions is a helper method to get all transactions
// In production, this should be implemented in the repository with proper filtering
func (s *transactionService) getAllTransactions() ([]*models.PaymentTransaction, error) {
	// This is a simplified implementation
	// In a real scenario, you'd implement proper repository methods for filtering

	// For now, we'll need to get transactions by iterating through payments
	// This is not efficient and should be improved with proper repository methods

	// Return empty slice for now - this should be implemented properly
	return []*models.PaymentTransaction{}, nil
}
