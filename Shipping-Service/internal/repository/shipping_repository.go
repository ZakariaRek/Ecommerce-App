package repository

import (
	"errors"
	"fmt"

	"github.com/google/uuid"
	"gorm.io/gorm"

	"github.com/ZakariaRek/Ecommerce-App/Shipping-Service/internal/models"
)

// ShippingRepository handles database operations for shippings
type ShippingRepository struct {
	db *gorm.DB
}

// NewShippingRepository creates a new shipping repository
func NewShippingRepository(db *gorm.DB) *ShippingRepository {
	return &ShippingRepository{db: db}
}

// Create inserts a new shipping into the database
func (r *ShippingRepository) Create(shipping *models.Shipping) error {
	return r.db.Create(shipping).Error
}

// GetByID retrieves a shipping by its ID with preloaded relationships
func (r *ShippingRepository) GetByID(id uuid.UUID) (*models.Shipping, error) {
	var shipping models.Shipping

	if err := r.db.Preload("ShippingAddress").Preload("OriginAddress").Preload("TrackingHistory").
		First(&shipping, "id = ?", id).Error; err != nil {
		if errors.Is(err, gorm.ErrRecordNotFound) {
			return nil, fmt.Errorf("shipping not found: %s", id)
		}
		return nil, err
	}

	return &shipping, nil
}

// GetByIDWithoutPreload retrieves a shipping by its ID without preloading relationships
func (r *ShippingRepository) GetByIDWithoutPreload(id uuid.UUID) (*models.Shipping, error) {
	var shipping models.Shipping

	if err := r.db.First(&shipping, "id = ?", id).Error; err != nil {
		if errors.Is(err, gorm.ErrRecordNotFound) {
			return nil, fmt.Errorf("shipping not found: %s", id)
		}
		return nil, err
	}

	return &shipping, nil
}

// GetByOrderID retrieves a shipping by its order ID with preloaded relationships
func (r *ShippingRepository) GetByOrderID(orderID uuid.UUID) (*models.Shipping, error) {
	var shipping models.Shipping

	if err := r.db.Preload("ShippingAddress").Preload("OriginAddress").Preload("TrackingHistory").
		First(&shipping, "order_id = ?", orderID).Error; err != nil {
		if errors.Is(err, gorm.ErrRecordNotFound) {
			return nil, fmt.Errorf("shipping not found for order: %s", orderID)
		}
		return nil, err
	}

	return &shipping, nil
}

// GetAll retrieves all shippings with optional pagination and preloaded relationships
func (r *ShippingRepository) GetAll(limit, offset int) ([]models.Shipping, error) {
	var shippings []models.Shipping

	query := r.db.Preload("ShippingAddress").Preload("OriginAddress")
	if limit > 0 {
		query = query.Limit(limit)
	}
	if offset > 0 {
		query = query.Offset(offset)
	}

	if err := query.Find(&shippings).Error; err != nil {
		return nil, err
	}

	return shippings, nil
}

// Update updates an existing shipping in the database
func (r *ShippingRepository) Update(shipping *models.Shipping) error {
	return r.db.Save(shipping).Error
}

// UpdateStatus updates just the status of a shipping
func (r *ShippingRepository) UpdateStatus(id uuid.UUID, status models.ShippingStatus) error {
	var shipping models.Shipping

	if err := r.db.First(&shipping, "id = ?", id).Error; err != nil {
		return err
	}

	shipping.UpdateStatus(status)

	return r.db.Save(&shipping).Error
}

// UpdateLocation updates the current location of a shipping
func (r *ShippingRepository) UpdateLocation(id uuid.UUID, lat, lng float64) error {
	var shipping models.Shipping

	if err := r.db.First(&shipping, "id = ?", id).Error; err != nil {
		return err
	}

	shipping.UpdateCurrentLocation(lat, lng)

	return r.db.Save(&shipping).Error
}

// Delete removes a shipping by ID
func (r *ShippingRepository) Delete(id uuid.UUID) error {
	return r.db.Delete(&models.Shipping{}, "id = ?", id).Error
}

// GetShippingsInTransit returns all shippings that are currently in transit
func (r *ShippingRepository) GetShippingsInTransit() ([]models.Shipping, error) {
	var shippings []models.Shipping

	statuses := []models.ShippingStatus{
		models.StatusShipped,
		models.StatusInTransit,
		models.StatusOutForDelivery,
	}

	if err := r.db.Preload("ShippingAddress").Preload("OriginAddress").
		Where("status IN ?", statuses).Find(&shippings).Error; err != nil {
		return nil, err
	}

	return shippings, nil
}

// GetShippingsByStatus returns shippings filtered by status
func (r *ShippingRepository) GetShippingsByStatus(status models.ShippingStatus, limit, offset int) ([]models.Shipping, error) {
	var shippings []models.Shipping

	query := r.db.Preload("ShippingAddress").Preload("OriginAddress").Where("status = ?", status)

	if limit > 0 {
		query = query.Limit(limit)
	}
	if offset > 0 {
		query = query.Offset(offset)
	}

	if err := query.Find(&shippings).Error; err != nil {
		return nil, err
	}

	return shippings, nil
}
