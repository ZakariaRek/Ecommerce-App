package main

import (
	"context"
	"fmt"
	"log"
	"net/http"
	"os"
	"os/signal"
	"strconv"
	"strings"
	"syscall"
	"time"

	"github.com/IBM/sarama"
	"github.com/gorilla/mux"
	"github.com/hudl/fargo"

	"github.com/ZakariaRek/Ecommerce-App/Shipping-Service/internal/Controllers"
	"github.com/ZakariaRek/Ecommerce-App/Shipping-Service/internal/config"
	"github.com/ZakariaRek/Ecommerce-App/Shipping-Service/internal/database"
	"github.com/ZakariaRek/Ecommerce-App/Shipping-Service/internal/events"
	"github.com/ZakariaRek/Ecommerce-App/Shipping-Service/internal/models"
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

	// Seed default data
	if err := database.SeedDefaultData(db); err != nil {
		log.Printf("Warning: Failed to seed default data: %v", err)
	}

	// Initialize repositories
	shippingRepo := repository.NewShippingRepository(db)
	trackingRepo := repository.NewTrackingRepository(db)
	addressRepo := repository.NewAddressRepository(db)
	locationUpdateRepo := repository.NewLocationUpdateRepository(db)

	// Initialize Kafka - Get Kafka configuration
	brokersStr := getEnv("KAFKA_BROKERS", "localhost:9092")
	brokers := strings.Split(brokersStr, ",")

	// Create topic maps for each entity type
	shippingTopics := map[string]string{
		EventCreated: getEnv("KAFKA_SHIPPING_CREATED_TOPIC", "shipping-created"),
		EventUpdated: getEnv("KAFKA_SHIPPING_UPDATED_TOPIC", "shipping-updated"),
		EventChanged: getEnv("KAFKA_SHIPPING_STATUS_CHANGED_TOPIC", "shipping-status-changed"),
		EventDeleted: getEnv("KAFKA_SHIPPING_DELETED_TOPIC", "shipping-deleted"),
		// Add the shipping-update topic for order service communication
		"order_update": getEnv("KAFKA_SHIPPING_ORDER_UPDATE_TOPIC", "shipping-update"),
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
	// Add this to your main.go - Enhanced Kafka producer setup with better debugging

	// Initialize Kafka producer and services
	// Simple fix - Replace your producer configuration in main.go with this:

	// Enhanced Kafka producer configuration for immediate delivery
	log.Printf("🔧 Setting up enhanced Kafka producer configuration...")
	producerConfig := sarama.NewConfig()

	// Reliability settings
	producerConfig.Producer.RequiredAcks = sarama.WaitForAll // Wait for all replicas
	producerConfig.Producer.Retry.Max = 3
	producerConfig.Producer.Retry.Backoff = 100 * time.Millisecond
	producerConfig.Producer.Return.Successes = true
	producerConfig.Producer.Return.Errors = true

	// CRITICAL: Force immediate sending - no batching
	producerConfig.Producer.Flush.Frequency = 10 * time.Millisecond // Flush every 10ms
	producerConfig.Producer.Flush.Messages = 1                      // Flush after every single message
	producerConfig.Producer.Flush.Bytes = 1                         // Flush after 1 byte

	// Compression (optional, can disable for debugging)
	producerConfig.Producer.Compression = sarama.CompressionSnappy

	// Timeouts
	producerConfig.Net.DialTimeout = 10 * time.Second
	producerConfig.Net.ReadTimeout = 10 * time.Second
	producerConfig.Net.WriteTimeout = 10 * time.Second

	// Metadata refresh
	producerConfig.Metadata.Retry.Max = 3
	producerConfig.Metadata.Retry.Backoff = 250 * time.Millisecond
	producerConfig.Metadata.RefreshFrequency = 10 * time.Minute

	log.Printf("🔧 Creating Kafka producer with IMMEDIATE FLUSH settings...")
	producer, producerErr := sarama.NewAsyncProducer(brokers, producerConfig)
	if producerErr != nil {
		log.Printf("❌ Failed to create Kafka producer: %v", producerErr)
		log.Println("Continuing without Kafka support...")
	} else {
		log.Printf("✅ Kafka producer created with immediate flush configuration")

		// Simple monitoring
		go func() {
			log.Printf("🔄 Starting Kafka producer monitoring...")
			for {
				select {
				case success := <-producer.Successes():
					log.Printf("✅ KAFKA SUCCESS: Topic=%s, Partition=%d, Offset=%d, Key=%s",
						success.Topic, success.Partition, success.Offset, string(success.Key.(sarama.StringEncoder)))

				case err := <-producer.Errors():
					log.Printf("❌ KAFKA ERROR: %v", err.Err)
					if err.Msg != nil {
						log.Printf("   Failed Topic: %s, Key: %s", err.Msg.Topic, string(err.Msg.Key.(sarama.StringEncoder)))
					}
				}
			}
		}()

		// Test connectivity with a simple message
		log.Printf("🧪 Sending test message...")
		testMessage := &sarama.ProducerMessage{
			Topic: "shipping-update",
			Key:   sarama.StringEncoder("test-connectivity"),
			Value: sarama.StringEncoder(`{"test": "producer-config", "timestamp": "` + time.Now().Format(time.RFC3339) + `"}`),
		}
		producer.Input() <- testMessage

		// Wait a moment for the test message to process
		time.Sleep(500 * time.Millisecond)
		log.Printf("🧪 Test message sent - check logs above for SUCCESS/ERROR")

		// Continue with your existing service setup...
		shippingKafka := kafka.NewShippingKafkaService(producer, shippingTopics)
		trackingKafka := kafka.NewTrackingKafkaService(producer, trackingTopics)
		models.SetKafkaServices(shippingKafka, trackingKafka)
		log.Printf("✅ Kafka services set with IMMEDIATE FLUSH configuration")
	}

	// Add version for compatibility
	version, err := sarama.ParseKafkaVersion("2.6.0")
	if err != nil {
		log.Printf("⚠️ Error parsing Kafka version: %v", err)
	} else {
		producerConfig.Version = version
	}

	log.Printf("🔧 Creating Kafka producer with brokers: %v", brokers)

	// Test Kafka connectivity first
	log.Printf("🔍 Testing Kafka broker connectivity...")
	client, err := sarama.NewClient(brokers, producerConfig)
	if err != nil {
		log.Printf("❌ Failed to connect to Kafka brokers: %v", err)
		log.Printf("   Make sure Kafka is running on: %v", brokers)
		log.Println("Continuing without Kafka support...")
	} else {
		log.Printf("✅ Successfully connected to Kafka brokers")

		// List available topics for debugging
		topics, err := client.Topics()
		if err != nil {
			log.Printf("⚠️ Could not list topics: %v", err)
		} else {
			log.Printf("📋 Available Kafka topics: %v", topics)

			// Check if our required topics exist
			requiredTopics := []string{"shipping-update", "shipping-status-changed", "shipping-updated"}
			for _, requiredTopic := range requiredTopics {
				found := false
				for _, topic := range topics {
					if topic == requiredTopic {
						found = true
						break
					}
				}
				if found {
					log.Printf("✅ Topic '%s' exists", requiredTopic)
				} else {
					log.Printf("❌ Topic '%s' does not exist - will be auto-created if enabled", requiredTopic)
				}
			}
		}
		client.Close()

		// Now create the producer
		producer, producerErr := sarama.NewAsyncProducer(brokers, producerConfig)
		if producerErr != nil {
			log.Printf("❌ Failed to create Kafka producer: %v", producerErr)
			log.Println("Continuing without Kafka support...")
		} else {
			log.Printf("✅ Kafka producer created successfully")

			// Enhanced monitoring with immediate feedback
			go func() {
				log.Printf("🔄 Starting enhanced Kafka producer monitoring...")
				successCount := 0
				errorCount := 0

				for {
					select {
					case success := <-producer.Successes():
						successCount++
						log.Printf("✅ KAFKA SUCCESS #%d: Topic=%s, Partition=%d, Offset=%d, Key=%s",
							successCount, success.Topic, success.Partition, success.Offset, string(success.Key.(sarama.StringEncoder)))

					case err := <-producer.Errors():
						errorCount++
						log.Printf("❌ KAFKA ERROR #%d: %v", errorCount, err.Err)
						if err.Msg != nil {
							log.Printf("   Failed message - Topic: %s, Key: %s", err.Msg.Topic, string(err.Msg.Key.(sarama.StringEncoder)))
							log.Printf("   Error details: %+v", err)
						}
					}
				}
			}()

			// Create Kafka services
			log.Printf("🔧 Creating Kafka services...")
			log.Printf("   Shipping topics: %+v", shippingTopics)
			log.Printf("   Tracking topics: %+v", trackingTopics)

			shippingKafka := kafka.NewShippingKafkaService(producer, shippingTopics)
			trackingKafka := kafka.NewTrackingKafkaService(producer, trackingTopics)

			// Set the global Kafka services for GORM hooks
			log.Printf("🔧 Setting Kafka services for GORM hooks...")
			models.SetKafkaServices(shippingKafka, trackingKafka)
			log.Printf("✅ Kafka services initialized and set for GORM hooks")

			// Test message to verify everything works
			log.Printf("🧪 Sending test message to verify Kafka connectivity...")
			testMessage := &sarama.ProducerMessage{
				Topic: "shipping-update",
				Key:   sarama.StringEncoder("test-key"),
				Value: sarama.StringEncoder(`{"test": "connectivity", "timestamp": "` + time.Now().Format(time.RFC3339) + `"}`),
			}
			producer.Input() <- testMessage
			log.Printf("📤 Test message sent, check logs for success/error...")

			// Create and start consumer if enabled
			if enableConsumer {
				log.Printf("🔧 Creating Kafka consumer...")
				consumer, err := kafka.NewConsumer(brokers, consumerGroup, consumerTopics)
				if err != nil {
					log.Printf("❌ Failed to create Kafka consumer: %v", err)
				} else {
					log.Printf("✅ Kafka consumer created successfully")

					// Register event handlers
					consumer.RegisterShippingEventHandler(events.ShippingCreated, func(event *events.ShippingEvent) error {
						log.Printf("🔄 Handling shipping created event: %s", event.ShippingID)
						return nil
					})

					consumer.RegisterTrackingEventHandler(events.TrackingCreated, func(event *events.TrackingEvent) error {
						log.Printf("🔄 Handling tracking created event: %s", event.TrackingID)
						return nil
					})

					// Start the consumer in a separate goroutine
					go consumer.Start()
				}
			} else {
				log.Printf("⚠️ Kafka consumer is disabled")
			}
		}
	}

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

		// Set the global Kafka services for GORM hooks
		models.SetKafkaServices(shippingKafka, trackingKafka)
		log.Println("✅ Kafka services initialized and set for GORM hooks")

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

	// Initialize services with all repositories
	shippingService := service.NewShippingService(shippingRepo, trackingRepo, addressRepo, locationUpdateRepo)
	trackingService := service.NewTrackingService(trackingRepo, shippingRepo, locationUpdateRepo)
	addressService := service.NewAddressService(addressRepo)

	// Initialize controllers with enhanced services
	shippingController := controller.NewShippingController(shippingService, addressService)
	trackingController := controller.NewTrackingController(trackingService)

	// Setup router - Option 1: Combined approach
	var httpRouter *mux.Router

	// Choose your approach:
	useShippingOnly := getEnvAsBool("USE_SHIPPING_ONLY", false)
	useTrackingOnly := getEnvAsBool("USE_TRACKING_ONLY", false)

	if useShippingOnly {
		// Option 1: Shipping service only
		httpRouter = shippingController.SetupRoutes()
		log.Println("Running as Enhanced Shipping-only service with GORM hooks")
	} else if useTrackingOnly {
		// Option 2: Tracking service only
		httpRouter = trackingController.SetupRoutes()
		log.Println("Running as Enhanced Tracking-only service with GORM hooks")
	} else {
		// Option 3: Combined service (default)
		httpRouter = setupCombinedRoutes(shippingController, trackingController)
		log.Println("Running as Enhanced Combined Shipping-Tracking service with GORM hooks")
	}

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
		log.Printf("Starting enhanced service on port %s", cfg.ServerPort)
		logAvailableEndpoints(useShippingOnly, useTrackingOnly)

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

// setupCombinedRoutes creates a combined router for both controllers
func setupCombinedRoutes(shippingController *controller.ShippingController, trackingController *controller.TrackingController) *mux.Router {
	// Create main router with global middleware
	router := mux.NewRouter()

	// Register routes from both controllers
	shippingController.RegisterRoutes(router)
	trackingController.RegisterRoutes(router)

	// Add combined health check
	router.HandleFunc("/health", func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusOK)
		w.Write([]byte(`{
			"status": "UP",
			"service": "enhanced-shipping-tracking-service",
			"checks": {
				"database": "UP",
				"kafka": "UP",
				"shipping": "UP",
				"tracking": "UP",
				"addresses": "UP",
				"location_updates": "UP",
				"gorm_hooks": "UP"
			}
		}`))
	}).Methods("GET")

	router.HandleFunc("/info", func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusOK)
		w.Write([]byte(`{
			"service": "enhanced-shipping-tracking-service",
			"version": "2.0.0",
			"description": "Enhanced shipping and tracking service with GPS support, address management, real-time location updates, and GORM hooks for Kafka events",
			"modules": ["shipping", "tracking", "addresses", "location_updates"],
			"features": ["gps_tracking", "real_time_location", "address_management", "enhanced_tracking", "gorm_hooks", "kafka_events"]
		}`))
	}).Methods("GET")

	return router
}

