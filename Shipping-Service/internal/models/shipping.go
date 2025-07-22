// Shipping-Service/internal/models/shipping.go - Debug Version
package models

import (
	"log"
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

// Global variable to hold Kafka services (will be set during initialization)
var (
	GlobalShippingKafkaService KafkaShippingService
	GlobalTrackingKafkaService KafkaTrackingService
)

// Interface for Kafka services to avoid circular dependencies
type KafkaShippingService interface {
	PublishShippingCreated(shipping *Shipping) error
	PublishShippingUpdated(shipping *Shipping) error
	PublishShippingStatusChanged(shipping *Shipping, oldStatus ShippingStatus) error
	PublishShippingDeleted(shipping *Shipping) error
}

type KafkaTrackingService interface {
	PublishTrackingCreated(tracking *ShipmentTracking) error
	PublishTrackingUpdated(tracking *ShipmentTracking) error
	PublishTrackingDeleted(tracking *ShipmentTracking) error
}

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

	// Field to store old status for comparison (not persisted to DB)
	oldStatus ShippingStatus `gorm:"-"`
}

// BeforeCreate hook for Shipping
func (s *Shipping) BeforeCreate(tx *gorm.DB) error {
	log.Printf("🔄 GORM HOOK: BeforeCreate called for Shipping ID: %s", s.ID)
	if s.ID == uuid.Nil {
		s.ID = uuid.New()
		log.Printf("🆔 Generated new UUID: %s", s.ID)
	}
	if s.Status == "" {
		s.Status = StatusPending
		log.Printf("📊 Set default status: %s", s.Status)
	}
	return nil
}

// BeforeUpdate hook for Shipping - captures old status
func (s *Shipping) BeforeUpdate(tx *gorm.DB) error {
	log.Printf("🔄 GORM HOOK: BeforeUpdate called for Shipping ID: %s", s.ID)

	// Get the old record to compare status
	var oldShipping Shipping
	if err := tx.Where("id = ?", s.ID).First(&oldShipping).Error; err == nil {
		s.oldStatus = oldShipping.Status
		log.Printf("📊 Captured old status: %s -> new status: %s", s.oldStatus, s.Status)
	} else {
		log.Printf("⚠️ Could not fetch old shipping record: %v", err)
	}
	return nil
}

// AfterCreate hook for Shipping
func (s *Shipping) AfterCreate(tx *gorm.DB) error {
	log.Printf("🔄 GORM HOOK: AfterCreate called for Shipping ID: %s", s.ID)

	if GlobalShippingKafkaService == nil {
		log.Printf("❌ GlobalShippingKafkaService is nil! Cannot publish events.")
		return nil
	}

	log.Printf("📤 Publishing shipping created event for ID: %s", s.ID)
	if err := GlobalShippingKafkaService.PublishShippingCreated(s); err != nil {
		log.Printf("❌ Failed to publish shipping created event: %v", err)
		// Don't fail the transaction, just log the error
	} else {
		log.Printf("✅ Successfully published shipping created event")
	}
	return nil
}

// AfterUpdate hook for Shipping
func (s *Shipping) AfterUpdate(tx *gorm.DB) error {
	log.Printf("🔄 GORM HOOK: AfterUpdate called for Shipping ID: %s", s.ID)

	if GlobalShippingKafkaService == nil {
		log.Printf("❌ GlobalShippingKafkaService is nil! Cannot publish events.")
		return nil
	}

	log.Printf("📤 Publishing shipping updated event for ID: %s", s.ID)
	if err := GlobalShippingKafkaService.PublishShippingUpdated(s); err != nil {
		log.Printf("❌ Failed to publish shipping updated event: %v", err)
	} else {
		log.Printf("✅ Successfully published shipping updated event")
	}

	// Check if status changed
	if s.oldStatus != "" && s.oldStatus != s.Status {
		log.Printf("📊 Status changed detected: %s -> %s", s.oldStatus, s.Status)
		log.Printf("📤 Publishing shipping status changed event")
		if err := GlobalShippingKafkaService.PublishShippingStatusChanged(s, s.oldStatus); err != nil {
			log.Printf("❌ Failed to publish shipping status changed event: %v", err)
		} else {
			log.Printf("✅ Successfully published shipping status changed event")
		}
	} else {
		log.Printf("📊 No status change detected (old: %s, new: %s)", s.oldStatus, s.Status)
	}
	return nil
}

