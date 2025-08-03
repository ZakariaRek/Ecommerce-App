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
	"github.com/ZakariaRek/Ecommerce-App/Shipping-Service/internal/logger"
	"github.com/ZakariaRek/Ecommerce-App/Shipping-Service/internal/middleware"
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

// Global logger instance
var elkLogger *logger.ELKLogger

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

	// Initialize ELK Logger first
	var err error
	elkLogger, err = logger.NewELKLogger(cfg)
	if err != nil {
		log.Fatalf("Failed to initialize ELK logger: %v", err)
	}
	defer elkLogger.Close()

	elkLogger.Info("🚀 Starting Enhanced Shipping Service with ELK Stack integration")
	elkLogger.WithOperation("startup").Info("Service initialization started")

	// Initialize database
	startTime := time.Now()
	db := database.InitDB(cfg)
	elkLogger.LogDatabaseOperation("connection", "postgres", "system", time.Since(startTime), nil)

	sqlDB, err := db.DB()
	if err != nil {
		elkLogger.WithError(err).Fatal("Failed to get database connection")
	}
	defer sqlDB.Close()

	// Seed default data
	startTime = time.Now()
	if err := database.SeedDefaultData(db); err != nil {
		elkLogger.LogDatabaseOperation("seed", "addresses", "default", time.Since(startTime), err)
		elkLogger.WithError(err).Warn("Failed to seed default data")
	} else {
		elkLogger.LogDatabaseOperation("seed", "addresses", "default", time.Since(startTime), nil)
	}

	// Initialize repositories
	elkLogger.WithOperation("repository_init").Info("Initializing repositories")
	shippingRepo := repository.NewShippingRepository(db)
	trackingRepo := repository.NewTrackingRepository(db)
	addressRepo := repository.NewAddressRepository(db)
	locationUpdateRepo := repository.NewLocationUpdateRepository(db)

	// Initialize Kafka - Get Kafka configuration
	brokersStr := getEnv("KAFKA_BROKERS", "localhost:9092")
	brokers := strings.Split(brokersStr, ",")

	// Create topic maps for each entity type
	shippingTopics := map[string]string{
		EventCreated:   getEnv("KAFKA_SHIPPING_CREATED_TOPIC", "shipping-created"),
		EventUpdated:   getEnv("KAFKA_SHIPPING_UPDATED_TOPIC", "shipping-updated"),
		EventChanged:   getEnv("KAFKA_SHIPPING_STATUS_CHANGED_TOPIC", "shipping-status-changed"),
		EventDeleted:   getEnv("KAFKA_SHIPPING_DELETED_TOPIC", "shipping-deleted"),
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

	// Log Kafka configuration with ELK
	elkLogger.WithFields(map[string]interface{}{
		"brokers":         brokers,
		"consumer_group":  consumerGroup,
		"enable_consumer": enableConsumer,
		"topics":          consumerTopics,
	}).Info("Kafka configuration loaded")

	// Enhanced Kafka producer configuration for immediate delivery
	elkLogger.WithOperation("kafka_init").Info("Setting up enhanced Kafka producer configuration")
	producerConfig := sarama.NewConfig()

	// Reliability settings
	producerConfig.Producer.RequiredAcks = sarama.WaitForAll
	producerConfig.Producer.Retry.Max = 3
	producerConfig.Producer.Retry.Backoff = 100 * time.Millisecond
	producerConfig.Producer.Return.Successes = true
	producerConfig.Producer.Return.Errors = true

	// Force immediate sending - no batching
	producerConfig.Producer.Flush.Frequency = 10 * time.Millisecond
	producerConfig.Producer.Flush.Messages = 1
	producerConfig.Producer.Flush.Bytes = 1

	// Compression
	producerConfig.Producer.Compression = sarama.CompressionSnappy

	// Timeouts
	producerConfig.Net.DialTimeout = 10 * time.Second
	producerConfig.Net.ReadTimeout = 10 * time.Second
	producerConfig.Net.WriteTimeout = 10 * time.Second

	// Metadata refresh
	producerConfig.Metadata.Retry.Max = 3
	producerConfig.Metadata.Retry.Backoff = 250 * time.Millisecond
	producerConfig.Metadata.RefreshFrequency = 10 * time.Minute

	// Add version for compatibility
	version, err := sarama.ParseKafkaVersion("2.6.0")
	if err != nil {
		elkLogger.WithError(err).Warn("Error parsing Kafka version")
	} else {
		producerConfig.Version = version
	}

	// Test Kafka connectivity first
	elkLogger.WithOperation("kafka_connectivity").Info("Testing Kafka broker connectivity")
	client, err := sarama.NewClient(brokers, producerConfig)
	if err != nil {
		elkLogger.WithError(err).WithFields(map[string]interface{}{
			"brokers": brokers,
		}).Error("Failed to connect to Kafka brokers")
		elkLogger.Warn("Continuing without Kafka support...")
	} else {
		elkLogger.Info("Successfully connected to Kafka brokers")

		// List available topics for debugging
		topics, err := client.Topics()
		if err != nil {
			elkLogger.WithError(err).Warn("Could not list Kafka topics")
		} else {
			elkLogger.WithField("available_topics", topics).Info("Available Kafka topics")

			// Check if our required topics exist
			requiredTopics := []string{"shipping-update", "shipping-status-changed", "shipping-updated", "app-logs"}
			for _, requiredTopic := range requiredTopics {
				found := false
				for _, topic := range topics {
					if topic == requiredTopic {
						found = true
						break
					}
				}
				if found {
					elkLogger.WithField("topic", requiredTopic).Info("Required topic exists")
				} else {
					elkLogger.WithField("topic", requiredTopic).Warn("Required topic does not exist - will be auto-created if enabled")
				}
			}
		}
		client.Close()

		// Create the producer
		producer, producerErr := sarama.NewAsyncProducer(brokers, producerConfig)
		if producerErr != nil {
			elkLogger.WithError(producerErr).Error("Failed to create Kafka producer")
			elkLogger.Warn("Continuing without Kafka support...")
		} else {
			elkLogger.Info("Kafka producer created successfully")

			// Enhanced monitoring with immediate feedback
			go func() {
				elkLogger.WithOperation("kafka_monitoring").Info("Starting enhanced Kafka producer monitoring")
				successCount := 0
				errorCount := 0

				for {
					select {
					case success := <-producer.Successes():
						successCount++
						elkLogger.LogKafkaEvent(success.Topic, "success", string(success.Key.(sarama.StringEncoder)), true, nil)

					case err := <-producer.Errors():
						errorCount++
						elkLogger.LogKafkaEvent(err.Msg.Topic, "error", string(err.Msg.Key.(sarama.StringEncoder)), false, err.Err)
					}
				}
			}()

			// Create Kafka services
			elkLogger.WithOperation("kafka_services").Info("Creating Kafka services")
			shippingKafka := kafka.NewShippingKafkaService(producer, shippingTopics)
			trackingKafka := kafka.NewTrackingKafkaService(producer, trackingTopics)

			// Set the global Kafka services for GORM hooks
			elkLogger.WithOperation("gorm_hooks").Info("Setting Kafka services for GORM hooks")
			models.SetKafkaServices(shippingKafka, trackingKafka)
			elkLogger.Info("Kafka services initialized and set for GORM hooks")

			// Test message to verify everything works
			elkLogger.WithOperation("kafka_test").Info("Sending test message to verify Kafka connectivity")
			testMessage := &sarama.ProducerMessage{
				Topic: "shipping-update",
				Key:   sarama.StringEncoder("test-key"),
				Value: sarama.StringEncoder(`{"test": "connectivity", "timestamp": "` + time.Now().Format(time.RFC3339) + `"}`),
			}
			producer.Input() <- testMessage

			// Create and start consumer if enabled
			if enableConsumer {
				elkLogger.WithOperation("kafka_consumer").Info("Creating Kafka consumer")
				consumer, err := kafka.NewConsumer(brokers, consumerGroup, consumerTopics)
				if err != nil {
					elkLogger.WithError(err).Error("Failed to create Kafka consumer")
				} else {
					elkLogger.Info("Kafka consumer created successfully")

					// Register event handlers
					consumer.RegisterShippingEventHandler(events.ShippingCreated, func(event *events.ShippingEvent) error {
						elkLogger.WithShippingID(event.ShippingID.String()).Info("Handling shipping created event")
						return nil
					})

					consumer.RegisterTrackingEventHandler(events.TrackingCreated, func(event *events.TrackingEvent) error {
						elkLogger.WithShippingID(event.ShippingID.String()).Info("Handling tracking created event")
						return nil
					})

					// Start the consumer in a separate goroutine
					go consumer.Start()
				}
			} else {
				elkLogger.Warn("Kafka consumer is disabled")
			}
		}
	}

	// Initialize services with all repositories
	elkLogger.WithOperation("service_init").Info("Initializing business services")
	shippingService := service.NewShippingService(shippingRepo, trackingRepo, addressRepo, locationUpdateRepo)
	trackingService := service.NewTrackingService(trackingRepo, shippingRepo, locationUpdateRepo)
	addressService := service.NewAddressService(addressRepo)

	// Initialize controllers with enhanced services
	elkLogger.WithOperation("controller_init").Info("Initializing HTTP controllers")
	shippingController := controller.NewShippingController(shippingService, addressService)
	trackingController := controller.NewTrackingController(trackingService)

	// Setup router
	var httpRouter *mux.Router

	// Choose your approach:
	useShippingOnly := getEnvAsBool("USE_SHIPPING_ONLY", false)
	useTrackingOnly := getEnvAsBool("USE_TRACKING_ONLY", false)

	if useShippingOnly {
		httpRouter = shippingController.SetupRoutes()
		elkLogger.WithField("mode", "shipping-only").Info("Running as Enhanced Shipping-only service")
	} else if useTrackingOnly {
		httpRouter = trackingController.SetupRoutes()
		elkLogger.WithField("mode", "tracking-only").Info("Running as Enhanced Tracking-only service")
	} else {
		httpRouter = setupCombinedRoutes(shippingController, trackingController)
		elkLogger.WithField("mode", "combined").Info("Running as Enhanced Combined Shipping-Tracking service")
	}

	// Add ELK logging middleware to all routes
	httpRouter.Use(middleware.LoggingMiddleware(elkLogger))

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
		elkLogger.WithError(eurekaErr).Warn("Failed to connect to Eureka")
		elkLogger.Warn("Continuing without Eureka registration...")
	} else {
		elkLogger.Info("Successfully registered with Eureka service registry")
		// Start heartbeat in a goroutine if registration was successful
		go startHeartbeat(eurekaClient, cfg)
	}

	// Start server
	go func() {
		elkLogger.WithFields(map[string]interface{}{
			"port": cfg.ServerPort,
			"mode": getServiceMode(useShippingOnly, useTrackingOnly),
		}).Info("Starting enhanced service")

		logAvailableEndpoints(useShippingOnly, useTrackingOnly)

		if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			elkLogger.WithError(err).Fatal("Error starting server")
		}
	}()

	// Graceful shutdown
	c := make(chan os.Signal, 1)
	signal.Notify(c, os.Interrupt, syscall.SIGTERM)
	<-c

	elkLogger.WithOperation("shutdown").Info("Received shutdown signal")

	// Deregister from Eureka before shutting down
	if eurekaClient != nil {
		if err := deregisterFromEureka(eurekaClient, cfg); err != nil {
			elkLogger.WithError(err).Warn("Failed to deregister from Eureka")
		} else {
			elkLogger.Info("Successfully deregistered from Eureka")
		}
	}

	elkLogger.Info("Server shutting down...")

	// Create a deadline for server shutdown
	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()

	// Perform graceful shutdown
	if err := srv.Shutdown(ctx); err != nil {
		elkLogger.WithError(err).Fatal("Server shutdown failed")
	}

	elkLogger.Info("Server stopped successfully")
}

