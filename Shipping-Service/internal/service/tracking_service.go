// internal/service/tracking_service.go
package service

import (
	"errors"
	"time"

	"github.com/google/uuid"

	"github.com/ZakariaRek/Ecommerce-App/Shipping-Service/internal/models"
	"github.com/ZakariaRek/Ecommerce-App/Shipping-Service/internal/repository"
)

// TrackingService provides business logic for tracking operations
type TrackingService struct {
	trackingRepo *repository.TrackingRepository
	shippingRepo *repository.ShippingRepository
}

// NewTrackingService creates a new tracking service
func NewTrackingService(
	trackingRepo *repository.TrackingRepository,
	shippingRepo *repository.ShippingRepository,
) *TrackingService {
	return &TrackingService{
		trackingRepo: trackingRepo,
		shippingRepo: shippingRepo,
	}
}

// CreateTracking creates a new tracking record
func (s *TrackingService) CreateTracking(shippingID uuid.UUID, location, status, notes string) (*models.ShipmentTracking, error) {
	// Verify that the shipping exists
	_, err := s.shippingRepo.GetByID(shippingID)
	if err != nil {
		return nil, errors.New("shipping not found")
	}

	// Create new tracking record
	tracking := &models.ShipmentTracking{
		ShippingID: shippingID,
		Location:   location,
		Status:     status,
		Notes:      notes,
		Timestamp:  time.Now(),
		CreatedAt:  time.Now(),
	}

	// Save to database
	if err := s.trackingRepo.Create(tracking); err != nil {
		return nil, err
	}

	return tracking, nil
}

// GetTracking retrieves a tracking record by ID
func (s *TrackingService) GetTracking(id uuid.UUID) (*models.ShipmentTracking, error) {
	return s.trackingRepo.GetByID(id)
}

// GetTrackingHistory retrieves all tracking records for a shipping
func (s *TrackingService) GetTrackingHistory(shippingID uuid.UUID) ([]models.ShipmentTracking, error) {
	// Verify that the shipping exists
	_, err := s.shippingRepo.GetByID(shippingID)
	if err != nil {
		return nil, errors.New("shipping not found")
	}

	return s.trackingRepo.GetTrackingHistory(shippingID)
}

// UpdateTracking updates an existing tracking record
func (s *TrackingService) UpdateTracking(tracking *models.ShipmentTracking) error {
	// Verify that the tracking record exists
	existing, err := s.trackingRepo.GetByID(tracking.ID)
	if err != nil {
		return errors.New("tracking record not found")
	}

	// Verify that the shipping exists
	_, err = s.shippingRepo.GetByID(tracking.ShippingID)
	if err != nil {
		return errors.New("shipping not found")
	}

	// Update timestamp if location changed
	if tracking.Location != existing.Location {
		tracking.Timestamp = time.Now()
	}

	return s.trackingRepo.Update(tracking)
}

// UpdateTrackingLocation updates the location of a tracking record
func (s *TrackingService) UpdateTrackingLocation(id uuid.UUID, location string) error {
	tracking, err := s.trackingRepo.GetByID(id)
	if err != nil {
		return errors.New("tracking record not found")
	}

	tracking.UpdateLocation(location)

	return s.trackingRepo.Update(tracking)
}

// DeleteTracking deletes a tracking record
func (s *TrackingService) DeleteTracking(id uuid.UUID) error {
	// Verify that the tracking record exists
	_, err := s.trackingRepo.GetByID(id)
	if err != nil {
		return errors.New("tracking record not found")
	}

	return s.trackingRepo.Delete(id)
}

// GetTrackingWithShipping gets tracking record with shipping details
func (s *TrackingService) GetTrackingWithShipping(id uuid.UUID) (*models.ShipmentTracking, *models.Shipping, error) {
	tracking, err := s.trackingRepo.GetByID(id)
	if err != nil {
		return nil, nil, errors.New("tracking record not found")
	}

	shipping, err := s.shippingRepo.GetByID(tracking.ShippingID)
	if err != nil {
		return nil, nil, errors.New("associated shipping not found")
	}

	return tracking, shipping, nil
}

// AddTrackingUpdate adds a new tracking update for existing shipping
func (s *TrackingService) AddTrackingUpdate(shippingID uuid.UUID, location, status, notes string) (*models.ShipmentTracking, error) {
	// This is an alias for CreateTracking for semantic clarity
	return s.CreateTracking(shippingID, location, status, notes)
}

// GetLatestTracking gets the most recent tracking record for a shipping
func (s *TrackingService) GetLatestTracking(shippingID uuid.UUID) (*models.ShipmentTracking, error) {
	trackingHistory, err := s.GetTrackingHistory(shippingID)
	if err != nil {
		return nil, err
	}

	if len(trackingHistory) == 0 {
		return nil, errors.New("no tracking records found")
	}

	// Return the first record (they're ordered by timestamp DESC)
	return &trackingHistory[0], nil
}
