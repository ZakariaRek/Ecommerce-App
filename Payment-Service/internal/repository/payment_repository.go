package repository

import (
	"github.com/google/uuid"
	"github.com/yourorg/payment-system/internal/models"
	"gorm.io/gorm"
)

// PaymentRepository interface
type PaymentRepository interface {
	Create(payment *models.Payment) error
	FindByID(id uuid.UUID) (*models.Payment, error)
	Update(payment *models.Payment) error
	Delete(id uuid.UUID) error
	FindAll() ([]*models.Payment, error)
	FindByOrderID(orderID uuid.UUID) ([]*models.Payment, error)
	UpdateStatus(id uuid.UUID, status models.PaymentStatus) error
}

// paymentRepository implements PaymentRepository
type paymentRepository struct {
	db *gorm.DB
}

// NewPaymentRepository creates a new payment repository
func NewPaymentRepository(db *gorm.DB) PaymentRepository {
	return &paymentRepository{db}
}

// Create creates a new payment
func (r *paymentRepository) Create(payment *models.Payment) error {
	return r.db.Create(payment).Error
}

// FindByID finds a payment by ID
func (r *paymentRepository) FindByID(id uuid.UUID) (*models.Payment, error) {
	var payment models.Payment
	err := r.db.Where("id = ?", id).First(&payment).Error
	return &payment, err
}

// Update updates a payment
func (r *paymentRepository) Update(payment *models.Payment) error {
	return r.db.Save(payment).Error
}

// Delete deletes a payment
func (r *paymentRepository) Delete(id uuid.UUID) error {
	return r.db.Delete(&models.Payment{}, id).Error
}

// FindAll returns all payments
func (r *paymentRepository) FindAll() ([]*models.Payment, error) {
	var payments []*models.Payment
	err := r.db.Find(&payments).Error
	return payments, err
}

// FindByOrderID finds payments by order ID
func (r *paymentRepository) FindByOrderID(orderID uuid.UUID) ([]*models.Payment, error) {
	var payments []*models.Payment
	err := r.db.Where("order_id = ?", orderID).Find(&payments).Error
	return payments, err
}

// UpdateStatus updates the payment status
func (r *paymentRepository) UpdateStatus(id uuid.UUID, status models.PaymentStatus) error {
	return r.db.Model(&models.Payment{}).Where("id = ?", id).Update("status", status).Error
}
