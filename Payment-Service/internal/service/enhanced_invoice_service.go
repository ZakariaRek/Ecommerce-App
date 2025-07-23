// // Payment-Service/internal/service/enhanced_invoice_service.go
package service

//
//import (
//	"bytes"
//	"encoding/json"
//	"fmt"
//	"html/template"
//	"log"
//	"time"
//
//	"github.com/ZakariaRek/Ecommerce-App/Payment-Service/internal/models"
//	"github.com/ZakariaRek/Ecommerce-App/Payment-Service/internal/repository"
//	"github.com/google/uuid"
//)
//
//// EnhancedInvoiceService provides enhanced invoice functionality
//type EnhancedInvoiceService struct {
//	invoiceRepo repository.InvoiceRepository
//	paymentRepo repository.PaymentRepository
//}
//
//// NewEnhancedInvoiceService creates a new enhanced invoice service
//func NewEnhancedInvoiceService(
//	invoiceRepo repository.InvoiceRepository,
//	paymentRepo repository.PaymentRepository,
//) InvoiceService {
//	return &EnhancedInvoiceService{
//		invoiceRepo: invoiceRepo,
//		paymentRepo: paymentRepo,
//	}
//}
//
//// CreateInvoice creates a new invoice
//func (s *EnhancedInvoiceService) CreateInvoice(invoice *models.Invoice) error {
//	if invoice.InvoiceNumber == "" {
//		// Generate invoice number
//		invoice.InvoiceNumber = s.generateInvoiceNumber()
//	}
//
//	if invoice.IssueDate.IsZero() {
//		invoice.IssueDate = time.Now()
//	}
//
//	if invoice.DueDate.IsZero() {
//		// Default due date is 30 days after issue (or immediate for paid invoices)
//		invoice.DueDate = invoice.IssueDate.AddDate(0, 0, 30)
//	}
//
//	// Save invoice
//	if err := s.invoiceRepo.Create(invoice); err != nil {
//		return err
//	}
//
//	log.Printf("📄 INVOICE SERVICE: Created invoice %s for order %s",
//		invoice.InvoiceNumber, invoice.OrderID)
//
//	return nil
//}
//
//// GetInvoiceByID gets an invoice by ID
//func (s *EnhancedInvoiceService) GetInvoiceByID(id uuid.UUID) (*models.Invoice, error) {
//	return s.invoiceRepo.FindByID(id)
//}
//
//// GetInvoicesByOrderID gets invoices by order ID
//func (s *EnhancedInvoiceService) GetInvoicesByOrderID(orderID uuid.UUID) ([]*models.Invoice, error) {
//	return s.invoiceRepo.FindByOrderID(orderID)
//}
//
//// GetInvoiceByPaymentID gets an invoice by payment ID
//func (s *EnhancedInvoiceService) GetInvoiceByPaymentID(paymentID uuid.UUID) (*models.Invoice, error) {
//	return s.invoiceRepo.FindByPaymentID(paymentID)
//}
//
//// UpdateInvoice updates an invoice
//func (s *EnhancedInvoiceService) UpdateInvoice(invoice *models.Invoice) error {
//	return s.invoiceRepo.Update(invoice)
//}
//
//// GeneratePDF generates a professional PDF for an invoice
//func (s *EnhancedInvoiceService) GeneratePDF(id uuid.UUID) ([]byte, error) {
//	invoice, err := s.invoiceRepo.FindByID(id)
//	if err != nil {
//		return nil, err
//	}
//
//	// Get payment details
//	payment, err := s.paymentRepo.FindByID(invoice.PaymentID)
//	if err != nil {
//		return nil, fmt.Errorf("failed to get payment details: %w", err)
//	}
//
//	// Create invoice data for PDF generation
//	invoiceData := InvoiceData{
//		Invoice:     invoice,
//		Payment:     payment,
//		CompanyInfo: s.getCompanyInfo(),
//		GeneratedAt: time.Now(),
//	}
//
//	// Generate HTML content
//	htmlContent, err := s.generateInvoiceHTML(invoiceData)
//	if err != nil {
//		return nil, fmt.Errorf("failed to generate HTML: %w", err)
//	}
//
//	// For now, return HTML as bytes (in production, you'd convert HTML to PDF)
//	// You can integrate with libraries like wkhtmltopdf, chromedp, or similar
//	return []byte(htmlContent), nil
//}
//
//// InvoiceData contains all data needed for invoice generation
//type InvoiceData struct {
//	Invoice     *models.Invoice
//	Payment     *models.Payment
//	CompanyInfo CompanyInfo
//	GeneratedAt time.Time
//}
//
//// CompanyInfo contains company details for invoice
//type CompanyInfo struct {
//	Name    string
//	Address string
//	Phone   string
//	Email   string
//	Website string
//	TaxID   string
//}
//
//// generateInvoiceHTML creates HTML content for the invoice
//func (s *EnhancedInvoiceService) generateInvoiceHTML(data InvoiceData) (string, error) {
//	tmpl := `
//<!DOCTYPE html>
//<html>
//<head>
//    <meta charset="UTF-8">
//    <title>Invoice {{.Invoice.InvoiceNumber}}</title>
//    <style>
//        body {
//            font-family: Arial, sans-serif;
//            margin: 0;
//            padding: 20px;
//            color: #333;
//        }
//        .header {
//            display: flex;
//            justify-content: space-between;
//            margin-bottom: 30px;
//            border-bottom: 2px solid #007bff;
//            padding-bottom: 20px;
//        }
//        .company-info {
//            text-align: left;
//        }
//        .company-name {
//            font-size: 24px;
//            font-weight: bold;
//            color: #007bff;
//            margin-bottom: 10px;
//        }
//        .invoice-info {
//            text-align: right;
//        }
//        .invoice-title {
//            font-size: 32px;
//            font-weight: bold;
//            color: #007bff;
//            margin-bottom: 10px;
//        }
//        .info-section {
//            margin: 20px 0;
//        }
//        .info-row {
//            display: flex;
//            justify-content: space-between;
//            margin: 8px 0;
//        }
//        .label {
//            font-weight: bold;
//        }
//        .payment-details {
//            background-color: #f8f9fa;
//            padding: 15px;
//            border-radius: 5px;
//            margin: 20px 0;
//        }
//        .amount-section {
//            text-align: right;
//            margin: 30px 0;
//        }
//        .total-amount {
//            font-size: 24px;
//            font-weight: bold;
//            color: #28a745;
//            border-top: 2px solid #007bff;
//            padding-top: 10px;
//        }
//        .footer {
//            margin-top: 50px;
//            text-align: center;
//            font-size: 12px;
//            color: #666;
//        }
//        .status-badge {
//            display: inline-block;
//            padding: 4px 8px;
//            border-radius: 4px;
//            font-size: 12px;
//            font-weight: bold;
//            text-transform: uppercase;
//        }
//        .status-completed {
//            background-color: #d4edda;
//            color: #155724;
//        }
//        .status-pending {
//            background-color: #fff3cd;
//            color: #856404;
//        }
//    </style>
//</head>
//<body>
//    <div class="header">
//        <div class="company-info">
//            <div class="company-name">{{.CompanyInfo.Name}}</div>
//            <div>{{.CompanyInfo.Address}}</div>
//            <div>Phone: {{.CompanyInfo.Phone}}</div>
//            <div>Email: {{.CompanyInfo.Email}}</div>
//            <div>Website: {{.CompanyInfo.Website}}</div>
//        </div>
//        <div class="invoice-info">
//            <div class="invoice-title">INVOICE</div>
//            <div><strong>{{.Invoice.InvoiceNumber}}</strong></div>
//        </div>
//    </div>
//
//    <div class="info-section">
//        <div class="info-row">
//            <span class="label">Issue Date:</span>
//            <span>{{.Invoice.IssueDate.Format "2006-01-02"}}</span>
//        </div>
//        <div class="info-row">
//            <span class="label">Due Date:</span>
//            <span>{{.Invoice.DueDate.Format "2006-01-02"}}</span>
//        </div>
//        <div class="info-row">
//            <span class="label">Order ID:</span>
//            <span>{{.Invoice.OrderID}}</span>
//        </div>
//        <div class="info-row">
//            <span class="label">Payment ID:</span>
//            <span>{{.Invoice.PaymentID}}</span>
//        </div>
//    </div>
//
//    <div class="payment-details">
//        <h3>Payment Details</h3>
//        <div class="info-row">
//            <span class="label">Payment Method:</span>
//            <span>{{.Payment.Method}}</span>
//        </div>
//        <div class="info-row">
//            <span class="label">Payment Status:</span>
//            <span class="status-badge {{if eq .Payment.Status "COMPLETED"}}status-completed{{else}}status-pending{{end}}">
//                {{.Payment.Status}}
//            </span>
//        </div>
//        <div class="info-row">
//            <span class="label">Transaction Date:</span>
//            <span>{{.Payment.CreatedAt.Format "2006-01-02 15:04:05"}}</span>
//        </div>
//    </div>
//
//    <div class="amount-section">
//        <div class="info-row">
//            <span class="label">Amount Paid:</span>
//            <span>${{printf "%.2f" .Payment.Amount}}</span>
//        </div>
//        <div class="total-amount">
//            Total: ${{printf "%.2f" .Payment.Amount}}
//        </div>
//    </div>
//
//    <div class="footer">
//        <p>Thank you for your business!</p>
//        <p>This invoice was generated on {{.GeneratedAt.Format "2006-01-02 15:04:05"}}</p>
//        <p>Tax ID: {{.CompanyInfo.TaxID}}</p>
//    </div>
//</body>
//</html>
//`
//
//	// Parse and execute template
//	t, err := template.New("invoice").Parse(tmpl)
//	if err != nil {
//		return "", err
//	}
//
//	var buf bytes.Buffer
//	if err := t.Execute(&buf, data); err != nil {
//		return "", err
//	}
//
//	return buf.String(), nil
//}
//
//// SendToEmail sends an invoice to an email address
//func (s *EnhancedInvoiceService) SendToEmail(id uuid.UUID, email string) error {
//	invoice, err := s.invoiceRepo.FindByID(id)
//	if err != nil {
//		return err
//	}
//
//	// Get payment details
//	payment, err := s.paymentRepo.FindByID(invoice.PaymentID)
//	if err != nil {
//		return fmt.Errorf("failed to get payment details: %w", err)
//	}
//
//	// Generate PDF
//	pdfData, err := s.GeneratePDF(id)
//	if err != nil {
//		return fmt.Errorf("failed to generate PDF: %w", err)
//	}
//
//	// Create email data
//	emailData := map[string]interface{}{
//		"to":           email,
//		"subject":      fmt.Sprintf("Invoice %s - Payment Confirmation", invoice.InvoiceNumber),
//		"invoice":      invoice,
//		"payment":      payment,
//		"pdf_data":     pdfData,
//		"company_info": s.getCompanyInfo(),
//	}
//
//	// In a real application, you would integrate with an email service like:
//	// - AWS SES
//	// - SendGrid
//	// - Mailgun
//	// - SMTP server
//
//	// For now, we'll simulate sending the email
//	log.Printf("📧 EMAIL SERVICE: Sending invoice %s to %s", invoice.InvoiceNumber, email)
//	log.Printf("📧 EMAIL SERVICE: Email details: %s", s.formatEmailLog(emailData))
//
//	// Simulate email sending delay
//	time.Sleep(100 * time.Millisecond)
//
//	log.Printf("📧 EMAIL SERVICE: Invoice %s sent successfully to %s", invoice.InvoiceNumber, email)
//	return nil
//}
//
//// getCompanyInfo returns company information for invoices
//func (s *EnhancedInvoiceService) getCompanyInfo() CompanyInfo {
//	return CompanyInfo{
//		Name:    "E-Commerce Solutions Inc.",
//		Address: "123 Business St, Suite 100, City, State 12345",
//		Phone:   "+1 (555) 123-4567",
//		Email:   "billing@ecommerce-solutions.com",
//		Website: "www.ecommerce-solutions.com",
//		TaxID:   "TAX-123456789",
//	}
//}
//
//// generateInvoiceNumber creates a unique invoice number
//func (s *EnhancedInvoiceService) generateInvoiceNumber() string {
//	return fmt.Sprintf("INV-%s", time.Now().Format("20060102-150405"))
//}
//
//// formatEmailLog formats email data for logging
//func (s *EnhancedInvoiceService) formatEmailLog(emailData map[string]interface{}) string {
//	// Create a copy without the PDF data for logging
//	logData := make(map[string]interface{})
//	for k, v := range emailData {
//		if k != "pdf_data" {
//			logData[k] = v
//		}
//	}
//	logData["pdf_size"] = len(emailData["pdf_data"].([]byte))
//
//	jsonData, _ := json.MarshalIndent(logData, "", "  ")
//	return string(jsonData)
//}
