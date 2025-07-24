// internal/controller/shipping_controller.go - Updated with User ID parsing
package controller

import (
	"encoding/json"
	"net/http"
	"regexp"
	"strconv"
	"strings"

	"github.com/google/uuid"
	"github.com/gorilla/mux"

	"github.com/ZakariaRek/Ecommerce-App/Shipping-Service/internal/models"
	"github.com/ZakariaRek/Ecommerce-App/Shipping-Service/internal/service"
)

// ShippingController handles HTTP requests for shipping operations
type ShippingController struct {
	shippingService *service.ShippingService
	addressService  *service.AddressService
}

// NewShippingController creates a new shipping controller
func NewShippingController(shippingService *service.ShippingService, addressService *service.AddressService) *ShippingController {
	return &ShippingController{
		shippingService: shippingService,
		addressService:  addressService,
	}
}

// SetupRoutes configures all routes for the shipping controller
func (c *ShippingController) SetupRoutes() *mux.Router {
	router := mux.NewRouter()

	// Add CORS middleware
	router.Use(c.corsMiddleware)

	// Setup all routes
	c.setupShippingRoutes(router)
	c.setupAddressRoutes(router)
	c.setupHealthRoutes(router)

	return router
}

// RegisterRoutes registers shipping routes on an existing router
func (c *ShippingController) RegisterRoutes(router *mux.Router) {
	// Setup shipping routes on the provided router
	c.setupShippingRoutes(router)
	c.setupAddressRoutes(router)
}

// setupShippingRoutes configures shipping-related routes
func (c *ShippingController) setupShippingRoutes(router *mux.Router) {
	// API routes
	api := router.PathPrefix("/api").Subrouter()
	shippingRoutes := api.PathPrefix("/shipping").Subrouter()

	// CRUD operations
	shippingRoutes.HandleFunc("", c.CreateShipping).Methods("POST")
	shippingRoutes.HandleFunc("/with-address", c.CreateShippingWithAddress).Methods("POST")
	shippingRoutes.HandleFunc("", c.GetAllShippings).Methods("GET")
	shippingRoutes.HandleFunc("/{id}", c.GetShipping).Methods("GET")
	shippingRoutes.HandleFunc("/{id}", c.UpdateShipping).Methods("PUT")

	// User-specific routes
	shippingRoutes.HandleFunc("/user/{user_id}", c.GetShippingsByUser).Methods("GET")
	shippingRoutes.HandleFunc("/user/{user_id}/stats", c.GetUserShippingStats).Methods("GET")
	shippingRoutes.HandleFunc("/user/{user_id}/in-transit", c.GetUserShippingsInTransit).Methods("GET")
	shippingRoutes.HandleFunc("/user/{user_id}/status/{status}", c.GetShippingsByUserAndStatus).Methods("GET")

	// Status and tracking operations
	shippingRoutes.HandleFunc("/{id}/status", c.UpdateStatus).Methods("PATCH")
	shippingRoutes.HandleFunc("/{id}/status/gps", c.UpdateStatusWithGPS).Methods("PATCH")
	shippingRoutes.HandleFunc("/{id}/track", c.TrackOrder).Methods("GET")
	shippingRoutes.HandleFunc("/{id}/cost", c.GetShippingCost).Methods("GET")
	shippingRoutes.HandleFunc("/{id}/location", c.UpdateCurrentLocation).Methods("PATCH")
	shippingRoutes.HandleFunc("/{id}/location-update", c.AddLocationUpdate).Methods("POST")
	shippingRoutes.HandleFunc("/{id}/location-history", c.GetLocationHistory).Methods("GET")

	// Order-specific and filtering routes
	shippingRoutes.HandleFunc("/order/{order_id}", c.GetShippingByOrder).Methods("GET")
	shippingRoutes.HandleFunc("/status/{status}", c.GetShippingsByStatus).Methods("GET")
	shippingRoutes.HandleFunc("/in-transit", c.GetShippingsInTransit).Methods("GET")
}

