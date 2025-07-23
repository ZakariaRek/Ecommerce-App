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
	invoiceRepo := repository.NewInvoiceRepository(db)

	// 🎯 FIXED: Initialize services with correct parameters
	// Fix: Pass both invoiceRepo and paymentRepo to invoice service
	invoiceService := service.NewInvoiceService(invoiceRepo, paymentRepo)
	// Use enhanced payment service with auto-invoice generation
	paymentService := service.NewEnhancedPaymentService(paymentRepo, txRepo, invoiceService)

	// Initialize enhanced order payment service with invoice support
	//orderPaymentService := service.NewOrderPaymentService(
	//	paymentRepo,
	//	txRepo,
	//	invoiceRepo,
	//	invoiceService,
	//)
	orderPaymentService := service.NewOrderPaymentService(paymentRepo, txRepo, invoiceRepo, invoiceService)

	// Setup Kafka
	paymentKafka, producer := setupKafka(cfg)

	// Register GORM listeners for publishing events
	if paymentKafka != nil {
		paymentListener := listeners.NewPaymentListener(paymentKafka)
		paymentListener.RegisterCallbacks(db)

		// Register invoice listeners for invoice events
		invoiceTopics := map[string]string{
			"created": getEnv("KAFKA_INVOICE_CREATED_TOPIC", "invoice-created"),
			"updated": getEnv("KAFKA_INVOICE_UPDATED_TOPIC", "invoice-updated"),
			"changed": getEnv("KAFKA_INVOICE_DUE_DATE_CHANGED_TOPIC", "invoice-due-date-changed"),
			"deleted": getEnv("KAFKA_INVOICE_DELETED_TOPIC", "invoice-deleted"),
		}

		if len(invoiceTopics) > 0 && producer != nil {
			invoiceKafka := kafka.NewInvoiceKafkaService(producer, invoiceTopics)
			invoiceListener := listeners.NewInvoiceListener(invoiceKafka)
			invoiceListener.RegisterCallbacks(db)
			log.Printf("📋 INVOICE: GORM listeners registered for invoice events")
		}

		log.Printf("💳 PAYMENT SERVICE: GORM listeners registered for payment events")
	}

	// Initialize handlers
	paymentHandler := handler.NewPaymentHandler(paymentService)
	orderPaymentHandler := handler.NewOrderPaymentHandler(
		orderPaymentService,
		invoiceService,
		paymentKafka,
	)

	// Setup Chi router
	r := chi.NewRouter()

	// Middleware
	r.Use(middleware.Logger)
	r.Use(middleware.Recoverer)
	r.Use(middleware.RequestID)
	r.Use(middleware.RealIP)

	// Add CORS middleware
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
		// 🎯 FIXED: Regular payment routes use /payments prefix
		paymentHandler.RegisterRoutes(r)

		// Order-specific payment routes use /orders prefix
		orderPaymentHandler.RegisterRoutes(r)
	})

	// Test endpoint
	r.Get("/test", func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusOK)
		w.Write([]byte(`{"message":"Payment Service with Auto-Invoice Generation","port":"` + cfg.ServerPort + `"}`))
	})

	// Health check
	r.Get("/health", func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusOK)
		response := fmt.Sprintf(`{
			"status":"UP",
			"service":"payment-service",
			"features":["payments","auto-invoices","kafka"],
			"port":"%s",
			"timestamp":"%s",
			"endpoints":{
				"payments":[
					"POST /api/payments",
					"GET /api/payments/{id}",
					"PUT /api/payments/{id}",
					"DELETE /api/payments/{id}",
					"GET /api/payments",
					"GET /api/payments/order/{orderID}",
					"POST /api/payments/{id}/process",
					"POST /api/payments/{id}/refund",
					"GET /api/payments/{id}/status"
				],
				"order_payments":[
					"POST /api/orders/{orderID}/payments",
					"GET /api/orders/{orderID}/payments",
					"POST /api/orders/{orderID}/refund",
					"GET /api/orders/{orderID}/payments/status"
				],
				"invoices":[
					"GET /api/orders/{orderID}/invoices",
					"GET /api/orders/{orderID}/invoices/{invoiceID}",
					"GET /api/orders/{orderID}/invoices/{invoiceID}/pdf",
					"POST /api/orders/{orderID}/invoices/{invoiceID}/email",
					"GET /api/payments/{paymentID}/invoice"
				]
			}
		}`, cfg.ServerPort, time.Now().Format(time.RFC3339))
		w.Write([]byte(response))
	})

	// Create HTTP server
	srv := &http.Server{
		Addr:    ":" + cfg.ServerPort,
		Handler: r,
	}

	// Register with Eureka
	eurekaClient, eurekaErr := connectToEureka(cfg)
	if eurekaErr != nil {
		log.Printf("Warning: Failed to connect to Eureka: %v", eurekaErr)
		log.Println("Continuing without Eureka registration...")
	} else {
		log.Println("Successfully registered with Eureka service registry")
		go startHeartbeat(eurekaClient, cfg)
	}

	// Start the server
	go func() {
		fmt.Printf("🚀 Payment Service with Auto-Invoice Generation starting on port %s...\n", cfg.ServerPort)
		fmt.Printf("📋 Service available at: http://localhost:%s\n", cfg.ServerPort)
		fmt.Printf("🏥 Health check: http://localhost:%s/health\n", cfg.ServerPort)
		fmt.Printf("🧪 Test endpoint: http://localhost:%s/test\n", cfg.ServerPort)

		log.Printf("💳 Payment endpoints:")
		log.Printf("  POST http://localhost:%s/api/payments - Create payment", cfg.ServerPort)
		log.Printf("  GET  http://localhost:%s/api/payments/{id} - Get payment", cfg.ServerPort)
		log.Printf("  POST http://localhost:%s/api/payments/{id}/process - Process payment", cfg.ServerPort)
		log.Printf("  POST http://localhost:%s/api/payments/{id}/refund - Refund payment", cfg.ServerPort)

		log.Printf("📋 Order Payment endpoints:")
		log.Printf("  POST http://localhost:%s/api/orders/{orderID}/payments - Process order payment (Auto-generates invoice)", cfg.ServerPort)
		log.Printf("  GET  http://localhost:%s/api/orders/{orderID}/payments - Get order payments", cfg.ServerPort)
		log.Printf("  POST http://localhost:%s/api/orders/{orderID}/refund - Refund order payment", cfg.ServerPort)

		log.Printf("📋 Invoice endpoints:")
		log.Printf("  GET  http://localhost:%s/api/orders/{orderID}/invoices - Get order invoices", cfg.ServerPort)
		log.Printf("  GET  http://localhost:%s/api/payments/{paymentID}/invoice - Get payment invoice", cfg.ServerPort)

		if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			log.Fatalf("Failed to start server: %v", err)
		}
	}()

	// Wait for interrupt signal
	quit := make(chan os.Signal, 1)
	signal.Notify(quit, syscall.SIGINT, syscall.SIGTERM)
	<-quit

	// Graceful shutdown
	if eurekaClient != nil {
		if err := deregisterFromEureka(eurekaClient, cfg); err != nil {
			log.Printf("Warning: Failed to deregister from Eureka: %v", err)
		} else {
			log.Println("Successfully deregistered from Eureka")
		}
	}

	fmt.Println("🛑 Shutting down server...")

	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()

	if err := srv.Shutdown(ctx); err != nil {
		log.Fatalf("Server shutdown failed: %v", err)
	}

	if producer != nil {
		log.Printf("💳 PAYMENT SERVICE: Closing Kafka producer...")
		if err := producer.Close(); err != nil {
			log.Printf("💳 PAYMENT SERVICE: Error closing Kafka producer: %v", err)
		} else {
			log.Printf("💳 PAYMENT SERVICE: Kafka producer closed successfully")
		}
	}

	fmt.Println("✅ Server stopped")
}

