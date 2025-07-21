package repository

import (
	"errors"

	"github.com/google/uuid"
	"gorm.io/gorm"

	"github.com/ZakariaRek/Ecommerce-App/Shipping-Service/internal/models"
)

// TrackingRepository handles database operations for shipment tracking
type TrackingRepository struct {
	db *gorm.DB
}

// NewTrackingRepository creates a new tracking repository
func NewTrackingRepository(db *gorm.DB) *TrackingRepository {
	return &TrackingRepository{db: db}
}

// Create inserts a new tracking record into the database
func (r *TrackingRepository) Create(tracking *models.ShipmentTracking) error {
	return r.db.Create(tracking).Error
}

// GetByID retrieves a tracking record by its ID
func (r *TrackingRepository) GetByID(id uuid.UUID) (*models.ShipmentTracking, error) {
	var tracking models.ShipmentTracking

	if err := r.db.First(&tracking, "id = ?", id).Error; err != nil {
		if errors.Is(err, gorm.ErrRecordNotFound) {
			return nil, errors.New("tracking record not found")
		}
		return nil, err
	}

	return &tracking, nil
}

// GetTrackingHistory retrieves all tracking records for a shipping
func (r *TrackingRepository) GetTrackingHistory(shippingID uuid.UUID) ([]models.ShipmentTracking, error) {
	var trackings []models.ShipmentTracking

	if err := r.db.Where("shipping_id = ?", shippingID).Order("timestamp DESC").Find(&trackings).Error; err != nil {
		return nil, err
	}

	return trackings, nil
}

// GetLatestTracking retrieves the most recent tracking record for a shipping
func (r *TrackingRepository) GetLatestTracking(shippingID uuid.UUID) (*models.ShipmentTracking, error) {
	var tracking models.ShipmentTracking

	if err := r.db.Where("shipping_id = ?", shippingID).
		Order("timestamp DESC").
		First(&tracking).Error; err != nil {
		if errors.Is(err, gorm.ErrRecordNotFound) {
			return nil, errors.New("no tracking records found")
		}
		return nil, err
	}

	return &tracking, nil
}

// Update updates an existing tracking record
func (r *TrackingRepository) Update(tracking *models.ShipmentTracking) error {
	return r.db.Save(tracking).Error
}

// UpdateLocation updates the location and GPS coordinates of a tracking record
func (r *TrackingRepository) UpdateLocation(id uuid.UUID, location string, lat, lng *float64) error {
	var tracking models.ShipmentTracking

	if err := r.db.First(&tracking, "id = ?", id).Error; err != nil {
		return err
	}

	tracking.UpdateLocation(location, lat, lng)

	return r.db.Save(&tracking).Error
}

// Delete removes a tracking record by ID
func (r *TrackingRepository) Delete(id uuid.UUID) error {
	return r.db.Delete(&models.ShipmentTracking{}, "id = ?", id).Error
}

// GetTrackingByDeviceID retrieves tracking records by device ID
func (r *TrackingRepository) GetTrackingByDeviceID(deviceID string, limit int) ([]models.ShipmentTracking, error) {
	var trackings []models.ShipmentTracking

	query := r.db.Where("device_id = ?", deviceID).Order("timestamp DESC")

	if limit > 0 {
		query = query.Limit(limit)
	}

	if err := query.Find(&trackings).Error; err != nil {
		return nil, err
	}

	return trackings, nil
}

// GetTrackingByDriverID retrieves tracking records by driver ID
func (r *TrackingRepository) GetTrackingByDriverID(driverID string, limit int) ([]models.ShipmentTracking, error) {
	var trackings []models.ShipmentTracking

	query := r.db.Where("driver_id = ?", driverID).Order("timestamp DESC")

	if limit > 0 {
		query = query.Limit(limit)
	}

	if err := query.Find(&trackings).Error; err != nil {
		return nil, err
	}

	return trackings, nil
}

// GetTrackingWithGPS retrieves tracking records that have GPS coordinates
func (r *TrackingRepository) GetTrackingWithGPS(shippingID uuid.UUID) ([]models.ShipmentTracking, error) {
	var trackings []models.ShipmentTracking

	if err := r.db.Where("shipping_id = ? AND latitude IS NOT NULL AND longitude IS NOT NULL", shippingID).
		Order("timestamp DESC").Find(&trackings).Error; err != nil {
		return nil, err
	}

	return trackings, nil
}
