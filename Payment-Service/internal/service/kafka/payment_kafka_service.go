// Payment-Service/internal/service/kafka/payment_kafka_service.go - FIXED VERSION
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
	topics   map[string]string
	handlers map[events.PaymentEventType][]PaymentEventHandler
}

// NewPaymentKafkaService creates a new PaymentKafkaService
func NewPaymentKafkaService(producer sarama.AsyncProducer, topics map[string]string) *PaymentKafkaService {
	return &PaymentKafkaService{
		producer: producer,
		topics:   topics,
		handlers: make(map[events.PaymentEventType][]PaymentEventHandler),
	}
}

// PublishEvent publishes a payment event to kafka
func (s *PaymentKafkaService) PublishEvent(event *events.PaymentEvent, topic string) error {
	data, err := json.Marshal(event)
	if err != nil {
		return err
	}

	s.producer.Input() <- &sarama.ProducerMessage{
		Topic: topic,
		Key:   sarama.StringEncoder(event.PaymentID.String()),
		Value: sarama.ByteEncoder(data),
	}

	return nil
}

// FIXED: PublishOrderPaymentEvent - sends message directly in Order Service format
func (s *PaymentKafkaService) PublishOrderPaymentEvent(topic string, eventData map[string]interface{}) error {
	// Convert the event data to JSON
	data, err := json.Marshal(eventData)
	if err != nil {
		log.Printf("💳 PAYMENT SERVICE: Failed to marshal event data: %v", err)
		return err
	}

	// Extract order ID for the key
	orderIDStr := ""
	if orderId, ok := eventData["orderId"].(string); ok {
		orderIDStr = orderId
	}

	// Send message to Kafka
	message := &sarama.ProducerMessage{
		Topic: topic,
		Key:   sarama.StringEncoder(orderIDStr),
		Value: sarama.ByteEncoder(data),
	}

	log.Printf("💳 PAYMENT SERVICE: Sending message to topic '%s' with key '%s'", topic, orderIDStr)
	log.Printf("💳 PAYMENT SERVICE: Message content: %s", string(data))

	s.producer.Input() <- message

	// Wait for confirmation from producer success channel
	go func() {
		select {
		case success := <-s.producer.Successes():
			log.Printf("💳 PAYMENT SERVICE: Successfully sent event to topic '%s' for order %s",
				success.Topic, orderIDStr)
		case err := <-s.producer.Errors():
			log.Printf("💳 PAYMENT SERVICE: Failed to send event to topic '%s': %v",
				topic, err)
		}
	}()

	return nil
}

// PublishPaymentCreated publishes a payment created event
func (s *PaymentKafkaService) PublishPaymentCreated(payment *models.Payment) error {
	paymentMap, err := structToMap(payment)
	if err != nil {
		return err
	}

	event := events.NewPaymentEvent(events.PaymentCreated, payment.ID, paymentMap)
	return s.PublishEvent(event, s.topics[EventCreated])
}

// PublishPaymentUpdated publishes a payment updated event
func (s *PaymentKafkaService) PublishPaymentUpdated(payment *models.Payment) error {
	paymentMap, err := structToMap(payment)
	if err != nil {
		return err
	}

	event := events.NewPaymentEvent(events.PaymentUpdated, payment.ID, paymentMap)
	return s.PublishEvent(event, s.topics[EventUpdated])
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
	return s.PublishEvent(event, s.topics[EventChanged])
}

// PublishPaymentDeleted publishes a payment deleted event
func (s *PaymentKafkaService) PublishPaymentDeleted(payment *models.Payment) error {
	paymentMap, err := structToMap(payment)
	if err != nil {
		return err
	}

	event := events.NewPaymentEvent(events.PaymentDeleted, payment.ID, paymentMap)
	return s.PublishEvent(event, s.topics[EventDeleted])
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
