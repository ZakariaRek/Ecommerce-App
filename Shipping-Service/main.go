package main

import (
	"context"
	"fmt"
	"log"
	"net"
	"net/http"
	"os"
	"os/signal"
	"strconv"
	"strings"
	"syscall"
	"time"

	"github.com/IBM/sarama"
	"github.com/hudl/fargo"

	"github.com/ZakariaRek/Ecommerce-App/Shipping-Service/internal/config"
	"github.com/ZakariaRek/Ecommerce-App/Shipping-Service/internal/controllers"
	"github.com/ZakariaRek/Ecommerce-App/Shipping-Service/internal/database"
	"github.com/ZakariaRek/Ecommerce-App/Shipping-Service/internal/events"
	"github.com/ZakariaRek/Ecommerce-App/Shipping-Service/internal/listeners"
	"github.com/ZakariaRek/Ecommerce-App/Shipping-Service/internal/repository"
	"github.com/ZakariaRek/Ecommerce-App/Shipping-Service/internal/service"
	"github.com/ZakariaRek/Ecommerce-App/Shipping-Service/internal/service/kafka"
)

// Constants for Kafka event types
const (
	EventCreated = "created"
	EventUpdated = "updated"
	EventChanged = "changed"
	EventDeleted = "deleted"
)

// Helper functions for getting environment values
func getEnv(key, defaultValue string) string {
	if value, exists := os.LookupEnv(key); exists {
		return value
	}
	return defaultValue
}

func getEnvAsBool(key string, defaultValue bool) bool {
	strValue := getEnv(key, "")
	if strValue == "" {
		return defaultValue
	}
	return strValue == "true" || strValue == "1" || strValue == "yes"
}

