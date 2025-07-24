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
	shippingRepo       *repository.ShippingRepository
	trackingRepo       *repository.TrackingRepository
	addressRepo        *repository.AddressRepository
	locationUpdateRepo *repository.LocationUpdateRepository
}

// NewShippingService creates a new shipping service
func NewShippingService(
	shippingRepo *repository.ShippingRepository,
	trackingRepo *repository.TrackingRepository,
	addressRepo *repository.AddressRepository,
	locationUpdateRepo *repository.LocationUpdateRepository,
) *ShippingService {
	return &ShippingService{
		shippingRepo:       shippingRepo,
		trackingRepo:       trackingRepo,
		addressRepo:        addressRepo,
		locationUpdateRepo: locationUpdateRepo,
	}
}

// CreateShippingRequest represents a request to create shipping
type CreateShippingRequest struct {
	OrderID           uuid.UUID `json:"order_id"`
	UserID            uuid.UUID `json:"user_id"`
	Carrier           string    `json:"carrier"`
	ShippingAddressID uuid.UUID `json:"shipping_address_id"`
	Weight            float64   `json:"weight"`
	Dimensions        string    `json:"dimensions"`
}

// UserShippingStats represents shipping statistics for a user
type UserShippingStats struct {
	Total     int64            `json:"total"`
	InTransit int64            `json:"in_transit"`
	Delivered int64            `json:"delivered"`
	Delayed   int64            `json:"delayed"`
	ByStatus  map[string]int64 `json:"by_status"`
}

// CreateShippingWithAddress creates a new shipping with address
func (s *ShippingService) CreateShippingWithAddress(req *CreateShippingRequest) (*models.Shipping, error) {
	// Check if shipping already exists for this order
	_, err := s.shippingRepo.GetByOrderID(req.OrderID)
	if err == nil {
		return nil, errors.New("shipping already exists for this order")
	}

	// Validate shipping address exists
	_, err = s.addressRepo.GetByID(req.ShippingAddressID)
	if err != nil {
		return nil, errors.New("shipping address not found")
	}

	// Get default origin address
	originAddress, err := s.addressRepo.GetDefaultOriginAddress()
	if err != nil {
		return nil, errors.New("no default origin address configured")
	}

	// Create new shipping
	shipping := &models.Shipping{
		OrderID:           req.OrderID,
		UserID:            req.UserID, // Set UserID
		Status:            models.StatusPending,
		Carrier:           req.Carrier,
		ShippingAddressID: req.ShippingAddressID,
		OriginAddressID:   originAddress.ID,
		Weight:            req.Weight,
		Dimensions:        req.Dimensions,
		CreatedAt:         time.Now(),
		UpdatedAt:         time.Now(),
	}

	// Generate a tracking number
	shipping.TrackingNumber = generateTrackingNumber()

	// Estimate delivery date
	estimatedDelivery := time.Now().AddDate(0, 0, 3)
	shipping.EstimatedDelivery = &estimatedDelivery

	// Calculate shipping cost
	shipping.ShippingCost = shipping.CalculateShippingCost()

	// Save to database
	if err := s.shippingRepo.Create(shipping); err != nil {
		return nil, err
	}

	// Create initial tracking entry
	tracking := &models.ShipmentTracking{
		ShippingID: shipping.ID,
		Location:   originAddress.City + ", " + originAddress.State,
		Status:     "Order received and processing",
		Timestamp:  time.Now(),
		CreatedAt:  time.Now(),
	}

	if err := s.trackingRepo.Create(tracking); err != nil {
		return nil, err
	}

	// Load relationships for return
	return s.shippingRepo.GetByID(shipping.ID)
}

