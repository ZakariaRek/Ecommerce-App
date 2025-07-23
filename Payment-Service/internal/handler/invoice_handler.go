// // Payment-Service/internal/handler/invoice_handler.go
package handler

//
//import (
//	"encoding/json"
//	"net/http"
//	"strconv"
//
//	"github.com/ZakariaRek/Ecommerce-App/Payment-Service/internal/models"
//	"github.com/ZakariaRek/Ecommerce-App/Payment-Service/internal/service"
//	"github.com/go-chi/chi/v5"
//	"github.com/google/uuid"
//)
//
//// InvoiceResponse represents the invoice response
//type InvoiceResponse struct {
//	ID            string `json:"id"`
//	OrderID       string `json:"order_id"`
//	PaymentID     string `json:"payment_id"`
//	InvoiceNumber string `json:"invoice_number"`
//	IssueDate     string `json:"issue_date"`
//	DueDate       string `json:"due_date"`
//	DownloadURL   string `json:"download_url"`
//	PDFUrl        string `json:"pdf_url"`
//}
//
//// InvoiceHandler handles invoice-related HTTP requests
//type InvoiceHandler struct {
//	invoiceService      service.InvoiceService
//	orderPaymentService service.OrderPaymentService
//}
//
//// NewInvoiceHandler creates a new invoice handler
//func NewInvoiceHandler(
//	invoiceService service.InvoiceService,
//	orderPaymentService service.OrderPaymentService,
//) *InvoiceHandler {
//	return &InvoiceHandler{
//		invoiceService:      invoiceService,
//		orderPaymentService: orderPaymentService,
//	}
//}
//
//// RegisterRoutes registers routes for invoice handler
//func (h *InvoiceHandler) RegisterRoutes(r chi.Router) {
//	r.Route("/invoices", func(r chi.Router) {
//		r.Get("/{id}", h.GetInvoice)
//		r.Get("/{id}/pdf", h.DownloadInvoicePDF)
//		r.Post("/{id}/email", h.EmailInvoice)
//	})
//
//	// Order-specific invoice routes
//	r.Route("/orders/{orderID}/invoices", func(r chi.Router) {
//		r.Get("/", h.GetOrderInvoices)
//		r.Post("/generate", h.GenerateInvoiceForOrder)
//	})
//
//	// Payment-specific invoice routes
//	r.Route("/payments/{paymentID}/invoice", func(r chi.Router) {
//		r.Get("/", h.GetPaymentInvoice)
//	})
//}
//
//// GetInvoice retrieves a specific invoice
//func (h *InvoiceHandler) GetInvoice(w http.ResponseWriter, r *http.Request) {
//	invoiceIDStr := chi.URLParam(r, "id")
//	invoiceID, err := uuid.Parse(invoiceIDStr)
//	if err != nil {
//		http.Error(w, "Invalid invoice ID", http.StatusBadRequest)
//		return
//	}
//
//	invoice, err := h.invoiceService.GetInvoiceByID(invoiceID)
//	if err != nil {
//		http.Error(w, "Invoice not found", http.StatusNotFound)
//		return
//	}
//
//	response := h.toInvoiceResponse(invoice)
//	w.Header().Set("Content-Type", "application/json")
//	json.NewEncoder(w).Encode(response)
//}
//
//// GetOrderInvoices retrieves all invoices for an order
//func (h *InvoiceHandler) GetOrderInvoices(w http.ResponseWriter, r *http.Request) {
//	orderIDStr := chi.URLParam(r, "orderID")
//	orderID, err := uuid.Parse(orderIDStr)
//	if err != nil {
//		http.Error(w, "Invalid order ID", http.StatusBadRequest)
//		return
//	}
//
//	invoices, err := h.orderPaymentService.GetOrderInvoices(orderID)
//	if err != nil {
//		http.Error(w, err.Error(), http.StatusInternalServerError)
//		return
//	}
//
//	responses := make([]InvoiceResponse, len(invoices))
//	for i, invoice := range invoices {
//		responses[i] = h.toInvoiceResponse(invoice)
//	}
//
//	w.Header().Set("Content-Type", "application/json")
//	json.NewEncoder(w).Encode(responses)
//}
//
//// GetPaymentInvoice retrieves invoice for a specific payment
//func (h *InvoiceHandler) GetPaymentInvoice(w http.ResponseWriter, r *http.Request) {
//	paymentIDStr := chi.URLParam(r, "paymentID")
//	paymentID, err := uuid.Parse(paymentIDStr)
//	if err != nil {
//		http.Error(w, "Invalid payment ID", http.StatusBadRequest)
//		return
//	}
//
//	invoice, err := h.orderPaymentService.GetInvoiceByPayment(paymentID)
//	if err != nil {
//		http.Error(w, "Invoice not found for payment", http.StatusNotFound)
//		return
//	}
//
//	response := h.toInvoiceResponse(invoice)
//	w.Header().Set("Content-Type", "application/json")
//	json.NewEncoder(w).Encode(response)
//}
//
//// DownloadInvoicePDF downloads invoice as PDF
//func (h *InvoiceHandler) DownloadInvoicePDF(w http.ResponseWriter, r *http.Request) {
//	invoiceIDStr := chi.URLParam(r, "id")
//	invoiceID, err := uuid.Parse(invoiceIDStr)
//	if err != nil {
//		http.Error(w, "Invalid invoice ID", http.StatusBadRequest)
//		return
//	}
//
//	// Get invoice details first
//	invoice, err := h.invoiceService.GetInvoiceByID(invoiceID)
//	if err != nil {
//		http.Error(w, "Invoice not found", http.StatusNotFound)
//		return
//	}
//
//	// Generate PDF
//	pdfData, err := h.orderPaymentService.GenerateInvoicePDF(invoiceID)
//	if err != nil {
//		http.Error(w, "Failed to generate PDF", http.StatusInternalServerError)
//		return
//	}
//
//	// Set headers for PDF download
//	w.Header().Set("Content-Type", "application/pdf")
//	w.Header().Set("Content-Disposition",
//		"attachment; filename=invoice_"+invoice.InvoiceNumber+".pdf")
//	w.Header().Set("Content-Length", strconv.Itoa(len(pdfData)))
//
//	// Write PDF data
//	w.Write(pdfData)
//}
//
//// EmailInvoice sends invoice via email
//func (h *InvoiceHandler) EmailInvoice(w http.ResponseWriter, r *http.Request) {
//	invoiceIDStr := chi.URLParam(r, "id")
//	invoiceID, err := uuid.Parse(invoiceIDStr)
//	if err != nil {
//		http.Error(w, "Invalid invoice ID", http.StatusBadRequest)
//		return
//	}
//
//	// Parse email request
//	var emailRequest struct {
//		Email string `json:"email"`
//	}
//
//	if err := json.NewDecoder(r.Body).Decode(&emailRequest); err != nil {
//		http.Error(w, "Invalid request body", http.StatusBadRequest)
//		return
//	}
//
//	if emailRequest.Email == "" {
//		http.Error(w, "Email address is required", http.StatusBadRequest)
//		return
//	}
//
//	// Send invoice via email
//	if err := h.invoiceService.SendToEmail(invoiceID, emailRequest.Email); err != nil {
//		http.Error(w, "Failed to send email", http.StatusInternalServerError)
//		return
//	}
//
//	response := map[string]interface{}{
//		"message":    "Invoice sent successfully",
//		"email":      emailRequest.Email,
//		"invoice_id": invoiceID,
//	}
//
//	w.Header().Set("Content-Type", "application/json")
//	json.NewEncoder(w).Encode(response)
//}
//
//// GenerateInvoiceForOrder manually generates an invoice for an order (if needed)
//func (h *InvoiceHandler) GenerateInvoiceForOrder(w http.ResponseWriter, r *http.Request) {
//	orderIDStr := chi.URLParam(r, "orderID")
//	orderID, err := uuid.Parse(orderIDStr)
//	if err != nil {
//		http.Error(w, "Invalid order ID", http.StatusBadRequest)
//		return
//	}
//
//	// Parse generation request
//	var generateRequest struct {
//		PaymentID string `json:"payment_id"`
//	}
//
//	if err := json.NewDecoder(r.Body).Decode(&generateRequest); err != nil {
//		http.Error(w, "Invalid request body", http.StatusBadRequest)
//		return
//	}
//
//	paymentID, err := uuid.Parse(generateRequest.PaymentID)
//	if err != nil {
//		http.Error(w, "Invalid payment ID", http.StatusBadRequest)
//		return
//	}
//
//	// Create manual invoice
//	invoice := &models.Invoice{
//		ID:        uuid.New(),
//		OrderID:   orderID,
//		PaymentID: paymentID,
//	}
//
//	if err := h.invoiceService.CreateInvoice(invoice); err != nil {
//		http.Error(w, "Failed to create invoice", http.StatusInternalServerError)
//		return
//	}
//
//	response := h.toInvoiceResponse(invoice)
//	w.Header().Set("Content-Type", "application/json")
//	w.WriteHeader(http.StatusCreated)
//	json.NewEncoder(w).Encode(response)
//}
//
//// toInvoiceResponse converts Invoice model to response format
//func (h *InvoiceHandler) toInvoiceResponse(invoice *models.Invoice) InvoiceResponse {
//	return InvoiceResponse{
//		ID:            invoice.ID.String(),
//		OrderID:       invoice.OrderID.String(),
//		PaymentID:     invoice.PaymentID.String(),
//		InvoiceNumber: invoice.InvoiceNumber,
//		IssueDate:     invoice.IssueDate.Format("2006-01-02"),
//		DueDate:       invoice.DueDate.Format("2006-01-02"),
//		DownloadURL:   "/api/invoices/" + invoice.ID.String() + "/pdf",
//		PDFUrl:        "/api/invoices/" + invoice.ID.String() + "/pdf",
//	}
//}
