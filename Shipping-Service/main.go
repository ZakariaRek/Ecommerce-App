package main

import (
	"log"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/gorilla/mux"

	"github.com/ZakariaRek/Ecommerce-App/Shipping-Service/internal/config"
	"github.com/ZakariaRek/Ecommerce-App/Shipping-Service/internal/database"
	"github.com/ZakariaRek/Ecommerce-App/Shipping-Service/internal/handler"
	"github.com/ZakariaRek/Ecommerce-App/Shipping-Service/internal/repository"
	"github.com/ZakariaRek/Ecommerce-App/Shipping-Service/internal/service"
)

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

	// Configure server
	srv := &http.Server{
		Addr:         ":" + cfg.ServerPort,
		Handler:      router,
		ReadTimeout:  15 * time.Second,
		WriteTimeout: 15 * time.Second,
		IdleTimeout:  60 * time.Second,
	}

	// Start server
	go func() {
		log.Printf("Starting server on port %s", cfg.ServerPort)
		if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			log.Fatalf("Error starting server: %v", err)
		}
	}()

	// Graceful shutdown
	c := make(chan os.Signal, 1)
	signal.Notify(c, os.Interrupt, syscall.SIGTERM)
	<-c

	log.Println("Server shutting down...")
}
