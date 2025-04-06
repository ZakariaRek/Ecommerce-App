package service

import (
	"errors"

	"github.com/google/uuid"
	"github.com/yourorg/payment-system/internal/models"
	"github.com/yourorg/payment-system/internal/repository"
)

// PaymentService interface
type PaymentService interface {
	CreatePayment(payment *models.Payment) error
	GetPaymentByID(id uuid.UUID) (*models.Payment, error)
	UpdatePayment(payment *models.Payment) error
	DeletePayment(id uuid.UUID) error
	GetAllPayments() ([]*models.Payment, error)
	GetPaymentsByOrderID(orderID uuid.UUID) ([]*models.Payment, error)
	ProcessPayment(id uuid.UUID) error
	RefundPayment(id uuid.UUID) error
	GetPaymentStatus(id uuid.UUID) (models.PaymentStatus, error)
}

// paymentService implements PaymentService
type paymentService struct {
	repo      repository.PaymentRepository
	transRepo repository.PaymentTransactionRepository
}

// NewPaymentService creates a new payment service
func NewPaymentService(repo repository.PaymentRepository, transRepo repository.PaymentTransactionRepository) PaymentService {
	return &paymentService{repo, transRepo}
}

// CreatePayment creates a new payment
func (s *paymentService) CreatePayment(payment *models.Payment) error {
	// Set initial status
	if payment.Status == "" {
		payment.Status = models.Pending
	}
	return s.repo.Create(payment)
}

// GetPaymentByID gets a payment by ID
func (s *paymentService) GetPaymentByID(id uuid.UUID) (*models.Payment, error) {
	return s.repo.FindByID(id)
}

// UpdatePayment updates a payment
func (s *paymentService) UpdatePayment(payment *models.Payment) error {
	return s.repo.Update(payment)
}

// DeletePayment deletes a payment
func (s *paymentService) DeletePayment(id uuid.UUID) error {
	return s.repo.Delete(id)
}

// GetAllPayments gets all payments
func (s *paymentService) GetAllPayments() ([]*models.Payment, error) {
	return s.repo.FindAll()
}

// GetPaymentsByOrderID gets payments by order ID
func (s *paymentService) GetPaymentsByOrderID(orderID uuid.UUID) ([]*models.Payment, error) {
	return s.repo.FindByOrderID(orderID)
}

// ProcessPayment processes a payment
func (s *paymentService) ProcessPayment(id uuid.UUID) error {
	// In a real application, we would integrate with payment gateway
	payment, err := s.repo.FindByID(id)
	if err != nil {
		return err
	}

	if payment.Status != models.Pending {
		return errors.New("payment is not in pending status")
	}

	// Simulate payment processing
	// In a real app, this would involve third-party payment gateway integration
	payment.Status = models.Completed
	return s.repo.Update(payment)
}

// RefundPayment refunds a payment
func (s *paymentService) RefundPayment(id uuid.UUID) error {
	payment, err := s.repo.FindByID(id)
	if err != nil {
		return err
	}

	if payment.Status != models.Completed {
		return errors.New("payment is not in completed status")
	}

	// Simulate refund processing
	payment.Status = models.Refunded
	return s.repo.Update(payment)
}

// GetPaymentStatus gets payment status
func (s *paymentService) GetPaymentStatus(id uuid.UUID) (models.PaymentStatus, error) {
	payment, err := s.repo.FindByID(id)
	if err != nil {
		return "", err
	}
	return payment.Status, nil
}
