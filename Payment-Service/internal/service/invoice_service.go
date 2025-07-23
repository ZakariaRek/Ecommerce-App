// Payment-Service/internal/service/invoice_service.go - ENHANCED VERSION
package service

import (
	"fmt"
	"log"
	"time"

	"github.com/ZakariaRek/Ecommerce-App/Payment-Service/internal/models"
	"github.com/ZakariaRek/Ecommerce-App/Payment-Service/internal/repository"
	"github.com/google/uuid"
)

// InvoiceService interface
type InvoiceService interface {
	CreateInvoice(invoice *models.Invoice) error
	AutoGenerateInvoiceForPayment(payment *models.Payment) (*models.Invoice, error)
	GetInvoiceByID(id uuid.UUID) (*models.Invoice, error)
	GetInvoicesByOrderID(orderID uuid.UUID) ([]*models.Invoice, error)
	GetInvoiceByPaymentID(paymentID uuid.UUID) (*models.Invoice, error)
	UpdateInvoice(invoice *models.Invoice) error
	GeneratePDF(id uuid.UUID) ([]byte, error)
	SendToEmail(id uuid.UUID, email string) error
	GetInvoiceByNumber(invoiceNumber string) (*models.Invoice, error)
}

// invoiceService implements InvoiceService
type invoiceService struct {
	repo        repository.InvoiceRepository
	paymentRepo repository.PaymentRepository
}

// NewInvoiceService creates a new invoice service
func NewInvoiceService(repo repository.InvoiceRepository, paymentRepo repository.PaymentRepository) InvoiceService {
	return &invoiceService{
		repo:        repo,
		paymentRepo: paymentRepo,
	}
}

//func NewInvoiceService(repo repository.InvoiceRepository) InvoiceService {
//	return &invoiceService{
//		repo:        repo,
//	}}

// AutoGenerateInvoiceForPayment automatically generates an invoice for a completed payment
func (s *invoiceService) AutoGenerateInvoiceForPayment(payment *models.Payment) (*models.Invoice, error) {
	log.Printf("🧾 INVOICE SERVICE: Auto-generating invoice for payment %s, order %s",
		payment.ID.String(), payment.OrderID.String())

	// Check if invoice already exists for this payment
	existingInvoice, err := s.repo.FindByPaymentID(payment.ID)
	if err == nil && existingInvoice != nil {
		log.Printf("🧾 INVOICE SERVICE: Invoice already exists for payment %s: %s",
			payment.ID.String(), existingInvoice.InvoiceNumber)
		return existingInvoice, nil
	}

	// Generate unique invoice number
	invoiceNumber := s.generateInvoiceNumber(payment.OrderID, payment.ID)

	// Create new invoice
	invoice := &models.Invoice{
		ID:            uuid.New(),
		OrderID:       payment.OrderID,
		PaymentID:     payment.ID,
		InvoiceNumber: invoiceNumber,
		IssueDate:     time.Now(),
		DueDate:       time.Now().AddDate(0, 0, 30), // 30 days from now
	}

	// Save invoice
	err = s.repo.Create(invoice)
	if err != nil {
		log.Printf("🧾 INVOICE SERVICE: Failed to create invoice for payment %s: %v",
			payment.ID.String(), err)
		return nil, fmt.Errorf("failed to create invoice: %w", err)
	}

	log.Printf("🧾 INVOICE SERVICE: Successfully created invoice %s for payment %s",
		invoice.InvoiceNumber, payment.ID.String())

	return invoice, nil
}

// generateInvoiceNumber creates a unique invoice number
func (s *invoiceService) generateInvoiceNumber(orderID, paymentID uuid.UUID) string {
	// Format: INV-YYYYMMDD-ORDER_PREFIX-PAYMENT_PREFIX
	now := time.Now()
	orderPrefix := orderID.String()[:8]
	paymentPrefix := paymentID.String()[:8]

	return fmt.Sprintf("INV-%s-%s-%s",
		now.Format("20060102"),
		orderPrefix,
		paymentPrefix)
}

// CreateInvoice creates a new invoice
func (s *invoiceService) CreateInvoice(invoice *models.Invoice) error {
	if invoice.InvoiceNumber == "" {
		// Generate invoice number if not provided
		invoice.InvoiceNumber = fmt.Sprintf("INV-%s", time.Now().Format("20060102-150405"))
	}
	if invoice.IssueDate.IsZero() {
		invoice.IssueDate = time.Now()
	}
	if invoice.DueDate.IsZero() {
		// Default due date is 30 days after issue
		invoice.DueDate = invoice.IssueDate.AddDate(0, 0, 30)
	}
	return s.repo.Create(invoice)
}

