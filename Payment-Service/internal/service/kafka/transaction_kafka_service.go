package kafka

import (
	"encoding/json"
	"log"

	"github.com/IBM/sarama"
	"github.com/ZakariaRek/Ecommerce-App/Payment-Service/internal/events"
	"github.com/ZakariaRek/Ecommerce-App/Payment-Service/internal/models"
)

// TransactionEventHandler defines a function to handle transaction events
type TransactionEventHandler func(event *events.TransactionEvent) error

// TransactionKafkaService handles kafka operations for transactions
type TransactionKafkaService struct {
	producer sarama.AsyncProducer
	topic    string
	handlers map[events.TransactionEventType][]TransactionEventHandler
}

// NewTransactionKafkaService creates a new TransactionKafkaService
func NewTransactionKafkaService(producer sarama.AsyncProducer, topic string) *TransactionKafkaService {
	return &TransactionKafkaService{
		producer: producer,
		topic:    topic,
		handlers: make(map[events.TransactionEventType][]TransactionEventHandler),
	}
}

// PublishEvent publishes a transaction event to kafka
func (s *TransactionKafkaService) PublishEvent(event *events.TransactionEvent) error {
	data, err := json.Marshal(event)
	if err != nil {
		return err
	}

	s.producer.Input() <- &sarama.ProducerMessage{
		Topic: s.topic,
		Key:   sarama.StringEncoder(event.TransactionID.String()),
		Value: sarama.ByteEncoder(data),
	}

	return nil
}

// PublishTransactionCreated publishes a transaction created event
func (s *TransactionKafkaService) PublishTransactionCreated(transaction *models.PaymentTransaction) error {
	transactionMap, err := structToMap(transaction)
	if err != nil {
		return err
	}

	event := events.NewTransactionEvent(events.TransactionCreated, transaction.ID, transactionMap)
	return s.PublishEvent(event)
}

// PublishTransactionUpdated publishes a transaction updated event
func (s *TransactionKafkaService) PublishTransactionUpdated(transaction *models.PaymentTransaction) error {
	transactionMap, err := structToMap(transaction)
	if err != nil {
		return err
	}

	event := events.NewTransactionEvent(events.TransactionUpdated, transaction.ID, transactionMap)
	return s.PublishEvent(event)
}

// PublishTransactionStatusChanged publishes a transaction status changed event
func (s *TransactionKafkaService) PublishTransactionStatusChanged(transaction *models.PaymentTransaction, oldStatus string) error {
	transactionMap, err := structToMap(transaction)
	if err != nil {
		return err
	}

	// Add old status to the data
	transactionMap["old_status"] = oldStatus

	event := events.NewTransactionEvent(events.TransactionStatusChanged, transaction.ID, transactionMap)
	return s.PublishEvent(event)
}

// RegisterHandler registers a handler for a specific transaction event type
func (s *TransactionKafkaService) RegisterHandler(eventType events.TransactionEventType, handler TransactionEventHandler) {
	s.handlers[eventType] = append(s.handlers[eventType], handler)
}

// HandleEvent processes a transaction event
func (s *TransactionKafkaService) HandleEvent(event *events.TransactionEvent) {
	handlers, exists := s.handlers[event.Type]
	if !exists {
		return
	}

	for _, handler := range handlers {
		if err := handler(event); err != nil {
			log.Printf("Error handling transaction event %s: %v", event.Type, err)
		}
	}
}
