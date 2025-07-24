// Payment-Service/internal/config/config.go
package config

import (
	"fmt"
	"log"
	"net"
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
	EurekaURL              string
	HostName               string
	IPAddress              string
	AppName                string
	EurekaPreferIpAddress  bool
	EurekaInstanceId       string
	EurekaInstanceHostname string

	// Kafka configuration
	KafkaBrokers      []string
	KafkaInvoiceTopic string

	// Config Server settings
	ConfigServerURL string
	ConfigProfile   string
	ConfigLabel     string
}

// LoadConfig loads configuration from Config Server first, then environment variables as fallback
func LoadConfig() *Config {
	// Load .env file if it exists
	err := godotenv.Load()
	if err != nil {
		log.Println("No .env file found or error loading it")
	}

	// Get hostname for registration
	hostname, err := os.Hostname()
	if err != nil {
		hostname = "localhost"
	}

	// Initialize config with defaults and environment variables
	config := &Config{
		ServerPort:  getEnv("SERVER_PORT", "8080"),
		DBHost:      getEnv("DB_HOST", "localhost"),
		DBPort:      getEnv("DB_PORT", "5432"),
		DBUser:      getEnv("DB_USER", "postgres"),
		DBPassword:  getEnv("DB_PASSWORD", "yahyasd56"),
		DBName:      getEnv("DB_NAME", "payment_system"),
		Environment: getEnv("ENVIRONMENT", "development"),

		// Eureka configuration - Fixed to match Spring Cloud naming conventions
		EurekaURL:              getEnv("EUREKA_URL", "http://localhost:8761/eureka"),
		HostName:               getEnv("HOST_NAME", hostname),
		IPAddress:              getEnv("SERVICE_IP", getOutboundIP()),
		AppName:                getEnv("APP_NAME", "PAYMENT-SERVICE"), // This should match the Order Service expectation
		EurekaPreferIpAddress:  getEnvAsBool("EUREKA_PREFER_IP_ADDRESS", true),
		EurekaInstanceHostname: getEnv("EUREKA_INSTANCE_HOSTNAME", "localhost"),

		// Config Server settings
		ConfigServerURL: getEnv("CONFIG_SERVER_URL", "http://localhost:8888"),
		ConfigProfile:   getEnv("CONFIG_PROFILE", "development"),
		ConfigLabel:     getEnv("CONFIG_LABEL", "main"),

		// Kafka configuration defaults
		KafkaInvoiceTopic: getEnv("KAFKA_INVOICE_TOPIC", "invoices"),
	}

	// Set Eureka instance ID to match Spring Boot pattern
	config.EurekaInstanceId = getEnv("EUREKA_INSTANCE_ID",
		fmt.Sprintf("%s:%s", strings.ToLower(config.AppName), config.ServerPort))

	// Parse Kafka brokers from comma-separated list
	kafkaBrokersStr := getEnv("KAFKA_BROKERS", "localhost:9092")
	config.KafkaBrokers = strings.Split(kafkaBrokersStr, ",")

	// Try to fetch configuration from Config Server
	if config.ConfigServerURL != "" {
		log.Printf("Attempting to fetch configuration from Config Server: %s", config.ConfigServerURL)

		configClient := NewConfigClient(
			config.ConfigServerURL,
			strings.ToLower(config.AppName), // Convert to lowercase for config server
			config.ConfigProfile,
			config.ConfigLabel,
		)

		serverConfig, err := configClient.FetchConfig()
		if err != nil {
			log.Printf("Warning: Failed to fetch configuration from Config Server: %v", err)
			log.Println("Falling back to environment variables and defaults")
		} else {
			log.Println("Successfully fetched configuration from Config Server")
			config.mergeServerConfig(serverConfig)
		}
	}

	// Log final configuration for debugging
	log.Printf("Payment Service Configuration:")
	log.Printf("  Server Port: %s", config.ServerPort)
	log.Printf("  App Name: %s", config.AppName)
	log.Printf("  Eureka URL: %s", config.EurekaURL)
	log.Printf("  Instance ID: %s", config.EurekaInstanceId)
	log.Printf("  IP Address: %s", config.IPAddress)
	log.Printf("  Hostname: %s", config.HostName)

	return config
}

// mergeServerConfig merges configuration from Config Server with existing config
func (c *Config) mergeServerConfig(serverConfig *ConfigServerResponse) {
	// Server configuration
	c.ServerPort = serverConfig.GetStringProperty("server.port", c.ServerPort)

	// Database configuration
	c.DBHost = serverConfig.GetStringProperty("datasource.host", c.DBHost)
	c.DBPort = serverConfig.GetStringProperty("datasource.port", c.DBPort)
	c.DBUser = serverConfig.GetStringProperty("datasource.username", c.DBUser)
	c.DBPassword = serverConfig.GetStringProperty("datasource.password", c.DBPassword)
	c.DBName = serverConfig.GetStringProperty("datasource.database", c.DBName)

	// Application configuration
	c.Environment = serverConfig.GetStringProperty("app.environment", c.Environment)
	c.AppName = serverConfig.GetStringProperty("app.name", c.AppName)

	// Eureka configuration
	c.EurekaURL = serverConfig.GetStringProperty("eureka.client.service-url.defaultZone", c.EurekaURL)
	c.EurekaPreferIpAddress = serverConfig.GetBoolProperty("eureka.instance.preferIpAddress", c.EurekaPreferIpAddress)
	c.EurekaInstanceHostname = serverConfig.GetStringProperty("eureka.instance.hostname", c.EurekaInstanceHostname)

	// Kafka configuration
	if kafkaBrokers := serverConfig.GetStringProperty("kafka.brokers", ""); kafkaBrokers != "" {
		c.KafkaBrokers = strings.Split(kafkaBrokers, ",")
	}
	if instanceId := serverConfig.GetStringProperty("eureka.instance.instanceId", ""); instanceId != "" {
		c.EurekaInstanceId = instanceId
	}
	c.KafkaInvoiceTopic = serverConfig.GetStringProperty("kafka.topics.invoice", c.KafkaInvoiceTopic)

	log.Printf("Configuration merged from Config Server for application: %s, profile: %s",
		serverConfig.Name, strings.Join(serverConfig.Profiles, ","))
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
	conn, err := net.Dial("udp", "8.8.8.8:80")
	if err != nil {
		log.Printf("Failed to get outbound IP: %v", err)
		return "127.0.0.1"
	}
	defer conn.Close()

	localAddr := conn.LocalAddr().(*net.UDPAddr)
	return localAddr.IP.String()
}
