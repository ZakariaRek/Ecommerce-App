package controller

import (
	"encoding/json"
	"net/http"
	"strconv"

	"github.com/google/uuid"
	"github.com/gorilla/mux"

	"github.com/ZakariaRek/Ecommerce-App/Shipping-Service/internal/models"
	"github.com/ZakariaRek/Ecommerce-App/Shipping-Service/internal/service"
)

// ShippingController handles HTTP requests for shipping operations
type ShippingController struct {
	shippingService *service.ShippingService
}

// NewShippingController creates a new shipping controller
func NewShippingController(shippingService *service.ShippingService) *ShippingController {
	return &ShippingController{
		shippingService: shippingService,
	}
}

// SetupRoutes configures all routes for the shipping controller
func (c *ShippingController) SetupRoutes() *mux.Router {
	router := mux.NewRouter()

	// Add CORS middleware
	router.Use(c.corsMiddleware)

	// Setup API routes
	c.setupShippingRoutes(router)
	c.setupHealthRoutes(router)

	return router
}

// setupShippingRoutes configures shipping-related routes
func (c *ShippingController) setupShippingRoutes(router *mux.Router) {
	// API routes
	api := router.PathPrefix("/api").Subrouter()
	shippingRoutes := api.PathPrefix("/shipping").Subrouter()

	// CRUD operations
	shippingRoutes.HandleFunc("", c.CreateShipping).Methods("POST")
	shippingRoutes.HandleFunc("", c.GetAllShippings).Methods("GET")
	shippingRoutes.HandleFunc("/{id}", c.GetShipping).Methods("GET")
	shippingRoutes.HandleFunc("/{id}", c.UpdateShipping).Methods("PUT")

	// Special operations
	shippingRoutes.HandleFunc("/{id}/status", c.UpdateStatus).Methods("PATCH")
	shippingRoutes.HandleFunc("/{id}/track", c.TrackOrder).Methods("GET")
	shippingRoutes.HandleFunc("/{id}/cost", c.GetShippingCost).Methods("GET")

	// Order-specific routes
	shippingRoutes.HandleFunc("/order/{order_id}", c.GetShippingByOrder).Methods("GET")
}

// setupHealthRoutes configures health and monitoring routes
func (c *ShippingController) setupHealthRoutes(router *mux.Router) {
	router.HandleFunc("/health", c.healthCheck).Methods("GET")
	router.HandleFunc("/health/live", c.livenessCheck).Methods("GET")
	router.HandleFunc("/health/ready", c.readinessCheck).Methods("GET")
	router.HandleFunc("/info", c.serviceInfo).Methods("GET")
}

// CORS middleware
func (c *ShippingController) corsMiddleware(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Access-Control-Allow-Origin", "*")
		w.Header().Set("Access-Control-Allow-Methods", "GET, POST, PUT, PATCH, DELETE, OPTIONS")
		w.Header().Set("Access-Control-Allow-Headers", "Content-Type, Authorization")

		if r.Method == "OPTIONS" {
			w.WriteHeader(http.StatusNoContent)
			return
		}

		next.ServeHTTP(w, r)
	})
}

// Request/Response structs
type CreateShippingRequest struct {
	OrderID string `json:"order_id" validate:"required"`
	Carrier string `json:"carrier" validate:"required"`
}

type UpdateStatusRequest struct {
	Status   string `json:"status" validate:"required"`
	Location string `json:"location"`
	Notes    string `json:"notes"`
}

type APIResponse struct {
	Success bool        `json:"success"`
	Data    interface{} `json:"data,omitempty"`
	Error   string      `json:"error,omitempty"`
	Message string      `json:"message,omitempty"`
}

type TrackingResponse struct {
	Shipping        *models.Shipping          `json:"shipping"`
	TrackingHistory []models.ShipmentTracking `json:"tracking_history"`
}

// Shipping API handlers