// AfterDelete hook for Shipping
func (s *Shipping) AfterDelete(tx *gorm.DB) error {
	log.Printf("🔄 GORM HOOK: AfterDelete called for Shipping ID: %s", s.ID)

	if GlobalShippingKafkaService == nil {
		log.Printf("❌ GlobalShippingKafkaService is nil! Cannot publish events.")
		return nil
	}

	if err := GlobalShippingKafkaService.PublishShippingDeleted(s); err != nil {
		log.Printf("❌ Failed to publish shipping deleted event: %v", err)
	} else {
		log.Printf("✅ Successfully published shipping deleted event")
	}
	return nil
}

// UpdateStatus updates the shipping status and related dates
func (s *Shipping) UpdateStatus(status ShippingStatus) {
	log.Printf("📊 UpdateStatus called: %s -> %s", s.Status, status)
	now := time.Now()
	s.Status = status
	s.UpdatedAt = now

	switch status {
	case StatusShipped:
		s.ShippedDate = &now
		log.Printf("📅 Set shipped date: %v", now)
	case StatusDelivered:
		s.DeliveredDate = &now
		log.Printf("📅 Set delivered date: %v", now)
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

// BeforeCreate hook for ShipmentTracking
func (st *ShipmentTracking) BeforeCreate(tx *gorm.DB) error {
	log.Printf("🔄 GORM HOOK: BeforeCreate called for ShipmentTracking ID: %s", st.ID)
	if st.ID == uuid.Nil {
		st.ID = uuid.New()
	}
	if st.Timestamp.IsZero() {
		st.Timestamp = time.Now()
	}
	return nil
}

// AfterCreate hook for ShipmentTracking
func (st *ShipmentTracking) AfterCreate(tx *gorm.DB) error {
	log.Printf("🔄 GORM HOOK: AfterCreate called for ShipmentTracking ID: %s", st.ID)

	if GlobalTrackingKafkaService == nil {
		log.Printf("❌ GlobalTrackingKafkaService is nil! Cannot publish events.")
		return nil
	}

	if err := GlobalTrackingKafkaService.PublishTrackingCreated(st); err != nil {
		log.Printf("❌ Failed to publish tracking created event: %v", err)
	} else {
		log.Printf("✅ Successfully published tracking created event")
	}
	return nil
}

// AfterUpdate hook for ShipmentTracking
func (st *ShipmentTracking) AfterUpdate(tx *gorm.DB) error {
	log.Printf("🔄 GORM HOOK: AfterUpdate called for ShipmentTracking ID: %s", st.ID)

	if GlobalTrackingKafkaService == nil {
		log.Printf("❌ GlobalTrackingKafkaService is nil! Cannot publish events.")
		return nil
	}

	if err := GlobalTrackingKafkaService.PublishTrackingUpdated(st); err != nil {
		log.Printf("❌ Failed to publish tracking updated event: %v", err)
	} else {
		log.Printf("✅ Successfully published tracking updated event")
	}
	return nil
}

// AfterDelete hook for ShipmentTracking
func (st *ShipmentTracking) AfterDelete(tx *gorm.DB) error {
	log.Printf("🔄 GORM HOOK: AfterDelete called for ShipmentTracking ID: %s", st.ID)

	if GlobalTrackingKafkaService == nil {
		log.Printf("❌ GlobalTrackingKafkaService is nil! Cannot publish events.")
		return nil
	}

	if err := GlobalTrackingKafkaService.PublishTrackingDeleted(st); err != nil {
		log.Printf("❌ Failed to publish tracking deleted event: %v", err)
	} else {
		log.Printf("✅ Successfully published tracking deleted event")
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

// SetKafkaServices sets the global Kafka services
func SetKafkaServices(shippingKafka KafkaShippingService, trackingKafka KafkaTrackingService) {
	log.Printf("🔧 Setting Kafka services - Shipping: %v, Tracking: %v", shippingKafka != nil, trackingKafka != nil)
	GlobalShippingKafkaService = shippingKafka
	GlobalTrackingKafkaService = trackingKafka

	if GlobalShippingKafkaService != nil {
		log.Printf("✅ GlobalShippingKafkaService set successfully")
	} else {
		log.Printf("❌ GlobalShippingKafkaService is nil")
	}

	if GlobalTrackingKafkaService != nil {
		log.Printf("✅ GlobalTrackingKafkaService set successfully")
	} else {
		log.Printf("❌ GlobalTrackingKafkaService is nil")
	}
}
