package database

import (
	"fmt"
	"log"

	"github.com/ZakariaRek/Ecommerce-App/Shipping-Service/internal/config"
	"github.com/ZakariaRek/Ecommerce-App/Shipping-Service/internal/models"
	"gorm.io/driver/postgres"
	"gorm.io/gorm"
)

// InitDB initializes the database connection
func InitDB(cfg *config.Config) *gorm.DB {
	dsn := fmt.Sprintf("host=%s user=%s password=%s dbname=%s port=%s sslmode=disable TimeZone=UTC",
		cfg.DBHost, cfg.DBUser, cfg.DBPassword, cfg.DBName, cfg.DBPort)

	db, err := gorm.Open(postgres.Open(dsn), &gorm.Config{})
	if err != nil {
		log.Fatalf("Failed to connect to database: %v", err)
	}

	// Auto migrate the schema
	err = db.AutoMigrate(
		&models.Address{},
		&models.Shipping{},
		&models.ShipmentTracking{},
		&models.LocationUpdate{},
	)
	if err != nil {
		log.Fatalf("Failed to migrate database: %v", err)
	}

	log.Println("Database migration completed successfully")
	return db
}

// SeedDefaultData seeds the database with default addresses if needed
func SeedDefaultData(db *gorm.DB) error {
	// Create a default origin address if it doesn't exist
	var originAddress models.Address
	if err := db.Where("company = ? AND address_line1 = ?", "Shipping Company", "123 Warehouse St").First(&originAddress).Error; err != nil {
		if err == gorm.ErrRecordNotFound {
			originAddress = models.Address{
				FirstName:    "Shipping",
				LastName:     "Department",
				Company:      "Shipping Company",
				AddressLine1: "123 Warehouse St",
				City:         "Distribution City",
				State:        "DC",
				PostalCode:   "12345",
				Country:      "USA",
				Phone:        "+1-555-0123",
				Email:        "shipping@company.com",
			}
			if err := db.Create(&originAddress).Error; err != nil {
				return fmt.Errorf("failed to create default origin address: %v", err)
			}
			log.Println("Created default origin address")
		} else {
			return fmt.Errorf("failed to check for origin address: %v", err)
		}
	}

	return nil
}
