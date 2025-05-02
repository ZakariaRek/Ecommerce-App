package models

import (
	"time"

	"github.com/google/uuid"
	"gorm.io/gorm"
)

// Payment model
type Payment struct {
	ID        uuid.UUID     `gorm:"type:uuid;primary_key" json:"id"`
	OrderID   uuid.UUID     `gorm:"type:uuid" json:"order_id"`
	Amount    float64       `json:"amount"`
	Method    PaymentMethod `gorm:"type:varchar(20)" json:"method"`
	Status    PaymentStatus `gorm:"type:varchar(20)" json:"status"`
	CreatedAt time.Time     `json:"created_at"`
	UpdatedAt time.Time     `json:"updated_at"`
}

// BeforeCreate hook for Payment to set UUID
func (p *Payment) BeforeCreate(tx *gorm.DB) error {
	if p.ID == uuid.Nil {
		p.ID = uuid.New()
	}
	return nil
}
