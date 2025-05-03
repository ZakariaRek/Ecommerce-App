package kafka

import (
	"encoding/json"
	"log"
	"time"

	"github.com/IBM/sarama"
	"github.com/ZakariaRek/Ecommerce-App/Payment-Service/internal/events"
	"github.com/ZakariaRek/Ecommerce-App/Payment-Service/internal/models"
)

// InvoiceEventHandler defines a function to handle invoice events
type InvoiceEventHandler func(event *events.InvoiceEvent) error

// InvoiceKafkaService handles kafka operations for invoices
type InvoiceKafkaService struct {
	producer sarama.AsyncProducer
	topics   map[string]string
	handlers map[events.InvoiceEventType][]InvoiceEventHandler
}

// NewInvoiceKafkaService creates a new InvoiceKafkaService
func NewInvoiceKafkaService(producer sarama.AsyncProducer, topics map[string]string) *InvoiceKafkaService {
	return &InvoiceKafkaService{
		producer: producer,
		topics:   topics,
		handlers: make(map[events.InvoiceEventType][]InvoiceEventHandler),
	}
}

// PublishEvent publishes an invoice event to kafka
func (s *InvoiceKafkaService) PublishEvent(event *events.InvoiceEvent, topic string) error {
	data, err := json.Marshal(event)
	if err != nil {
		return err
	}

	s.producer.Input() <- &sarama.ProducerMessage{
		Topic: topic,
		Key:   sarama.StringEncoder(event.InvoiceID.String()),
		Value: sarama.ByteEncoder(data),
	}

	return nil
}

// PublishInvoiceCreated publishes an invoice created event
func (s *InvoiceKafkaService) PublishInvoiceCreated(invoice *models.Invoice) error {
	invoiceMap, err := structToMap(invoice)
	if err != nil {
		return err
	}

	event := events.NewInvoiceEvent(events.InvoiceCreated, invoice.ID, invoiceMap)
	return s.PublishEvent(event, s.topics[EventCreated])
}

// PublishInvoiceUpdated publishes an invoice updated event
func (s *InvoiceKafkaService) PublishInvoiceUpdated(invoice *models.Invoice) error {
	invoiceMap, err := structToMap(invoice)
	if err != nil {
		return err
	}

	event := events.NewInvoiceEvent(events.InvoiceUpdated, invoice.ID, invoiceMap)
	return s.PublishEvent(event, s.topics[EventUpdated])
}

// PublishInvoiceDueDateChanged publishes an invoice due date changed event
func (s *InvoiceKafkaService) PublishInvoiceDueDateChanged(invoice *models.Invoice, oldDueDate time.Time) error {
	invoiceMap, err := structToMap(invoice)
	if err != nil {
		return err
	}

	// Add old due date to the data
	invoiceMap["old_due_date"] = oldDueDate

	event := events.NewInvoiceEvent(events.InvoiceDueDateChanged, invoice.ID, invoiceMap)
	return s.PublishEvent(event, s.topics[EventChanged])
}

// PublishInvoiceDeleted publishes an invoice deleted event
func (s *InvoiceKafkaService) PublishInvoiceDeleted(invoice *models.Invoice) error {
	invoiceMap, err := structToMap(invoice)
	if err != nil {
		return err
	}

	event := events.NewInvoiceEvent(events.InvoiceDeleted, invoice.ID, invoiceMap)
	return s.PublishEvent(event, s.topics[EventDeleted])
}

// RegisterHandler registers a handler for a specific invoice event type
func (s *InvoiceKafkaService) RegisterHandler(eventType events.InvoiceEventType, handler InvoiceEventHandler) {
	s.handlers[eventType] = append(s.handlers[eventType], handler)
}

// HandleEvent processes an invoice event
func (s *InvoiceKafkaService) HandleEvent(event *events.InvoiceEvent) {
	handlers, exists := s.handlers[event.Type]
	if !exists {
		return
	}

	for _, handler := range handlers {
		if err := handler(event); err != nil {
			log.Printf("Error handling invoice event %s: %v", event.Type, err)
		}
	}
}
