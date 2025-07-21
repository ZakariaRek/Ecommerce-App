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
	trackingRepo       *repository.TrackingRepository
	shippingRepo       *repository.ShippingRepository
	locationUpdateRepo *repository.LocationUpdateRepository
}

// NewTrackingService creates a new tracking service
func NewTrackingService(
	trackingRepo *repository.TrackingRepository,
	shippingRepo *repository.ShippingRepository,
	locationUpdateRepo *repository.LocationUpdateRepository,
) *TrackingService {
	return &TrackingService{
		trackingRepo:       trackingRepo,
		shippingRepo:       shippingRepo,
		locationUpdateRepo: locationUpdateRepo,
	}
}

// CreateTrackingRequest represents a request to create a tracking record
type CreateTrackingRequest struct {
	ShippingID uuid.UUID `json:"shipping_id"`
	Location   string    `json:"location"`
	Status     string    `json:"status"`
	Notes      string    `json:"notes"`
	Latitude   *float64  `json:"latitude,omitempty"`
	Longitude  *float64  `json:"longitude,omitempty"`
	DeviceID   string    `json:"device_id,omitempty"`
	DriverID   string    `json:"driver_id,omitempty"`
}

// CreateTracking creates a new tracking record
func (s *TrackingService) CreateTracking(shippingID uuid.UUID, location, status, notes string) (*models.ShipmentTracking, error) {
	// Verify that the shipping exists
	_, err := s.shippingRepo.GetByIDWithoutPreload(shippingID)
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

// CreateTrackingWithGPS creates a new tracking record with GPS coordinates
func (s *TrackingService) CreateTrackingWithGPS(req *CreateTrackingRequest) (*models.ShipmentTracking, error) {
	// Verify that the shipping exists
	_, err := s.shippingRepo.GetByIDWithoutPreload(req.ShippingID)
	if err != nil {
		return nil, errors.New("shipping not found")
	}

	// Create new tracking record
	tracking := &models.ShipmentTracking{
		ShippingID: req.ShippingID,
		Location:   req.Location,
		Status:     req.Status,
		Notes:      req.Notes,
		Latitude:   req.Latitude,
		Longitude:  req.Longitude,
		DeviceID:   req.DeviceID,
		DriverID:   req.DriverID,
		Timestamp:  time.Now(),
		CreatedAt:  time.Now(),
	}

	// Save to database
	if err := s.trackingRepo.Create(tracking); err != nil {
		return nil, err
	}

	// Update shipping's current location if GPS coordinates provided
	if req.Latitude != nil && req.Longitude != nil {
		s.shippingRepo.UpdateLocation(req.ShippingID, *req.Latitude, *req.Longitude)
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
	_, err := s.shippingRepo.GetByIDWithoutPreload(shippingID)
	if err != nil {
		return nil, errors.New("shipping not found")
	}

	return s.trackingRepo.GetTrackingHistory(shippingID)
}

// GetTrackingWithGPS retrieves tracking records with GPS coordinates for a shipping
func (s *TrackingService) GetTrackingWithGPS(shippingID uuid.UUID) ([]models.ShipmentTracking, error) {
	// Verify that the shipping exists
	_, err := s.shippingRepo.GetByIDWithoutPreload(shippingID)
	if err != nil {
		return nil, errors.New("shipping not found")
	}

	return s.trackingRepo.GetTrackingWithGPS(shippingID)
}

// UpdateTracking updates an existing tracking record
func (s *TrackingService) UpdateTracking(tracking *models.ShipmentTracking) error {
	// Verify that the tracking record exists
	existing, err := s.trackingRepo.GetByID(tracking.ID)
	if err != nil {
		return errors.New("tracking record not found")
	}

	// Verify that the shipping exists
	_, err = s.shippingRepo.GetByIDWithoutPreload(tracking.ShippingID)
	if err != nil {
		return errors.New("shipping not found")
	}

	// Update timestamp if location changed
	if tracking.Location != existing.Location {
		tracking.Timestamp = time.Now()
	}

	// Update shipping's current location if GPS coordinates provided
	if tracking.Latitude != nil && tracking.Longitude != nil {
		s.shippingRepo.UpdateLocation(tracking.ShippingID, *tracking.Latitude, *tracking.Longitude)
	}

	return s.trackingRepo.Update(tracking)
}

// UpdateTrackingLocation updates the location of a tracking record
func (s *TrackingService) UpdateTrackingLocation(id uuid.UUID, location string) error {
	tracking, err := s.trackingRepo.GetByID(id)
	if err != nil {
		return errors.New("tracking record not found")
	}

	tracking.UpdateLocation(location, nil, nil)

	return s.trackingRepo.Update(tracking)
}

// UpdateTrackingLocationWithGPS updates the location with GPS coordinates
func (s *TrackingService) UpdateTrackingLocationWithGPS(id uuid.UUID, location string, lat, lng float64) error {
	tracking, err := s.trackingRepo.GetByID(id)
	if err != nil {
		return errors.New("tracking record not found")
	}

	tracking.UpdateLocation(location, &lat, &lng)

	// Update shipping's current location
	s.shippingRepo.UpdateLocation(tracking.ShippingID, lat, lng)

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
	return s.CreateTracking(shippingID, location, status, notes)
}

// AddTrackingUpdateWithGPS adds a new tracking update with GPS coordinates
func (s *TrackingService) AddTrackingUpdateWithGPS(req *CreateTrackingRequest) (*models.ShipmentTracking, error) {
	return s.CreateTrackingWithGPS(req)
}

// GetLatestTracking gets the most recent tracking record for a shipping
func (s *TrackingService) GetLatestTracking(shippingID uuid.UUID) (*models.ShipmentTracking, error) {
	// Verify that the shipping exists
	_, err := s.shippingRepo.GetByIDWithoutPreload(shippingID)
	if err != nil {
		return nil, errors.New("shipping not found")
	}

	return s.trackingRepo.GetLatestTracking(shippingID)
}

// GetTrackingByDevice gets tracking records by device ID
func (s *TrackingService) GetTrackingByDevice(deviceID string, limit int) ([]models.ShipmentTracking, error) {
	return s.trackingRepo.GetTrackingByDeviceID(deviceID, limit)
}

// GetTrackingByDriver gets tracking records by driver ID
func (s *TrackingService) GetTrackingByDriver(driverID string, limit int) ([]models.ShipmentTracking, error) {
	return s.trackingRepo.GetTrackingByDriverID(driverID, limit)
}

// GetLocationUpdates gets real-time location updates for a shipping
func (s *TrackingService) GetLocationUpdates(shippingID uuid.UUID, limit int) ([]models.LocationUpdate, error) {
	return s.locationUpdateRepo.GetByShippingID(shippingID, limit)
}

// GetLatestLocationUpdate gets the most recent location update for a shipping
func (s *TrackingService) GetLatestLocationUpdate(shippingID uuid.UUID) (*models.LocationUpdate, error) {
	return s.locationUpdateRepo.GetLatestByShippingID(shippingID)
}

// GetLocationHistory gets location update history within a time range
func (s *TrackingService) GetLocationHistory(shippingID uuid.UUID, from, to time.Time) ([]models.LocationUpdate, error) {
	return s.locationUpdateRepo.GetLocationHistory(shippingID, from, to)
}