// setupAddressRoutes configures address-related routes
func (c *ShippingController) setupAddressRoutes(router *mux.Router) {
	api := router.PathPrefix("/api").Subrouter()
	addressRoutes := api.PathPrefix("/addresses").Subrouter()

	// CRUD operations for addresses
	addressRoutes.HandleFunc("", c.CreateAddress).Methods("POST")
	addressRoutes.HandleFunc("", c.GetAllAddresses).Methods("GET")
	addressRoutes.HandleFunc("/{id}", c.GetAddress).Methods("GET")
	addressRoutes.HandleFunc("/{id}", c.UpdateAddress).Methods("PUT")
	addressRoutes.HandleFunc("/{id}", c.DeleteAddress).Methods("DELETE")

	// Address search and utilities
	addressRoutes.HandleFunc("/search", c.SearchAddresses).Methods("GET")
	addressRoutes.HandleFunc("/default-origin", c.GetDefaultOriginAddress).Methods("GET")
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

// Helper function to parse user ID to UUID
func (c *ShippingController) parseUserIDToUUID(userIDStr string) (uuid.UUID, error) {
	// Remove any whitespace
	userIDStr = strings.TrimSpace(userIDStr)

	// Check if it's already a valid UUID
	if userID, err := uuid.Parse(userIDStr); err == nil {
		return userID, nil
	}

	// Check if it's a MongoDB ObjectId (24 character hex string)
	if matched, _ := regexp.MatchString("^[0-9a-fA-F]{24}$", userIDStr); matched && len(userIDStr) == 24 {
		// Convert ObjectId to UUID format
		// Pad to 32 characters and format as UUID
		padded := userIDStr + "00000000" // Pad with 8 zeros to make 32 chars
		uuidStr := padded[0:8] + "-" + padded[8:12] + "-4" + padded[13:16] + "-a" + padded[17:20] + "-" + padded[20:32]

		if userID, err := uuid.Parse(uuidStr); err == nil {
			return userID, nil
		}
	}

	// If all else fails, try to create a deterministic UUID from the string
	// Using namespace UUID for consistent conversion
	namespace := uuid.MustParse("6ba7b810-9dad-11d1-80b4-00c04fd430c8") // Standard namespace UUID
	return uuid.NewSHA1(namespace, []byte(userIDStr)), nil
}

// Helper function to parse user ID from request body
func (c *ShippingController) parseUserIDFromRequest(userIDStr string) (uuid.UUID, error) {
	if userIDStr == "" {
		return uuid.Nil, nil // Allow empty user ID in some cases
	}
	return c.parseUserIDToUUID(userIDStr)
}

// Request/Response structs
type CreateShippingRequest struct {
	OrderID           string  `json:"order_id" validate:"required"`
	UserID            string  `json:"user_id,omitempty"` // Changed to string
	Carrier           string  `json:"carrier" validate:"required"`
	ShippingAddressID string  `json:"shipping_address_id,omitempty"`
	Weight            float64 `json:"weight,omitempty"`
	Dimensions        string  `json:"dimensions,omitempty"`
}

type CreateShippingWithAddressRequest struct {
	OrderID         string               `json:"order_id" validate:"required"`
	UserID          string               `json:"user_id,omitempty"` // Changed to string
	Carrier         string               `json:"carrier" validate:"required"`
	ShippingAddress CreateAddressRequest `json:"shipping_address" validate:"required"`
	Weight          float64              `json:"weight,omitempty"`
	Dimensions      string               `json:"dimensions,omitempty"`
}

type CreateAddressRequest struct {
	FirstName    string   `json:"first_name" validate:"required"`
	LastName     string   `json:"last_name" validate:"required"`
	Company      string   `json:"company,omitempty"`
	AddressLine1 string   `json:"address_line1" validate:"required"`
	AddressLine2 string   `json:"address_line2,omitempty"`
	City         string   `json:"city" validate:"required"`
	State        string   `json:"state" validate:"required"`
	PostalCode   string   `json:"postal_code" validate:"required"`
	Country      string   `json:"country" validate:"required"`
	Phone        string   `json:"phone,omitempty"`
	Email        string   `json:"email,omitempty"`
	Latitude     *float64 `json:"latitude,omitempty"`
	Longitude    *float64 `json:"longitude,omitempty"`
}

type UpdateStatusRequest struct {
	Status   string `json:"status" validate:"required"`
	Location string `json:"location"`
	Notes    string `json:"notes"`
}

type UpdateStatusWithGPSRequest struct {
	Status    string   `json:"status" validate:"required"`
	Location  string   `json:"location"`
	Notes     string   `json:"notes"`
	Latitude  *float64 `json:"latitude,omitempty"`
	Longitude *float64 `json:"longitude,omitempty"`
	DeviceID  string   `json:"device_id,omitempty"`
	DriverID  string   `json:"driver_id,omitempty"`
}

type UpdateLocationRequest struct {
	Latitude  float64 `json:"latitude" validate:"required"`
	Longitude float64 `json:"longitude" validate:"required"`
	DeviceID  string  `json:"device_id,omitempty"`
}

type AddLocationUpdateRequest struct {
	DeviceID  string  `json:"device_id" validate:"required"`
	Latitude  float64 `json:"latitude" validate:"required"`
	Longitude float64 `json:"longitude" validate:"required"`
	Speed     float64 `json:"speed,omitempty"`
	Heading   float64 `json:"heading,omitempty"`
	Accuracy  float64 `json:"accuracy,omitempty"`
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

// User-specific API handlers

// GetShippingsByUser handles retrieval of all shippings for a specific user
func (c *ShippingController) GetShippingsByUser(w http.ResponseWriter, r *http.Request) {
	vars := mux.Vars(r)
	userIDStr := vars["user_id"]

	userID, err := c.parseUserIDToUUID(userIDStr)
	if err != nil {
		c.respondWithError(w, http.StatusBadRequest, "Invalid user ID format: "+err.Error())
		return
	}

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

	shippings, err := c.shippingService.GetShippingsByUser(userID, limit, offset)
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
		"user_id": userID,
	}

	c.respondWithSuccess(w, http.StatusOK, response, "")
}

// GetUserShippingStats handles retrieval of shipping statistics for a user
func (c *ShippingController) GetUserShippingStats(w http.ResponseWriter, r *http.Request) {
	vars := mux.Vars(r)
	userIDStr := vars["user_id"]

	userID, err := c.parseUserIDToUUID(userIDStr)
	if err != nil {
		c.respondWithError(w, http.StatusBadRequest, "Invalid user ID format: "+err.Error())
		return
	}

	stats, err := c.shippingService.GetUserShippingStats(userID)
	if err != nil {
		c.respondWithError(w, http.StatusInternalServerError, err.Error())
		return
	}

	c.respondWithSuccess(w, http.StatusOK, stats, "")
}

// GetUserShippingsInTransit handles retrieval of user's in-transit shippings
func (c *ShippingController) GetUserShippingsInTransit(w http.ResponseWriter, r *http.Request) {
	vars := mux.Vars(r)
	userIDStr := vars["user_id"]

	userID, err := c.parseUserIDToUUID(userIDStr)
	if err != nil {
		c.respondWithError(w, http.StatusBadRequest, "Invalid user ID format: "+err.Error())
		return
	}

	shippings, err := c.shippingService.GetUserShippingsInTransit(userID)
	if err != nil {
		c.respondWithError(w, http.StatusInternalServerError, err.Error())
		return
	}

	c.respondWithSuccess(w, http.StatusOK, shippings, "")
}

// GetShippingsByUserAndStatus handles retrieval of user's shippings by status
func (c *ShippingController) GetShippingsByUserAndStatus(w http.ResponseWriter, r *http.Request) {
	vars := mux.Vars(r)
	userIDStr := vars["user_id"]

	userID, err := c.parseUserIDToUUID(userIDStr)
	if err != nil {
		c.respondWithError(w, http.StatusBadRequest, "Invalid user ID format: "+err.Error())
		return
	}

	statusStr := vars["status"]
	status := models.ShippingStatus(statusStr)
	if !status.IsValid() {
		c.respondWithError(w, http.StatusBadRequest, "Invalid shipping status")
		return
	}

	// Parse pagination parameters
	limitStr := r.URL.Query().Get("limit")
	offsetStr := r.URL.Query().Get("offset")

	limit := 10
	offset := 0

	if limitStr != "" {
		if parsedLimit, err := strconv.Atoi(limitStr); err == nil && parsedLimit > 0 {
			limit = parsedLimit
		}
	}

	if offsetStr != "" {
		if parsedOffset, err := strconv.Atoi(offsetStr); err == nil && parsedOffset >= 0 {
			offset = parsedOffset
		}
	}

	shippings, err := c.shippingService.GetShippingsByUserAndStatus(userID, status, limit, offset)
	if err != nil {
		c.respondWithError(w, http.StatusInternalServerError, err.Error())
		return
	}

	response := map[string]interface{}{
		"shippings": shippings,
		"pagination": map[string]interface{}{
			"limit":  limit,
			"offset": offset,
			"count":  len(shippings),
		},
		"user_id": userID,
		"status":  status,
	}

	c.respondWithSuccess(w, http.StatusOK, response, "")
}

// Existing API handlers (updated to support UserID)

// CreateShipping handles the creation of a new shipping (backward compatibility)
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

	// Parse user ID if provided
	var userID uuid.UUID
	if req.UserID != "" {
		userID, err = c.parseUserIDFromRequest(req.UserID)
		if err != nil {
			c.respondWithError(w, http.StatusBadRequest, "Invalid user ID format: "+err.Error())
			return
		}
	}

	// For backward compatibility, create shipping with or without user ID
	var shipping *models.Shipping
	if req.UserID != "" && req.ShippingAddressID != "" {
		shippingAddressID, err := uuid.Parse(req.ShippingAddressID)
		if err != nil {
			c.respondWithError(w, http.StatusBadRequest, "Invalid shipping address ID format")
			return
		}

		shipping, err = c.shippingService.CreateShippingForUser(orderID, userID, req.Carrier, shippingAddressID, req.Weight, req.Dimensions)
		if err != nil {
			c.respondWithError(w, http.StatusInternalServerError, err.Error())
			return
		}
	} else {
		shipping, err = c.shippingService.CreateShipping(orderID, req.Carrier)
		if err != nil {
			c.respondWithError(w, http.StatusInternalServerError, err.Error())
			return
		}
	}

	c.respondWithSuccess(w, http.StatusCreated, shipping, "Shipping created successfully")
}

