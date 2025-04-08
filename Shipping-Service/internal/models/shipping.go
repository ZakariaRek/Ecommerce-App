package models

import (
	"time"

	"github.com/google/uuid"
	"gorm.io/gorm"
)

// ShippingStatus enum
type ShippingStatus string

// ShippingStatus constants
const (
	StatusPending        ShippingStatus = "PENDING"
	StatusPreparing      ShippingStatus = "PREPARING"
	StatusShipped        ShippingStatus = "SHIPPED"
	StatusInTransit      ShippingStatus = "IN_TRANSIT"
	StatusOutForDelivery ShippingStatus = "OUT_FOR_DELIVERY"
	StatusDelivered      ShippingStatus = "DELIVERED"
	StatusFailed         ShippingStatus = "FAILED"
	StatusReturned       ShippingStatus = "RETURNED"
)

// IsValid checks if the status is a valid shipping status
func (s ShippingStatus) IsValid() bool {
	switch s {
	case StatusPending, StatusPreparing, StatusShipped, StatusInTransit,
		StatusOutForDelivery, StatusDelivered, StatusFailed, StatusReturned:
		return true
	default:
		return false
	}
}

// String returns the string representation of the status
func (s ShippingStatus) String() string {
	return string(s)
}

// Shipping model
type Shipping struct {
	ID                uuid.UUID          `gorm:"type:uuid;primary_key" json:"id"`
	OrderID           uuid.UUID          `gorm:"type:uuid;not null" json:"order_id"`
	Status            ShippingStatus     `gorm:"type:varchar(20);not null;default:'PENDING'" json:"status"`
	Carrier           string             `gorm:"type:varchar(100)" json:"carrier"`
	TrackingNumber    string             `gorm:"type:varchar(100)" json:"tracking_number"`
	EstimatedDelivery *time.Time         `gorm:"type:timestamp" json:"estimated_delivery,omitempty"`
	ShippedDate       *time.Time         `gorm:"type:timestamp" json:"shipped_date,omitempty"`
	DeliveredDate     *time.Time         `gorm:"type:timestamp" json:"delivered_date,omitempty"`
	CreatedAt         time.Time          `gorm:"type:timestamp;not null;default:now()" json:"created_at"`
	UpdatedAt         time.Time          `gorm:"type:timestamp;not null;default:now()" json:"updated_at"`
	TrackingHistory   []ShipmentTracking `gorm:"foreignKey:ShippingID" json:"tracking_history,omitempty"`
}

// BeforeCreate hook for Shipping to set UUID
func (s *Shipping) BeforeCreate(tx *gorm.DB) error {
	if s.ID == uuid.Nil {
		s.ID = uuid.New()
	}
	if s.Status == "" {
		s.Status = StatusPending
	}
	return nil
}

// UpdateStatus updates the shipping status and related dates
func (s *Shipping) UpdateStatus(status ShippingStatus) {
	now := time.Now()
	s.Status = status
	s.UpdatedAt = now

	switch status {
	case StatusShipped:
		s.ShippedDate = &now
	case StatusDelivered:
		s.DeliveredDate = &now
	}
}

// CalculateShippingCost is a placeholder for shipping cost calculation
func (s *Shipping) CalculateShippingCost() float64 {
	// This would typically involve more complex logic based on
	// weight, distance, carrier rates, etc.
	// For now, returning a dummy value
	return 10.99
}

// ShipmentTracking model
type ShipmentTracking struct {
	ID         uuid.UUID `gorm:"type:uuid;primary_key" json:"id"`
	ShippingID uuid.UUID `gorm:"type:uuid;not null;index" json:"shipping_id"`
	Location   string    `gorm:"type:varchar(255);not null" json:"location"`
	Timestamp  time.Time `gorm:"type:timestamp;not null" json:"timestamp"`
	Status     string    `gorm:"type:varchar(100);not null" json:"status"`
	Notes      string    `gorm:"type:text" json:"notes,omitempty"`
	CreatedAt  time.Time `gorm:"type:timestamp;not null;default:now()" json:"created_at"`
	Shipping   *Shipping `gorm:"foreignKey:ShippingID" json:"-"`
}

// BeforeCreate hook for ShipmentTracking to set UUID
func (st *ShipmentTracking) BeforeCreate(tx *gorm.DB) error {
	if st.ID == uuid.Nil {
		st.ID = uuid.New()
	}
	if st.Timestamp.IsZero() {
		st.Timestamp = time.Now()
	}
	return nil
}

// UpdateLocation updates the location and timestamp of the tracking
func (st *ShipmentTracking) UpdateLocation(location string) {
	st.Location = location
	st.Timestamp = time.Now()
}
