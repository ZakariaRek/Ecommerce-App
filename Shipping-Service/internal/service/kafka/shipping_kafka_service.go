// internal/service/kafka/shipping_kafka_service.go
package kafka

import (
	"encoding/json"
	"fmt"
	"log"

	"github.com/IBM/sarama"
	"github.com/ZakariaRek/Ecommerce-App/Shipping-Service/internal/events"
	"github.com/ZakariaRek/Ecommerce-App/Shipping-Service/internal/models"
)

// ShippingKafkaService handles kafka operations for shippings
type ShippingKafkaService struct {
	producer sarama.AsyncProducer
	topics   map[string]string
	handlers map[events.ShippingEventType][]ShippingEventHandler
}

// ShippingUpdateEvent represents a shipping update event for order service
type ShippingUpdateEvent struct {
	OrderID           string  `json:"orderId"`
	ShippingID        string  `json:"shippingId"`
	Status            string  `json:"status"`
	TrackingNumber    string  `json:"trackingNumber,omitempty"`
	Carrier           string  `json:"carrier,omitempty"`
	EstimatedDelivery *string `json:"estimatedDelivery,omitempty"`
	ShippedDate       *string `json:"shippedDate,omitempty"`
	DeliveredDate     *string `json:"deliveredDate,omitempty"`
	CurrentLocation   string  `json:"currentLocation,omitempty"`
	Timestamp         string  `json:"timestamp"`
}

// NewShippingKafkaService creates a new ShippingKafkaService
func NewShippingKafkaService(producer sarama.AsyncProducer, topics map[string]string) *ShippingKafkaService {
	return &ShippingKafkaService{
		producer: producer,
		topics:   topics,
		handlers: make(map[events.ShippingEventType][]ShippingEventHandler),
	}
}

// PublishEvent publishes a shipping event to kafka
func (s *ShippingKafkaService) PublishEvent(event *events.ShippingEvent, topic string) error {
	data, err := json.Marshal(event)
	if err != nil {
		return err
	}

	s.producer.Input() <- &sarama.ProducerMessage{
		Topic: topic,
		Key:   sarama.StringEncoder(event.ShippingID.String()),
		Value: sarama.ByteEncoder(data),
	}

	return nil
}

// PublishShippingCreated publishes a shipping created event
func (s *ShippingKafkaService) PublishShippingCreated(shipping *models.Shipping) error {
	shippingMap, err := structToMap(shipping)
	if err != nil {
		return err
	}

	event := events.NewShippingEvent(events.ShippingCreated, shipping.ID, shippingMap)
	return s.PublishEvent(event, s.topics["created"])
}

// PublishShippingUpdated publishes a shipping updated event
func (s *ShippingKafkaService) PublishShippingUpdated(shipping *models.Shipping) error {
	shippingMap, err := structToMap(shipping)
	if err != nil {
		return err
	}

	event := events.NewShippingEvent(events.ShippingUpdated, shipping.ID, shippingMap)
	return s.PublishEvent(event, s.topics["updated"])
}

// PublishShippingStatusChanged publishes a shipping status changed event
func (s *ShippingKafkaService) PublishShippingStatusChanged(shipping *models.Shipping, oldStatus models.ShippingStatus) error {
	shippingMap, err := structToMap(shipping)
	if err != nil {
		return err
	}

	// Add old status to the data
	shippingMap["old_status"] = oldStatus.String()

	event := events.NewShippingEvent(events.ShippingStatusChanged, shipping.ID, shippingMap)

	// Publish to the standard topic
	if err := s.PublishEvent(event, s.topics["changed"]); err != nil {
		return err
	}

	// Also publish to the shipping-update topic for order service compatibility
	return s.PublishShippingUpdateForOrderService(shipping, oldStatus)
}

// PublishShippingUpdateForOrderService publishes a shipping update event specifically for the order service
func (s *ShippingKafkaService) PublishShippingUpdateForOrderService(shipping *models.Shipping, oldStatus models.ShippingStatus) error {
	// Create the shipping update event in the format expected by order service
	updateEvent := ShippingUpdateEvent{
		OrderID:        shipping.OrderID.String(),
		ShippingID:     shipping.ID.String(),
		Status:         shipping.Status.String(),
		TrackingNumber: shipping.TrackingNumber,
		Carrier:        shipping.Carrier,
		Timestamp:      shipping.UpdatedAt.Format("2006-01-02T15:04:05Z07:00"),
	}

	// Add optional fields if they exist
	if shipping.EstimatedDelivery != nil {
		estimatedStr := shipping.EstimatedDelivery.Format("2006-01-02T15:04:05Z07:00")
		updateEvent.EstimatedDelivery = &estimatedStr
	}

	if shipping.ShippedDate != nil {
		shippedStr := shipping.ShippedDate.Format("2006-01-02T15:04:05Z07:00")
		updateEvent.ShippedDate = &shippedStr
	}

	if shipping.DeliveredDate != nil {
		deliveredStr := shipping.DeliveredDate.Format("2006-01-02T15:04:05Z07:00")
		updateEvent.DeliveredDate = &deliveredStr
	}

	// Add current location if available
	if shipping.CurrentLatitude != nil && shipping.CurrentLongitude != nil {
		updateEvent.CurrentLocation = fmt.Sprintf("%.6f,%.6f", *shipping.CurrentLatitude, *shipping.CurrentLongitude)
	}

	// Marshal the event
	data, err := json.Marshal(updateEvent)
	if err != nil {
		log.Printf("Error marshaling shipping update event: %v", err)
		return err
	}

	// Send to shipping-update topic (the one order service listens to)
	message := &sarama.ProducerMessage{
		Topic: "shipping-update",                               // Topic that order service listens to
		Key:   sarama.StringEncoder(shipping.OrderID.String()), // Use order ID as key
		Value: sarama.ByteEncoder(data),
	}

	s.producer.Input() <- message

	log.Printf("📦 SHIPPING SERVICE: Published shipping update event for order %s, new status: %s",
		shipping.OrderID.String(), shipping.Status.String())

	return nil
}

// PublishShippingDeleted publishes a shipping deleted event
func (s *ShippingKafkaService) PublishShippingDeleted(shipping *models.Shipping) error {
	shippingMap, err := structToMap(shipping)
	if err != nil {
		return err
	}

	event := events.NewShippingEvent(events.ShippingDeleted, shipping.ID, shippingMap)
	return s.PublishEvent(event, s.topics["deleted"])
}

// RegisterHandler registers a handler for a specific shipping event type
func (s *ShippingKafkaService) RegisterHandler(eventType events.ShippingEventType, handler ShippingEventHandler) {
	s.handlers[eventType] = append(s.handlers[eventType], handler)
}

// HandleEvent processes a shipping event
func (s *ShippingKafkaService) HandleEvent(event *events.ShippingEvent) {
	handlers, exists := s.handlers[event.Type]
	if !exists {
		return
	}

	for _, handler := range handlers {
		if err := handler(event); err != nil {
			log.Printf("Error handling shipping event %s: %v", event.Type, err)
		}
	}
}
