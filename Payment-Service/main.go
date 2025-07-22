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
	orderPaymentService := service.NewOrderPaymentService(paymentRepo, txRepo)

	// Initialize Kafka
	brokersStr := getEnv("KAFKA_BROKERS", "localhost:9092")
	brokers := strings.Split(brokersStr, ",")

	// Create topic maps for each entity type
	paymentTopics := map[string]string{
		"created": getEnv("KAFKA_PAYMENT_CREATED_TOPIC", "payment-created"),
		"updated": getEnv("KAFKA_PAYMENT_UPDATED_TOPIC", "payment-updated"),
		"changed": getEnv("KAFKA_PAYMENT_STATUS_CHANGED_TOPIC", "payment-confirmed"),
		"deleted": getEnv("KAFKA_PAYMENT_DELETED_TOPIC", "payment-deleted"),
	}

	// Create Kafka producer
	producerConfig := sarama.NewConfig()
	producerConfig.Producer.RequiredAcks = sarama.WaitForLocal
	producerConfig.Producer.Compression = sarama.CompressionSnappy
	producerConfig.Producer.Return.Successes = true
	producerConfig.Producer.Return.Errors = true

	var paymentKafka *kafka.PaymentKafkaService

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
		paymentKafka = kafka.NewPaymentKafkaService(producer, paymentTopics)

		// Register GORM listeners for publishing events
		paymentListener := listeners.NewPaymentListener(paymentKafka)
		paymentListener.RegisterCallbacks(db)
	}

	// Initialize handlers
	paymentHandler := handler.NewPaymentHandler(paymentService)
	orderPaymentHandler := handler.NewOrderPaymentHandler(orderPaymentService, paymentKafka)

	// Setup Chi router
	r := chi.NewRouter()

	// Middleware
	r.Use(middleware.Logger)
	r.Use(middleware.Recoverer)
	r.Use(middleware.RequestID)
	r.Use(middleware.RealIP)

	// Add CORS middleware for cross-origin requests
	r.Use(func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			w.Header().Set("Access-Control-Allow-Origin", "*")
			w.Header().Set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
			w.Header().Set("Access-Control-Allow-Headers", "Content-Type, Authorization")

			if r.Method == "OPTIONS" {
				w.WriteHeader(http.StatusOK)
				return
			}

			next.ServeHTTP(w, r)
		})
	})

	// Routes
	r.Route("/api", func(r chi.Router) {
		// Regular payment routes
		r.Route("/payments", func(r chi.Router) {
			r.Post("/", paymentHandler.CreatePayment)
			r.Get("/{id}", paymentHandler.GetPayment)
			r.Put("/{id}", paymentHandler.UpdatePayment)
			r.Delete("/{id}", paymentHandler.DeletePayment)
			r.Get("/", paymentHandler.GetAllPayments)
			r.Get("/order/{orderID}", paymentHandler.GetPaymentsByOrder)
			r.Post("/{id}/process", paymentHandler.ProcessPayment)
			r.Post("/{id}/refund", paymentHandler.RefundPayment)
			r.Get("/{id}/status", paymentHandler.GetPaymentStatus)
		})

		// Order payment routes
		r.Route("/orders", func(r chi.Router) {
			r.Post("/{orderID}/payments", orderPaymentHandler.ProcessOrderPayment)
			r.Get("/{orderID}/payments", orderPaymentHandler.GetOrderPayments)
			r.Post("/{orderID}/refund", orderPaymentHandler.RefundOrderPayment)
			r.Get("/{orderID}/payments/status", orderPaymentHandler.GetOrderPaymentStatus)
		})
	})

	// Health check
	r.Get("/health", func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusOK)
		w.Write([]byte(`{"status":"UP","service":"payment-service"}`))
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
		go startHeartbeat(eurekaClient, cfg)
	}

	// Start the server in a goroutine
	go func() {
		fmt.Printf("🚀 Starting Payment Service on port %s...\n", cfg.ServerPort)
		log.Printf("📋 Available endpoints:")
		log.Printf("  POST /api/orders/{orderID}/payments - Process order payment")
		log.Printf("  GET  /api/orders/{orderID}/payments - Get order payments")
		log.Printf("  POST /api/orders/{orderID}/refund - Refund order payment")
		log.Printf("  GET  /api/orders/{orderID}/payments/status - Get order payment status")
		log.Printf("  GET  /health - Health check")
		log.Printf("🏥 Health check URL: http://localhost:%s/health", cfg.ServerPort)

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

	fmt.Println("🛑 Shutting down server...")

	// Create a deadline for server shutdown
	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()

	// Perform graceful shutdown
	if err := srv.Shutdown(ctx); err != nil {
		log.Fatalf("Server shutdown failed: %v", err)
	}

	fmt.Println("✅ Server stopped")
}