// CreateShippingWithAddress handles the creation of shipping with a complete address
func (c *ShippingController) CreateShippingWithAddress(w http.ResponseWriter, r *http.Request) {
	var req CreateShippingWithAddressRequest

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

	// Parse user ID (required for this endpoint)
	var userID uuid.UUID
	if req.UserID == "" {
		c.respondWithError(w, http.StatusBadRequest, "user_id is required")
		return
	}

	userID, err = c.parseUserIDFromRequest(req.UserID)
	if err != nil {
		c.respondWithError(w, http.StatusBadRequest, "Invalid user ID format: "+err.Error())
		return
	}

	// Create shipping address first
	addressReq := &service.CreateAddressRequest{
		FirstName:    req.ShippingAddress.FirstName,
		LastName:     req.ShippingAddress.LastName,
		Company:      req.ShippingAddress.Company,
		AddressLine1: req.ShippingAddress.AddressLine1,
		AddressLine2: req.ShippingAddress.AddressLine2,
		City:         req.ShippingAddress.City,
		State:        req.ShippingAddress.State,
		PostalCode:   req.ShippingAddress.PostalCode,
		Country:      req.ShippingAddress.Country,
		Phone:        req.ShippingAddress.Phone,
		Email:        req.ShippingAddress.Email,
		Latitude:     req.ShippingAddress.Latitude,
		Longitude:    req.ShippingAddress.Longitude,
	}

	address, err := c.addressService.CreateAddress(addressReq)
	if err != nil {
		c.respondWithError(w, http.StatusInternalServerError, err.Error())
		return
	}

	// Create shipping with address and user ID
	shippingReq := &service.CreateShippingRequest{
		OrderID:           orderID,
		UserID:            userID,
		Carrier:           req.Carrier,
		ShippingAddressID: address.ID,
		Weight:            req.Weight,
		Dimensions:        req.Dimensions,
	}

	shipping, err := c.shippingService.CreateShippingWithAddress(shippingReq)
	if err != nil {
		c.respondWithError(w, http.StatusInternalServerError, err.Error())
		return
	}

	c.respondWithSuccess(w, http.StatusCreated, shipping, "Shipping with address created successfully")
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

// GetShippingsByStatus handles retrieval of shippings by status
func (c *ShippingController) GetShippingsByStatus(w http.ResponseWriter, r *http.Request) {
	vars := mux.Vars(r)
	statusStr := vars["status"]

	status := models.ShippingStatus(statusStr)
	if !status.IsValid() {
		c.respondWithError(w, http.StatusBadRequest, "Invalid shipping status")
		return
	}

	// Parse query parameters
	limitStr := r.URL.Query().Get("limit")
	offsetStr := r.URL.Query().Get("offset")

	limit := 10
	offset := 0

	if limitStr != "" {
		if parsedLimit, err := strconv.Atoi(limitStr); err == nil && parsedLimit > 0 {
			limit = parsedLimit
		}
	}

	if offsetStr != "" {
		if parsedOffset, err := strconv.Atoi(offsetStr); err == nil && parsedOffset >= 0 {
			offset = parsedOffset
		}
	}

	shippings, err := c.shippingService.GetShippingsByStatus(status, limit, offset)
	if err != nil {
		c.respondWithError(w, http.StatusInternalServerError, err.Error())
		return
	}

	c.respondWithSuccess(w, http.StatusOK, shippings, "")
}

// GetShippingsInTransit handles retrieval of in-transit shippings
func (c *ShippingController) GetShippingsInTransit(w http.ResponseWriter, r *http.Request) {
	shippings, err := c.shippingService.GetShippingsInTransit()
	if err != nil {
		c.respondWithError(w, http.StatusInternalServerError, err.Error())
		return
	}

	c.respondWithSuccess(w, http.StatusOK, shippings, "")
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

// UpdateStatusWithGPS handles updates to shipping status with GPS coordinates
func (c *ShippingController) UpdateStatusWithGPS(w http.ResponseWriter, r *http.Request) {
	vars := mux.Vars(r)
	id, err := uuid.Parse(vars["id"])
	if err != nil {
		c.respondWithError(w, http.StatusBadRequest, "Invalid shipping ID format")
		return
	}

	var req UpdateStatusWithGPSRequest
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

	err = c.shippingService.UpdateStatusWithGPS(id, status, req.Location, req.Notes, req.Latitude, req.Longitude, req.DeviceID, req.DriverID)
	if err != nil {
		c.respondWithError(w, http.StatusInternalServerError, err.Error())
		return
	}

	c.respondWithSuccess(w, http.StatusOK, nil, "Status with GPS updated successfully")
}

// UpdateCurrentLocation handles updates to current GPS location
func (c *ShippingController) UpdateCurrentLocation(w http.ResponseWriter, r *http.Request) {
	vars := mux.Vars(r)
	id, err := uuid.Parse(vars["id"])
	if err != nil {
		c.respondWithError(w, http.StatusBadRequest, "Invalid shipping ID format")
		return
	}

	var req UpdateLocationRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		c.respondWithError(w, http.StatusBadRequest, "Invalid request body")
		return
	}

	err = c.shippingService.UpdateCurrentLocation(id, req.Latitude, req.Longitude)
	if err != nil {
		c.respondWithError(w, http.StatusInternalServerError, err.Error())
		return
	}

	c.respondWithSuccess(w, http.StatusOK, nil, "Current location updated successfully")
}

// AddLocationUpdate handles adding real-time location updates
func (c *ShippingController) AddLocationUpdate(w http.ResponseWriter, r *http.Request) {
	vars := mux.Vars(r)
	id, err := uuid.Parse(vars["id"])
	if err != nil {
		c.respondWithError(w, http.StatusBadRequest, "Invalid shipping ID format")
		return
	}

	var req AddLocationUpdateRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		c.respondWithError(w, http.StatusBadRequest, "Invalid request body")
		return
	}

	err = c.shippingService.AddLocationUpdate(id, req.DeviceID, req.Latitude, req.Longitude, req.Speed, req.Heading, req.Accuracy)
	if err != nil {
		c.respondWithError(w, http.StatusInternalServerError, err.Error())
		return
	}

	c.respondWithSuccess(w, http.StatusCreated, nil, "Location update added successfully")
}

