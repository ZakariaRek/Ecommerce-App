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

// Address represents a shipping or billing address
type Address struct {
	ID           uuid.UUID `gorm:"type:uuid;primary_key" json:"id"`
	FirstName    string    `gorm:"type:varchar(100);not null" json:"first_name"`
	LastName     string    `gorm:"type:varchar(100);not null" json:"last_name"`
	Company      string    `gorm:"type:varchar(150)" json:"company,omitempty"`
	AddressLine1 string    `gorm:"type:varchar(255);not null" json:"address_line1"`
	AddressLine2 string    `gorm:"type:varchar(255)" json:"address_line2,omitempty"`
	City         string    `gorm:"type:varchar(100);not null" json:"city"`
	State        string    `gorm:"type:varchar(100);not null" json:"state"`
	PostalCode   string    `gorm:"type:varchar(20);not null" json:"postal_code"`
	Country      string    `gorm:"type:varchar(100);not null" json:"country"`
	Phone        string    `gorm:"type:varchar(20)" json:"phone,omitempty"`
	Email        string    `gorm:"type:varchar(255)" json:"email,omitempty"`
	// Coordinates for location tracking
	Latitude  *float64  `gorm:"type:decimal(10,8)" json:"latitude,omitempty"`
	Longitude *float64  `gorm:"type:decimal(11,8)" json:"longitude,omitempty"`
	CreatedAt time.Time `gorm:"type:timestamp;not null;default:now()" json:"created_at"`
	UpdatedAt time.Time `gorm:"type:timestamp;not null;default:now()" json:"updated_at"`
}

// BeforeCreate hook for Address to set UUID
func (a *Address) BeforeCreate(tx *gorm.DB) error {
	if a.ID == uuid.Nil {
		a.ID = uuid.New()
	}
	return nil
}

// GetFullAddress returns the complete formatted address
func (a *Address) GetFullAddress() string {
	address := a.AddressLine1
	if a.AddressLine2 != "" {
		address += ", " + a.AddressLine2
	}
	return address + ", " + a.City + ", " + a.State + " " + a.PostalCode + ", " + a.Country
}

// GetFullName returns the complete name
func (a *Address) GetFullName() string {
	return a.FirstName + " " + a.LastName
}

