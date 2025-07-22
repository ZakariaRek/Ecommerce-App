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
		paymentHandler.RegisterRoutes(r)

		// Order payment routes - EXPLICITLY REGISTER THESE ROUTES
		r.Route("/orders", func(r chi.Router) {
			r.Post("/{orderID}/payments", func(w http.ResponseWriter, req *http.Request) {
				log.Printf("💳 PAYMENT SERVICE: Received payment request for order: %s", chi.URLParam(req, "orderID"))
				orderPaymentHandler.ProcessOrderPayment(w, req)
			})

			r.Get("/{orderID}/payments", func(w http.ResponseWriter, req *http.Request) {
				log.Printf("💳 PAYMENT SERVICE: Getting payments for order: %s", chi.URLParam(req, "orderID"))
				orderPaymentHandler.GetOrderPayments(w, req)
			})

			r.Post("/{orderID}/refund", func(w http.ResponseWriter, req *http.Request) {
				log.Printf("💳 PAYMENT SERVICE: Processing refund for order: %s", chi.URLParam(req, "orderID"))
				orderPaymentHandler.RefundOrderPayment(w, req)
			})

			r.Get("/{orderID}/payments/status", func(w http.ResponseWriter, req *http.Request) {
				log.Printf("💳 PAYMENT SERVICE: Getting payment status for order: %s", chi.URLParam(req, "orderID"))
				orderPaymentHandler.GetOrderPaymentStatus(w, req)
			})
		})
	})

	// Add a test endpoint to verify the service is running
	r.Get("/test", func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusOK)
		w.Write([]byte(`{"message":"Payment Service is running","port":"` + cfg.ServerPort + `"}`))
	})

	// Health check with more detailed information
	r.Get("/health", func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusOK)
		response := fmt.Sprintf(`{
			"status":"UP",
			"service":"payment-service",
			"port":"%s",
			"timestamp":"%s",
			"routes":[
				"POST /api/orders/{orderID}/payments",
				"GET /api/orders/{orderID}/payments", 
				"POST /api/orders/{orderID}/refund",
				"GET /api/orders/{orderID}/payments/status"
			]
		}`, cfg.ServerPort, time.Now().Format(time.RFC3339))
		w.Write([]byte(response))
	})

	// Log all registered routes for debugging
	walkFunc := func(method string, route string, handler http.Handler, middlewares ...func(http.Handler) http.Handler) error {
		log.Printf("📍 Route registered: %s %s", method, route)
		return nil
	}
	if err := chi.Walk(r, walkFunc); err != nil {
		log.Printf("Error walking routes: %v", err)
	}

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
		fmt.Printf("🚀 Payment Service starting on port %s...\n", cfg.ServerPort)
		fmt.Printf("📋 Service available at: http://localhost:%s\n", cfg.ServerPort)
		fmt.Printf("🏥 Health check: http://localhost:%s/health\n", cfg.ServerPort)
		fmt.Printf("🧪 Test endpoint: http://localhost:%s/test\n", cfg.ServerPort)
		log.Printf("📋 Order Payment endpoints:")
		log.Printf("  POST http://localhost:%s/api/orders/{orderID}/payments - Process order payment", cfg.ServerPort)
		log.Printf("  GET  http://localhost:%s/api/orders/{orderID}/payments - Get order payments", cfg.ServerPort)
		log.Printf("  POST http://localhost:%s/api/orders/{orderID}/refund - Refund order payment", cfg.ServerPort)
		log.Printf("  GET  http://localhost:%s/api/orders/{orderID}/payments/status - Get order payment status", cfg.ServerPort)

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

// Helper functions (keep the existing helper functions)
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
