package events

import (
	"github.com/google/uuid"
	"time"
)

// InvoiceEventType enum
type InvoiceEventType string

const (
	InvoiceCreated        InvoiceEventType = "INVOICE_CREATED"
	InvoiceUpdated        InvoiceEventType = "INVOICE_UPDATED"
	InvoiceDueDateChanged InvoiceEventType = "INVOICE_DUE_DATE_CHANGED"
)

// InvoiceEvent represents an invoice-related event
type InvoiceEvent struct {
	Type      InvoiceEventType       `json:"type"`
	InvoiceID uuid.UUID              `json:"invoice_id"`
	Timestamp int64                  `json:"timestamp"`
	Data      map[string]interface{} `json:"data"`
}

// NewInvoiceEvent creates a new invoice event
func NewInvoiceEvent(eventType InvoiceEventType, invoiceID uuid.UUID, data map[string]interface{}) *InvoiceEvent {
	return &InvoiceEvent{
		Type:      eventType,
		InvoiceID: invoiceID,
		Timestamp: time.Now().Unix(),
		Data:      data,
	}
}
