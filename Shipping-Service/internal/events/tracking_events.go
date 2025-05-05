package events

import (
	"github.com/google/uuid"
	"time"
)

// TrackingEventType enum
type TrackingEventType string

const (
	TrackingCreated TrackingEventType = "TRACKING_CREATED"
	TrackingUpdated TrackingEventType = "TRACKING_UPDATED"
	TrackingDeleted TrackingEventType = "TRACKING_DELETED"
)

// TrackingEvent represents a tracking-related event
type TrackingEvent struct {
	Type       TrackingEventType      `json:"type"`
	TrackingID uuid.UUID              `json:"tracking_id"`
	ShippingID uuid.UUID              `json:"shipping_id"`
	Timestamp  int64                  `json:"timestamp"`
	Data       map[string]interface{} `json:"data"`
}

// NewTrackingEvent creates a new tracking event
func NewTrackingEvent(eventType TrackingEventType, trackingID uuid.UUID, shippingID uuid.UUID, data map[string]interface{}) *TrackingEvent {
	return &TrackingEvent{
		Type:       eventType,
		TrackingID: trackingID,
		ShippingID: shippingID,
		Timestamp:  time.Now().Unix(),
		Data:       data,
	}
}