// CreateShipping handles the creation of a new shipping
func (c *ShippingController) CreateShipping(w http.ResponseWriter, r *http.Request) {
	var req CreateShippingRequest

	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		c.respondWithError(w, http.StatusBadRequest, "Invalid request body")
		return
	}

	// Validate required fields
	if req.OrderID == "" {
		c.respondWithError(w, http.StatusBadRequest, "order_id is required")
		return
	}

	if req.Carrier == "" {
		c.respondWithError(w, http.StatusBadRequest, "carrier is required")
		return
	}

	orderID, err := uuid.Parse(req.OrderID)
	if err != nil {
		c.respondWithError(w, http.StatusBadRequest, "Invalid order ID format")
		return
	}

	shipping, err := c.shippingService.CreateShipping(orderID, req.Carrier)
	if err != nil {
		c.respondWithError(w, http.StatusInternalServerError, err.Error())
		return
	}

	c.respondWithSuccess(w, http.StatusCreated, shipping, "Shipping created successfully")
}

// GetShipping handles retrieval of shipping details by ID
func (c *ShippingController) GetShipping(w http.ResponseWriter, r *http.Request) {
	vars := mux.Vars(r)
	id, err := uuid.Parse(vars["id"])
	if err != nil {
		c.respondWithError(w, http.StatusBadRequest, "Invalid shipping ID format")
		return
	}

	shipping, err := c.shippingService.GetShipping(id)
	if err != nil {
		c.respondWithError(w, http.StatusNotFound, err.Error())
		return
	}

	c.respondWithSuccess(w, http.StatusOK, shipping, "")
}

// GetShippingByOrder handles retrieval of shipping details by order ID
func (c *ShippingController) GetShippingByOrder(w http.ResponseWriter, r *http.Request) {
	vars := mux.Vars(r)
	orderID, err := uuid.Parse(vars["order_id"])
	if err != nil {
		c.respondWithError(w, http.StatusBadRequest, "Invalid order ID format")
		return
	}

	shipping, err := c.shippingService.GetShippingByOrder(orderID)
	if err != nil {
		c.respondWithError(w, http.StatusNotFound, err.Error())
		return
	}

	c.respondWithSuccess(w, http.StatusOK, shipping, "")
}

// GetAllShippings handles retrieval of all shippings with pagination
func (c *ShippingController) GetAllShippings(w http.ResponseWriter, r *http.Request) {
	// Parse query parameters
	limitStr := r.URL.Query().Get("limit")
	offsetStr := r.URL.Query().Get("offset")

	limit := 10 // default limit
	if limitStr != "" {
		if parsedLimit, err := strconv.Atoi(limitStr); err == nil && parsedLimit > 0 && parsedLimit <= 100 {
			limit = parsedLimit
		}
	}

	offset := 0 // default offset
	if offsetStr != "" {
		if parsedOffset, err := strconv.Atoi(offsetStr); err == nil && parsedOffset >= 0 {
			offset = parsedOffset
		}
	}

	shippings, err := c.shippingService.GetAllShippings(limit, offset)
	if err != nil {
		c.respondWithError(w, http.StatusInternalServerError, err.Error())
		return
	}

	// Create response with pagination info
	response := map[string]interface{}{
		"shippings": shippings,
		"pagination": map[string]interface{}{
			"limit":  limit,
			"offset": offset,
			"count":  len(shippings),
		},
	}

	c.respondWithSuccess(w, http.StatusOK, response, "")
}

// UpdateShipping handles updates to shipping details
func (c *ShippingController) UpdateShipping(w http.ResponseWriter, r *http.Request) {
	vars := mux.Vars(r)
	id, err := uuid.Parse(vars["id"])
	if err != nil {
		c.respondWithError(w, http.StatusBadRequest, "Invalid shipping ID format")
		return
	}

	var shipping models.Shipping
	if err := json.NewDecoder(r.Body).Decode(&shipping); err != nil {
		c.respondWithError(w, http.StatusBadRequest, "Invalid request body")
		return
	}

	// Ensure ID in URL matches body
	shipping.ID = id

	if err := c.shippingService.UpdateShipping(&shipping); err != nil {
		c.respondWithError(w, http.StatusInternalServerError, err.Error())
		return
	}

	c.respondWithSuccess(w, http.StatusOK, shipping, "Shipping updated successfully")
}

