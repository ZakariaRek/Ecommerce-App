package repository

import (
	"errors"
	"fmt"

	"github.com/google/uuid"
	"gorm.io/gorm"

	"github.com/ZakariaRek/Ecommerce-App/Shipping-Service/internal/models"
)

// AddressRepository handles database operations for addresses
type AddressRepository struct {
	db *gorm.DB
}

// NewAddressRepository creates a new address repository
func NewAddressRepository(db *gorm.DB) *AddressRepository {
	return &AddressRepository{db: db}
}

// Create inserts a new address into the database
func (r *AddressRepository) Create(address *models.Address) error {
	return r.db.Create(address).Error
}

// GetByID retrieves an address by its ID
func (r *AddressRepository) GetByID(id uuid.UUID) (*models.Address, error) {
	var address models.Address

	if err := r.db.First(&address, "id = ?", id).Error; err != nil {
		if errors.Is(err, gorm.ErrRecordNotFound) {
			return nil, fmt.Errorf("address not found: %s", id)
		}
		return nil, err
	}

	return &address, nil
}

// GetAll retrieves all addresses with optional pagination
func (r *AddressRepository) GetAll(limit, offset int) ([]models.Address, error) {
	var addresses []models.Address

	query := r.db
	if limit > 0 {
		query = query.Limit(limit)
	}
	if offset > 0 {
		query = query.Offset(offset)
	}

	if err := query.Find(&addresses).Error; err != nil {
		return nil, err
	}

	return addresses, nil
}

// Update updates an existing address in the database
func (r *AddressRepository) Update(address *models.Address) error {
	return r.db.Save(address).Error
}

// Delete removes an address by ID
func (r *AddressRepository) Delete(id uuid.UUID) error {
	return r.db.Delete(&models.Address{}, "id = ?", id).Error
}

// FindByNameAndLocation finds addresses by name and location criteria
func (r *AddressRepository) FindByNameAndLocation(firstName, lastName, city, state string) ([]models.Address, error) {
	var addresses []models.Address

	query := r.db
	if firstName != "" {
		query = query.Where("first_name ILIKE ?", "%"+firstName+"%")
	}
	if lastName != "" {
		query = query.Where("last_name ILIKE ?", "%"+lastName+"%")
	}
	if city != "" {
		query = query.Where("city ILIKE ?", "%"+city+"%")
	}
	if state != "" {
		query = query.Where("state ILIKE ?", "%"+state+"%")
	}

	if err := query.Find(&addresses).Error; err != nil {
		return nil, err
	}

	return addresses, nil
}

// GetDefaultOriginAddress returns the default origin address
func (r *AddressRepository) GetDefaultOriginAddress() (*models.Address, error) {
	var address models.Address

	if err := r.db.Where("company = ?", "Shipping Company").First(&address).Error; err != nil {
		if errors.Is(err, gorm.ErrRecordNotFound) {
			return nil, fmt.Errorf("no default origin address found")
		}
		return nil, err
	}

	return &address, nil
}
