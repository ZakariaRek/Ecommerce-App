package main

import (
	"fmt"
	"log"
	"os"

	"gorm.io/driver/postgres" // You can also use mysql, sqlite, sqlserver
	"gorm.io/gorm"
)

// User is a model definition for the users table
type User struct {
	ID        uint   `gorm:"primaryKey"`
	Name      string `gorm:"size:255;not null"`
	Email     string `gorm:"size:255;uniqueIndex;not null"`
	CreatedAt int64  `gorm:"autoCreateTime"`
	UpdatedAt int64  `gorm:"autoUpdateTime"`
}

// Product is another example model
type Product struct {
	ID          uint   `gorm:"primaryKey"`
	Name        string `gorm:"size:255;not null"`
	Description string `gorm:"type:text"`
	Price       float64
	UserID      uint  // Foreign key to User
	CreatedAt   int64 `gorm:"autoCreateTime"`
	UpdatedAt   int64 `gorm:"autoUpdateTime"`
}

// DBConnection handles database connection
type DBConnection struct {
	DB *gorm.DB
}

// NewDBConnection creates a new database connection
func NewDBConnection() (*DBConnection, error) {
	// Get database credentials from environment variables
	// (you could also use a config file)
	host := getEnv("DB_HOST", "localhost")
	port := getEnv("DB_PORT", "5432")
	user := getEnv("DB_USER", "postgres")
	password := getEnv("DB_PASSWORD", "yahyasd56")
	dbname := getEnv("DB_NAME", "testdb")
	sslmode := getEnv("DB_SSLMODE", "disable")

	// Create DSN (Data Source Name)
	dsn := fmt.Sprintf("host=%s port=%s user=%s password=%s dbname=%s sslmode=%s",
		host, port, user, password, dbname, sslmode)

	// Connect to the database
	db, err := gorm.Open(postgres.Open(dsn), &gorm.Config{})
	if err != nil {
		return nil, fmt.Errorf("failed to connect to database: %w", err)
	}

	// Return the connection
	return &DBConnection{DB: db}, nil
}

// AutoMigrate automatically migrates the schema
func (conn *DBConnection) AutoMigrate() error {
	return conn.DB.AutoMigrate(&User{}, &Product{})
}

// Close closes the database connection
func (conn *DBConnection) Close() error {
	sqlDB, err := conn.DB.DB()
	if err != nil {
		return err
	}
	return sqlDB.Close()
}

// Helper function to get environment variables with default values
func getEnv(key, defaultValue string) string {
	value := os.Getenv(key)
	if value == "" {
		return defaultValue
	}
	return value
}

func main() {
	// Create database connection
	conn, err := NewDBConnection()
	if err != nil {
		log.Fatalf("Failed to connect to database: %v", err)
	}

	// Automigrate schema
	if err := conn.AutoMigrate(); err != nil {
		log.Fatalf("Failed to automigrate schema: %v", err)
	}

	// Example: Create a new user
	user := User{
		Name:  "John Doe",
		Email: "john@exahgtmmmple.com",
	}

	// Insert the user into the database
	result := conn.DB.Create(&user)
	if result.Error != nil {
		log.Fatalf("Failed to create user: %v", result.Error)
	}
	fmt.Printf("Created user with ID: %d\n", user.ID)

	// Example: Query users
	var users []User
	if err := conn.DB.Find(&users).Error; err != nil {
		log.Fatalf("Failed to query users: %v", err)
	}
	fmt.Printf("Found %d users\n", len(users))
	for _, u := range users {
		fmt.Printf("User ID: %d, Name: %s, Email: %s\n", u.ID, u.Name, u.Email)
	}
}