// CreateShipping creates a new shipping for an order (backward compatibility)
func (s *ShippingService) CreateShipping(orderID uuid.UUID, carrier string) (*models.Shipping, error) {
	// Create a dummy shipping address (for backward compatibility)
	dummyAddress := &models.Address{
		FirstName:    "Customer",
		LastName:     "Customer",
		AddressLine1: "123 Customer St",
		City:         "Customer City",
		State:        "CS",
		PostalCode:   "12345",
		Country:      "USA",
	}

	if err := s.addressRepo.Create(dummyAddress); err != nil {
		return nil, err
	}

	// Generate a dummy user ID for backward compatibility
	dummyUserID := uuid.New()

	req := &CreateShippingRequest{
		OrderID:           orderID,
		UserID:            dummyUserID,
		Carrier:           carrier,
		ShippingAddressID: dummyAddress.ID,
		Weight:            1.0,      // Default weight
		Dimensions:        "10x8x6", // Default dimensions
	}

	return s.CreateShippingWithAddress(req)
}

// CreateShippingForUser creates a new shipping for a specific user
func (s *ShippingService) CreateShippingForUser(orderID, userID uuid.UUID, carrier string, shippingAddressID uuid.UUID, weight float64, dimensions string) (*models.Shipping, error) {
	req := &CreateShippingRequest{
		OrderID:           orderID,
		UserID:            userID,
		Carrier:           carrier,
		ShippingAddressID: shippingAddressID,
		Weight:            weight,
		Dimensions:        dimensions,
	}

	return s.CreateShippingWithAddress(req)
}

// GetShippingsByUser retrieves all shippings for a specific user
func (s *ShippingService) GetShippingsByUser(userID uuid.UUID, limit, offset int) ([]models.Shipping, error) {
	return s.shippingRepo.GetByUserID(userID, limit, offset)
}

// GetShippingsByUserAndStatus retrieves shippings for a user by status
func (s *ShippingService) GetShippingsByUserAndStatus(userID uuid.UUID, status models.ShippingStatus, limit, offset int) ([]models.Shipping, error) {
	return s.shippingRepo.GetByUserIDAndStatus(userID, status, limit, offset)
}

// GetUserShippingStats returns shipping statistics for a user
func (s *ShippingService) GetUserShippingStats(userID uuid.UUID) (*UserShippingStats, error) {
	// Get total count
	total, err := s.shippingRepo.CountByUserID(userID)
	if err != nil {
		return nil, err
	}

	// Get in-transit count
	inTransitStatuses := []models.ShippingStatus{
		models.StatusShipped,
		models.StatusInTransit,
		models.StatusOutForDelivery,
	}

	var inTransit int64
	for _, status := range inTransitStatuses {
		count, err := s.shippingRepo.CountByUserIDAndStatus(userID, status)
		if err != nil {
			return nil, err
		}
		inTransit += count
	}

	// Get delivered count
	delivered, err := s.shippingRepo.CountByUserIDAndStatus(userID, models.StatusDelivered)
	if err != nil {
		return nil, err
	}

	// Get delayed count (for now, using failed status as proxy)
	delayed, err := s.shippingRepo.CountByUserIDAndStatus(userID, models.StatusFailed)
	if err != nil {
		return nil, err
	}

	// Get breakdown by status
	byStatus, err := s.shippingRepo.GetUserShippingStats(userID)
	if err != nil {
		return nil, err
	}

	return &UserShippingStats{
		Total:     total,
		InTransit: inTransit,
		Delivered: delivered,
		Delayed:   delayed,
		ByStatus:  byStatus,
	}, nil
}