// GetLocationHistory handles retrieval of location update history
func (c *ShippingController) GetLocationHistory(w http.ResponseWriter, r *http.Request) {
	vars := mux.Vars(r)
	id, err := uuid.Parse(vars["id"])
	if err != nil {
		c.respondWithError(w, http.StatusBadRequest, "Invalid shipping ID format")
		return
	}

	limitStr := r.URL.Query().Get("limit")
	limit := 50 // default limit

	if limitStr != "" {
		if parsedLimit, err := strconv.Atoi(limitStr); err == nil && parsedLimit > 0 {
			limit = parsedLimit
		}
	}

	locationHistory, err := c.shippingService.GetLocationHistory(id, limit)
	if err != nil {
		c.respondWithError(w, http.StatusInternalServerError, err.Error())
		return
	}

	c.respondWithSuccess(w, http.StatusOK, locationHistory, "")
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

// Address API handlers

// CreateAddress handles the creation of a new address
func (c *ShippingController) CreateAddress(w http.ResponseWriter, r *http.Request) {
	var req CreateAddressRequest

	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		c.respondWithError(w, http.StatusBadRequest, "Invalid request body")
		return
	}

	addressReq := &service.CreateAddressRequest{
		FirstName:    req.FirstName,
		LastName:     req.LastName,
		Company:      req.Company,
		AddressLine1: req.AddressLine1,
		AddressLine2: req.AddressLine2,
		City:         req.City,
		State:        req.State,
		PostalCode:   req.PostalCode,
		Country:      req.Country,
		Phone:        req.Phone,
		Email:        req.Email,
		Latitude:     req.Latitude,
		Longitude:    req.Longitude,
	}

	address, err := c.addressService.CreateAddress(addressReq)
	if err != nil {
		c.respondWithError(w, http.StatusInternalServerError, err.Error())
		return
	}

	c.respondWithSuccess(w, http.StatusCreated, address, "Address created successfully")
}

