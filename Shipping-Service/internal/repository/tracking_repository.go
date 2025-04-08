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

// Update updates an existing tracking record
func (r *TrackingRepository) Update(tracking *models.ShipmentTracking) error {
	return r.db.Save(tracking).Error
}

// Delete removes a tracking record by ID
func (r *TrackingRepository) Delete(id uuid.UUID) error {
	return r.db.Delete(&models.ShipmentTracking{}, "id = ?", id).Error
}
