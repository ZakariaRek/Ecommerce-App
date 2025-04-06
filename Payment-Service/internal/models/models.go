package models

import (
	"database/sql/driver"
	"encoding/json"
	"errors"
	"time"

	"github.com/google/uuid"
	"gorm.io/gorm"
)

// PaymentMethod enum
type PaymentMethod string

const (
	CreditCard   PaymentMethod = "CREDIT_CARD"
	DebitCard    PaymentMethod = "DEBIT_CARD"
	PayPal       PaymentMethod = "PAYPAL"
	BankTransfer PaymentMethod = "BANK_TRANSFER"
	Crypto       PaymentMethod = "CRYPTO"
	Points       PaymentMethod = "POINTS"
	GiftCard     PaymentMethod = "GIFT_CARD"
)

// PaymentStatus enum
type PaymentStatus string

const (
	Pending           PaymentStatus = "PENDING"
	Completed         PaymentStatus = "COMPLETED"
	Failed            PaymentStatus = "FAILED"
	Refunded          PaymentStatus = "REFUNDED"
	PartiallyRefunded PaymentStatus = "PARTIALLY_REFUNDED"
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

// JSON custom type for PostgreSQL jsonb
type JSON map[string]interface{}

// Scan scans value into JSON, implements sql.Scanner interface
func (j *JSON) Scan(value interface{}) error {
	bytes, ok := value.([]byte)
	if !ok {
		return errors.New("type assertion to []byte failed")
	}
	return json.Unmarshal(bytes, &j)
}

// Value returns json value, implements driver.Valuer interface
func (j JSON) Value() (driver.Value, error) {
	if len(j) == 0 {
		return nil, nil
	}
	return json.Marshal(j)
}

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
