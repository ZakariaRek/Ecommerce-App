// Payment-Service/internal/service/order_payment_service.go
package service

import (
	"errors"
	"fmt"
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
		return nil, fmt.Errorf("failed to create payment record: %w", err)
	}

	// Authorize payment
	if err := s.AuthorizePayment(payment); err != nil {
		// Update payment status to failed
		payment.Status = models.Failed
		s.paymentRepo.Update(payment)
		return payment, err
	}

	// If authorization successful, capture payment
	if err := s.CapturePayment(payment.ID); err != nil {
		// Update payment status to failed
		payment.Status = models.Failed
		s.paymentRepo.Update(payment)
		return payment, err
	}

	return payment, nil
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
		return fmt.Errorf("failed to create capture transaction: %w", err)
	}

	// Simulate payment gateway capture
	if err := s.simulatePaymentGateway(payment, "capture"); err != nil {
		transaction.Status = "capture_failed"
		transaction.ResponseData["error"] = err.Error()
		s.transactionRepo.Update(transaction)
		return err
	}

	// Update transaction and payment status
	transaction.Status = "captured"
	transaction.ResponseData["captured_at"] = time.Now()
	s.transactionRepo.Update(transaction)

	// Update payment status to completed
	payment.Status = models.Completed
	return s.paymentRepo.Update(payment)
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
func (s *orderPaymentService) simulatePaymentGateway(payment *models.Payment, operation string) error {
	// Simulate processing time
	time.Sleep(100 * time.Millisecond)

	// Simulate random failures (5% failure rate)
	//if time.Now().UnixNano()%20 == 0 {
	//	return fmt.Errorf("payment gateway error: %s operation failed for payment %s", operation, payment.ID)
	//}

	// Simulate specific validation failures
	if payment.Amount <= 0 && operation != "refund" {
		return errors.New("invalid payment amount")
	}

	if payment.Amount > 10000 {
		return errors.New("payment amount exceeds limit")
	}

	// Simulate method-specific failures
	switch payment.Method {
	case models.CreditCard, models.DebitCard:
		// Simulate card validation
		//if operation == "authorize" && time.Now().UnixNano()%50 == 0 {
		//	return errors.New("card declined")
		//}
	case models.BankTransfer:
		// Simulate bank processing time
		time.Sleep(200 * time.Millisecond)
	}

	return nil
}
