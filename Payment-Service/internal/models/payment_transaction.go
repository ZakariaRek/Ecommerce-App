package models

import (
	"time"

	"github.com/google/uuid"
	"gorm.io/gorm"
)

// PaymentTransaction model
type PaymentTransaction struct {
	ID             uuid.UUID `gorm:"type:uuid;primary_key" json:"id"`
	PaymentID      uuid.UUID `gorm:"type:uuid" json:"payment_id"`
	TransactionID  string    `json:"transaction_id"`
	PaymentGateway string    `json:"payment_gateway"`
	Status         string    `json:"status"`
	ResponseData   JSON      `gorm:"type:jsonb" json:"response_data"`
	Timestamp      time.Time `json:"timestamp"`
	Payment        Payment   `gorm:"foreignKey:PaymentID" json:"-"`
}

// BeforeCreate hook for PaymentTransaction to set UUID
func (pt *PaymentTransaction) BeforeCreate(tx *gorm.DB) error {
	if pt.ID == uuid.Nil {
		pt.ID = uuid.New()
	}
	return nil
}