// Rest of the functions remain the same...
func setupKafka(cfg *config.Config) (*kafka.PaymentKafkaService, sarama.AsyncProducer) {
	brokersStr := getEnv("KAFKA_BROKERS", "localhost:9092")
	brokers := strings.Split(brokersStr, ",")

	log.Printf("💳 PAYMENT SERVICE: Connecting to Kafka brokers: %v", brokers)

	paymentTopics := map[string]string{
		"created": getEnv("KAFKA_PAYMENT_CREATED_TOPIC", "payment-created"),
		"updated": getEnv("KAFKA_PAYMENT_UPDATED_TOPIC", "payment-updated"),
		"changed": getEnv("KAFKA_PAYMENT_STATUS_CHANGED_TOPIC", "payment-confirmed"),
		"deleted": getEnv("KAFKA_PAYMENT_DELETED_TOPIC", "payment-deleted"),
	}

	log.Printf("💳 PAYMENT SERVICE: Kafka topics configured:")
	for event, topic := range paymentTopics {
		log.Printf("  %s: %s", event, topic)
	}

	producerConfig := sarama.NewConfig()
	producerConfig.Producer.RequiredAcks = sarama.WaitForAll
	producerConfig.Producer.Retry.Max = 3
	producerConfig.Producer.Return.Successes = true
	producerConfig.Producer.Return.Errors = true
	producerConfig.Producer.Compression = sarama.CompressionSnappy
	producerConfig.Producer.Flush.Frequency = 500 * time.Millisecond
	producerConfig.Producer.Flush.Messages = 1
	producerConfig.Producer.Idempotent = true
	producerConfig.Net.MaxOpenRequests = 1

	var paymentKafka *kafka.PaymentKafkaService
	var producer sarama.AsyncProducer

	var producerErr error
	producer, producerErr = sarama.NewAsyncProducer(brokers, producerConfig)
	if producerErr != nil {
		log.Printf("💳 PAYMENT SERVICE: Failed to create Kafka producer: %v", producerErr)
		log.Println("💳 PAYMENT SERVICE: Continuing without Kafka support...")
		return nil, nil
	}

	log.Printf("💳 PAYMENT SERVICE: Kafka producer created successfully")

	go func() {
		for {
			select {
			case success := <-producer.Successes():
				log.Printf("💳 PAYMENT SERVICE: Message delivered successfully - Topic: %s, Partition: %d, Offset: %d",
					success.Topic, success.Partition, success.Offset)
			case err := <-producer.Errors():
				log.Printf("💳 PAYMENT SERVICE: Failed to produce Kafka message - Topic: %s, Error: %v",
					err.Msg.Topic, err.Err)
				if err.Msg.Key != nil {
					log.Printf("💳 PAYMENT SERVICE: Failed message key: %s", string(err.Msg.Key.(sarama.StringEncoder)))
				}
			}
		}
	}()

	paymentKafka = kafka.NewPaymentKafkaService(producer, paymentTopics)
	log.Printf("💳 PAYMENT SERVICE: Kafka services initialized successfully")
	return paymentKafka, producer
}

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
