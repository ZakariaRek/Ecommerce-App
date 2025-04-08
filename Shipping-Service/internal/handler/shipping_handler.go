package handler

import (
	"encoding/json"
	"net/http"
	"strconv"

	"github.com/google/uuid"
	"github.com/gorilla/mux"

	"github.com/ZakariaRek/Ecommerce-App/Shipping-Service/internal/models"
	"github.com/ZakariaRek/Ecommerce-App/Shipping-Service/internal/service"
)

// ShippingHandler handles HTTP requests for shipping operations
type ShippingHandler struct {
	service *service.ShippingService
}

// NewShippingHandler creates a new shipping handler
func NewShippingHandler(service *service.ShippingService) *ShippingHandler {
	return &ShippingHandler{service: service}
}

// CreateShipping handles the creation of a new shipping
func (h *ShippingHandler) CreateShipping(w http.ResponseWriter, r *http.Request) {
	var req struct {
		OrderID string `json:"order_id"`
		Carrier string `json:"carrier"`
	}

	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		respondWithError(w, http.StatusBadRequest, "Invalid request body")
		return
	}

	orderID, err := uuid.Parse(req.OrderID)
	if err != nil {
		respondWithError(w, http.StatusBadRequest, "Invalid order ID")
		return
	}

	shipping, err := h.service.CreateShipping(orderID, req.Carrier)
	if err != nil {
		respondWithError(w, http.StatusInternalServerError, err.Error())
		return
	}

	respondWithJSON(w, http.StatusCreated, shipping)
}

// GetShipping handles retrieval of shipping details by ID
func (h *ShippingHandler) GetShipping(w http.ResponseWriter, r *http.Request) {
	vars := mux.Vars(r)
	id, err := uuid.Parse(vars["id"])
	if err != nil {
		respondWithError(w, http.StatusBadRequest, "Invalid shipping ID")
		return
	}

	shipping, err := h.service.GetShipping(id)
	if err != nil {
		respondWithError(w, http.StatusNotFound, err.Error())
		return
	}

	respondWithJSON(w, http.StatusOK, shipping)
}

// GetShippingByOrder handles retrieval of shipping details by order ID
func (h *ShippingHandler) GetShippingByOrder(w http.ResponseWriter, r *http.Request) {
	vars := mux.Vars(r)
	orderID, err := uuid.Parse(vars["order_id"])
	if err != nil {
		respondWithError(w, http.StatusBadRequest, "Invalid order ID")
		return
	}

	shipping, err := h.service.GetShippingByOrder(orderID)
	if err != nil {
		respondWithError(w, http.StatusNotFound, err.Error())
		return
	}

	respondWithJSON(w, http.StatusOK, shipping)
}

// GetAllShippings handles retrieval of all shippings with pagination
func (h *ShippingHandler) GetAllShippings(w http.ResponseWriter, r *http.Request) {
	limitStr := r.URL.Query().Get("limit")
	offsetStr := r.URL.Query().Get("offset")

	limit := 10 // default limit
	if limitStr != "" {
		parsedLimit, err := strconv.Atoi(limitStr)
		if err == nil && parsedLimit > 0 {
			limit = parsedLimit
		}
	}

	offset := 0 // default offset
	if offsetStr != "" {
		parsedOffset, err := strconv.Atoi(offsetStr)
		if err == nil && parsedOffset >= 0 {
			offset = parsedOffset
		}
	}

	shippings, err := h.service.GetAllShippings(limit, offset)
	if err != nil {
		respondWithError(w, http.StatusInternalServerError, err.Error())
		return
	}

	respondWithJSON(w, http.StatusOK, shippings)
}

// UpdateShipping handles updates to shipping details
func (h *ShippingHandler) UpdateShipping(w http.ResponseWriter, r *http.Request) {
	vars := mux.Vars(r)
	id, err := uuid.Parse(vars["id"])
	if err != nil {
		respondWithError(w, http.StatusBadRequest, "Invalid shipping ID")
		return
	}

	var shipping models.Shipping
	if err := json.NewDecoder(r.Body).Decode(&shipping); err != nil {
		respondWithError(w, http.StatusBadRequest, "Invalid request body")
		return
	}

	// Ensure ID in URL matches body
	shipping.ID = id

	if err := h.service.UpdateShipping(&shipping); err != nil {
		respondWithError(w, http.StatusInternalServerError, err.Error())
		return
	}

	respondWithJSON(w, http.StatusOK, shipping)
}

// UpdateStatus handles updates to shipping status
func (h *ShippingHandler) UpdateStatus(w http.ResponseWriter, r *http.Request) {
	vars := mux.Vars(r)
	id, err := uuid.Parse(vars["id"])
	if err != nil {
		respondWithError(w, http.StatusBadRequest, "Invalid shipping ID")
		return
	}

	var req struct {
		Status   string `json:"status"`
		Location string `json:"location"`
		Notes    string `json:"notes"`
	}

	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		respondWithError(w, http.StatusBadRequest, "Invalid request body")
		return
	}

	err = h.service.UpdateStatus(id, models.ShippingStatus(req.Status), req.Location, req.Notes)
	if err != nil {
		respondWithError(w, http.StatusInternalServerError, err.Error())
		return
	}

	respondWithJSON(w, http.StatusOK, map[string]string{"message": "Status updated successfully"})
}

// TrackOrder handles tracking of a shipment
func (h *ShippingHandler) TrackOrder(w http.ResponseWriter, r *http.Request) {
	vars := mux.Vars(r)
	id, err := uuid.Parse(vars["id"])
	if err != nil {
		respondWithError(w, http.StatusBadRequest, "Invalid shipping ID")
		return
	}

	shipping, history, err := h.service.TrackOrder(id)
	if err != nil {
		respondWithError(w, http.StatusNotFound, err.Error())
		return
	}

	response := struct {
		Shipping        *models.Shipping          `json:"shipping"`
		TrackingHistory []models.ShipmentTracking `json:"tracking_history"`
	}{
		Shipping:        shipping,
		TrackingHistory: history,
	}

	respondWithJSON(w, http.StatusOK, response)
}

// Helper functions for HTTP responses
func respondWithError(w http.ResponseWriter, code int, message string) {
	respondWithJSON(w, code, map[string]string{"error": message})
}

func respondWithJSON(w http.ResponseWriter, code int, payload interface{}) {
	response, _ := json.Marshal(payload)
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(code)
	w.Write(response)
}
