package listeners

import (
	"context"
	"reflect"
	"time"

	"github.com/ZakariaRek/Ecommerce-App/Payment-Service/internal/models"
	"github.com/ZakariaRek/Ecommerce-App/Payment-Service/internal/service"
	"github.com/ZakariaRek/Ecommerce-App/Payment-Service/internal/service/kafka"
	"gorm.io/gorm"
)

// InvoiceListener handles GORM callbacks for Invoice entities
type InvoiceListener struct {
	kafkaService *service.InvoiceKafkaService
}

// NewInvoiceListener creates a new invoice listener
func NewInvoiceListener(kafkaService *service.InvoiceKafkaService) *InvoiceListener {
	return &InvoiceListener{
		kafkaService: kafkaService,
	}
}

// RegisterCallbacks registers callbacks with GORM
func (l *InvoiceListener) RegisterCallbacks(db *gorm.DB) {
	db.Callback().Create().After("gorm:create").Register("invoice:after_create", l.afterCreate)
	db.Callback().Update().After("gorm:update").Register("invoice:after_update", l.afterUpdate)
}

// afterCreate handles post-creation events
func (l *InvoiceListener) afterCreate(db *gorm.DB) {
	if db.Statement.Schema.Name == "Invoice" {
		if invoice, ok := db.Statement.ReflectValue.Interface().(*models.Invoice); ok {
			_ = l.kafkaService.PublishInvoiceCreated(invoice)
		}
	}
}

// afterUpdate handles post-update events
func (l *InvoiceListener) afterUpdate(db *gorm.DB) {
	if db.Statement.Schema.Name == "Invoice" {
		if invoice, ok := db.Statement.ReflectValue.Interface().(*models.Invoice); ok {
			_ = l.kafkaService.PublishInvoiceUpdated(invoice)

			// Check if due date changed
			if db.Statement.Changed("DueDate") {
				oldDueDate := getOriginalValue(db, "DueDate").(time.Time)
				_ = l.kafkaService.PublishInvoiceDueDateChanged(invoice, oldDueDate)
			}
		}
	}
}
