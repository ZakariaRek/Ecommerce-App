package events

import (
	"github.com/google/uuid"
	"time"
)

// PaymentEventType enum
type PaymentEventType string

const (
	PaymentCreated       PaymentEventType = "PAYMENT_CREATED"
	PaymentUpdated       PaymentEventType = "PAYMENT_UPDATED"
	PaymentStatusChanged PaymentEventType = "PAYMENT_STATUS_CHANGED"
	PaymentDeleted       PaymentEventType = "PAYMENT_DELETED"
)

// PaymentEvent represents a payment-related event
type PaymentEvent struct {
	Type      PaymentEventType       `json:"type"`
	PaymentID uuid.UUID              `json:"payment_id"`
	Timestamp int64                  `json:"timestamp"`
	Data      map[string]interface{} `json:"data"`
}

// NewPaymentEvent creates a new payment event
func NewPaymentEvent(eventType PaymentEventType, paymentID uuid.UUID, data map[string]interface{}) *PaymentEvent {
	return &PaymentEvent{
		Type:      eventType,
		PaymentID: paymentID,
		Timestamp: time.Now().Unix(),
		Data:      data,
	}
}
