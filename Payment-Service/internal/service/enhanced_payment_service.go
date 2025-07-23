// Payment-Service/internal/service/enhanced_payment_service.go
package service

import (
	"errors"
	"fmt"
	"log"

	"github.com/ZakariaRek/Ecommerce-App/Payment-Service/internal/models"
	"github.com/ZakariaRek/Ecommerce-App/Payment-Service/internal/repository"
	"github.com/google/uuid"
)

// EnhancedPaymentService extends PaymentService with automatic invoice generation
type EnhancedPaymentService struct {
	paymentRepo    repository.PaymentRepository
	transRepo      repository.PaymentTransactionRepository
	invoiceService InvoiceService
}

// NewEnhancedPaymentService creates a new enhanced payment service
func NewEnhancedPaymentService(
	paymentRepo repository.PaymentRepository,
	transRepo repository.PaymentTransactionRepository,
	invoiceService InvoiceService,
) PaymentService {
	return &EnhancedPaymentService{
		paymentRepo:    paymentRepo,
		transRepo:      transRepo,
		invoiceService: invoiceService,
	}
}

// CreatePayment creates a new payment
func (s *EnhancedPaymentService) CreatePayment(payment *models.Payment) error {
	// Set initial status
	if payment.Status == "" {
		payment.Status = models.Pending
	}
	return s.paymentRepo.Create(payment)
}

// GetPaymentByID gets a payment by ID
func (s *EnhancedPaymentService) GetPaymentByID(id uuid.UUID) (*models.Payment, error) {
	return s.paymentRepo.FindByID(id)
}

// UpdatePayment updates a payment
func (s *EnhancedPaymentService) UpdatePayment(payment *models.Payment) error {
	return s.paymentRepo.Update(payment)
}

// DeletePayment deletes a payment
func (s *EnhancedPaymentService) DeletePayment(id uuid.UUID) error {
	return s.paymentRepo.Delete(id)
}

// GetAllPayments gets all payments
func (s *EnhancedPaymentService) GetAllPayments() ([]*models.Payment, error) {
	return s.paymentRepo.FindAll()
}

// GetPaymentsByOrderID gets payments by order ID
func (s *EnhancedPaymentService) GetPaymentsByOrderID(orderID uuid.UUID) ([]*models.Payment, error) {
	return s.paymentRepo.FindByOrderID(orderID)
}

// ProcessPayment processes a payment with automatic invoice generation
func (s *EnhancedPaymentService) ProcessPayment(id uuid.UUID) error {
	payment, err := s.paymentRepo.FindByID(id)
	if err != nil {
		return err
	}

	if payment.Status != models.Pending {
		return errors.New("payment is not in pending status")
	}

	oldStatus := payment.Status

	// Simulate payment processing
	payment.Status = models.Completed
	err = s.paymentRepo.Update(payment)
	if err != nil {
		return fmt.Errorf("failed to update payment status: %w", err)
	}

	log.Printf("💳 PAYMENT SERVICE: Payment %s processed successfully", payment.ID)

	// 🎯 NEW: Automatically generate invoice when payment is completed
	if err := s.generateInvoiceForPayment(payment); err != nil {
		// Log error but don't fail the payment - invoice generation is secondary
		log.Printf("⚠️  Warning: Failed to generate invoice for payment %s: %v", payment.ID, err)
	}

	log.Printf("💳 PAYMENT SERVICE: Payment %s status updated from %s to %s",
		payment.ID, oldStatus, payment.Status)

	return nil
}

// RefundPayment refunds a payment with automatic credit note generation
func (s *EnhancedPaymentService) RefundPayment(id uuid.UUID) error {
	payment, err := s.paymentRepo.FindByID(id)
	if err != nil {
		return err
	}

	if payment.Status != models.Completed {
		return errors.New("payment is not in completed status")
	}

	oldStatus := payment.Status

	// Process refund
	payment.Status = models.Refunded
	err = s.paymentRepo.Update(payment)
	if err != nil {
		return fmt.Errorf("failed to update payment status: %w", err)
	}

	log.Printf("💳 PAYMENT SERVICE: Payment %s refunded successfully", payment.ID)

	// 🎯 NEW: Generate credit note for refund
	if err := s.generateRefundInvoiceForPayment(payment); err != nil {
		log.Printf("⚠️  Warning: Failed to generate refund invoice for payment %s: %v", payment.ID, err)
	}

	log.Printf("💳 PAYMENT SERVICE: Payment %s status updated from %s to %s",
		payment.ID, oldStatus, payment.Status)

	return nil
}

// GetPaymentStatus gets payment status
func (s *EnhancedPaymentService) GetPaymentStatus(id uuid.UUID) (models.PaymentStatus, error) {
	payment, err := s.paymentRepo.FindByID(id)
	if err != nil {
		return "", err
	}
	return payment.Status, nil
}

// 🎯 NEW: generateInvoiceForPayment automatically creates an invoice for a completed payment
func (s *EnhancedPaymentService) generateInvoiceForPayment(payment *models.Payment) error {
	log.Printf("📋 INVOICE: Auto-generating invoice for payment %s (Order: %s)",
		payment.ID, payment.OrderID)

	// Use the invoice service to auto-generate the invoice
	invoice, err := s.invoiceService.AutoGenerateInvoiceForPayment(payment)
	if err != nil {
		return fmt.Errorf("failed to auto-generate invoice: %w", err)
	}

	log.Printf("📋 INVOICE: Successfully auto-generated invoice %s for payment %s",
		invoice.InvoiceNumber, payment.ID)

	return nil
}

// 🎯 NEW: generateRefundInvoiceForPayment creates a credit note for refunds
func (s *EnhancedPaymentService) generateRefundInvoiceForPayment(payment *models.Payment) error {
	log.Printf("📋 REFUND INVOICE: Auto-generating credit note for refund payment %s", payment.ID)

	// Create a refund invoice (credit note) using the existing payment
	// We'll create a new invoice entry for the refund
	invoice, err := s.invoiceService.AutoGenerateInvoiceForPayment(payment)
	if err != nil {
		return fmt.Errorf("failed to auto-generate refund invoice: %w", err)
	}

	log.Printf("📋 REFUND INVOICE: Successfully auto-generated credit note %s for refund payment %s",
		invoice.InvoiceNumber, payment.ID)

	return nil
}
