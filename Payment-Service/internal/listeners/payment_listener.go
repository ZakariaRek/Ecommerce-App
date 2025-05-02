package listeners

import (
	"context"
	"github.com/ZakariaRek/Ecommerce-App/Payment-Service/internal/service/kafka"
	"reflect"

	"github.com/ZakariaRek/Ecommerce-App/Payment-Service/internal/models"

	"gorm.io/gorm"
)

// PaymentListener handles GORM callbacks for Payment entities
type PaymentListener struct {
	kafkaService *kafka.PaymentKafkaService
}

// NewPaymentListener creates a new payment listener
func NewPaymentListener(kafkaService *kafka.PaymentKafkaService) *PaymentListener {
	return &PaymentListener{
		kafkaService: kafkaService,
	}
}

// RegisterCallbacks registers callbacks with GORM
func (l *PaymentListener) RegisterCallbacks(db *gorm.DB) {
	db.Callback().Create().After("gorm:create").Register("payment:after_create", l.afterCreate)
	db.Callback().Update().After("gorm:update").Register("payment:after_update", l.afterUpdate)
}

// afterCreate handles post-creation events
func (l *PaymentListener) afterCreate(db *gorm.DB) {
	if db.Statement.Schema.Name == "Payment" {
		if payment, ok := db.Statement.ReflectValue.Interface().(*models.Payment); ok {
			_ = l.kafkaService.PublishPaymentCreated(payment)
		}
	}
}

// afterUpdate handles post-update events
func (l *PaymentListener) afterUpdate(db *gorm.DB) {
	if db.Statement.Schema.Name == "Payment" {
		if payment, ok := db.Statement.ReflectValue.Interface().(*models.Payment); ok {
			_ = l.kafkaService.PublishPaymentUpdated(payment)

			// Check if status changed
			if db.Statement.Changed("Status") {
				oldStatus := getOriginalValue(db, "Status").(models.PaymentStatus)
				_ = l.kafkaService.PublishPaymentStatusChanged(payment, oldStatus)
			}
		}
	}
}

// Helper function to get original value before update
func getOriginalValue(db *gorm.DB, fieldName string) interface{} {
	if db.Statement.Changed(fieldName) {
		field, _ := db.Statement.Schema.FieldsByName[fieldName]
		originalValue, _ := field.ValueOf(context.Background(), reflect.ValueOf(db.Statement.Dest))
		return originalValue
	}

	field, _ := db.Statement.Schema.FieldsByName[fieldName]
	value, _ := field.ValueOf(context.Background(), db.Statement.ReflectValue)
	return value
}
