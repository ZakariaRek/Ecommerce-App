package models

import (
	"time"

	"github.com/google/uuid"
	"gorm.io/gorm"
)

// Invoice model
type Invoice struct {
	ID            uuid.UUID `gorm:"type:uuid;primary_key" json:"id"`
	OrderID       uuid.UUID `gorm:"type:uuid" json:"order_id"`
	PaymentID     uuid.UUID `gorm:"type:uuid" json:"payment_id"`
	InvoiceNumber string    `json:"invoice_number"`
	IssueDate     time.Time `json:"issue_date"`
	DueDate       time.Time `json:"due_date"`
	Payment       Payment   `gorm:"foreignKey:PaymentID" json:"-"`
}

// BeforeCreate hook for Invoice to set UUID
func (i *Invoice) BeforeCreate(tx *gorm.DB) error {
	if i.ID == uuid.Nil {
		i.ID = uuid.New()
	}
	return nil
}
