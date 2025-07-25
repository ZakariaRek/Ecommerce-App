// Payment-Service/main.go - FIXED VERSION
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

	// 🎯 FIXED: Initialize both basic and enhanced repositories
	paymentRepo := repository.NewPaymentRepository(db)
	enhancedPaymentRepo := repository.NewEnhancedPaymentRepository(db) // NEW: Enhanced repo
	txRepo := repository.NewPaymentTransactionRepository(db)
	invoiceRepo := repository.NewInvoiceRepository(db)

	// Initialize services
	invoiceService := service.NewInvoiceService(invoiceRepo, paymentRepo)
	paymentService := service.NewEnhancedPaymentService(paymentRepo, txRepo, invoiceService)
	orderPaymentService := service.NewOrderPaymentService(paymentRepo, txRepo, invoiceRepo, invoiceService)

	// Initialize transaction service
	transactionService := service.NewTransactionService(txRepo, paymentRepo)

	// Setup Kafka
	paymentKafka, producer := setupKafka(cfg)

	// Register GORM listeners for publishing events
	if paymentKafka != nil {
		paymentListener := listeners.NewPaymentListener(paymentKafka)
		paymentListener.RegisterCallbacks(db)

		// Register invoice listeners
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

		// Register transaction listeners
		transactionTopics := map[string]string{
			"created": getEnv("KAFKA_TRANSACTION_CREATED_TOPIC", "transaction-created"),
			"updated": getEnv("KAFKA_TRANSACTION_UPDATED_TOPIC", "transaction-updated"),
			"changed": getEnv("KAFKA_TRANSACTION_STATUS_CHANGED_TOPIC", "transaction-status-changed"),
			"deleted": getEnv("KAFKA_TRANSACTION_DELETED_TOPIC", "transaction-deleted"),
		}

		if len(transactionTopics) > 0 && producer != nil {
			transactionKafka := kafka.NewTransactionKafkaService(producer, transactionTopics)
			transactionListener := listeners.NewTransactionListener(transactionKafka)
			transactionListener.RegisterCallbacks(db)
			log.Printf("💳 TRANSACTION: GORM listeners registered for transaction events")
		}

		log.Printf("💳 PAYMENT SERVICE: All GORM listeners registered successfully")
	}

	// 🎯 FIXED: Initialize handlers with enhanced repository
	paymentHandler := handler.NewPaymentHandler(
		paymentService,
		enhancedPaymentRepo, // NEW: Pass enhanced repository
		orderPaymentService,
		invoiceService,
	)

	orderPaymentHandler := handler.NewOrderPaymentHandler(
		orderPaymentService,
		invoiceService,
		paymentKafka,
	)

	analyticsHandler := handler.NewAnalyticsHandler(
		paymentService,
		orderPaymentService,
		invoiceService,
	)

	transactionHandler := handler.NewTransactionHandler(
		transactionService,
		paymentService,
	)

	// Setup Chi router
	r := chi.NewRouter()

	//// Basic middleware
	//r.Use(middleware.Logger)
	//r.Use(middleware.Recoverer)
	//r.Use(middleware.RequestID)
	//r.Use(middleware.RealIP)
	//
	//// Simple CORS middleware
	//r.Use(func(next http.Handler) http.Handler {
	//	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
	//		w.Header().Set("Access-Control-Allow-Origin", "*")
	//		w.Header().Set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
	//		w.Header().Set("Access-Control-Allow-Headers", "Content-Type, Authorization")
	//
	//		if r.Method == "OPTIONS" {
	//			w.WriteHeader(http.StatusOK)
	//			return
	//		}
	//
	//		next.ServeHTTP(w, r)
	//	})
	//})

	// Routes
	r.Route("/api/payments", func(r chi.Router) {
		// Payment routes
		paymentHandler.RegisterRoutes(r)

		// Order-specific payment routes
		orderPaymentHandler.RegisterRoutes(r)

		// Analytics routes
		analyticsHandler.RegisterRoutes(r)

		// Transaction routes
		transactionHandler.RegisterRoutes(r)
	})

	// Test endpoint
	r.Get("/test", func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusOK)
		w.Write([]byte(`{"message":"Payment Service with Advanced Filtering","port":"` + cfg.ServerPort + `","version":"2.2"}`))
	})

	// Enhanced health check
	r.Get("/health", func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusOK)
		response := fmt.Sprintf(`{
			"status":"UP",
			"service":"payment-service",
			"version":"2.2",
			"features":["payments","filtering","pagination","auto-invoices","kafka","analytics","transactions"],
			"port":"%s",
			"timestamp":"%s",
			"endpoints":{
				"payments":[
					"GET /api/payments?page=2&limit=20&status=COMPLETED&method=CREDIT_CARD&dateFrom=2024-06-01&dateTo=2024-06-30",
					"POST /api/payments",
					"GET /api/payments/{id}",
					"PUT /api/payments/{id}",
					"DELETE /api/payments/{id}",
					"GET /api/payments/order/{orderID}",
					"POST /api/payments/{id}/process",
					"POST /api/payments/{id}/refund",
					"GET /api/payments/{id}/status",
					"GET /api/payments/{id}/invoice",
					"GET /api/payments/{id}/invoice/pdf",
					"POST /api/payments/{id}/invoice/email",
					"GET /api/payments/{id}/transactions"
				],
				"filtering_parameters":[
					"page (int): Page number (default: 1)",
					"limit (int): Items per page (default: 50, max: 100)",
					"status: PENDING, COMPLETED, FAILED, REFUNDED, PARTIALLY_REFUNDED",
					"method: CREDIT_CARD, DEBIT_CARD, PAYPAL, BANK_TRANSFER, CRYPTO, POINTS, GIFT_CARD",
					"dateFrom (YYYY-MM-DD): Start date filter",
					"dateTo (YYYY-MM-DD): End date filter",
					"amountMin (float): Minimum amount filter",
					"amountMax (float): Maximum amount filter",
					"orderID (UUID): Filter by specific order"
				]
			}
		}`, cfg.ServerPort, time.Now().Format(time.RFC3339))
		w.Write([]byte(response))
	})

	// 🎯 NEW: Filtering examples endpoint
	r.Get("/examples", func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusOK)
		baseURL := fmt.Sprintf("http://localhost:%s", cfg.ServerPort)
		examples := fmt.Sprintf(`{
			"filtering_examples": {
				"basic_pagination": "%s/api/payments?page=1&limit=10",
				"filter_by_status": "%s/api/payments?status=COMPLETED",
				"filter_by_method": "%s/api/payments?method=CREDIT_CARD",
				"filter_by_date_range": "%s/api/payments?dateFrom=2024-06-01&dateTo=2024-06-30",
				"filter_by_amount": "%s/api/payments?amountMin=100&amountMax=1000",
				"combined_filters": "%s/api/payments?page=2&limit=20&status=COMPLETED&method=CREDIT_CARD&dateFrom=2024-06-01&dateTo=2024-06-30",
				"original_fixed_url": "%s/api/payments?page=2&limit=20&status=COMPLETED&method=CREDIT_CARD&dateFrom=2024-06-01&dateTo=2024-06-30"
			},
			"valid_status_values": ["PENDING", "COMPLETED", "FAILED", "REFUNDED", "PARTIALLY_REFUNDED"],
			"valid_method_values": ["CREDIT_CARD", "DEBIT_CARD", "PAYPAL", "BANK_TRANSFER", "CRYPTO", "POINTS", "GIFT_CARD"],
			"date_format": "YYYY-MM-DD (e.g., 2024-06-01)",
			"response_format": {
				"payments": "Array of payment objects",
				"pagination": {
					"page": "Current page number",
					"limit": "Items per page",
					"total": "Total number of payments",
					"totalPages": "Total number of pages"
				},
				"filters": "Applied filters object"
			}
		}`, baseURL, baseURL, baseURL, baseURL, baseURL, baseURL, baseURL)
		w.Write([]byte(examples))
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
		fmt.Printf("🚀 Payment Service v2.2 with Advanced Filtering starting on port %s...\n", cfg.ServerPort)
		fmt.Printf("📋 Service available at: http://localhost:%s\n", cfg.ServerPort)
		fmt.Printf("🏥 Health check: http://localhost:%s/health\n", cfg.ServerPort)
		fmt.Printf("🧪 Test endpoint: http://localhost:%s/test\n", cfg.ServerPort)
		fmt.Printf("📖 Filtering examples: http://localhost:%s/examples\n", cfg.ServerPort)

		log.Printf("✅ FIXED URL: http://localhost:%s/api/payments?page=2&limit=20&status=COMPLETED&method=CREDIT_CARD&dateFrom=2024-06-01&dateTo=2024-06-30", cfg.ServerPort)

		log.Printf("💳 Available filters:")
		log.Printf("  status: PENDING, COMPLETED, FAILED, REFUNDED, PARTIALLY_REFUNDED")
		log.Printf("  method: CREDIT_CARD, DEBIT_CARD, PAYPAL, BANK_TRANSFER, CRYPTO, POINTS, GIFT_CARD")
		log.Printf("  dateFrom/dateTo: YYYY-MM-DD format")
		log.Printf("  amountMin/amountMax: Decimal numbers")
		log.Printf("  orderID: UUID format")
		log.Printf("  page: Page number (default: 1)")
		log.Printf("  limit: Items per page (default: 50, max: 100)")

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

// Kafka setup function and other utility functions remain the same...
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