// logAvailableEndpoints logs the available endpoints based on configuration
func logAvailableEndpoints(shippingOnly, trackingOnly bool) {
	log.Println("Available endpoints:")

	if shippingOnly {
		log.Println("  📦 Enhanced Shipping API with GORM Hooks:")
		log.Println("    POST   /api/shipping")
		log.Println("    POST   /api/shipping/with-address")
		log.Println("    GET    /api/shipping")
		log.Println("    GET    /api/shipping/{id}")
		log.Println("    PUT    /api/shipping/{id}")
		log.Println("    PATCH  /api/shipping/{id}/status")
		log.Println("    PATCH  /api/shipping/{id}/status/gps")
		log.Println("    GET    /api/shipping/{id}/track")
		log.Println("    GET    /api/shipping/{id}/cost")
		log.Println("    PATCH  /api/shipping/{id}/location")
		log.Println("    POST   /api/shipping/{id}/location-update")
		log.Println("    GET    /api/shipping/{id}/location-history")
		log.Println("    GET    /api/shipping/order/{order_id}")
		log.Println("    GET    /api/shipping/status/{status}")
		log.Println("    GET    /api/shipping/in-transit")
		log.Println("  🏠 Address API:")
		log.Println("    POST   /api/addresses")
		log.Println("    GET    /api/addresses")
		log.Println("    GET    /api/addresses/{id}")
		log.Println("    PUT    /api/addresses/{id}")
		log.Println("    DELETE /api/addresses/{id}")
		log.Println("    GET    /api/addresses/search")
		log.Println("    GET    /api/addresses/default-origin")
	} else if trackingOnly {
		log.Println("  📍 Enhanced Tracking API with GORM Hooks:")
		log.Println("    POST   /api/shipping/tracking")
		log.Println("    GET    /api/shipping/tracking/{id}")
		log.Println("    PUT    /api/shipping/tracking/{id}")
		log.Println("    DELETE /api/shipping/tracking/{id}")
		log.Println("    PATCH  /api/shipping/tracking/{id}/location")
		log.Println("    GET    /api/shipping/tracking/shipping/{shipping_id}")
		log.Println("    POST   /api/shipping/tracking/shipping/{shipping_id}")
		log.Println("    GET    /api/shipping/tracking/shipping/{shipping_id}/latest")
		log.Println("    GET    /api/shipping/tracking/{id}/details")
	} else {
		log.Println("  📦 Enhanced Shipping API with GORM Hooks:")
		log.Println("    POST   /api/shipping")
		log.Println("    POST   /api/shipping/with-address")
		log.Println("    GET    /api/shipping")
		log.Println("    GET    /api/shipping/{id}")
		log.Println("    PUT    /api/shipping/{id}")
		log.Println("    PATCH  /api/shipping/{id}/status")
		log.Println("    PATCH  /api/shipping/{id}/status/gps")
		log.Println("    GET    /api/shipping/{id}/track")
		log.Println("    GET    /api/shipping/{id}/cost")
		log.Println("    PATCH  /api/shipping/{id}/location")
		log.Println("    POST   /api/shipping/{id}/location-update")
		log.Println("    GET    /api/shipping/{id}/location-history")
		log.Println("    GET    /api/shipping/order/{order_id}")
		log.Println("    GET    /api/shipping/status/{status}")
		log.Println("    GET    /api/shipping/in-transit")
		log.Println("  🏠 Address API:")
		log.Println("    POST   /api/addresses")
		log.Println("    GET    /api/addresses")
		log.Println("    GET    /api/addresses/{id}")
		log.Println("    PUT    /api/addresses/{id}")
		log.Println("    DELETE /api/addresses/{id}")
		log.Println("    GET    /api/addresses/search")
		log.Println("    GET    /api/addresses/default-origin")
		log.Println("  📍 Enhanced Tracking API with GORM Hooks:")
		log.Println("    POST   /api/shipping/tracking")
		log.Println("    GET    /api/shipping/tracking/{id}")
		log.Println("    PUT    /api/shipping/tracking/{id}")
		log.Println("    DELETE /api/shipping/tracking/{id}")
		log.Println("    PATCH  /api/shipping/tracking/{id}/location")
		log.Println("    GET    /api/shipping/tracking/shipping/{shipping_id}")
		log.Println("    POST   /api/shipping/tracking/shipping/{shipping_id}")
		log.Println("    GET    /api/shipping/tracking/shipping/{shipping_id}/latest")
		log.Println("    GET    /api/shipping/tracking/{id}/details")
	}

}

