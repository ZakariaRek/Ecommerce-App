package repository

import (
	"github.com/google/uuid"
	"github.com/yourorg/payment-system/internal/models"
	"gorm.io/gorm"
)

// PaymentTransactionRepository interface
type PaymentTransactionRepository interface {
	Create(transaction *models.PaymentTransaction) error
	FindByID(id uuid.UUID) (*models.PaymentTransaction, error)
	FindByPaymentID(paymentID uuid.UUID) ([]*models.PaymentTransaction, error)
	Update(transaction *models.PaymentTransaction) error
}

// paymentTransactionRepository implements PaymentTransactionRepository
type paymentTransactionRepository struct {
	db *gorm.DB
}

// NewPaymentTransactionRepository creates a new payment transaction repository
func NewPaymentTransactionRepository(db *gorm.DB) PaymentTransactionRepository {
	return &paymentTransactionRepository{db}
}

// Create creates a new payment transaction
func (r *paymentTransactionRepository) Create(transaction *models.PaymentTransaction) error {
	return r.db.Create(transaction).Error
}

// FindByID finds a payment transaction by ID
func (r *paymentTransactionRepository) FindByID(id uuid.UUID) (*models.PaymentTransaction, error) {
	var transaction models.PaymentTransaction
	err := r.db.Where("id = ?", id).First(&transaction).Error
	return &transaction, err
}

// FindByPaymentID finds payment transactions by payment ID
func (r *paymentTransactionRepository) FindByPaymentID(paymentID uuid.UUID) ([]*models.PaymentTransaction, error) {
	var transactions []*models.PaymentTransaction
	err := r.db.Where("payment_id = ?", paymentID).Find(&transactions).Error
	return transactions, err
}

// Update updates a payment transaction
func (r *paymentTransactionRepository) Update(transaction *models.PaymentTransaction) error {
	return r.db.Save(transaction).Error
}
