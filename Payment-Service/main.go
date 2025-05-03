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
	"github.com/ZakariaRek/Ecommerce-App/Payment-Service/internal/config"
	"github.com/ZakariaRek/Ecommerce-App/Payment-Service/internal/database"
	"github.com/ZakariaRek/Ecommerce-App/Payment-Service/internal/events"
	"github.com/ZakariaRek/Ecommerce-App/Payment-Service/internal/handler"
	"github.com/ZakariaRek/Ecommerce-App/Payment-Service/internal/listeners"
	"github.com/ZakariaRek/Ecommerce-App/Payment-Service/internal/repository"
	"github.com/ZakariaRek/Ecommerce-App/Payment-Service/internal/service"
	"github.com/ZakariaRek/Ecommerce-App/Payment-Service/internal/service/kafka"

	"github.com/go-chi/chi/v5"
	"github.com/go-chi/chi/v5/middleware"
	"github.com/hudl/fargo"
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
	EntityPayment     = "payment"
	EntityInvoice     = "invoice"
	EntityTransaction = "transaction"
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

	// Initialize database connection
	db, err := database.InitDB(cfg)
	if err != nil {
		log.Fatalf("Failed to initialize database: %v", err)
	}

	// Initialize repositories
	paymentRepo := repository.NewPaymentRepository(db)
	txRepo := repository.NewPaymentTransactionRepository(db)

	// Initialize services
	paymentService := service.NewPaymentService(paymentRepo, txRepo)

	// Initialize Kafka - Get Kafka configuration
	brokersStr := getEnv("KAFKA_BROKERS", "localhost:9092")
	brokers := strings.Split(brokersStr, ",")

	// Create topic maps for each entity type
	paymentTopics := map[string]string{
		EventCreated: getEnv("KAFKA_PAYMENT_CREATED_TOPIC", "payment-created"),
		EventUpdated: getEnv("KAFKA_PAYMENT_UPDATED_TOPIC", "payment-updated"),
		EventChanged: getEnv("KAFKA_PAYMENT_STATUS_CHANGED_TOPIC", "payment-status-changed"),
		EventDeleted: getEnv("KAFKA_PAYMENT_DELETED_TOPIC", "payment-deleted"),
	}

	invoiceTopics := map[string]string{
		EventCreated: getEnv("KAFKA_INVOICE_CREATED_TOPIC", "invoice-created"),
		EventUpdated: getEnv("KAFKA_INVOICE_UPDATED_TOPIC", "invoice-updated"),
		EventChanged: getEnv("KAFKA_INVOICE_DUE_DATE_CHANGED_TOPIC", "invoice-due-date-changed"),
		EventDeleted: getEnv("KAFKA_INVOICE_DELETED_TOPIC", "invoice-deleted"),
	}

	transactionTopics := map[string]string{
		EventCreated: getEnv("KAFKA_TRANSACTION_CREATED_TOPIC", "transaction-created"),
		EventUpdated: getEnv("KAFKA_TRANSACTION_UPDATED_TOPIC", "transaction-updated"),
		EventChanged: getEnv("KAFKA_TRANSACTION_STATUS_CHANGED_TOPIC", "transaction-status-changed"),
		EventDeleted: getEnv("KAFKA_TRANSACTION_DELETED_TOPIC", "transaction-deleted"),
	}

	// Collect all topics for consumer
	allTopics := []string{}
	for _, topic := range paymentTopics {
		allTopics = append(allTopics, topic)
	}
	for _, topic := range invoiceTopics {
		allTopics = append(allTopics, topic)
	}
	for _, topic := range transactionTopics {
		allTopics = append(allTopics, topic)
	}

	// Get consumer configuration
	enableConsumer := getEnvAsBool("KAFKA_ENABLE_CONSUMER", true)
	consumerGroup := getEnv("KAFKA_CONSUMER_GROUP", "payment-service-group")
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
		paymentKafka := kafka.NewPaymentKafkaService(producer, paymentTopics)
		invoiceKafka := kafka.NewInvoiceKafkaService(producer, invoiceTopics)
		transactionKafka := kafka.NewTransactionKafkaService(producer, transactionTopics)

		// Register GORM listeners for publishing events
		paymentListener := listeners.NewPaymentListener(paymentKafka)
		paymentListener.RegisterCallbacks(db)

		invoiceListener := listeners.NewInvoiceListener(invoiceKafka)
		invoiceListener.RegisterCallbacks(db)

		transactionListener := listeners.NewTransactionListener(transactionKafka)
		transactionListener.RegisterCallbacks(db)

		// Create and start consumer if enabled
		if enableConsumer {
			consumer, err := kafka.NewConsumer(brokers, consumerGroup, consumerTopics)
			if err != nil {
				log.Printf("Warning: Failed to create Kafka consumer: %v", err)
			} else {
				// Register event handlers
				consumer.RegisterPaymentEventHandler(events.PaymentCreated, func(event *events.PaymentEvent) error {
					log.Printf("Handling payment created event: %s", event.PaymentID)
					return nil
				})

				consumer.RegisterInvoiceEventHandler(events.InvoiceCreated, func(event *events.InvoiceEvent) error {
					log.Printf("Handling invoice created event: %s", event.InvoiceID)
					return nil
				})

				consumer.RegisterTransactionEventHandler(events.TransactionCreated, func(event *events.TransactionEvent) error {
					log.Printf("Handling transaction created event: %s", event.TransactionID)
					return nil
				})

				// Start the consumer in a separate goroutine
				go consumer.Start()
			}
		}
	}

	// Initialize handlers
	paymentHandler := handler.NewPaymentHandler(paymentService)

	// Setup Chi router
	r := chi.NewRouter()

	// Middleware
	r.Use(middleware.Logger)
	r.Use(middleware.Recoverer)
	r.Use(middleware.RequestID)
	r.Use(middleware.RealIP)

	// Routes
	r.Route("/api", func(r chi.Router) {
		// Register payment routes
		paymentHandler.RegisterRoutes(r)
	})

	// Health check
	r.Get("/health", func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
		w.Write([]byte("OK"))
	})

	// Create HTTP server
	srv := &http.Server{
		Addr:    ":" + cfg.ServerPort,
		Handler: r,
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

	// Start the server in a goroutine
	go func() {
		fmt.Printf("Starting payment service on port %s...\n", cfg.ServerPort)
		if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			log.Fatalf("Failed to start server: %v", err)
		}
	}()

	// Wait for interrupt signal to gracefully shut down the server
	quit := make(chan os.Signal, 1)
	signal.Notify(quit, syscall.SIGINT, syscall.SIGTERM)
	<-quit

	// Deregister from Eureka before shutting down
	if eurekaClient != nil {
		if err := deregisterFromEureka(eurekaClient, cfg); err != nil {
			log.Printf("Warning: Failed to deregister from Eureka: %v", err)
		} else {
			log.Println("Successfully deregistered from Eureka")
		}
	}

	fmt.Println("Shutting down server...")

	// Create a deadline for server shutdown
	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()

	// Perform graceful shutdown
	if err := srv.Shutdown(ctx); err != nil {
		log.Fatalf("Server shutdown failed: %v", err)
	}

	fmt.Println("Server stopped")
}

// connectToEureka registers the service with Eureka server
func connectToEureka(cfg *config.Config) (*fargo.EurekaConnection, error) {
	// Create a new Eureka connection
	eurekaConn := fargo.NewConn(cfg.EurekaURL)

	// Convert port string to integer
	portInt, err := strconv.Atoi(cfg.ServerPort)
	if err != nil {
		portInt = 8080 // Default port if conversion fails
	}

	// Create Eureka instance info
	instance := &fargo.Instance{
		HostName:         cfg.HostName,
		Port:             portInt,
		App:              cfg.AppName,
		IPAddr:           cfg.IPAddress,
		VipAddress:       "payment-service",
		SecureVipAddress: "payment-service",
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
				portInt = 8080 // Default port if conversion fails
			}

			newInstance := &fargo.Instance{
				HostName:         cfg.HostName,
				Port:             portInt,
				App:              cfg.AppName,
				IPAddr:           cfg.IPAddress,
				VipAddress:       "payment-service",
				SecureVipAddress: "payment-service",
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
