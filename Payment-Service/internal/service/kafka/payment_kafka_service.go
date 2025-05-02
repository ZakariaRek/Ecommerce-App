package kafka

import (
	"encoding/json"
	"log"

	"github.com/IBM/sarama"
	"github.com/ZakariaRek/Ecommerce-App/Payment-Service/internal/events"
	"github.com/ZakariaRek/Ecommerce-App/Payment-Service/internal/models"
)

// PaymentEventHandler defines a function to handle payment events
type PaymentEventHandler func(event *events.PaymentEvent) error

// PaymentKafkaService handles kafka operations for payments
type PaymentKafkaService struct {
	producer sarama.AsyncProducer
	topic    string
	handlers map[events.PaymentEventType][]PaymentEventHandler
}

// NewPaymentKafkaService creates a new PaymentKafkaService
func NewPaymentKafkaService(producer sarama.AsyncProducer, topic string) *PaymentKafkaService {
	return &PaymentKafkaService{
		producer: producer,
		topic:    topic,
		handlers: make(map[events.PaymentEventType][]PaymentEventHandler),
	}
}

// PublishEvent publishes a payment event to kafka
func (s *PaymentKafkaService) PublishEvent(event *events.PaymentEvent) error {
	data, err := json.Marshal(event)
	if err != nil {
		return err
	}

	s.producer.Input() <- &sarama.ProducerMessage{
		Topic: s.topic,
		Key:   sarama.StringEncoder(event.PaymentID.String()),
		Value: sarama.ByteEncoder(data),
	}

	return nil
}

// PublishPaymentCreated publishes a payment created event
func (s *PaymentKafkaService) PublishPaymentCreated(payment *models.Payment) error {
	paymentMap, err := structToMap(payment)
	if err != nil {
		return err
	}

	event := events.NewPaymentEvent(events.PaymentCreated, payment.ID, paymentMap)
	return s.PublishEvent(event)
}

// PublishPaymentUpdated publishes a payment updated event
func (s *PaymentKafkaService) PublishPaymentUpdated(payment *models.Payment) error {
	paymentMap, err := structToMap(payment)
	if err != nil {
		return err
	}

	event := events.NewPaymentEvent(events.PaymentUpdated, payment.ID, paymentMap)
	return s.PublishEvent(event)
}

// PublishPaymentStatusChanged publishes a payment status changed event
func (s *PaymentKafkaService) PublishPaymentStatusChanged(payment *models.Payment, oldStatus models.PaymentStatus) error {
	paymentMap, err := structToMap(payment)
	if err != nil {
		return err
	}

	// Add old status to the data
	paymentMap["old_status"] = oldStatus

	event := events.NewPaymentEvent(events.PaymentStatusChanged, payment.ID, paymentMap)
	return s.PublishEvent(event)
}

// RegisterHandler registers a handler for a specific payment event type
func (s *PaymentKafkaService) RegisterHandler(eventType events.PaymentEventType, handler PaymentEventHandler) {
	s.handlers[eventType] = append(s.handlers[eventType], handler)
}

// HandleEvent processes a payment event
func (s *PaymentKafkaService) HandleEvent(event *events.PaymentEvent) {
	handlers, exists := s.handlers[event.Type]
	if !exists {
		return
	}

	for _, handler := range handlers {
		if err := handler(event); err != nil {
			log.Printf("Error handling payment event %s: %v", event.Type, err)
		}
	}
}
