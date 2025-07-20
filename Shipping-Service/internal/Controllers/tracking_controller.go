// internal/controller/tracking_controller.go
package controller

import (
	"encoding/json"
	"github.com/google/uuid"
	"github.com/gorilla/mux"
	"net/http"

	"github.com/ZakariaRek/Ecommerce-App/Shipping-Service/internal/models"
	"github.com/ZakariaRek/Ecommerce-App/Shipping-Service/internal/service"
)

// TrackingController handles HTTP requests for tracking operations
type TrackingController struct {
	trackingService *service.TrackingService
}

// NewTrackingController creates a new tracking controller
func NewTrackingController(trackingService *service.TrackingService) *TrackingController {
	return &TrackingController{
		trackingService: trackingService,
	}
}

// SetupRoutes configures all routes for the tracking controller
func (c *TrackingController) SetupRoutes() *mux.Router {
	router := mux.NewRouter()

	// Add CORS middleware
	router.Use(c.corsMiddleware)

	// Setup all routes
	c.setupTrackingRoutes(router)
	c.setupHealthRoutes(router)

	return router
}

// RegisterRoutes registers tracking routes on an existing router
func (c *TrackingController) RegisterRoutes(router *mux.Router) {
	// Setup tracking routes on the provided router
	c.setupTrackingRoutes(router)
}

// setupTrackingRoutes configures tracking-related routes
func (c *TrackingController) setupTrackingRoutes(router *mux.Router) {
	// API routes
	api := router.PathPrefix("/api/shipping").Subrouter()
	trackingRoutes := api.PathPrefix("/tracking").Subrouter()

	// CRUD operations
	trackingRoutes.HandleFunc("", c.CreateTracking).Methods("POST")
	trackingRoutes.HandleFunc("/{id}", c.GetTracking).Methods("GET")
	trackingRoutes.HandleFunc("/{id}", c.UpdateTracking).Methods("PUT")
	trackingRoutes.HandleFunc("/{id}", c.DeleteTracking).Methods("DELETE")
	trackingRoutes.HandleFunc("/{id}/location", c.UpdateTrackingLocation).Methods("PATCH")

	// Shipping-specific tracking operations
	trackingRoutes.HandleFunc("/shipping/{shipping_id}", c.GetTrackingHistory).Methods("GET")
	trackingRoutes.HandleFunc("/shipping/{shipping_id}", c.AddTrackingUpdate).Methods("POST")
	trackingRoutes.HandleFunc("/shipping/{shipping_id}/latest", c.GetLatestTracking).Methods("GET")

	// Combined operations
	trackingRoutes.HandleFunc("/{id}/details", c.GetTrackingWithShipping).Methods("GET")
}

// setupHealthRoutes configures health and monitoring routes
func (c *TrackingController) setupHealthRoutes(router *mux.Router) {
	router.HandleFunc("/health", c.healthCheck).Methods("GET")
	router.HandleFunc("/health/live", c.livenessCheck).Methods("GET")
	router.HandleFunc("/health/ready", c.readinessCheck).Methods("GET")
	router.HandleFunc("/info", c.serviceInfo).Methods("GET")
}

// CORS middleware
func (c *TrackingController) corsMiddleware(next http.Handler) http.Handler {
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
type CreateTrackingRequest struct {
	ShippingID string `json:"shipping_id" validate:"required"`
	Location   string `json:"location" validate:"required"`
	Status     string `json:"status" validate:"required"`
	Notes      string `json:"notes"`
}

type AddTrackingUpdateRequest struct {
	Location string `json:"location" validate:"required"`
	Status   string `json:"status" validate:"required"`
	Notes    string `json:"notes"`
}

type UpdateTrackingLocationRequest struct {
	Location string `json:"location" validate:"required"`
}

type TrackingWithShippingResponse struct {
	Tracking *models.ShipmentTracking `json:"tracking"`
	Shipping *models.Shipping         `json:"shipping"`
}

type TrackingHistoryResponse struct {
	ShippingID      uuid.UUID                 `json:"shipping_id"`
	TrackingHistory []models.ShipmentTracking `json:"tracking_history"`
	Count           int                       `json:"count"`
}

// Tracking API handlers

// CreateTracking handles the creation of a new tracking record
func (c *TrackingController) CreateTracking(w http.ResponseWriter, r *http.Request) {
	var req CreateTrackingRequest

	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		c.respondWithError(w, http.StatusBadRequest, "Invalid request body")
		return
	}

	// Validate required fields
	if req.ShippingID == "" {
		c.respondWithError(w, http.StatusBadRequest, "shipping_id is required")
		return
	}

	if req.Location == "" {
		c.respondWithError(w, http.StatusBadRequest, "location is required")
		return
	}

	if req.Status == "" {
		c.respondWithError(w, http.StatusBadRequest, "status is required")
		return
	}

	shippingID, err := uuid.Parse(req.ShippingID)
	if err != nil {
		c.respondWithError(w, http.StatusBadRequest, "Invalid shipping ID format")
		return
	}

	tracking, err := c.trackingService.CreateTracking(shippingID, req.Location, req.Status, req.Notes)
	if err != nil {
		c.respondWithError(w, http.StatusInternalServerError, err.Error())
		return
	}

	c.respondWithSuccess(w, http.StatusCreated, tracking, "Tracking record created successfully")
}