// Helper functions
func getEnv(key, defaultValue string) string {
	if value, exists := os.LookupEnv(key); exists {
		return value
	}
	return defaultValue
}
func connectToEureka(cfg *config.Config) (*fargo.EurekaConnection, error) {
	eurekaConn := fargo.NewConn(cfg.EurekaURL)

	portInt, err := strconv.Atoi(cfg.ServerPort)
	if err != nil {
		portInt = 8080
	}

	instance := &fargo.Instance{
		HostName:         cfg.HostName,
		Port:             portInt,
		App:              cfg.AppName,
		IPAddr:           cfg.IPAddress,
		VipAddress:       strings.ToLower(cfg.AppName),
		SecureVipAddress: strings.ToLower(cfg.AppName),
		DataCenterInfo:   fargo.DataCenterInfo{Name: fargo.MyOwn},
		Status:           fargo.UP,
		HomePageUrl:      fmt.Sprintf("http://%s:%s/", cfg.IPAddress, cfg.ServerPort),
		StatusPageUrl:    fmt.Sprintf("http://%s:%s/health", cfg.IPAddress, cfg.ServerPort),
		HealthCheckUrl:   fmt.Sprintf("http://%s:%s/health", cfg.IPAddress, cfg.ServerPort),
		Metadata: fargo.InstanceMetadata{
			"management.port":         cfg.ServerPort,
			"management.context-path": "/api",
			"zone":                    "default",
			"instanceId":              cfg.EurekaInstanceId,
		},
	}

	log.Printf("Registering with Eureka:")
	log.Printf("  App Name: %s", instance.App)
	log.Printf("  VIP Address: %s", instance.VipAddress)
	log.Printf("  Host: %s:%d", instance.HostName, instance.Port)
	log.Printf("  IP Address: %s", instance.IPAddr)
	log.Printf("  Instance ID: %s", cfg.EurekaInstanceId)

	err = eurekaConn.RegisterInstance(instance)
	if err != nil {
		return nil, fmt.Errorf("failed to register with Eureka: %v", err)
	}

	log.Printf("Successfully registered with Eureka at %s", cfg.EurekaURL)
	return &eurekaConn, nil
}

func startHeartbeat(eurekaConn *fargo.EurekaConnection, cfg *config.Config) {
	ticker := time.NewTicker(30 * time.Second)
	defer ticker.Stop()

	portInt, err := strconv.Atoi(cfg.ServerPort)
	if err != nil {
		portInt = 8080
	}

	instance := &fargo.Instance{
		HostName: cfg.HostName,
		Port:     portInt,
		App:      cfg.AppName,
		IPAddr:   cfg.IPAddress,
	}

	for {
		<-ticker.C
		err := eurekaConn.HeartBeatInstance(instance)
		if err != nil {
			log.Printf("Failed to send heartbeat to Eureka: %v", err)

			newInstance := &fargo.Instance{
				HostName:         cfg.HostName,
				Port:             portInt,
				App:              cfg.AppName,
				IPAddr:           cfg.IPAddress,
				VipAddress:       strings.ToLower(cfg.AppName),
				SecureVipAddress: strings.ToLower(cfg.AppName),
				DataCenterInfo:   fargo.DataCenterInfo{Name: fargo.MyOwn},
				Status:           fargo.UP,
				HomePageUrl:      fmt.Sprintf("http://%s:%s/", cfg.IPAddress, cfg.ServerPort),
				StatusPageUrl:    fmt.Sprintf("http://%s:%s/health", cfg.IPAddress, cfg.ServerPort),
				HealthCheckUrl:   fmt.Sprintf("http://%s:%s/health", cfg.IPAddress, cfg.ServerPort),
				Metadata: fargo.InstanceMetadata{
					"management.port":         cfg.ServerPort,
					"management.context-path": "/api",
					"zone":                    "default",
					"instanceId":              cfg.EurekaInstanceId,
				},
			}

			regErr := eurekaConn.RegisterInstance(newInstance)
			if regErr != nil {
				log.Printf("Failed to re-register with Eureka: %v", regErr)
			} else {
				log.Printf("Successfully re-registered with Eureka")
				instance = newInstance
			}
		} else {
			log.Printf("Heartbeat sent successfully to Eureka")
		}
	}
}

func deregisterFromEureka(eurekaConn *fargo.EurekaConnection, cfg *config.Config) error {
	portInt, err := strconv.Atoi(cfg.ServerPort)
	if err != nil {
		portInt = 8080
	}

	instance := &fargo.Instance{
		HostName: cfg.HostName,
		Port:     portInt,
		App:      cfg.AppName,
		IPAddr:   cfg.IPAddress,
	}

	err = eurekaConn.DeregisterInstance(instance)
	if err != nil {
		return fmt.Errorf("failed to deregister from Eureka: %v", err)
	}

	return nil
}
