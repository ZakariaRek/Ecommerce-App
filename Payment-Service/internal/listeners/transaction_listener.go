package listeners

import (
	"github.com/ZakariaRek/Ecommerce-App/Payment-Service/internal/models"
	"github.com/ZakariaRek/Ecommerce-App/Payment-Service/internal/service/kafka"
	"gorm.io/gorm"
)

// TransactionListener handles GORM callbacks for PaymentTransaction entities
type TransactionListener struct {
	kafkaService *kafka.TransactionKafkaService
}

// NewTransactionListener creates a new transaction listener
func NewTransactionListener(kafkaService *kafka.TransactionKafkaService) *TransactionListener {
	return &TransactionListener{
		kafkaService: kafkaService,
	}
}

// RegisterCallbacks registers callbacks with GORM
func (l *TransactionListener) RegisterCallbacks(db *gorm.DB) {
	db.Callback().Create().After("gorm:create").Register("transaction:after_create", l.afterCreate)
	db.Callback().Update().After("gorm:update").Register("transaction:after_update", l.afterUpdate)
	db.Callback().Delete().After("gorm:delete").Register("transaction:after_delete", l.afterDelete)
}

// afterCreate handles post-creation events
func (l *TransactionListener) afterCreate(db *gorm.DB) {
	if db.Statement.Schema.Name == "PaymentTransaction" {
		if transaction, ok := db.Statement.ReflectValue.Interface().(*models.PaymentTransaction); ok {
			_ = l.kafkaService.PublishTransactionCreated(transaction)
		}
	}
}

// afterUpdate handles post-update events
func (l *TransactionListener) afterUpdate(db *gorm.DB) {
	if db.Statement.Schema.Name == "PaymentTransaction" {
		if transaction, ok := db.Statement.ReflectValue.Interface().(*models.PaymentTransaction); ok {
			_ = l.kafkaService.PublishTransactionUpdated(transaction)

			// Check if status changed
			if db.Statement.Changed("Status") {
				oldStatus := getOriginalValue(db, "Status").(string)
				_ = l.kafkaService.PublishTransactionStatusChanged(transaction, oldStatus)
			}
		}
	}
}

// afterDelete handles post-deletion events
func (l *TransactionListener) afterDelete(db *gorm.DB) {
	if db.Statement.Schema.Name == "PaymentTransaction" {
		if transaction, ok := db.Statement.ReflectValue.Interface().(*models.PaymentTransaction); ok {
			_ = l.kafkaService.PublishTransactionDeleted(transaction)
		}
	}
}
