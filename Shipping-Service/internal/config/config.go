package config

import (
	"log"
	"os"
	"strconv"
	"strings"

	"github.com/joho/godotenv"
)

// Config holds all configuration
type Config struct {
	ServerPort  string
	DBHost      string
	DBPort      string
	DBUser      string
	DBPassword  string
	DBName      string
	Environment string

	// Eureka configuration
	EurekaURL string
	HostName  string
	IPAddress string
	AppName   string

	// Kafka configuration
	KafkaBrokers       []string
	KafkaConsumerGroup string
}

// LoadConfig loads configuration from environment
func LoadConfig() *Config {
	// Load .env file if it exists
	err := godotenv.Load()
	if err != nil {
		log.Println("No .env file found or error loading it")
	}

	// Get hostname for registration
	hostname, err := os.Hostname()
	if err != nil {
		hostname = "unknown"
	}

	// Parse Kafka brokers from comma-separated list
	kafkaBrokersStr := getEnv("KAFKA_BROKERS", "localhost:9092")
	kafkaBrokers := strings.Split(kafkaBrokersStr, ",")

	return &Config{
		ServerPort:  getEnv("SERVER_PORT", "8082"),
		DBHost:      getEnv("DB_HOST", "localhost"),
		DBPort:      getEnv("DB_PORT", "5432"),
		DBUser:      getEnv("DB_USER", "postgres"),
		DBPassword:  getEnv("DB_PASSWORD", "zakaria"),
		DBName:      getEnv("DB_NAME", "shipping_service"),
		Environment: getEnv("ENVIRONMENT", "development"),

		// Eureka configuration
		EurekaURL: getEnv("EUREKA_URL", "http://localhost:8761/eureka"),
		HostName:  getEnv("HOST_NAME", hostname),
		IPAddress: getEnv("SERVICE_IP", getOutboundIP()),
		AppName:   getEnv("APP_NAME", "SHIPPING-SERVICE"),

		// Kafka configuration
		KafkaBrokers:       kafkaBrokers,
		KafkaConsumerGroup: getEnv("KAFKA_CONSUMER_GROUP", "shipping-service-group"),
	}
}

func getEnv(key, defaultValue string) string {
	if value, exists := os.LookupEnv(key); exists {
		return value
	}
	return defaultValue
}

func getEnvAsBool(key string, defaultValue bool) bool {
	if value, exists := os.LookupEnv(key); exists {
		val, err := strconv.ParseBool(value)
		if err != nil {
			return defaultValue
		}
		return val
	}
	return defaultValue
}

// getOutboundIP gets the preferred outbound IP of this machine
func getOutboundIP() string {
	// In a real implementation, you would determine the actual IP
	// For now, we'll use a placeholder approach
	return "127.0.0.1" // Default to localhost
}
