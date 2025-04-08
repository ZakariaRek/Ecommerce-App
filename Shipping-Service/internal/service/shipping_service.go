package service

import (
	"errors"
	"time"

	"github.com/google/uuid"

	"github.com/ZakariaRek/Ecommerce-App/Shipping-Service/internal/models"
	"github.com/ZakariaRek/Ecommerce-App/Shipping-Service/internal/repository"
)

// ShippingService provides business logic for shipping operations
type ShippingService struct {
	shippingRepo *repository.ShippingRepository
	trackingRepo *repository.TrackingRepository
}

// NewShippingService creates a new shipping service
func NewShippingService(
	shippingRepo *repository.ShippingRepository,
	trackingRepo *repository.TrackingRepository,
) *ShippingService {
	return &ShippingService{
		shippingRepo: shippingRepo,
		trackingRepo: trackingRepo,
	}
}

// CreateShipping creates a new shipping for an order
func (s *ShippingService) CreateShipping(orderID uuid.UUID, carrier string) (*models.Shipping, error) {
	// Check if shipping already exists for this order
	_, err := s.shippingRepo.GetByOrderID(orderID)
	if err == nil {
		return nil, errors.New("shipping already exists for this order")
	}

	// Create new shipping
	shipping := &models.Shipping{
		OrderID:   orderID,
		Status:    models.StatusPending,
		Carrier:   carrier,
		CreatedAt: time.Now(),
		UpdatedAt: time.Now(),
	}

	// Generate a tracking number (in a real system, this would come from the carrier's API)
	shipping.TrackingNumber = generateTrackingNumber()

	// Estimate delivery date (in a real system, this would be from carrier's API)
	estimatedDelivery := time.Now().AddDate(0, 0, 3) // 3 days from now
	shipping.EstimatedDelivery = &estimatedDelivery

	// Save to database
	if err := s.shippingRepo.Create(shipping); err != nil {
		return nil, err
	}

	// Create initial tracking entry
	tracking := &models.ShipmentTracking{
		ShippingID: shipping.ID,
		Location:   "Distribution Center",
		Status:     "Order received and processing",
		Timestamp:  time.Now(),
		CreatedAt:  time.Now(),
	}

	if err := s.trackingRepo.Create(tracking); err != nil {
		return nil, err
	}

	return shipping, nil
}

// GetShipping retrieves shipping details by ID
func (s *ShippingService) GetShipping(id uuid.UUID) (*models.Shipping, error) {
	return s.shippingRepo.GetByID(id)
}

// GetShippingByOrder retrieves shipping details by order ID
func (s *ShippingService) GetShippingByOrder(orderID uuid.UUID) (*models.Shipping, error) {
	return s.shippingRepo.GetByOrderID(orderID)
}

// GetAllShippings retrieves all shippings with optional pagination
func (s *ShippingService) GetAllShippings(limit, offset int) ([]models.Shipping, error) {
	return s.shippingRepo.GetAll(limit, offset)
}

// UpdateShipping updates shipping details
func (s *ShippingService) UpdateShipping(shipping *models.Shipping) error {
	return s.shippingRepo.Update(shipping)
}

// UpdateStatus updates the shipping status and adds a tracking entry
func (s *ShippingService) UpdateStatus(id uuid.UUID, status models.ShippingStatus, location, notes string) error {
	// Validate status
	if !status.IsValid() {
		return errors.New("invalid shipping status")
	}

	// Get existing shipping
	shipping, err := s.shippingRepo.GetByID(id)
	if err != nil {
		return err
	}

	// Update shipping status
	shipping.UpdateStatus(status)
	if err := s.shippingRepo.Update(shipping); err != nil {
		return err
	}

	// Add tracking entry
	tracking := &models.ShipmentTracking{
		ShippingID: id,
		Location:   location,
		Status:     status.String(),
		Timestamp:  time.Now(),
		Notes:      notes,
		CreatedAt:  time.Now(),
	}

	return s.trackingRepo.Create(tracking)
}

// TrackOrder gets the tracking history for a shipment
func (s *ShippingService) TrackOrder(id uuid.UUID) (*models.Shipping, []models.ShipmentTracking, error) {
	// Get shipping info
	shipping, err := s.shippingRepo.GetByID(id)
	if err != nil {
		return nil, nil, err
	}

	// Get tracking history
	trackingHistory, err := s.trackingRepo.GetTrackingHistory(id)
	if err != nil {
		return nil, nil, err
	}

	return shipping, trackingHistory, nil
}

// CalculateShippingCost calculates the shipping cost
func (s *ShippingService) CalculateShippingCost(id uuid.UUID) (float64, error) {
	shipping, err := s.shippingRepo.GetByID(id)
	if err != nil {
		return 0, err
	}

	// Use the model's method to calculate the cost
	return shipping.CalculateShippingCost(), nil
}

// generateTrackingNumber creates a mock tracking number
// In a real system, this would be provided by the shipping carrier
func generateTrackingNumber() string {
	// Format: SHP-UUID-shortened
	id := uuid.New().String()
	shortID := id[:8] // Use first 8 characters of UUID
	return "SHP-" + shortID
}
