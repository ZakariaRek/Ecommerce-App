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

	"github.com/ZakariaRek/Ecommerce-App/Shipping-Service/internal/config"
	"github.com/ZakariaRek/Ecommerce-App/Shipping-Service/internal/database"
	"github.com/ZakariaRek/Ecommerce-App/Shipping-Service/internal/events"
	"github.com/ZakariaRek/Ecommerce-App/Shipping-Service/internal/handler"
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

// Constants for Kafka entity types
const (
	EntityShipping = "shipping"
	EntityTracking = "tracking"
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

	// Create Kafka producer
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

	// Initialize handlers
	shippingHandler := handler.NewShippingHandler(shippingService)

	// Create router
	router := mux.NewRouter()

	// Register routes
	router.HandleFunc("/api/shipping", shippingHandler.CreateShipping).Methods("POST")
	router.HandleFunc("/api/shipping/{id}", shippingHandler.GetShipping).Methods("GET")
	router.HandleFunc("/api/shipping/order/{order_id}", shippingHandler.GetShippingByOrder).Methods("GET")
	router.HandleFunc("/api/shipping", shippingHandler.GetAllShippings).Methods("GET")
	router.HandleFunc("/api/shipping/{id}", shippingHandler.UpdateShipping).Methods("PUT")
	router.HandleFunc("/api/shipping/{id}/status", shippingHandler.UpdateStatus).Methods("PATCH")
	router.HandleFunc("/api/shipping/{id}/track", shippingHandler.TrackOrder).Methods("GET")

	// Health check endpoint for Eureka
	router.HandleFunc("/health", func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
		w.Write([]byte("OK"))
	})

	// Configure server
	srv := &http.Server{
		Addr:         ":" + cfg.ServerPort,
		Handler:      router,
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

// connectToEureka registers the service with Eureka server
func connectToEureka(cfg *config.Config) (*fargo.EurekaConnection, error) {
	// Create a new Eureka connection
	eurekaConn := fargo.NewConn(cfg.EurekaURL)

	// Convert port string to integer
	portInt, err := strconv.Atoi(cfg.ServerPort)
	if err != nil {
		portInt = 8082 // Default port if conversion fails
	}

	// Create Eureka instance info
	instance := &fargo.Instance{
		HostName:         cfg.HostName,
		Port:             portInt,
		App:              cfg.AppName,
		IPAddr:           cfg.IPAddress,
		VipAddress:       "shipping-service",
		SecureVipAddress: "shipping-service",
		DataCenterInfo:   fargo.DataCenterInfo{Name: fargo.MyOwn},
		Status:           fargo.UP,
		HomePageUrl:      fmt.Sprintf("http://%s:%s/", cfg.HostName, cfg.ServerPort),
		StatusPageUrl:    fmt.Sprintf("http://%s:%s/health", cfg.HostName, cfg.ServerPort),
		HealthCheckUrl:   fmt.Sprintf("http://%s:%s/health", cfg.HostName, cfg.ServerPort),
	}

	// Try to register with Eureka
	err = eurekaConn.RegisterInstance(instance)
	if err != nil {
		return nil, fmt.Errorf("failed to register with Eureka: %v", err)
	}

	return &eurekaConn, nil
}

// startHeartbeat sends heartbeats to Eureka to keep the registration active
func startHeartbeat(eurekaConn *fargo.EurekaConnection, cfg *config.Config) {
	ticker := time.NewTicker(30 * time.Second)
	defer ticker.Stop()

	instance := &fargo.Instance{
		HostName: cfg.HostName,
		App:      cfg.AppName,
	}

	for {
		<-ticker.C
		err := eurekaConn.HeartBeatInstance(instance)
		if err != nil {
			log.Printf("Failed to send heartbeat to Eureka: %v", err)

			// Try to re-register if heartbeat fails
			portInt, convErr := strconv.Atoi(cfg.ServerPort)
			if convErr != nil {
				portInt = 8082 // Default port if conversion fails
			}

			newInstance := &fargo.Instance{
				HostName:         cfg.HostName,
				Port:             portInt,
				App:              cfg.AppName,
				IPAddr:           cfg.IPAddress,
				VipAddress:       "shipping-service",
				SecureVipAddress: "shipping-service",
				DataCenterInfo:   fargo.DataCenterInfo{Name: fargo.MyOwn},
				Status:           fargo.UP,
				HomePageUrl:      fmt.Sprintf("http://%s:%s/", cfg.HostName, cfg.ServerPort),
				StatusPageUrl:    fmt.Sprintf("http://%s:%s/health", cfg.HostName, cfg.ServerPort),
				HealthCheckUrl:   fmt.Sprintf("http://%s:%s/health", cfg.HostName, cfg.ServerPort),
			}

			regErr := eurekaConn.RegisterInstance(newInstance)
			if regErr != nil {
				log.Printf("Failed to re-register with Eureka: %v", regErr)
			} else {
				log.Printf("Successfully re-registered with Eureka")
				// Update our reference
				instance = newInstance
			}
		}
	}
}

// deregisterFromEureka removes the service from Eureka registry
func deregisterFromEureka(eurekaConn *fargo.EurekaConnection, cfg *config.Config) error {
	instance := &fargo.Instance{
		HostName: cfg.HostName,
		App:      cfg.AppName,
	}

	err := eurekaConn.DeregisterInstance(instance)
	if err != nil {
		return fmt.Errorf("failed to deregister from Eureka: %v", err)
	}

	return nil
}
