package repository

import (
	"github.com/ZakariaRek/Ecommerce-App/Payment-Service/internal/models"
	"github.com/google/uuid"
	"gorm.io/gorm"
)

// InvoiceRepository interface
type InvoiceRepository interface {
	Create(invoice *models.Invoice) error
	FindByID(id uuid.UUID) (*models.Invoice, error)
	FindByOrderID(orderID uuid.UUID) ([]*models.Invoice, error)
	FindByPaymentID(paymentID uuid.UUID) (*models.Invoice, error)
	Update(invoice *models.Invoice) error
}

// invoiceRepository implements InvoiceRepository
type invoiceRepository struct {
	db *gorm.DB
}

// NewInvoiceRepository creates a new invoice repository
func NewInvoiceRepository(db *gorm.DB) InvoiceRepository {
	return &invoiceRepository{db}
}

// Create creates a new invoice
func (r *invoiceRepository) Create(invoice *models.Invoice) error {
	return r.db.Create(invoice).Error
}

// FindByID finds an invoice by ID
func (r *invoiceRepository) FindByID(id uuid.UUID) (*models.Invoice, error) {
	var invoice models.Invoice
	err := r.db.Where("id = ?", id).First(&invoice).Error
	return &invoice, err
}

// FindByOrderID finds invoices by order ID
func (r *invoiceRepository) FindByOrderID(orderID uuid.UUID) ([]*models.Invoice, error) {
	var invoices []*models.Invoice
	err := r.db.Where("order_id = ?", orderID).Find(&invoices).Error
	return invoices, err
}

// FindByPaymentID finds an invoice by payment ID
func (r *invoiceRepository) FindByPaymentID(paymentID uuid.UUID) (*models.Invoice, error) {
	var invoice models.Invoice
	err := r.db.Where("payment_id = ?", paymentID).First(&invoice).Error
	return &invoice, err
}

// Update updates an invoice
func (r *invoiceRepository) Update(invoice *models.Invoice) error {
	return r.db.Save(invoice).Error
}
