package listeners

import (
	"github.com/ZakariaRek/Ecommerce-App/Shipping-Service/internal/models"
	"github.com/ZakariaRek/Ecommerce-App/Shipping-Service/internal/service/kafka"
	"gorm.io/gorm"
)

// TrackingListener handles GORM callbacks for ShipmentTracking entities
type TrackingListener struct {
	kafkaService *kafka.TrackingKafkaService
}

// NewTrackingListener creates a new tracking listener
func NewTrackingListener(kafkaService *kafka.TrackingKafkaService) *TrackingListener {
	return &TrackingListener{
		kafkaService: kafkaService,
	}
}

// RegisterCallbacks registers callbacks with GORM
func (l *TrackingListener) RegisterCallbacks(db *gorm.DB) {
	db.Callback().Create().After("gorm:create").Register("tracking:after_create", l.afterCreate)
	db.Callback().Update().After("gorm:update").Register("tracking:after_update", l.afterUpdate)
	db.Callback().Delete().After("gorm:delete").Register("tracking:after_delete", l.afterDelete)
}

// afterCreate handles post-creation events
func (l *TrackingListener) afterCreate(db *gorm.DB) {
	if db.Statement.Schema.Name == "ShipmentTracking" {
		if tracking, ok := db.Statement.ReflectValue.Interface().(*models.ShipmentTracking); ok {
			_ = l.kafkaService.PublishTrackingCreated(tracking)
		}
	}
}

// afterUpdate handles post-update events
func (l *TrackingListener) afterUpdate(db *gorm.DB) {
	if db.Statement.Schema.Name == "ShipmentTracking" {
		if tracking, ok := db.Statement.ReflectValue.Interface().(*models.ShipmentTracking); ok {
			_ = l.kafkaService.PublishTrackingUpdated(tracking)
		}
	}
}

// afterDelete handles post-deletion events
func (l *TrackingListener) afterDelete(db *gorm.DB) {
	if db.Statement.Schema.Name == "ShipmentTracking" {
		if tracking, ok := db.Statement.ReflectValue.Interface().(*models.ShipmentTracking); ok {
			_ = l.kafkaService.PublishTrackingDeleted(tracking)
		}
	}
}