func main() {
	// Load configuration
	cfg := config.LoadConfig()

	// Initialize database
	db := database.InitDB(cfg)
	sqlDB, err := db.DB()
	if err != nil {
		log.Fatalf("Failed to get database connection: %v", err)
	}
	defer sqlDB.Close()

	// Initialize repositories
	shippingRepo := repository.NewShippingRepository(db)
	trackingRepo := repository.NewTrackingRepository(db)

	// Initialize Kafka - Get Kafka configuration
	brokersStr := getEnv("KAFKA_BROKERS", "localhost:9092")
	brokers := strings.Split(brokersStr, ",")

	// Create topic maps for each entity type
	shippingTopics := map[string]string{
		EventCreated: getEnv("KAFKA_SHIPPING_CREATED_TOPIC", "shipping-created"),
		EventUpdated: getEnv("KAFKA_SHIPPING_UPDATED_TOPIC", "shipping-updated"),
		EventChanged: getEnv("KAFKA_SHIPPING_STATUS_CHANGED_TOPIC", "shipping-status-changed"),
		EventDeleted: getEnv("KAFKA_SHIPPING_DELETED_TOPIC", "shipping-deleted"),
	}

	trackingTopics := map[string]string{
		EventCreated: getEnv("KAFKA_TRACKING_CREATED_TOPIC", "tracking-created"),
		EventUpdated: getEnv("KAFKA_TRACKING_UPDATED_TOPIC", "tracking-updated"),
		EventDeleted: getEnv("KAFKA_TRACKING_DELETED_TOPIC", "tracking-deleted"),
	}

	// Collect all topics for consumer
	allTopics := []string{}
	for _, topic := range shippingTopics {
		allTopics = append(allTopics, topic)
	}
	for _, topic := range trackingTopics {
		allTopics = append(allTopics, topic)
	}

	// Get consumer configuration
	enableConsumer := getEnvAsBool("KAFKA_ENABLE_CONSUMER", true)
	consumerGroup := getEnv("KAFKA_CONSUMER_GROUP", "shipping-service-group")
	consumerTopicsStr := getEnv("KAFKA_CONSUMER_TOPICS", strings.Join(allTopics, ","))
	consumerTopics := strings.Split(consumerTopicsStr, ",")

	// Log Kafka configuration
	log.Println("Kafka Configuration:")
	log.Printf("Brokers: %s", strings.Join(brokers, ", "))
	log.Printf("Consumer Group: %s", consumerGroup)
	log.Printf("Enable Consumer: %t", enableConsumer)
	log.Printf("Topics to consume: %s", strings.Join(consumerTopics, ", "))

	// Initialize Kafka producer and services
	producerConfig := sarama.NewConfig()
	producerConfig.Producer.RequiredAcks = sarama.WaitForLocal
	producerConfig.Producer.Compression = sarama.CompressionSnappy
	producerConfig.Producer.Return.Successes = true
	producerConfig.Producer.Return.Errors = true

	producer, producerErr := sarama.NewAsyncProducer(brokers, producerConfig)
	if producerErr != nil {
		log.Printf("Warning: Failed to create Kafka producer: %v", producerErr)
		log.Println("Continuing without Kafka support...")
	} else {
		// Monitor producer errors in background
		go func() {
			for err := range producer.Errors() {
				log.Printf("Failed to produce Kafka message: %v", err)
			}
		}()

		// Create Kafka services
		shippingKafka := kafka.NewShippingKafkaService(producer, shippingTopics)
		trackingKafka := kafka.NewTrackingKafkaService(producer, trackingTopics)

		// Register GORM listeners for publishing events
		shippingListener := listeners.NewShippingListener(shippingKafka)
		shippingListener.RegisterCallbacks(db)

		trackingListener := listeners.NewTrackingListener(trackingKafka)
		trackingListener.RegisterCallbacks(db)

		// Create and start consumer if enabled
		if enableConsumer {
			consumer, err := kafka.NewConsumer(brokers, consumerGroup, consumerTopics)
			if err != nil {
				log.Printf("Warning: Failed to create Kafka consumer: %v", err)
			} else {
				// Register event handlers
				consumer.RegisterShippingEventHandler(events.ShippingCreated, func(event *events.ShippingEvent) error {
					log.Printf("Handling shipping created event: %s", event.ShippingID)
					return nil
				})

				consumer.RegisterTrackingEventHandler(events.TrackingCreated, func(event *events.TrackingEvent) error {
					log.Printf("Handling tracking created event: %s", event.TrackingID)
					return nil
				})

				// Start the consumer in a separate goroutine
				go consumer.Start()
			}
		}
	}

	// Initialize services
	shippingService := service.NewShippingService(shippingRepo, trackingRepo)

	// Initialize controller and setup routes
	shippingController := controller.NewShippingController(shippingService)
	httpRouter := shippingController.SetupRoutes()

	// Configure server
	srv := &http.Server{
		Addr:         ":" + cfg.ServerPort,
		Handler:      httpRouter,
		ReadTimeout:  15 * time.Second,
		WriteTimeout: 15 * time.Second,
		IdleTimeout:  60 * time.Second,
	}

	// Register with Eureka server
	eurekaClient, eurekaErr := connectToEureka(cfg)
	if eurekaErr != nil {
		log.Printf("Warning: Failed to connect to Eureka: %v", eurekaErr)
		log.Println("Continuing without Eureka registration...")
	} else {
		log.Println("Successfully registered with Eureka service registry")
		// Start heartbeat in a goroutine if registration was successful
		go startHeartbeat(eurekaClient, cfg)
	}

	// Start server
	go func() {
		log.Printf("Starting shipping service on port %s", cfg.ServerPort)
		log.Println("Available endpoints:")
		log.Println("  API Endpoints:")
		log.Println("    POST   /api/shipping                    # Create shipping")
		log.Println("    GET    /api/shipping                    # Get all shippings")
		log.Println("    GET    /api/shipping/{id}               # Get shipping by ID")
		log.Println("    PUT    /api/shipping/{id}               # Update shipping")
		log.Println("    PATCH  /api/shipping/{id}/status        # Update status")
		log.Println("    GET    /api/shipping/{id}/track         # Track shipment")
		log.Println("    GET    /api/shipping/{id}/cost          # Get shipping cost")
		log.Println("    GET    /api/shipping/order/{order_id}   # Get by order ID")
		log.Println("  Health & Monitoring:")
		log.Println("    GET    /health                          # Health check")
		log.Println("    GET    /health/live                     # Liveness probe")
		log.Println("    GET    /health/ready                    # Readiness probe")
		log.Println("    GET    /info                            # Service info")

		if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			log.Fatalf("Error starting server: %v", err)
		}
	}()

	// Graceful shutdown
	c := make(chan os.Signal, 1)
	signal.Notify(c, os.Interrupt, syscall.SIGTERM)
	<-c

	// Deregister from Eureka before shutting down
	if eurekaClient != nil {
		if err := deregisterFromEureka(eurekaClient, cfg); err != nil {
			log.Printf("Warning: Failed to deregister from Eureka: %v", err)
		} else {
			log.Println("Successfully deregistered from Eureka")
		}
	}

	log.Println("Server shutting down...")

	// Create a deadline for server shutdown
	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()

	// Perform graceful shutdown
	if err := srv.Shutdown(ctx); err != nil {
		log.Fatalf("Server shutdown failed: %v", err)
	}

	log.Println("Server stopped")
}

