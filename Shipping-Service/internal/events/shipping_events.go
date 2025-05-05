package events

import (
	"github.com/google/uuid"
	"time"
)

// ShippingEventType enum
type ShippingEventType string

const (
	ShippingCreated       ShippingEventType = "SHIPPING_CREATED"
	ShippingUpdated       ShippingEventType = "SHIPPING_UPDATED"
	ShippingStatusChanged ShippingEventType = "SHIPPING_STATUS_CHANGED"
	ShippingDeleted       ShippingEventType = "SHIPPING_DELETED"
)

// ShippingEvent represents a shipping-related event
type ShippingEvent struct {
	Type       ShippingEventType      `json:"type"`
	ShippingID uuid.UUID              `json:"shipping_id"`
	Timestamp  int64                  `json:"timestamp"`
	Data       map[string]interface{} `json:"data"`
}

// NewShippingEvent creates a new shipping event
func NewShippingEvent(eventType ShippingEventType, shippingID uuid.UUID, data map[string]interface{}) *ShippingEvent {
	return &ShippingEvent{
		Type:       eventType,
		ShippingID: shippingID,
		Timestamp:  time.Now().Unix(),
		Data:       data,
	}
}
