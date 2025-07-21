package repository

import (
	"errors"
	"time"

	"github.com/google/uuid"
	"gorm.io/gorm"

	"github.com/ZakariaRek/Ecommerce-App/Shipping-Service/internal/models"
)

// LocationUpdateRepository handles database operations for location updates
type LocationUpdateRepository struct {
	db *gorm.DB
}

// NewLocationUpdateRepository creates a new location update repository
func NewLocationUpdateRepository(db *gorm.DB) *LocationUpdateRepository {
	return &LocationUpdateRepository{db: db}
}

// Create inserts a new location update into the database
func (r *LocationUpdateRepository) Create(locationUpdate *models.LocationUpdate) error {
	return r.db.Create(locationUpdate).Error
}

// GetByID retrieves a location update by its ID
func (r *LocationUpdateRepository) GetByID(id uuid.UUID) (*models.LocationUpdate, error) {
	var locationUpdate models.LocationUpdate

	if err := r.db.First(&locationUpdate, "id = ?", id).Error; err != nil {
		if errors.Is(err, gorm.ErrRecordNotFound) {
			return nil, errors.New("location update not found")
		}
		return nil, err
	}

	return &locationUpdate, nil
}

// GetByShippingID retrieves all location updates for a shipping
func (r *LocationUpdateRepository) GetByShippingID(shippingID uuid.UUID, limit int) ([]models.LocationUpdate, error) {
	var locationUpdates []models.LocationUpdate

	query := r.db.Where("shipping_id = ?", shippingID).Order("timestamp DESC")

	if limit > 0 {
		query = query.Limit(limit)
	}

	if err := query.Find(&locationUpdates).Error; err != nil {
		return nil, err
	}

	return locationUpdates, nil
}

// GetLatestByShippingID retrieves the most recent location update for a shipping
func (r *LocationUpdateRepository) GetLatestByShippingID(shippingID uuid.UUID) (*models.LocationUpdate, error) {
	var locationUpdate models.LocationUpdate

	if err := r.db.Where("shipping_id = ?", shippingID).
		Order("timestamp DESC").
		First(&locationUpdate).Error; err != nil {
		if errors.Is(err, gorm.ErrRecordNotFound) {
			return nil, errors.New("no location updates found")
		}
		return nil, err
	}

	return &locationUpdate, nil
}

// GetByDeviceID retrieves location updates by device ID
func (r *LocationUpdateRepository) GetByDeviceID(deviceID string, limit int) ([]models.LocationUpdate, error) {
	var locationUpdates []models.LocationUpdate

	query := r.db.Where("device_id = ?", deviceID).Order("timestamp DESC")

	if limit > 0 {
		query = query.Limit(limit)
	}

	if err := query.Find(&locationUpdates).Error; err != nil {
		return nil, err
	}

	return locationUpdates, nil
}

// GetRecentUpdates retrieves location updates within a time range
func (r *LocationUpdateRepository) GetRecentUpdates(since time.Time, limit int) ([]models.LocationUpdate, error) {
	var locationUpdates []models.LocationUpdate

	query := r.db.Where("timestamp >= ?", since).Order("timestamp DESC")

	if limit > 0 {
		query = query.Limit(limit)
	}

	if err := query.Find(&locationUpdates).Error; err != nil {
		return nil, err
	}

	return locationUpdates, nil
}

// Update updates an existing location update
func (r *LocationUpdateRepository) Update(locationUpdate *models.LocationUpdate) error {
	return r.db.Save(locationUpdate).Error
}

// Delete removes a location update by ID
func (r *LocationUpdateRepository) Delete(id uuid.UUID) error {
	return r.db.Delete(&models.LocationUpdate{}, "id = ?", id).Error
}

// DeleteOldUpdates removes location updates older than the specified time
func (r *LocationUpdateRepository) DeleteOldUpdates(before time.Time) error {
	return r.db.Where("timestamp < ?", before).Delete(&models.LocationUpdate{}).Error
}

// GetLocationHistory retrieves location history for a shipping within a time range
func (r *LocationUpdateRepository) GetLocationHistory(shippingID uuid.UUID, from, to time.Time) ([]models.LocationUpdate, error) {
	var locationUpdates []models.LocationUpdate

	if err := r.db.Where("shipping_id = ? AND timestamp BETWEEN ? AND ?", shippingID, from, to).
		Order("timestamp ASC").Find(&locationUpdates).Error; err != nil {
		return nil, err
	}

	return locationUpdates, nil
}