// GetUserShippingsInTransit returns user's shippings currently in transit
func (s *ShippingService) GetUserShippingsInTransit(userID uuid.UUID) ([]models.Shipping, error) {
	return s.shippingRepo.GetUserShippingsInTransit(userID)
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

// GetShippingsByStatus retrieves shippings by status
func (s *ShippingService) GetShippingsByStatus(status models.ShippingStatus, limit, offset int) ([]models.Shipping, error) {
	return s.shippingRepo.GetShippingsByStatus(status, limit, offset)
}

// UpdateShipping updates shipping details
func (s *ShippingService) UpdateShipping(shipping *models.Shipping) error {
	// Recalculate cost if weight or dimensions changed
	shipping.ShippingCost = shipping.CalculateShippingCost()

	return s.shippingRepo.Update(shipping)
}

// UpdateStatus updates the shipping status and adds a tracking entry
func (s *ShippingService) UpdateStatus(id uuid.UUID, status models.ShippingStatus, location, notes string) error {
	// Validate status
	if !status.IsValid() {
		return errors.New("invalid shipping status")
	}

	// Get existing shipping
	shipping, err := s.shippingRepo.GetByIDWithoutPreload(id)
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

// UpdateStatusWithGPS updates the shipping status with GPS coordinates
func (s *ShippingService) UpdateStatusWithGPS(id uuid.UUID, status models.ShippingStatus, location, notes string, lat, lng *float64, deviceID, driverID string) error {
	// Validate status
	if !status.IsValid() {
		return errors.New("invalid shipping status")
	}

	// Get existing shipping
	shipping, err := s.shippingRepo.GetByIDWithoutPreload(id)
	if err != nil {
		return err
	}

	// Update shipping status and location
	shipping.UpdateStatus(status)
	if lat != nil && lng != nil {
		shipping.UpdateCurrentLocation(*lat, *lng)
	}

	if err := s.shippingRepo.Update(shipping); err != nil {
		return err
	}

	// Add tracking entry with GPS
	tracking := &models.ShipmentTracking{
		ShippingID: id,
		Location:   location,
		Status:     status.String(),
		Timestamp:  time.Now(),
		Notes:      notes,
		Latitude:   lat,
		Longitude:  lng,
		DeviceID:   deviceID,
		DriverID:   driverID,
		CreatedAt:  time.Now(),
	}

	return s.trackingRepo.Create(tracking)
}

// UpdateCurrentLocation updates the current GPS location of a shipping
func (s *ShippingService) UpdateCurrentLocation(id uuid.UUID, lat, lng float64) error {
	return s.shippingRepo.UpdateLocation(id, lat, lng)
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
	shipping, err := s.shippingRepo.GetByIDWithoutPreload(id)
	if err != nil {
		return 0, err
	}

	// Use the model's method to calculate the cost
	cost := shipping.CalculateShippingCost()

	// Update the shipping record with the calculated cost
	shipping.ShippingCost = cost
	s.shippingRepo.Update(shipping)

	return cost, nil
}

// GetShippingsInTransit returns all shippings currently in transit
func (s *ShippingService) GetShippingsInTransit() ([]models.Shipping, error) {
	return s.shippingRepo.GetShippingsInTransit()
}

// AddLocationUpdate adds a real-time location update
func (s *ShippingService) AddLocationUpdate(shippingID uuid.UUID, deviceID string, lat, lng, speed, heading, accuracy float64) error {
	// Verify shipping exists
	_, err := s.shippingRepo.GetByIDWithoutPreload(shippingID)
	if err != nil {
		return errors.New("shipping not found")
	}

	// Create location update
	locationUpdate := &models.LocationUpdate{
		ShippingID: shippingID,
		DeviceID:   deviceID,
		Latitude:   lat,
		Longitude:  lng,
		Speed:      &speed,
		Heading:    &heading,
		Accuracy:   &accuracy,
		Timestamp:  time.Now(),
		CreatedAt:  time.Now(),
	}

	if err := s.locationUpdateRepo.Create(locationUpdate); err != nil {
		return err
	}

	// Update shipping's current location
	return s.shippingRepo.UpdateLocation(shippingID, lat, lng)
}

// GetLocationHistory gets location update history for a shipping
func (s *ShippingService) GetLocationHistory(shippingID uuid.UUID, limit int) ([]models.LocationUpdate, error) {
	return s.locationUpdateRepo.GetByShippingID(shippingID, limit)
}

// generateTrackingNumber creates a mock tracking number
func generateTrackingNumber() string {
	id := uuid.New().String()
	shortID := id[:8]
	return "SHP-" + shortID
}
