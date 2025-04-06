package service

import (
	"time"

	"github.com/ZakariaRek/Ecommerce-App/Payment-Service/internal/models"
	"github.com/ZakariaRek/Ecommerce-App/Payment-Service/internal/repository"
	"github.com/google/uuid"
)

// PaymentTransactionService interface
type PaymentTransactionService interface {
	RecordTransaction(transaction *models.PaymentTransaction) error
	GetTransactionByID(id uuid.UUID) (*models.PaymentTransaction, error)
	GetTransactionsByPaymentID(paymentID uuid.UUID) ([]*models.PaymentTransaction, error)
	VerifyTransaction(id uuid.UUID) (bool, error)
}

// paymentTransactionService implements PaymentTransactionService
type paymentTransactionService struct {
	repo repository.PaymentTransactionRepository
}

// NewPaymentTransactionService creates a new payment transaction service
func NewPaymentTransactionService(repo repository.PaymentTransactionRepository) PaymentTransactionService {
	return &paymentTransactionService{repo}
}

// RecordTransaction records a payment transaction
func (s *paymentTransactionService) RecordTransaction(transaction *models.PaymentTransaction) error {
	if transaction.Timestamp.IsZero() {
		transaction.Timestamp = time.Now()
	}
	return s.repo.Create(transaction)
}

// GetTransactionByID gets a transaction by ID
func (s *paymentTransactionService) GetTransactionByID(id uuid.UUID) (*models.PaymentTransaction, error) {
	return s.repo.FindByID(id)
}

// GetTransactionsByPaymentID gets transactions by payment ID
func (s *paymentTransactionService) GetTransactionsByPaymentID(paymentID uuid.UUID) ([]*models.PaymentTransaction, error) {
	return s.repo.FindByPaymentID(paymentID)
}

// VerifyTransaction verifies a transaction
func (s *paymentTransactionService) VerifyTransaction(id uuid.UUID) (bool, error) {
	// In a real application, this would verify the transaction with a payment gateway
	transaction, err := s.repo.FindByID(id)
	if err != nil {
		return false, err
	}

	// Simple verification based on status
	return transaction.Status == "success", nil
}
