package events

import (
	"github.com/google/uuid"
	"time"
)

// TransactionEventType enum
type TransactionEventType string

const (
	TransactionCreated       TransactionEventType = "TRANSACTION_CREATED"
	TransactionUpdated       TransactionEventType = "TRANSACTION_UPDATED"
	TransactionStatusChanged TransactionEventType = "TRANSACTION_STATUS_CHANGED"
	TransactionDeleted       TransactionEventType = "TRANSACTION_DELETED"
)

// TransactionEvent represents a transaction-related event
type TransactionEvent struct {
	Type          TransactionEventType   `json:"type"`
	TransactionID uuid.UUID              `json:"transaction_id"`
	Timestamp     int64                  `json:"timestamp"`
	Data          map[string]interface{} `json:"data"`
}

// NewTransactionEvent creates a new transaction event
func NewTransactionEvent(eventType TransactionEventType, transactionID uuid.UUID, data map[string]interface{}) *TransactionEvent {
	return &TransactionEvent{
		Type:          eventType,
		TransactionID: transactionID,
		Timestamp:     time.Now().Unix(),
		Data:          data,
	}
}
