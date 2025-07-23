// // Payment-Service/internal/service/improved_invoice_service.go
package service

//
//import (
//	"fmt"
//	"log"
//	"strings"
//	"time"
//
//	"github.com/ZakariaRek/Ecommerce-App/Payment-Service/internal/models"
//	"github.com/ZakariaRek/Ecommerce-App/Payment-Service/internal/repository"
//	"github.com/google/uuid"
//)
//
//// improvedInvoiceService implements InvoiceService with enhanced auto-generation
//type improvedInvoiceService struct {
//	repo        repository.InvoiceRepository
//	paymentRepo repository.PaymentRepository
//}
//
//// NewInvoiceService creates a new improved invoice service
//func NewInvoiceService(repo repository.InvoiceRepository, paymentRepo repository.PaymentRepository) InvoiceService {
//	return &improvedInvoiceService{
//		repo:        repo,
//		paymentRepo: paymentRepo,
//	}
//}
//
//// AutoGenerateInvoiceForPayment automatically generates an invoice for a completed payment
//func (s *improvedInvoiceService) AutoGenerateInvoiceForPayment(payment *models.Payment) (*models.Invoice, error) {
//	log.Printf("🧾 INVOICE SERVICE: Auto-generating invoice for payment %s, order %s, status %s",
//		payment.ID.String(), payment.OrderID.String(), payment.Status)
//
//	// Check if invoice already exists for this payment
//	existingInvoice, err := s.repo.FindByPaymentID(payment.ID)
//	if err == nil && existingInvoice != nil {
//		log.Printf("🧾 INVOICE SERVICE: Invoice already exists for payment %s: %s",
//			payment.ID.String(), existingInvoice.InvoiceNumber)
//		return existingInvoice, nil
//	}
//
//	// Generate appropriate invoice number based on payment status
//	var invoiceNumber string
//	if payment.Status == models.Refunded {
//		invoiceNumber = s.generateCreditNoteNumber(payment.OrderID, payment.ID)
//	} else {
//		invoiceNumber = s.generateInvoiceNumber(payment.OrderID, payment.ID)
//	}
//
//	// Create new invoice
//	invoice := &models.Invoice{
//		ID:            uuid.New(),
//		OrderID:       payment.OrderID,
//		PaymentID:     payment.ID,
//		InvoiceNumber: invoiceNumber,
//		IssueDate:     time.Now(),
//		DueDate:       time.Now(), // For completed payments, due date is immediate
//	}
//
//	// Adjust due date based on payment status
//	if payment.Status == models.Pending {
//		invoice.DueDate = time.Now().AddDate(0, 0, 30) // 30 days for pending payments
//	}
//
//	// Save invoice
//	err = s.repo.Create(invoice)
//	if err != nil {
//		log.Printf("🧾 INVOICE SERVICE: Failed to create invoice for payment %s: %v",
//			payment.ID.String(), err)
//		return nil, fmt.Errorf("failed to create invoice: %w", err)
//	}
//
//	log.Printf("🧾 INVOICE SERVICE: Successfully created %s %s for payment %s",
//		s.getInvoiceType(payment.Status), invoice.InvoiceNumber, payment.ID.String())
//
//	return invoice, nil
//}
//
//// generateInvoiceNumber creates a unique invoice number for regular payments
//func (s *improvedInvoiceService) generateInvoiceNumber(orderID, paymentID uuid.UUID) string {
//	// Format: INV-YYYYMMDD-ORDER_PREFIX-PAYMENT_PREFIX
//	now := time.Now()
//	orderPrefix := orderID.String()[:8]
//	paymentPrefix := paymentID.String()[:8]
//
//	return fmt.Sprintf("INV-%s-%s-%s",
//		now.Format("20060102"),
//		strings.ToUpper(orderPrefix),
//		strings.ToUpper(paymentPrefix))
//}
//
//// generateCreditNoteNumber creates a unique credit note number for refunds
//func (s *improvedInvoiceService) generateCreditNoteNumber(orderID, paymentID uuid.UUID) string {
//	// Format: CN-YYYYMMDD-ORDER_PREFIX-PAYMENT_PREFIX (CN = Credit Note)
//	now := time.Now()
//	orderPrefix := orderID.String()[:8]
//	paymentPrefix := paymentID.String()[:8]
//
//	return fmt.Sprintf("CN-%s-%s-%s",
//		now.Format("20060102"),
//		strings.ToUpper(orderPrefix),
//		strings.ToUpper(paymentPrefix))
//}
//
//// getInvoiceType returns the invoice type based on payment status
//func (s *improvedInvoiceService) getInvoiceType(status models.PaymentStatus) string {
//	switch status {
//	case models.Refunded:
//		return "credit note"
//	case models.PartiallyRefunded:
//		return "partial credit note"
//	default:
//		return "invoice"
//	}
//}
//
//// CreateInvoice creates a new invoice manually
//func (s *improvedInvoiceService) CreateInvoice(invoice *models.Invoice) error {
//	if invoice.InvoiceNumber == "" {
//		// Generate invoice number if not provided
//		invoice.InvoiceNumber = fmt.Sprintf("INV-%s", time.Now().Format("20060102-150405"))
//	}
//	if invoice.IssueDate.IsZero() {
//		invoice.IssueDate = time.Now()
//	}
//	if invoice.DueDate.IsZero() {
//		// Default due date is 30 days after issue
//		invoice.DueDate = invoice.IssueDate.AddDate(0, 0, 30)
//	}
//
//	log.Printf("🧾 INVOICE SERVICE: Creating manual invoice %s for order %s",
//		invoice.InvoiceNumber, invoice.OrderID)
//
//	return s.repo.Create(invoice)
//}
//
//// GetInvoiceByID gets an invoice by ID
//func (s *improvedInvoiceService) GetInvoiceByID(id uuid.UUID) (*models.Invoice, error) {
//	return s.repo.FindByID(id)
//}
//
//// GetInvoicesByOrderID gets invoices by order ID
//func (s *improvedInvoiceService) GetInvoicesByOrderID(orderID uuid.UUID) ([]*models.Invoice, error) {
//	return s.repo.FindByOrderID(orderID)
//}
//
//// GetInvoiceByPaymentID gets an invoice by payment ID
//func (s *improvedInvoiceService) GetInvoiceByPaymentID(paymentID uuid.UUID) (*models.Invoice, error) {
//	return s.repo.FindByPaymentID(paymentID)
//}
//
//// GetInvoiceByNumber gets an invoice by invoice number
//func (s *improvedInvoiceService) GetInvoiceByNumber(invoiceNumber string) (*models.Invoice, error) {
//	return s.repo.FindByInvoiceNumber(invoiceNumber)
//}
//
//// UpdateInvoice updates an invoice
//func (s *improvedInvoiceService) UpdateInvoice(invoice *models.Invoice) error {
//	return s.repo.Update(invoice)
//}
//
//// GeneratePDF generates a PDF for an invoice with enhanced content
//func (s *improvedInvoiceService) GeneratePDF(id uuid.UUID) ([]byte, error) {
//	invoice, err := s.repo.FindByID(id)
//	if err != nil {
//		return nil, fmt.Errorf("invoice not found: %w", err)
//	}
//
//	// Get payment details
//	payment, err := s.paymentRepo.FindByID(invoice.PaymentID)
//	if err != nil {
//		log.Printf("🧾 INVOICE SERVICE: Warning - could not fetch payment details for invoice %s: %v",
//			invoice.InvoiceNumber, err)
//	}
//
//	// Generate enhanced PDF content
//	pdfContent := s.generatePDFContent(invoice, payment)
//
//	log.Printf("🧾 INVOICE SERVICE: Generated PDF for invoice %s", invoice.InvoiceNumber)
//
//	return []byte(pdfContent), nil
//}
//
//// generatePDFContent creates formatted invoice content with enhanced details
//func (s *improvedInvoiceService) generatePDFContent(invoice *models.Invoice, payment *models.Payment) string {
//	documentType := "INVOICE"
//	if strings.HasPrefix(invoice.InvoiceNumber, "CN-") {
//		documentType = "CREDIT NOTE"
//	}
//
//	content := fmt.Sprintf(`
//%s
//%s
//
//Document Number: %s
//Issue Date: %s
//Due Date: %s
//
//Order Details:
//--------------
//Order ID: %s
//Payment ID: %s
//`,
//		documentType,
//		strings.Repeat("=", len(documentType)),
//		invoice.InvoiceNumber,
//		invoice.IssueDate.Format("2006-01-02"),
//		invoice.DueDate.Format("2006-01-02"),
//		invoice.OrderID.String(),
//		invoice.PaymentID.String())
//
//	if payment != nil {
//		content += fmt.Sprintf(`
//Payment Details:
//---------------
//Amount: $%.2f
//Payment Method: %s
//Status: %s
//Processed At: %s
//`,
//			payment.Amount,
//			payment.Method,
//			payment.Status,
//			payment.CreatedAt.Format("2006-01-02 15:04:05"))
//
//		// Add refund-specific information
//		if payment.Status == models.Refunded {
//			content += fmt.Sprintf(`
//
//Refund Information:
//------------------
//This document serves as a credit note for the refund processed.
//Original payment has been fully refunded.
//`)
//		}
//	}
//
//	content += fmt.Sprintf(`
//
//Thank you for your business!
//
//Generated on: %s
//Document ID: %s
//`, time.Now().Format("2006-01-02 15:04:05"), invoice.ID.String())
//
//	return content
//}
//
//// SendToEmail sends an invoice to an email with enhanced content
//func (s *improvedInvoiceService) SendToEmail(id uuid.UUID, email string) error {
//	invoice, err := s.repo.FindByID(id)
//	if err != nil {
//		return fmt.Errorf("invoice not found: %w", err)
//	}
//
//	// Get payment details for email content
//	payment, err := s.paymentRepo.FindByID(invoice.PaymentID)
//	if err != nil {
//		log.Printf("🧾 INVOICE SERVICE: Warning - could not fetch payment for email: %v", err)
//	}
//
//	documentType := "Invoice"
//	if strings.HasPrefix(invoice.InvoiceNumber, "CN-") {
//		documentType = "Credit Note"
//	}
//
//	// In a real application, this would integrate with email service
//	log.Printf("🧾 INVOICE SERVICE: Sending %s %s to email %s",
//		documentType, invoice.InvoiceNumber, email)
//
//	if payment != nil {
//		log.Printf("🧾 INVOICE SERVICE: Email content - Order: %s, Amount: $%.2f, Method: %s, Status: %s",
//			invoice.OrderID.String(), payment.Amount, payment.Method, payment.Status)
//	}
//
//	// Simulate email sending delay
//	time.Sleep(100 * time.Millisecond)
//
//	log.Printf("🧾 INVOICE SERVICE: %s %s sent successfully to %s",
//		documentType, invoice.InvoiceNumber, email)
//
//	return nil
//}
