package main

import (
	"context"
	"fmt"
	"log"
	"net/http"
	"os"
	"os/signal"
	"strconv"
	"syscall"
	"time"

	"github.com/ZakariaRek/Ecommerce-App/Payment-Service/internal/config"
	"github.com/ZakariaRek/Ecommerce-App/Payment-Service/internal/database"
	"github.com/ZakariaRek/Ecommerce-App/Payment-Service/internal/handler"
	"github.com/ZakariaRek/Ecommerce-App/Payment-Service/internal/repository"
	"github.com/ZakariaRek/Ecommerce-App/Payment-Service/internal/service"

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
		// Payment routes
		r.Route("/payments", func(r chi.Router) {
			r.Post("/", paymentHandler.CreatePayment)
			r.Get("/{id}", paymentHandler.GetPayment)
			// Add more payment routes as needed
		})

		// Invoice routes
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

	// Correction: Return the address of eurekaConn
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