// GetTracking handles retrieval of tracking details by ID
func (c *TrackingController) GetTracking(w http.ResponseWriter, r *http.Request) {
	vars := mux.Vars(r)
	id, err := uuid.Parse(vars["id"])
	if err != nil {
		c.respondWithError(w, http.StatusBadRequest, "Invalid tracking ID format")
		return
	}

	tracking, err := c.trackingService.GetTracking(id)
	if err != nil {
		c.respondWithError(w, http.StatusNotFound, err.Error())
		return
	}

	c.respondWithSuccess(w, http.StatusOK, tracking, "")
}

// GetTrackingHistory handles retrieval of tracking history for a shipping
func (c *TrackingController) GetTrackingHistory(w http.ResponseWriter, r *http.Request) {
	vars := mux.Vars(r)
	shippingID, err := uuid.Parse(vars["shipping_id"])
	if err != nil {
		c.respondWithError(w, http.StatusBadRequest, "Invalid shipping ID format")
		return
	}

	trackingHistory, err := c.trackingService.GetTrackingHistory(shippingID)
	if err != nil {
		c.respondWithError(w, http.StatusNotFound, err.Error())
		return
	}

	response := TrackingHistoryResponse{
		ShippingID:      shippingID,
		TrackingHistory: trackingHistory,
		Count:           len(trackingHistory),
	}

	c.respondWithSuccess(w, http.StatusOK, response, "")
}

// UpdateTracking handles updates to tracking details
func (c *TrackingController) UpdateTracking(w http.ResponseWriter, r *http.Request) {
	vars := mux.Vars(r)
	id, err := uuid.Parse(vars["id"])
	if err != nil {
		c.respondWithError(w, http.StatusBadRequest, "Invalid tracking ID format")
		return
	}

	var tracking models.ShipmentTracking
	if err := json.NewDecoder(r.Body).Decode(&tracking); err != nil {
		c.respondWithError(w, http.StatusBadRequest, "Invalid request body")
		return
	}

	// Ensure ID in URL matches body
	tracking.ID = id

	if err := c.trackingService.UpdateTracking(&tracking); err != nil {
		c.respondWithError(w, http.StatusInternalServerError, err.Error())
		return
	}

	c.respondWithSuccess(w, http.StatusOK, tracking, "Tracking record updated successfully")
}

// UpdateTrackingLocation handles updates to tracking location
func (c *TrackingController) UpdateTrackingLocation(w http.ResponseWriter, r *http.Request) {
	vars := mux.Vars(r)
	id, err := uuid.Parse(vars["id"])
	if err != nil {
		c.respondWithError(w, http.StatusBadRequest, "Invalid tracking ID format")
		return
	}

	var req UpdateTrackingLocationRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		c.respondWithError(w, http.StatusBadRequest, "Invalid request body")
		return
	}

	if req.Location == "" {
		c.respondWithError(w, http.StatusBadRequest, "location is required")
		return
	}

	err = c.trackingService.UpdateTrackingLocation(id, req.Location)
	if err != nil {
		c.respondWithError(w, http.StatusInternalServerError, err.Error())
		return
	}

	c.respondWithSuccess(w, http.StatusOK, nil, "Tracking location updated successfully")
}