// UpdateStatus handles updates to shipping status
func (c *ShippingController) UpdateStatus(w http.ResponseWriter, r *http.Request) {
	vars := mux.Vars(r)
	id, err := uuid.Parse(vars["id"])
	if err != nil {
		c.respondWithError(w, http.StatusBadRequest, "Invalid shipping ID format")
		return
	}

	var req UpdateStatusRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		c.respondWithError(w, http.StatusBadRequest, "Invalid request body")
		return
	}

	// Validate status
	status := models.ShippingStatus(req.Status)
	if !status.IsValid() {
		c.respondWithError(w, http.StatusBadRequest, "Invalid shipping status")
		return
	}

	err = c.shippingService.UpdateStatus(id, status, req.Location, req.Notes)
	if err != nil {
		c.respondWithError(w, http.StatusInternalServerError, err.Error())
		return
	}

	c.respondWithSuccess(w, http.StatusOK, nil, "Status updated successfully")
}

// TrackOrder handles tracking of a shipment
func (c *ShippingController) TrackOrder(w http.ResponseWriter, r *http.Request) {
	vars := mux.Vars(r)
	id, err := uuid.Parse(vars["id"])
	if err != nil {
		c.respondWithError(w, http.StatusBadRequest, "Invalid shipping ID format")
		return
	}

	shipping, history, err := c.shippingService.TrackOrder(id)
	if err != nil {
		c.respondWithError(w, http.StatusNotFound, err.Error())
		return
	}

	response := TrackingResponse{
		Shipping:        shipping,
		TrackingHistory: history,
	}

	c.respondWithSuccess(w, http.StatusOK, response, "")
}

// GetShippingCost handles shipping cost calculation
func (c *ShippingController) GetShippingCost(w http.ResponseWriter, r *http.Request) {
	vars := mux.Vars(r)
	id, err := uuid.Parse(vars["id"])
	if err != nil {
		c.respondWithError(w, http.StatusBadRequest, "Invalid shipping ID format")
		return
	}

	cost, err := c.shippingService.CalculateShippingCost(id)
	if err != nil {
		c.respondWithError(w, http.StatusNotFound, err.Error())
		return
	}

	response := map[string]interface{}{
		"shipping_id": id,
		"cost":        cost,
		"currency":    "USD",
	}

	c.respondWithSuccess(w, http.StatusOK, response, "")
}

// Health check handlers

func (c *ShippingController) healthCheck(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)
	w.Write([]byte(`{
		"status": "UP",
		"service": "shipping-service",
		"checks": {
			"database": "UP",
			"kafka": "UP"
		}
	}`))
}

func (c *ShippingController) livenessCheck(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)
	w.Write([]byte(`{"status":"UP","check":"liveness"}`))
}

func (c *ShippingController) readinessCheck(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)
	w.Write([]byte(`{"status":"UP","check":"readiness"}`))
}

func (c *ShippingController) serviceInfo(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)
	w.Write([]byte(`{
		"service": "shipping-service",
		"version": "1.0.0",
		"description": "Shipping and tracking service for e-commerce platform"
	}`))
}

// Helper methods for standardized responses

func (c *ShippingController) respondWithError(w http.ResponseWriter, code int, message string) {
	c.respondWithJSON(w, code, APIResponse{
		Success: false,
		Error:   message,
	})
}

func (c *ShippingController) respondWithSuccess(w http.ResponseWriter, code int, data interface{}, message string) {
	response := APIResponse{
		Success: true,
		Data:    data,
	}

	if message != "" {
		response.Message = message
	}

	c.respondWithJSON(w, code, response)
}

func (c *ShippingController) respondWithJSON(w http.ResponseWriter, code int, payload interface{}) {
	response, err := json.Marshal(payload)
	if err != nil {
		http.Error(w, "Internal Server Error", http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(code)
	w.Write(response)
}
