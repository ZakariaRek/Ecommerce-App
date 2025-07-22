// Payment-Service/internal/service/order_payment_service.go
package service

import (
	"errors"
	"fmt"
	"log"
	"time"

	"github.com/ZakariaRek/Ecommerce-App/Payment-Service/internal/models"
	"github.com/ZakariaRek/Ecommerce-App/Payment-Service/internal/repository"
	"github.com/google/uuid"
)

// OrderPaymentService handles order-specific payment processing
type OrderPaymentService interface {
	ProcessOrderPayment(orderID uuid.UUID, amount float64, method models.PaymentMethod) (*models.Payment, error)
	AuthorizePayment(payment *models.Payment) error
	CapturePayment(paymentID uuid.UUID) error
	RefundOrderPayment(orderID uuid.UUID, amount float64) (*models.Payment, error)
	GetOrderPayments(orderID uuid.UUID) ([]*models.Payment, error)
}

type orderPaymentService struct {
	paymentRepo     repository.PaymentRepository
	transactionRepo repository.PaymentTransactionRepository
}

// NewOrderPaymentService creates a new order payment service
func NewOrderPaymentService(
	paymentRepo repository.PaymentRepository,
	transactionRepo repository.PaymentTransactionRepository,
) OrderPaymentService {
	return &orderPaymentService{
		paymentRepo:     paymentRepo,
		transactionRepo: transactionRepo,
	}
}

// ProcessOrderPayment processes payment for an order
func (s *orderPaymentService) ProcessOrderPayment(
	orderID uuid.UUID,
	amount float64,
	method models.PaymentMethod,
) (*models.Payment, error) {
	log.Printf("💳 Starting payment processing for order %s, amount: %.2f, method: %s",
		orderID, amount, method)

	// Create payment record
	payment := &models.Payment{
		ID:      uuid.New(),
		OrderID: orderID,
		Amount:  amount,
		Method:  method,
		Status:  models.Pending,
	}

	// Save payment record
	if err := s.paymentRepo.Create(payment); err != nil {
		log.Printf("💳 Failed to create payment record: %v", err)
		return nil, fmt.Errorf("failed to create payment record: %w", err)
	}

	log.Printf("💳 Created payment record with ID: %s", payment.ID)

	// Step 1: Authorize payment
	log.Printf("💳 Starting authorization for payment %s", payment.ID)
	if err := s.AuthorizePayment(payment); err != nil {
		log.Printf("💳 Authorization failed for payment %s: %v", payment.ID, err)
		// Update payment status to failed
		payment.Status = models.Failed
		s.paymentRepo.Update(payment)
		return payment, fmt.Errorf("payment authorization failed: %w", err)
	}

	log.Printf("💳 Authorization successful for payment %s", payment.ID)

	// Step 2: Capture payment (this should update status to Completed)
	log.Printf("💳 Starting capture for payment %s", payment.ID)
	if err := s.CapturePayment(payment.ID); err != nil {
		log.Printf("💳 Capture failed for payment %s: %v", payment.ID, err)
		// Update payment status to failed
		payment.Status = models.Failed
		s.paymentRepo.Update(payment)
		return payment, fmt.Errorf("payment capture failed: %w", err)
	}

	// Fetch updated payment record (CapturePayment should have updated the status)
	updatedPayment, err := s.paymentRepo.FindByID(payment.ID)
	if err != nil {
		log.Printf("💳 Failed to fetch updated payment record: %v", err)
		return payment, nil // Return original payment if fetch fails
	}

	log.Printf("💳 Payment processing completed successfully for payment %s, final status: %s",
		updatedPayment.ID, updatedPayment.Status)

	return updatedPayment, nil
}

