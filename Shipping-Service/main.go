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
	"github.com/gorilla/mux"
	"github.com/hudl/fargo"

	"github.com/ZakariaRek/Ecommerce-App/Shipping-Service/internal/Controllers"
	"github.com/ZakariaRek/Ecommerce-App/Shipping-Service/internal/config"
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
		log.Println("Running as Enhanced Shipping-only service")
	} else if useTrackingOnly {
		// Option 2: Tracking service only
		httpRouter = trackingController.SetupRoutes()
		log.Println("Running as Enhanced Tracking-only service")
	} else {
		// Option 3: Combined service (default)
		httpRouter = setupCombinedRoutes(shippingController, trackingController)
		log.Println("Running as Enhanced Combined Shipping-Tracking service")
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
				"location_updates": "UP"
			}
		}`))
	}).Methods("GET")

	router.HandleFunc("/info", func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusOK)
		w.Write([]byte(`{
			"service": "enhanced-shipping-tracking-service",
			"version": "2.0.0",
			"description": "Enhanced shipping and tracking service with GPS support, address management, and real-time location updates",
			"modules": ["shipping", "tracking", "addresses", "location_updates"],
			"features": ["gps_tracking", "real_time_location", "address_management", "enhanced_tracking"]
		}`))
	}).Methods("GET")

	return router
}

// logAvailableEndpoints logs the available endpoints based on configuration
func logAvailableEndpoints(shippingOnly, trackingOnly bool) {
	log.Println("Available endpoints:")

	if shippingOnly {
		log.Println("  📦 Enhanced Shipping API:")
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
		log.Println("  📍 Enhanced Tracking API:")
		log.Println("    POST   /api/shipping/tracking")
		log.Println("    POST   /api/shipping/tracking/with-gps")
		log.Println("    GET    /api/shipping/tracking/{id}")
		log.Println("    PUT    /api/shipping/tracking/{id}")
		log.Println("    DELETE /api/shipping/tracking/{id}")
		log.Println("    PATCH  /api/shipping/tracking/{id}/location")
		log.Println("    PATCH  /api/shipping/tracking/{id}/location/gps")
		log.Println("    GET    /api/shipping/tracking/{id}/details")
		log.Println("    GET    /api/shipping/tracking/shipping/{id}")
		log.Println("    GET    /api/shipping/tracking/shipping/{id}/gps")
		log.Println("    POST   /api/shipping/tracking/shipping/{id}")
		log.Println("    POST   /api/shipping/tracking/shipping/{id}/gps")
		log.Println("    GET    /api/shipping/tracking/shipping/{id}/latest")
		log.Println("    GET    /api/shipping/tracking/device/{device_id}")
		log.Println("    GET    /api/shipping/tracking/driver/{driver_id}")
		log.Println("    GET    /api/shipping/tracking/location-updates/{shipping_id}")
		log.Println("    GET    /api/shipping/tracking/location-updates/{shipping_id}/latest")
		log.Println("    GET    /api/shipping/tracking/location-updates/{shipping_id}/history")
	} else {
		log.Println("  📦 Enhanced Shipping API:")
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
		log.Println("  📍 Enhanced Tracking API:")
		log.Println("    POST   /api/shipping/tracking")
		log.Println("    POST   /api/shipping/tracking/with-gps")
		log.Println("    GET    /api/shipping/tracking/{id}")
		log.Println("    PUT    /api/shipping/tracking/{id}")
		log.Println("    DELETE /api/shipping/tracking/{id}")
		log.Println("    PATCH  /api/shipping/tracking/{id}/location")
		log.Println("    PATCH  /api/shipping/tracking/{id}/location/gps")
		log.Println("    GET    /api/shipping/tracking/{id}/details")
		log.Println("    GET    /api/shipping/tracking/shipping/{id}")
		log.Println("    GET    /api/shipping/tracking/shipping/{id}/gps")
		log.Println("    POST   /api/shipping/tracking/shipping/{id}")
		log.Println("    POST   /api/shipping/tracking/shipping/{id}/gps")
		log.Println("    GET    /api/shipping/tracking/shipping/{id}/latest")
		log.Println("    GET    /api/shipping/tracking/device/{device_id}")
		log.Println("    GET    /api/shipping/tracking/driver/{driver_id}")
		log.Println("    GET    /api/shipping/tracking/location-updates/{shipping_id}")
		log.Println("    GET    /api/shipping/tracking/location-updates/{shipping_id}/latest")
		log.Println("    GET    /api/shipping/tracking/location-updates/{shipping_id}/history")
	}

	log.Println("  🏥 Health & Monitoring:")
	log.Println("    GET    /health")
	log.Println("    GET    /health/live")
	log.Println("    GET    /health/ready")
	log.Println("    GET    /info")
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
