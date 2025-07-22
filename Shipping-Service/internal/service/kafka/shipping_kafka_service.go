// Replace your shipping_kafka_service.go with this simplified version
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
	log.Printf("🔧 Creating ShippingKafkaService with topics: %+v", topics)

	service := &ShippingKafkaService{
		producer: producer,
		topics:   topics,
		handlers: make(map[events.ShippingEventType][]ShippingEventHandler),
	}

	log.Printf("✅ ShippingKafkaService created successfully")
	return service
}

// PublishEvent publishes a shipping event to kafka (simple version)
func (s *ShippingKafkaService) PublishEvent(event *events.ShippingEvent, topic string) error {
	log.Printf("📤 PublishEvent called - Event: %s, Topic: %s, ShippingID: %s", event.Type, topic, event.ShippingID)

	if s.producer == nil {
		log.Printf("❌ Kafka producer is nil!")
		return fmt.Errorf("kafka producer is nil")
	}

	data, err := json.Marshal(event)
	if err != nil {
		log.Printf("❌ Failed to marshal event: %v", err)
		return err
	}

	message := &sarama.ProducerMessage{
		Topic: topic,
		Key:   sarama.StringEncoder(event.ShippingID.String()),
		Value: sarama.ByteEncoder(data),
	}

	log.Printf("📤 Sending message to topic '%s' with key '%s'", topic, event.ShippingID.String())

	// Send to producer (fire and forget - let the monitoring goroutine handle success/errors)
	s.producer.Input() <- message

	log.Printf("✅ Message queued for topic '%s'", topic)
	return nil
}

// PublishShippingCreated publishes a shipping created event
func (s *ShippingKafkaService) PublishShippingCreated(shipping *models.Shipping) error {
	log.Printf("📤 PublishShippingCreated called for ID: %s", shipping.ID)

	shippingMap, err := structToMap(shipping)
	if err != nil {
		log.Printf("❌ Failed to convert shipping to map: %v", err)
		return err
	}

	event := events.NewShippingEvent(events.ShippingCreated, shipping.ID, shippingMap)
	topic := s.topics["created"]

	log.Printf("📤 Publishing to topic: %s", topic)
	return s.PublishEvent(event, topic)
}

// PublishShippingUpdated publishes a shipping updated event
func (s *ShippingKafkaService) PublishShippingUpdated(shipping *models.Shipping) error {
	log.Printf("📤 PublishShippingUpdated called for ID: %s", shipping.ID)

	shippingMap, err := structToMap(shipping)
	if err != nil {
		log.Printf("❌ Failed to convert shipping to map: %v", err)
		return err
	}

	event := events.NewShippingEvent(events.ShippingUpdated, shipping.ID, shippingMap)
	topic := s.topics["updated"]

	log.Printf("📤 Publishing to topic: %s", topic)
	return s.PublishEvent(event, topic)
}

// PublishShippingStatusChanged publishes a shipping status changed event
func (s *ShippingKafkaService) PublishShippingStatusChanged(shipping *models.Shipping, oldStatus models.ShippingStatus) error {
	log.Printf("📤 PublishShippingStatusChanged called for ID: %s, Old: %s, New: %s",
		shipping.ID, oldStatus, shipping.Status)

	shippingMap, err := structToMap(shipping)
	if err != nil {
		log.Printf("❌ Failed to convert shipping to map: %v", err)
		return err
	}

	// Add old status to the data
	shippingMap["old_status"] = oldStatus.String()

	event := events.NewShippingEvent(events.ShippingStatusChanged, shipping.ID, shippingMap)
	topic := s.topics["changed"]

	log.Printf("📤 Publishing status changed to topic: %s", topic)
	// Publish to the standard topic
	if err := s.PublishEvent(event, topic); err != nil {
		log.Printf("❌ Failed to publish to standard topic: %v", err)
		return err
	}

	log.Printf("📤 Publishing to order service topic...")
	// Also publish to the shipping-update topic for order service compatibility
	return s.PublishShippingUpdateForOrderService(shipping, oldStatus)
}

// PublishShippingUpdateForOrderService publishes a shipping update event specifically for the order service
func (s *ShippingKafkaService) PublishShippingUpdateForOrderService(shipping *models.Shipping, oldStatus models.ShippingStatus) error {
	log.Printf("📤 PublishShippingUpdateForOrderService called")
	log.Printf("   OrderID: %s", shipping.OrderID)
	log.Printf("   ShippingID: %s", shipping.ID)
	log.Printf("   Status: %s", shipping.Status)
	log.Printf("   Carrier: %s", shipping.Carrier)
	log.Printf("   TrackingNumber: %s", shipping.TrackingNumber)

	if s.producer == nil {
		log.Printf("❌ Kafka producer is nil!")
		return fmt.Errorf("kafka producer is nil")
	}

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
		log.Printf("   EstimatedDelivery: %s", estimatedStr)
	}

	if shipping.ShippedDate != nil {
		shippedStr := shipping.ShippedDate.Format("2006-01-02T15:04:05Z07:00")
		updateEvent.ShippedDate = &shippedStr
		log.Printf("   ShippedDate: %s", shippedStr)
	}

	if shipping.DeliveredDate != nil {
		deliveredStr := shipping.DeliveredDate.Format("2006-01-02T15:04:05Z07:00")
		updateEvent.DeliveredDate = &deliveredStr
		log.Printf("   DeliveredDate: %s", deliveredStr)
	}

	// Add current location if available
	if shipping.CurrentLatitude != nil && shipping.CurrentLongitude != nil {
		updateEvent.CurrentLocation = fmt.Sprintf("%.6f,%.6f", *shipping.CurrentLatitude, *shipping.CurrentLongitude)
		log.Printf("   CurrentLocation: %s", updateEvent.CurrentLocation)
	}

	// Marshal the event
	data, err := json.Marshal(updateEvent)
	if err != nil {
		log.Printf("❌ Error marshaling shipping update event: %v", err)
		return err
	}

	log.Printf("📦 Order Service Event Data: %s", string(data))

	// Send to shipping-update topic (the one order service listens to)
	topic := "shipping-update"
	message := &sarama.ProducerMessage{
		Topic: topic,
		Key:   sarama.StringEncoder(shipping.OrderID.String()), // Use order ID as key
		Value: sarama.ByteEncoder(data),
	}

	log.Printf("📤 Sending to ORDER SERVICE topic '%s' with key '%s'", topic, shipping.OrderID.String())
	s.producer.Input() <- message

	log.Printf("📦 SHIPPING SERVICE: Published shipping update event for order %s, new status: %s",
		shipping.OrderID.String(), shipping.Status.String())

	return nil
}

// PublishShippingDeleted publishes a shipping deleted event
func (s *ShippingKafkaService) PublishShippingDeleted(shipping *models.Shipping) error {
	log.Printf("📤 PublishShippingDeleted called for ID: %s", shipping.ID)

	shippingMap, err := structToMap(shipping)
	if err != nil {
		log.Printf("❌ Failed to convert shipping to map: %v", err)
		return err
	}

	event := events.NewShippingEvent(events.ShippingDeleted, shipping.ID, shippingMap)
	topic := s.topics["deleted"]

	log.Printf("📤 Publishing to topic: %s", topic)
	return s.PublishEvent(event, topic)
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

// Verify interface implementation at compile time
var _ models.KafkaShippingService = (*ShippingKafkaService)(nil)