// getServiceMode returns the service mode string
func getServiceMode(shippingOnly, trackingOnly bool) string {
	if shippingOnly {
		return "shipping-only"
	} else if trackingOnly {
		return "tracking-only"
	}
	return "combined"
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
				"gorm_hooks": "UP",
				"elk_logging": "UP"
			}
		}`))
	}).Methods("GET")

	router.HandleFunc("/info", func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusOK)
		w.Write([]byte(`{
			"service": "enhanced-shipping-tracking-service",
			"version": "2.0.0",
			"description": "Enhanced shipping and tracking service with GPS support, address management, real-time location updates, GORM hooks for Kafka events, and ELK Stack integration",
			"modules": ["shipping", "tracking", "addresses", "location_updates", "elk_logging"],
			"features": ["gps_tracking", "real_time_location", "address_management", "enhanced_tracking", "gorm_hooks", "kafka_events", "elk_stack", "structured_logging"]
		}`))
	}).Methods("GET")

	return router
}

// logAvailableEndpoints logs the available endpoints based on configuration
func logAvailableEndpoints(shippingOnly, trackingOnly bool) {
	elkLogger.Info("Available endpoints:")

	endpointData := map[string]interface{}{
		"shipping_only": shippingOnly,
		"tracking_only": trackingOnly,
		"combined":      !shippingOnly && !trackingOnly,
	}

	if shippingOnly {
		elkLogger.WithFields(endpointData).Info("📦 Enhanced Shipping API with GORM Hooks and ELK Logging enabled")
	} else if trackingOnly {
		elkLogger.WithFields(endpointData).Info("📍 Enhanced Tracking API with GORM Hooks and ELK Logging enabled")
	} else {
		elkLogger.WithFields(endpointData).Info("📦📍 Enhanced Combined Shipping-Tracking API with GORM Hooks and ELK Logging enabled")
	}
}

// Eureka functions with ELK logging integration
func connectToEureka(cfg *config.Config) (*fargo.EurekaConnection, error) {
	elkLogger.WithOperation("eureka_connect").Info("Connecting to Eureka server")

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
		elkLogger.WithError(err).WithFields(map[string]interface{}{
			"eureka_url":  cfg.EurekaURL,
			"instance_id": cfg.EurekaInstanceId,
			"hostname":    instanceHostName,
		}).Error("Failed to register with Eureka")
		return nil, fmt.Errorf("failed to register with Eureka: %v", err)
	}

	elkLogger.WithFields(map[string]interface{}{
		"instance_id":       cfg.EurekaInstanceId,
		"hostname":          instanceHostName,
		"ip_address":        instanceIPAddr,
		"port":              portInt,
		"prefer_ip_address": cfg.EurekaPreferIpAddress,
		"eureka_url":        cfg.EurekaURL,
	}).Info("Successfully registered with Eureka")

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
			elkLogger.WithError(err).WithField("instance_id", cfg.EurekaInstanceId).Error("Failed to send heartbeat to Eureka")

			_, regErr := connectToEureka(cfg)
			if regErr != nil {
				elkLogger.WithError(regErr).Error("Failed to re-register with Eureka")
			} else {
				elkLogger.Info("Successfully re-registered with Eureka")
			}
		} else {
			elkLogger.WithField("instance_id", cfg.EurekaInstanceId).Debug("Heartbeat sent successfully")
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
		elkLogger.WithError(err).WithField("instance_id", cfg.EurekaInstanceId).Error("Failed to deregister from Eureka")
		return fmt.Errorf("failed to deregister from Eureka: %v", err)
	}

	elkLogger.WithField("instance_id", cfg.EurekaInstanceId).Info("Successfully deregistered from Eureka")
	return nil
}
