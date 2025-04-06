package service

import (
	"fmt"
	"time"

	"github.com/google/uuid"
	"github.com/yourorg/payment-system/internal/models"
	"github.com/yourorg/payment-system/internal/repository"
)

// InvoiceService interface
type InvoiceService interface {
	CreateInvoice(invoice *models.Invoice) error
	GetInvoiceByID(id uuid.UUID) (*models.Invoice, error)
	GetInvoicesByOrderID(orderID uuid.UUID) ([]*models.Invoice, error)
	GetInvoiceByPaymentID(paymentID uuid.UUID) (*models.Invoice, error)
	UpdateInvoice(invoice *models.Invoice) error
	GeneratePDF(id uuid.UUID) ([]byte, error)
	SendToEmail(id uuid.UUID, email string) error
}

// invoiceService implements InvoiceService
type invoiceService struct {
	repo repository.InvoiceRepository
}

// NewInvoiceService creates a new invoice service
func NewInvoiceService(repo repository.InvoiceRepository) InvoiceService {
	return &invoiceService{repo}
}

// CreateInvoice creates a new invoice
func (s *invoiceService) CreateInvoice(invoice *models.Invoice) error {
	if invoice.InvoiceNumber == "" {
		// Generate invoice number
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

// UpdateInvoice updates an invoice
func (s *invoiceService) UpdateInvoice(invoice *models.Invoice) error {
	return s.repo.Update(invoice)
}

// GeneratePDF generates a PDF for an invoice
func (s *invoiceService) GeneratePDF(id uuid.UUID) ([]byte, error) {
	// In a real application, this would generate a PDF file
	// For now, we'll just return a placeholder
	invoice, err := s.repo.FindByID(id)
	if err != nil {
		return nil, err
	}

	// Placeholder for PDF generation
	return []byte(fmt.Sprintf("Invoice PDF for %s", invoice.InvoiceNumber)), nil
}

// SendToEmail sends an invoice to an email
func (s *invoiceService) SendToEmail(id uuid.UUID, email string) error {
	// In a real application, this would send an email with the invoice
	// For now, we'll just log it
	_, err := s.repo.FindByID(id)
	if err != nil {
		return err
	}

	// Placeholder for email sending
	fmt.Printf("Email with invoice %s sent to %s\n", id, email)
	return nil
}