// AuthorizePayment authorizes a payment (simulate payment gateway)
func (s *orderPaymentService) AuthorizePayment(payment *models.Payment) error {
	// Create transaction record for authorization
	transaction := &models.PaymentTransaction{
		ID:             uuid.New(),
		PaymentID:      payment.ID,
		TransactionID:  fmt.Sprintf("auth_%s_%d", payment.ID.String()[:8], time.Now().Unix()),
		PaymentGateway: s.getPaymentGateway(payment.Method),
		Status:         "authorizing",
		ResponseData: models.JSON{
			"authorization_code": fmt.Sprintf("AUTH_%d", time.Now().Unix()),
			"amount":             payment.Amount,
			"currency":           "USD",
		},
		Timestamp: time.Now(),
	}

	if err := s.transactionRepo.Create(transaction); err != nil {
		return fmt.Errorf("failed to create authorization transaction: %w", err)
	}

	// Simulate payment gateway authorization
	if err := s.simulatePaymentGateway(payment, "authorize"); err != nil {
		transaction.Status = "authorization_failed"
		transaction.ResponseData["error"] = err.Error()
		s.transactionRepo.Update(transaction)
		return err
	}

	// Update transaction status
	transaction.Status = "authorized"
	transaction.ResponseData["authorized_at"] = time.Now()
	s.transactionRepo.Update(transaction)

	return nil
}

// CapturePayment captures an authorized payment
func (s *orderPaymentService) CapturePayment(paymentID uuid.UUID) error {
	payment, err := s.paymentRepo.FindByID(paymentID)
	if err != nil {
		return fmt.Errorf("payment not found: %w", err)
	}

	log.Printf("💳 Capturing payment %s for amount %.2f", paymentID, payment.Amount)

	// Create transaction record for capture
	transaction := &models.PaymentTransaction{
		ID:             uuid.New(),
		PaymentID:      payment.ID,
		TransactionID:  fmt.Sprintf("capture_%s_%d", payment.ID.String()[:8], time.Now().Unix()),
		PaymentGateway: s.getPaymentGateway(payment.Method),
		Status:         "capturing",
		ResponseData: models.JSON{
			"capture_amount": payment.Amount,
			"currency":       "USD",
		},
		Timestamp: time.Now(),
	}

	if err := s.transactionRepo.Create(transaction); err != nil {
		log.Printf("💳 Failed to create capture transaction: %v", err)
		return fmt.Errorf("failed to create capture transaction: %w", err)
	}

	log.Printf("💳 Created capture transaction %s for payment %s", transaction.ID, paymentID)

	// Simulate payment gateway capture
	if err := s.simulatePaymentGateway(payment, "capture"); err != nil {
		log.Printf("💳 Payment gateway capture failed: %v", err)
		transaction.Status = "capture_failed"
		transaction.ResponseData["error"] = err.Error()
		s.transactionRepo.Update(transaction)
		return err
	}

	// Update transaction status
	transaction.Status = "captured"
	transaction.ResponseData["captured_at"] = time.Now()
	if err := s.transactionRepo.Update(transaction); err != nil {
		log.Printf("💳 Failed to update capture transaction status: %v", err)
	}

	// IMPORTANT: Update payment status to completed
	payment.Status = models.Completed
	if err := s.paymentRepo.Update(payment); err != nil {
		log.Printf("💳 Failed to update payment status to completed: %v", err)
		return fmt.Errorf("failed to update payment status: %w", err)
	}

	log.Printf("💳 Successfully captured payment %s, status updated to COMPLETED", paymentID)
	return nil
}