// Shipping model with address support
type Shipping struct {
	ID             uuid.UUID      `gorm:"type:uuid;primary_key" json:"id"`
	OrderID        uuid.UUID      `gorm:"type:uuid;not null" json:"order_id"`
	Status         ShippingStatus `gorm:"type:varchar(20);not null;default:'PENDING'" json:"status"`
	Carrier        string         `gorm:"type:varchar(100)" json:"carrier"`
	TrackingNumber string         `gorm:"type:varchar(100)" json:"tracking_number"`

	// Address references
	ShippingAddressID uuid.UUID `gorm:"type:uuid;not null" json:"shipping_address_id"`
	OriginAddressID   uuid.UUID `gorm:"type:uuid;not null" json:"origin_address_id"`

	// Shipping dates
	EstimatedDelivery *time.Time `gorm:"type:timestamp" json:"estimated_delivery,omitempty"`
	ShippedDate       *time.Time `gorm:"type:timestamp" json:"shipped_date,omitempty"`
	DeliveredDate     *time.Time `gorm:"type:timestamp" json:"delivered_date,omitempty"`

	// Cost information
	ShippingCost float64 `gorm:"type:decimal(10,2);default:0" json:"shipping_cost"`
	Weight       float64 `gorm:"type:decimal(8,2)" json:"weight,omitempty"`
	Dimensions   string  `gorm:"type:varchar(50)" json:"dimensions,omitempty"` // e.g., "10x8x6"

	// Current location tracking
	CurrentLatitude    *float64   `gorm:"type:decimal(10,8)" json:"current_latitude,omitempty"`
	CurrentLongitude   *float64   `gorm:"type:decimal(11,8)" json:"current_longitude,omitempty"`
	LastLocationUpdate *time.Time `gorm:"type:timestamp" json:"last_location_update,omitempty"`

	CreatedAt time.Time `gorm:"type:timestamp;not null;default:now()" json:"created_at"`
	UpdatedAt time.Time `gorm:"type:timestamp;not null;default:now()" json:"updated_at"`

	// Relationships
	ShippingAddress Address            `gorm:"foreignKey:ShippingAddressID" json:"shipping_address"`
	OriginAddress   Address            `gorm:"foreignKey:OriginAddressID" json:"origin_address"`
	TrackingHistory []ShipmentTracking `gorm:"foreignKey:ShippingID" json:"tracking_history,omitempty"`
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

// UpdateCurrentLocation updates the current GPS coordinates
func (s *Shipping) UpdateCurrentLocation(lat, lng float64) {
	now := time.Now()
	s.CurrentLatitude = &lat
	s.CurrentLongitude = &lng
	s.LastLocationUpdate = &now
	s.UpdatedAt = now
}

// CalculateShippingCost calculates shipping cost based on weight, distance, and carrier
func (s *Shipping) CalculateShippingCost() float64 {
	// Base cost
	baseCost := 5.99

	// Weight-based pricing (per kg)
	weightCost := s.Weight * 2.5

	// You can add distance calculation here using origin and shipping addresses
	// distanceCost := calculateDistanceCost(s.OriginAddress, s.ShippingAddress)

	total := baseCost + weightCost
	s.ShippingCost = total
	return total
}

// GetEstimatedDistance returns estimated distance between origin and destination
func (s *Shipping) GetEstimatedDistance() float64 {
	// This would calculate distance between origin and shipping address
	// You can use the Haversine formula with lat/lng coordinates
	// For now, returning a placeholder
	return 0.0
}

// ShipmentTracking model with enhanced location tracking
type ShipmentTracking struct {
	ID         uuid.UUID `gorm:"type:uuid;primary_key" json:"id"`
	ShippingID uuid.UUID `gorm:"type:uuid;not null;index" json:"shipping_id"`
	Location   string    `gorm:"type:varchar(255);not null" json:"location"`

	// GPS coordinates for precise tracking
	Latitude  *float64 `gorm:"type:decimal(10,8)" json:"latitude,omitempty"`
	Longitude *float64 `gorm:"type:decimal(11,8)" json:"longitude,omitempty"`

	Timestamp time.Time `gorm:"type:timestamp;not null" json:"timestamp"`
	Status    string    `gorm:"type:varchar(100);not null" json:"status"`
	Notes     string    `gorm:"type:text" json:"notes,omitempty"`

	// For mobile app tracking
	DeviceID string `gorm:"type:varchar(100)" json:"device_id,omitempty"`
	DriverID string `gorm:"type:varchar(100)" json:"driver_id,omitempty"`

	CreatedAt time.Time `gorm:"type:timestamp;not null;default:now()" json:"created_at"`
	Shipping  *Shipping `gorm:"foreignKey:ShippingID" json:"-"`
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

// UpdateLocation updates the location with GPS coordinates
func (st *ShipmentTracking) UpdateLocation(location string, lat, lng *float64) {
	st.Location = location
	st.Latitude = lat
	st.Longitude = lng
	st.Timestamp = time.Now()
}

// LocationUpdate represents real-time location updates for mobile app
type LocationUpdate struct {
	ID         uuid.UUID `gorm:"type:uuid;primary_key" json:"id"`
	ShippingID uuid.UUID `gorm:"type:uuid;not null;index" json:"shipping_id"`
	DeviceID   string    `gorm:"type:varchar(100);not null" json:"device_id"`
	Latitude   float64   `gorm:"type:decimal(10,8);not null" json:"latitude"`
	Longitude  float64   `gorm:"type:decimal(11,8);not null" json:"longitude"`
	Speed      *float64  `gorm:"type:decimal(5,2)" json:"speed,omitempty"`    // km/h
	Heading    *float64  `gorm:"type:decimal(5,2)" json:"heading,omitempty"`  // degrees
	Accuracy   *float64  `gorm:"type:decimal(8,2)" json:"accuracy,omitempty"` // meters
	Timestamp  time.Time `gorm:"type:timestamp;not null" json:"timestamp"`
	CreatedAt  time.Time `gorm:"type:timestamp;not null;default:now()" json:"created_at"`

	Shipping *Shipping `gorm:"foreignKey:ShippingID" json:"-"`
}

// BeforeCreate hook for LocationUpdate
func (lu *LocationUpdate) BeforeCreate(tx *gorm.DB) error {
	if lu.ID == uuid.Nil {
		lu.ID = uuid.New()
	}
	if lu.Timestamp.IsZero() {
		lu.Timestamp = time.Now()
	}
	return nil
}