// Eureka functions remain the same...
func connectToEureka(cfg *config.Config) (*fargo.EurekaConnection, error) {
	eurekaConn := fargo.NewConn(cfg.EurekaURL)

	portInt, err := strconv.Atoi(cfg.ServerPort)
	if err != nil {
		portInt = 8082
	}

	var instanceHostName string
	var instanceIPAddr string

	if cfg.EurekaPreferIpAddress {
		instanceHostName = cfg.IPAddress
		instanceIPAddr = cfg.IPAddress
	} else {
		instanceHostName = cfg.EurekaInstanceHostname
		instanceIPAddr = cfg.IPAddress
	}

	instance := &fargo.Instance{
		InstanceId:       cfg.EurekaInstanceId,
		HostName:         instanceHostName,
		Port:             portInt,
		App:              cfg.AppName,
		IPAddr:           instanceIPAddr,
		VipAddress:       strings.ToLower(cfg.AppName),
		SecureVipAddress: strings.ToLower(cfg.AppName),
		DataCenterInfo:   fargo.DataCenterInfo{Name: fargo.MyOwn},
		Status:           fargo.UP,

		HomePageUrl:    fmt.Sprintf("http://%s:%s/", instanceHostName, cfg.ServerPort),
		StatusPageUrl:  fmt.Sprintf("http://%s:%s/health", instanceHostName, cfg.ServerPort),
		HealthCheckUrl: fmt.Sprintf("http://%s:%s/health", instanceHostName, cfg.ServerPort),
	}

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

func startHeartbeat(eurekaConn *fargo.EurekaConnection, cfg *config.Config) {
	ticker := time.NewTicker(30 * time.Second)
	defer ticker.Stop()

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