// GetInvoiceByID gets an invoice by ID
func (s *invoiceService) GetInvoiceByID(id uuid.UUID) (*models.Invoice, error) {
	return s.repo.FindByID(id)
}

// GetInvoicesByOrderID gets invoices by order ID
func (s *invoiceService) GetInvoicesByOrderID(orderID uuid.UUID) ([]*models.Invoice, error) {
	return s.repo.FindByOrderID(orderID)
}

// GetInvoiceByPaymentID gets an invoice by payment ID
func (s *invoiceService) GetInvoiceByPaymentID(paymentID uuid.UUID) (*models.Invoice, error) {
	return s.repo.FindByPaymentID(paymentID)
}

// GetInvoiceByNumber gets an invoice by invoice number
func (s *invoiceService) GetInvoiceByNumber(invoiceNumber string) (*models.Invoice, error) {
	return s.repo.FindByInvoiceNumber(invoiceNumber)
}

// UpdateInvoice updates an invoice
func (s *invoiceService) UpdateInvoice(invoice *models.Invoice) error {
	return s.repo.Update(invoice)
}

// GeneratePDF generates a PDF for an invoice with enhanced content
func (s *invoiceService) GeneratePDF(id uuid.UUID) ([]byte, error) {
	invoice, err := s.repo.FindByID(id)
	if err != nil {
		return nil, fmt.Errorf("invoice not found: %w", err)
	}

	// Get payment details
	payment, err := s.paymentRepo.FindByID(invoice.PaymentID)
	if err != nil {
		log.Printf("🧾 INVOICE SERVICE: Warning - could not fetch payment details for invoice %s: %v",
			invoice.InvoiceNumber, err)
	}

	// Generate enhanced PDF content (placeholder - in real app use PDF library like gofpdf)
	pdfContent := s.generatePDFContent(invoice, payment)

	log.Printf("🧾 INVOICE SERVICE: Generated PDF for invoice %s", invoice.InvoiceNumber)

	return []byte(pdfContent), nil
}

// generatePDFContent creates formatted invoice content
func (s *invoiceService) generatePDFContent(invoice *models.Invoice, payment *models.Payment) string {
	content := fmt.Sprintf(`
INVOICE
=======

Invoice Number: %s
Issue Date: %s
Due Date: %s

Order Details:
--------------
Order ID: %s
Payment ID: %s
`,
		invoice.InvoiceNumber,
		invoice.IssueDate.Format("2006-01-02"),
		invoice.DueDate.Format("2006-01-02"),
		invoice.OrderID.String(),
		invoice.PaymentID.String())

	if payment != nil {
		content += fmt.Sprintf(`
Payment Details:
---------------
Amount: $%.2f
Payment Method: %s
Status: %s
Processed At: %s
`,
			payment.Amount,
			payment.Method,
			payment.Status,
			payment.CreatedAt.Format("2006-01-02 15:04:05"))
	}

	content += fmt.Sprintf(`

Thank you for your business!

Generated on: %s
`, time.Now().Format("2006-01-02 15:04:05"))

	return content
}

// SendToEmail sends an invoice to an email (enhanced with better logging)
func (s *invoiceService) SendToEmail(id uuid.UUID, email string) error {
	invoice, err := s.repo.FindByID(id)
	if err != nil {
		return fmt.Errorf("invoice not found: %w", err)
	}

	// Get payment details for email content
	payment, err := s.paymentRepo.FindByID(invoice.PaymentID)
	if err != nil {
		log.Printf("🧾 INVOICE SERVICE: Warning - could not fetch payment for email: %v", err)
	}

	// In a real application, this would integrate with email service
	log.Printf("🧾 INVOICE SERVICE: Sending invoice %s to email %s", invoice.InvoiceNumber, email)

	if payment != nil {
		log.Printf("🧾 INVOICE SERVICE: Email content - Order: %s, Amount: $%.2f, Method: %s",
			invoice.OrderID.String(), payment.Amount, payment.Method)
	}

	// Placeholder for actual email sending
	// Here you would integrate with services like SendGrid, AWS SES, etc.

	return nil
}