// connectToEureka registers the service with Eureka server using Spring Boot style config
func connectToEureka(cfg *config.Config) (*fargo.EurekaConnection, error) {
	// Create a new Eureka connection
	eurekaConn := fargo.NewConn(cfg.EurekaURL)

	// Convert port string to integer
	portInt, err := strconv.Atoi(cfg.ServerPort)
	if err != nil {
		portInt = 8082 // Default port if conversion fails
	}

	// Determine the hostname and IP based on preferIpAddress setting
	var instanceHostName string
	var instanceIPAddr string

	if cfg.EurekaPreferIpAddress {
		// Use IP address as hostname when preferIpAddress is true
		instanceHostName = cfg.IPAddress
		instanceIPAddr = cfg.IPAddress
	} else {
		// Use configured hostname
		instanceHostName = cfg.EurekaInstanceHostname
		instanceIPAddr = cfg.IPAddress
	}

	// Create Eureka instance info with Spring Boot style configuration
	instance := &fargo.Instance{
		InstanceId:       cfg.EurekaInstanceId, // Spring Boot style instanceId
		HostName:         instanceHostName,     // Use IP if preferIpAddress is true
		Port:             portInt,
		App:              cfg.AppName,
		IPAddr:           instanceIPAddr,
		VipAddress:       strings.ToLower(cfg.AppName), // Use app name in lowercase
		SecureVipAddress: strings.ToLower(cfg.AppName),
		DataCenterInfo:   fargo.DataCenterInfo{Name: fargo.MyOwn},
		Status:           fargo.UP,

		// URLs - use the appropriate host based on configuration
		HomePageUrl:    fmt.Sprintf("http://%s:%s/", instanceHostName, cfg.ServerPort),
		StatusPageUrl:  fmt.Sprintf("http://%s:%s/health", instanceHostName, cfg.ServerPort),
		HealthCheckUrl: fmt.Sprintf("http://%s:%s/health", instanceHostName, cfg.ServerPort),
	}

	// Try to register with Eureka
	err = eurekaConn.RegisterInstance(instance)
	if err != nil {
		return nil, fmt.Errorf("failed to register with Eureka: %v", err)
	}

	log.Printf("Successfully registered with Eureka:")
	log.Printf("  Instance ID: %s", cfg.EurekaInstanceId)
	log.Printf("  Hostname: %s", instanceHostName)
	log.Printf("  IP Address: %s", instanceIPAddr)
	log.Printf("  Port: %d", portInt)
	log.Printf("  Prefer IP Address: %t", cfg.EurekaPreferIpAddress)

	return &eurekaConn, nil
}

// startHeartbeat sends heartbeats to Eureka to keep the registration active
func startHeartbeat(eurekaConn *fargo.EurekaConnection, cfg *config.Config) {
	ticker := time.NewTicker(30 * time.Second)
	defer ticker.Stop()

	// Prepare instance for heartbeat (minimal info needed)
	instance := &fargo.Instance{
		InstanceId: cfg.EurekaInstanceId,
		HostName:   cfg.EurekaInstanceHostname,
		App:        cfg.AppName,
	}

	for {
		<-ticker.C
		err := eurekaConn.HeartBeatInstance(instance)
		if err != nil {
			log.Printf("Failed to send heartbeat to Eureka: %v", err)

			// Try to re-register if heartbeat fails
			_, regErr := connectToEureka(cfg)
			if regErr != nil {
				log.Printf("Failed to re-register with Eureka: %v", regErr)
			} else {
				log.Printf("Successfully re-registered with Eureka")
			}
		} else {
			log.Printf("Heartbeat sent successfully for instance: %s", cfg.EurekaInstanceId)
		}
	}
}

// deregisterFromEureka removes the service from Eureka registry
func deregisterFromEureka(eurekaConn *fargo.EurekaConnection, cfg *config.Config) error {
	instance := &fargo.Instance{
		InstanceId: cfg.EurekaInstanceId,
		HostName:   cfg.EurekaInstanceHostname,
		App:        cfg.AppName,
	}

	err := eurekaConn.DeregisterInstance(instance)
	if err != nil {
		return fmt.Errorf("failed to deregister from Eureka: %v", err)
	}

	log.Printf("Successfully deregistered instance: %s", cfg.EurekaInstanceId)
	return nil
}

// Helper function to get actual outbound IP address
func getOutboundIP() string {
	conn, err := net.Dial("udp", "8.8.8.8:80")
	if err != nil {
		log.Printf("Failed to get outbound IP: %v", err)
		return "127.0.0.1" // Fallback to localhost
	}
	defer conn.Close()

	localAddr := conn.LocalAddr().(*net.UDPAddr)
	return localAddr.IP.String()
}