// RefundOrderPayment processes a refund for an order
func (s *orderPaymentService) RefundOrderPayment(
	orderID uuid.UUID,
	refundAmount float64,
) (*models.Payment, error) {
	// Find original payment for the order
	payments, err := s.paymentRepo.FindByOrderID(orderID)
	if err != nil {
		return nil, fmt.Errorf("failed to find payments for order: %w", err)
	}

	if len(payments) == 0 {
		return nil, errors.New("no payments found for order")
	}

	// Find completed payment to refund
	var originalPayment *models.Payment
	for _, p := range payments {
		if p.Status == models.Completed {
			originalPayment = p
			break
		}
	}

	if originalPayment == nil {
		return nil, errors.New("no completed payment found to refund")
	}

	if refundAmount > originalPayment.Amount {
		return nil, errors.New("refund amount cannot exceed original payment amount")
	}

	// Create refund payment record
	refundPayment := &models.Payment{
		ID:      uuid.New(),
		OrderID: orderID,
		Amount:  -refundAmount, // Negative amount for refund
		Method:  originalPayment.Method,
		Status:  models.Pending,
	}

	if err := s.paymentRepo.Create(refundPayment); err != nil {
		return nil, fmt.Errorf("failed to create refund payment record: %w", err)
	}

	// Process refund transaction
	transaction := &models.PaymentTransaction{
		ID:             uuid.New(),
		PaymentID:      refundPayment.ID,
		TransactionID:  fmt.Sprintf("refund_%s_%d", originalPayment.ID.String()[:8], time.Now().Unix()),
		PaymentGateway: s.getPaymentGateway(originalPayment.Method),
		Status:         "refunding",
		ResponseData: models.JSON{
			"refund_amount":       refundAmount,
			"original_payment_id": originalPayment.ID,
			"currency":            "USD",
		},
		Timestamp: time.Now(),
	}

	if err := s.transactionRepo.Create(transaction); err != nil {
		refundPayment.Status = models.Failed
		s.paymentRepo.Update(refundPayment)
		return refundPayment, fmt.Errorf("failed to create refund transaction: %w", err)
	}

	// Simulate refund processing
	if err := s.simulatePaymentGateway(refundPayment, "refund"); err != nil {
		transaction.Status = "refund_failed"
		transaction.ResponseData["error"] = err.Error()
		s.transactionRepo.Update(transaction)

		refundPayment.Status = models.Failed
		s.paymentRepo.Update(refundPayment)
		return refundPayment, err
	}

	// Update transaction and payment status
	transaction.Status = "refunded"
	transaction.ResponseData["refunded_at"] = time.Now()
	s.transactionRepo.Update(transaction)

	refundPayment.Status = models.Refunded
	s.paymentRepo.Update(refundPayment)

	// Update original payment status
	if refundAmount == originalPayment.Amount {
		originalPayment.Status = models.Refunded
	} else {
		originalPayment.Status = models.PartiallyRefunded
	}
	s.paymentRepo.Update(originalPayment)

	return refundPayment, nil
}

// GetOrderPayments retrieves all payments for an order
func (s *orderPaymentService) GetOrderPayments(orderID uuid.UUID) ([]*models.Payment, error) {
	return s.paymentRepo.FindByOrderID(orderID)
}

// getPaymentGateway returns the payment gateway based on payment method
func (s *orderPaymentService) getPaymentGateway(method models.PaymentMethod) string {
	switch method {
	case models.CreditCard, models.DebitCard:
		return "stripe"
	case models.PayPal:
		return "paypal"
	case models.BankTransfer:
		return "bank_transfer"
	case models.Crypto:
		return "crypto_gateway"
	default:
		return "default_gateway"
	}
}

// simulatePaymentGateway simulates payment gateway operations
// Enhanced simulatePaymentGateway with better logging
func (s *orderPaymentService) simulatePaymentGateway(payment *models.Payment, operation string) error {
	log.Printf("💳 Simulating payment gateway %s operation for payment %s", operation, payment.ID)

	// Simulate processing time
	time.Sleep(100 * time.Millisecond)

	// Simulate specific validation failures
	if payment.Amount <= 0 && operation != "refund" {
		return errors.New("invalid payment amount")
	}

	if payment.Amount > 10000 {
		return errors.New("payment amount exceeds limit")
	}

	// Simulate method-specific processing
	switch payment.Method {
	case models.CreditCard, models.DebitCard:
		log.Printf("💳 Processing %s transaction", payment.Method)
		// Simulate additional processing time for card transactions
		time.Sleep(50 * time.Millisecond)
	case models.BankTransfer:
		log.Printf("💳 Processing bank transfer")
		// Simulate bank processing time
		time.Sleep(200 * time.Millisecond)
	case models.PayPal:
		log.Printf("💳 Processing PayPal transaction")
		time.Sleep(75 * time.Millisecond)
	}

	log.Printf("💳 Payment gateway %s operation completed successfully", operation)
	return nil
}
