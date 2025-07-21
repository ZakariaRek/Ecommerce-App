package service

import (
	"errors"
	"time"

	"github.com/google/uuid"

	"github.com/ZakariaRek/Ecommerce-App/Shipping-Service/internal/models"
	"github.com/ZakariaRek/Ecommerce-App/Shipping-Service/internal/repository"
)

// AddressService provides business logic for address operations
type AddressService struct {
	addressRepo *repository.AddressRepository
}

// NewAddressService creates a new address service
func NewAddressService(addressRepo *repository.AddressRepository) *AddressService {
	return &AddressService{
		addressRepo: addressRepo,
	}
}

// CreateAddressRequest represents a request to create an address
type CreateAddressRequest struct {
	FirstName    string   `json:"first_name" validate:"required"`
	LastName     string   `json:"last_name" validate:"required"`
	Company      string   `json:"company,omitempty"`
	AddressLine1 string   `json:"address_line1" validate:"required"`
	AddressLine2 string   `json:"address_line2,omitempty"`
	City         string   `json:"city" validate:"required"`
	State        string   `json:"state" validate:"required"`
	PostalCode   string   `json:"postal_code" validate:"required"`
	Country      string   `json:"country" validate:"required"`
	Phone        string   `json:"phone,omitempty"`
	Email        string   `json:"email,omitempty"`
	Latitude     *float64 `json:"latitude,omitempty"`
	Longitude    *float64 `json:"longitude,omitempty"`
}

// CreateAddress creates a new address
func (s *AddressService) CreateAddress(req *CreateAddressRequest) (*models.Address, error) {
	// Validate required fields
	if req.FirstName == "" || req.LastName == "" || req.AddressLine1 == "" ||
		req.City == "" || req.State == "" || req.PostalCode == "" || req.Country == "" {
		return nil, errors.New("missing required address fields")
	}

	// Create new address
	address := &models.Address{
		FirstName:    req.FirstName,
		LastName:     req.LastName,
		Company:      req.Company,
		AddressLine1: req.AddressLine1,
		AddressLine2: req.AddressLine2,
		City:         req.City,
		State:        req.State,
		PostalCode:   req.PostalCode,
		Country:      req.Country,
		Phone:        req.Phone,
		Email:        req.Email,
		Latitude:     req.Latitude,
		Longitude:    req.Longitude,
		CreatedAt:    time.Now(),
		UpdatedAt:    time.Now(),
	}

	// Save to database
	if err := s.addressRepo.Create(address); err != nil {
		return nil, err
	}

	return address, nil
}

// GetAddress retrieves an address by ID
func (s *AddressService) GetAddress(id uuid.UUID) (*models.Address, error) {
	return s.addressRepo.GetByID(id)
}

// GetAllAddresses retrieves all addresses with optional pagination
func (s *AddressService) GetAllAddresses(limit, offset int) ([]models.Address, error) {
	return s.addressRepo.GetAll(limit, offset)
}

// UpdateAddress updates an existing address
func (s *AddressService) UpdateAddress(address *models.Address) error {
	// Validate required fields
	if address.FirstName == "" || address.LastName == "" || address.AddressLine1 == "" ||
		address.City == "" || address.State == "" || address.PostalCode == "" || address.Country == "" {
		return errors.New("missing required address fields")
	}

	// Update timestamp
	address.UpdatedAt = time.Now()

	return s.addressRepo.Update(address)
}

// DeleteAddress deletes an address
func (s *AddressService) DeleteAddress(id uuid.UUID) error {
	// Verify that the address exists
	_, err := s.addressRepo.GetByID(id)
	if err != nil {
		return errors.New("address not found")
	}

	return s.addressRepo.Delete(id)
}

// SearchAddresses searches for addresses by name and location
func (s *AddressService) SearchAddresses(firstName, lastName, city, state string) ([]models.Address, error) {
	return s.addressRepo.FindByNameAndLocation(firstName, lastName, city, state)
}

// GetDefaultOriginAddress gets the default origin address
func (s *AddressService) GetDefaultOriginAddress() (*models.Address, error) {
	return s.addressRepo.GetDefaultOriginAddress()
}

// ValidateAddress validates an address structure
func (s *AddressService) ValidateAddress(address *models.Address) error {
	if address.FirstName == "" {
		return errors.New("first name is required")
	}
	if address.LastName == "" {
		return errors.New("last name is required")
	}
	if address.AddressLine1 == "" {
		return errors.New("address line 1 is required")
	}
	if address.City == "" {
		return errors.New("city is required")
	}
	if address.State == "" {
		return errors.New("state is required")
	}
	if address.PostalCode == "" {
		return errors.New("postal code is required")
	}
	if address.Country == "" {
		return errors.New("country is required")
	}

	return nil
}

// FormatAddress returns a formatted address string
func (s *AddressService) FormatAddress(id uuid.UUID) (string, error) {
	address, err := s.addressRepo.GetByID(id)
	if err != nil {
		return "", err
	}

	return address.GetFullAddress(), nil
}

// GetFullName returns the full name for an address
func (s *AddressService) GetFullName(id uuid.UUID) (string, error) {
	address, err := s.addressRepo.GetByID(id)
	if err != nil {
		return "", err
	}

	return address.GetFullName(), nil
}
