// Shipping-Service/internal/service/kafka/tracking_kafka_service.go
package kafka

import (
	"encoding/json"
	"log"

	"github.com/IBM/sarama"
	"github.com/ZakariaRek/Ecommerce-App/Shipping-Service/internal/events"
	"github.com/ZakariaRek/Ecommerce-App/Shipping-Service/internal/models"
)

// TrackingKafkaService handles kafka operations for tracking updates
type TrackingKafkaService struct {
	producer sarama.AsyncProducer
	topics   map[string]string
	handlers map[events.TrackingEventType][]TrackingEventHandler
}

// NewTrackingKafkaService creates a new TrackingKafkaService
func NewTrackingKafkaService(producer sarama.AsyncProducer, topics map[string]string) *TrackingKafkaService {
	return &TrackingKafkaService{
		producer: producer,
		topics:   topics,
		handlers: make(map[events.TrackingEventType][]TrackingEventHandler),
	}
}

// PublishEvent publishes a tracking event to kafka
func (s *TrackingKafkaService) PublishEvent(event *events.TrackingEvent, topic string) error {
	data, err := json.Marshal(event)
	if err != nil {
		return err
	}

	s.producer.Input() <- &sarama.ProducerMessage{
		Topic: topic,
		Key:   sarama.StringEncoder(event.TrackingID.String()),
		Value: sarama.ByteEncoder(data),
	}

	return nil
}

// PublishTrackingCreated publishes a tracking created event
func (s *TrackingKafkaService) PublishTrackingCreated(tracking *models.ShipmentTracking) error {
	trackingMap, err := structToMap(tracking)
	if err != nil {
		return err
	}

	event := events.NewTrackingEvent(events.TrackingCreated, tracking.ID, tracking.ShippingID, trackingMap)
	return s.PublishEvent(event, s.topics["created"])
}

// PublishTrackingUpdated publishes a tracking updated event
func (s *TrackingKafkaService) PublishTrackingUpdated(tracking *models.ShipmentTracking) error {
	trackingMap, err := structToMap(tracking)
	if err != nil {
		return err
	}

	event := events.NewTrackingEvent(events.TrackingUpdated, tracking.ID, tracking.ShippingID, trackingMap)
	return s.PublishEvent(event, s.topics["updated"])
}

// PublishTrackingDeleted publishes a tracking deleted event
func (s *TrackingKafkaService) PublishTrackingDeleted(tracking *models.ShipmentTracking) error {
	trackingMap, err := structToMap(tracking)
	if err != nil {
		return err
	}

	event := events.NewTrackingEvent(events.TrackingDeleted, tracking.ID, tracking.ShippingID, trackingMap)
	return s.PublishEvent(event, s.topics["deleted"])
}

// RegisterHandler registers a handler for a specific tracking event type
func (s *TrackingKafkaService) RegisterHandler(eventType events.TrackingEventType, handler TrackingEventHandler) {
	s.handlers[eventType] = append(s.handlers[eventType], handler)
}

// HandleEvent processes a tracking event
func (s *TrackingKafkaService) HandleEvent(event *events.TrackingEvent) {
	handlers, exists := s.handlers[event.Type]
	if !exists {
		return
	}

	for _, handler := range handlers {
		if err := handler(event); err != nil {
			log.Printf("Error handling tracking event %s: %v", event.Type, err)
		}
	}
}

// Verify interface implementation at compile time
var _ models.KafkaTrackingService = (*TrackingKafkaService)(nil)