// GetAddress handles retrieval of address details by ID
func (c *ShippingController) GetAddress(w http.ResponseWriter, r *http.Request) {
	vars := mux.Vars(r)
	id, err := uuid.Parse(vars["id"])
	if err != nil {
		c.respondWithError(w, http.StatusBadRequest, "Invalid address ID format")
		return
	}

	address, err := c.addressService.GetAddress(id)
	if err != nil {
		c.respondWithError(w, http.StatusNotFound, err.Error())
		return
	}

	c.respondWithSuccess(w, http.StatusOK, address, "")
}

// GetAllAddresses handles retrieval of all addresses with pagination
func (c *ShippingController) GetAllAddresses(w http.ResponseWriter, r *http.Request) {
	limitStr := r.URL.Query().Get("limit")
	offsetStr := r.URL.Query().Get("offset")

	limit := 10
	offset := 0

	if limitStr != "" {
		if parsedLimit, err := strconv.Atoi(limitStr); err == nil && parsedLimit > 0 {
			limit = parsedLimit
		}
	}

	if offsetStr != "" {
		if parsedOffset, err := strconv.Atoi(offsetStr); err == nil && parsedOffset >= 0 {
			offset = parsedOffset
		}
	}

	addresses, err := c.addressService.GetAllAddresses(limit, offset)
	if err != nil {
		c.respondWithError(w, http.StatusInternalServerError, err.Error())
		return
	}

	c.respondWithSuccess(w, http.StatusOK, addresses, "")
}

