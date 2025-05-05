package listeners

import (
	"context"
	"reflect"

	"github.com/ZakariaRek/Ecommerce-App/Shipping-Service/internal/models"
	"github.com/ZakariaRek/Ecommerce-App/Shipping-Service/internal/service/kafka"
	"gorm.io/gorm"
)

// ShippingListener handles GORM callbacks for Shipping entities
type ShippingListener struct {
	kafkaService *kafka.ShippingKafkaService
}

// NewShippingListener creates a new shipping listener
func NewShippingListener(kafkaService *kafka.ShippingKafkaService) *ShippingListener {
	return &ShippingListener{
		kafkaService: kafkaService,
	}
}

// RegisterCallbacks registers callbacks with GORM
func (l *ShippingListener) RegisterCallbacks(db *gorm.DB) {
	db.Callback().Create().After("gorm:create").Register("shipping:after_create", l.afterCreate)
	db.Callback().Update().After("gorm:update").Register("shipping:after_update", l.afterUpdate)
	db.Callback().Delete().After("gorm:delete").Register("shipping:after_delete", l.afterDelete)
}

// afterCreate handles post-creation events
func (l *ShippingListener) afterCreate(db *gorm.DB) {
	if db.Statement.Schema.Name == "Shipping" {
		if shipping, ok := db.Statement.ReflectValue.Interface().(*models.Shipping); ok {
			_ = l.kafkaService.PublishShippingCreated(shipping)
		}
	}
}

// afterUpdate handles post-update events
func (l *ShippingListener) afterUpdate(db *gorm.DB) {
	if db.Statement.Schema.Name == "Shipping" {
		if shipping, ok := db.Statement.ReflectValue.Interface().(*models.Shipping); ok {
			_ = l.kafkaService.PublishShippingUpdated(shipping)

			// Check if status changed
			if db.Statement.Changed("Status") {
				oldStatus := getOriginalValue(db, "Status").(models.ShippingStatus)
				_ = l.kafkaService.PublishShippingStatusChanged(shipping, oldStatus)
			}
		}
	}
}

// afterDelete handles post-deletion events
func (l *ShippingListener) afterDelete(db *gorm.DB) {
	if db.Statement.Schema.Name == "Shipping" {
		if shipping, ok := db.Statement.ReflectValue.Interface().(*models.Shipping); ok {
			_ = l.kafkaService.PublishShippingDeleted(shipping)
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
