package kafka

import (
	"encoding/json"
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
	return s.PublishEvent(event, s.topics["changed"])
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