// UpdateAddress handles updates to address details
func (c *ShippingController) UpdateAddress(w http.ResponseWriter, r *http.Request) {
	vars := mux.Vars(r)
	id, err := uuid.Parse(vars["id"])
	if err != nil {
		c.respondWithError(w, http.StatusBadRequest, "Invalid address ID format")
		return
	}

	var address models.Address
	if err := json.NewDecoder(r.Body).Decode(&address); err != nil {
		c.respondWithError(w, http.StatusBadRequest, "Invalid request body")
		return
	}

	address.ID = id

	if err := c.addressService.UpdateAddress(&address); err != nil {
		c.respondWithError(w, http.StatusInternalServerError, err.Error())
		return
	}

	c.respondWithSuccess(w, http.StatusOK, address, "Address updated successfully")
}

// DeleteAddress handles deletion of an address
func (c *ShippingController) DeleteAddress(w http.ResponseWriter, r *http.Request) {
	vars := mux.Vars(r)
	id, err := uuid.Parse(vars["id"])
	if err != nil {
		c.respondWithError(w, http.StatusBadRequest, "Invalid address ID format")
		return
	}

	err = c.addressService.DeleteAddress(id)
	if err != nil {
		c.respondWithError(w, http.StatusInternalServerError, err.Error())
		return
	}

	c.respondWithSuccess(w, http.StatusOK, nil, "Address deleted successfully")
}

// SearchAddresses handles address search
func (c *ShippingController) SearchAddresses(w http.ResponseWriter, r *http.Request) {
	firstName := r.URL.Query().Get("first_name")
	lastName := r.URL.Query().Get("last_name")
	city := r.URL.Query().Get("city")
	state := r.URL.Query().Get("state")

	addresses, err := c.addressService.SearchAddresses(firstName, lastName, city, state)
	if err != nil {
		c.respondWithError(w, http.StatusInternalServerError, err.Error())
		return
	}

	c.respondWithSuccess(w, http.StatusOK, addresses, "")
}

// GetDefaultOriginAddress handles retrieval of default origin address
func (c *ShippingController) GetDefaultOriginAddress(w http.ResponseWriter, r *http.Request) {
	address, err := c.addressService.GetDefaultOriginAddress()
	if err != nil {
		c.respondWithError(w, http.StatusNotFound, err.Error())
		return
	}

	c.respondWithSuccess(w, http.StatusOK, address, "")
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
		"description": "Enhanced shipping and address service for e-commerce platform with user support"
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
