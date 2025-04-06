package main

import (
	"context"
	"fmt"
	"log"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/ZakariaRek/Ecommerce-App/Payment-Service/internal/config"
	"github.com/ZakariaRek/Ecommerce-App/Payment-Service/internal/database"
	"github.com/ZakariaRek/Ecommerce-App/Payment-Service/internal/handler"
	"github.com/ZakariaRek/Ecommerce-App/Payment-Service/internal/repository"
	"github.com/ZakariaRek/Ecommerce-App/Payment-Service/internal/service"

	"github.com/go-chi/chi/v5"
	"github.com/go-chi/chi/v5/middleware"
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