// AddTrackingUpdate handles adding a new tracking update for existing shipping
func (c *TrackingController) AddTrackingUpdate(w http.ResponseWriter, r *http.Request) {
	vars := mux.Vars(r)
	shippingID, err := uuid.Parse(vars["shipping_id"])
	if err != nil {
		c.respondWithError(w, http.StatusBadRequest, "Invalid shipping ID format")
		return
	}

	var req AddTrackingUpdateRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		c.respondWithError(w, http.StatusBadRequest, "Invalid request body")
		return
	}

	// Validate required fields
	if req.Location == "" {
		c.respondWithError(w, http.StatusBadRequest, "location is required")
		return
	}

	if req.Status == "" {
		c.respondWithError(w, http.StatusBadRequest, "status is required")
		return
	}

	tracking, err := c.trackingService.AddTrackingUpdate(shippingID, req.Location, req.Status, req.Notes)
	if err != nil {
		c.respondWithError(w, http.StatusInternalServerError, err.Error())
		return
	}

	c.respondWithSuccess(w, http.StatusCreated, tracking, "Tracking update added successfully")
}

// GetLatestTracking handles retrieval of the latest tracking record for a shipping
func (c *TrackingController) GetLatestTracking(w http.ResponseWriter, r *http.Request) {
	vars := mux.Vars(r)
	shippingID, err := uuid.Parse(vars["shipping_id"])
	if err != nil {
		c.respondWithError(w, http.StatusBadRequest, "Invalid shipping ID format")
		return
	}

	tracking, err := c.trackingService.GetLatestTracking(shippingID)
	if err != nil {
		c.respondWithError(w, http.StatusNotFound, err.Error())
		return
	}

	c.respondWithSuccess(w, http.StatusOK, tracking, "")
}

// GetTrackingWithShipping handles retrieval of tracking with shipping details
func (c *TrackingController) GetTrackingWithShipping(w http.ResponseWriter, r *http.Request) {
	vars := mux.Vars(r)
	id, err := uuid.Parse(vars["id"])
	if err != nil {
		c.respondWithError(w, http.StatusBadRequest, "Invalid tracking ID format")
		return
	}

	tracking, shipping, err := c.trackingService.GetTrackingWithShipping(id)
	if err != nil {
		c.respondWithError(w, http.StatusNotFound, err.Error())
		return
	}

	response := TrackingWithShippingResponse{
		Tracking: tracking,
		Shipping: shipping,
	}

	c.respondWithSuccess(w, http.StatusOK, response, "")
}

// DeleteTracking handles deletion of a tracking record
func (c *TrackingController) DeleteTracking(w http.ResponseWriter, r *http.Request) {
	vars := mux.Vars(r)
	id, err := uuid.Parse(vars["id"])
	if err != nil {
		c.respondWithError(w, http.StatusBadRequest, "Invalid tracking ID format")
		return
	}

	err = c.trackingService.DeleteTracking(id)
	if err != nil {
		c.respondWithError(w, http.StatusInternalServerError, err.Error())
		return
	}

	c.respondWithSuccess(w, http.StatusOK, nil, "Tracking record deleted successfully")
}

// Health check handlers

func (c *TrackingController) healthCheck(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)
	w.Write([]byte(`{
		"status": "UP",
		"service": "tracking-service",
		"checks": {
			"database": "UP",
			"kafka": "UP"
		}
	}`))
}

func (c *TrackingController) livenessCheck(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)
	w.Write([]byte(`{"status":"UP","check":"liveness"}`))
}

func (c *TrackingController) readinessCheck(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)
	w.Write([]byte(`{"status":"UP","check":"readiness"}`))
}

func (c *TrackingController) serviceInfo(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)
	w.Write([]byte(`{
		"service": "tracking-service",
		"version": "1.0.0",
		"description": "Shipment tracking service for e-commerce platform"
	}`))
}

// Helper methods for standardized responses (reusing the same structure)

func (c *TrackingController) respondWithError(w http.ResponseWriter, code int, message string) {
	c.respondWithJSON(w, code, APIResponse{
		Success: false,
		Error:   message,
	})
}

func (c *TrackingController) respondWithSuccess(w http.ResponseWriter, code int, data interface{}, message string) {
	response := APIResponse{
		Success: true,
		Data:    data,
	}

	if message != "" {
		response.Message = message
	}

	c.respondWithJSON(w, code, response)
}

func (c *TrackingController) respondWithJSON(w http.ResponseWriter, code int, payload interface{}) {
	response, err := json.Marshal(payload)
	if err != nil {
		http.Error(w, "Internal Server Error", http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(code)
	w.Write(response)
}
