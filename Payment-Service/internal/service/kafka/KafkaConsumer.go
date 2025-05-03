package kafka

import (
	"context"
	"encoding/json"
	"log"
	"os"
	"os/signal"
	"strings"
	"sync"
	"syscall"

	"github.com/IBM/sarama"
	"github.com/ZakariaRek/Ecommerce-App/Payment-Service/internal/events"
)

// Consumer represents a Kafka consumer
type Consumer struct {
	ready               chan bool
	consumerGroup       sarama.ConsumerGroup
	topics              []string
	paymentHandlers     map[events.PaymentEventType][]PaymentEventHandler
	invoiceHandlers     map[events.InvoiceEventType][]InvoiceEventHandler
	transactionHandlers map[events.TransactionEventType][]TransactionEventHandler
	wg                  sync.WaitGroup
}

// NewConsumer creates a new Kafka consumer
func NewConsumer(brokers []string, groupID string, topics []string) (*Consumer, error) {
	config := sarama.NewConfig()
	config.Consumer.Group.Rebalance.Strategy = sarama.BalanceStrategyRoundRobin
	config.Consumer.Offsets.Initial = sarama.OffsetOldest

	consumerGroup, err := sarama.NewConsumerGroup(brokers, groupID, config)
	if err != nil {
		return nil, err
	}

	consumer := &Consumer{
		ready:               make(chan bool),
		consumerGroup:       consumerGroup,
		topics:              topics,
		paymentHandlers:     make(map[events.PaymentEventType][]PaymentEventHandler),
		invoiceHandlers:     make(map[events.InvoiceEventType][]InvoiceEventHandler),
		transactionHandlers: make(map[events.TransactionEventType][]TransactionEventHandler),
	}

	return consumer, nil
}

// RegisterPaymentEventHandler registers a handler for payment events
func (c *Consumer) RegisterPaymentEventHandler(eventType events.PaymentEventType, handler PaymentEventHandler) {
	c.paymentHandlers[eventType] = append(c.paymentHandlers[eventType], handler)
}

// RegisterInvoiceEventHandler registers a handler for invoice events
func (c *Consumer) RegisterInvoiceEventHandler(eventType events.InvoiceEventType, handler InvoiceEventHandler) {
	c.invoiceHandlers[eventType] = append(c.invoiceHandlers[eventType], handler)
}

// RegisterTransactionEventHandler registers a handler for transaction events
func (c *Consumer) RegisterTransactionEventHandler(eventType events.TransactionEventType, handler TransactionEventHandler) {
	c.transactionHandlers[eventType] = append(c.transactionHandlers[eventType], handler)
}

// Start starts the consumer
func (c *Consumer) Start() {
	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()

	// Track signal interrupts
	signals := make(chan os.Signal, 1)
	signal.Notify(signals, syscall.SIGINT, syscall.SIGTERM)

	c.wg.Add(1)
	go func() {
		defer c.wg.Done()
		for {
			// Join consumer group
			if err := c.consumerGroup.Consume(ctx, c.topics, c); err != nil {
				log.Printf("Error from consumer: %v", err)
			}
			// Check if the context was cancelled, signaling that consumption should stop
			if ctx.Err() != nil {
				log.Println("Consumer context cancelled")
				return
			}
			c.ready = make(chan bool)
		}
	}()

	<-c.ready // Wait until the consumer is ready
	log.Println("Kafka consumer started")

	select {
	case <-signals:
		log.Println("Signal received, shutting down consumer...")
	case <-ctx.Done():
		log.Println("Context cancelled, shutting down consumer...")
	}

	cancel()
	c.wg.Wait()
	if err := c.consumerGroup.Close(); err != nil {
		log.Printf("Error closing consumer group: %v", err)
	}
}

// Setup is run at the beginning of a new session
func (c *Consumer) Setup(session sarama.ConsumerGroupSession) error {
	log.Println("Consumer setup")
	close(c.ready)
	return nil
}

// Cleanup is run at the end of a session
func (c *Consumer) Cleanup(session sarama.ConsumerGroupSession) error {
	log.Println("Consumer cleanup")
	return nil
}

// ConsumeClaim is called for each set of messages from a topic partition
func (c *Consumer) ConsumeClaim(session sarama.ConsumerGroupSession, claim sarama.ConsumerGroupClaim) error {
	for message := range claim.Messages() {
		log.Printf("Message claimed: topic = %s, partition = %d, offset = %d, key = %s",
			message.Topic, message.Partition, message.Offset, string(message.Key))

		// Process message based on topic pattern
		topicLower := strings.ToLower(message.Topic)
		switch {
		case strings.Contains(topicLower, "payment"):
			c.handlePaymentEvent(message.Value)
		case strings.Contains(topicLower, "invoice"):
			c.handleInvoiceEvent(message.Value)
		case strings.Contains(topicLower, "transaction"):
			c.handleTransactionEvent(message.Value)
		}

		session.MarkMessage(message, "")
	}
	return nil
}

// handlePaymentEvent processes payment events
func (c *Consumer) handlePaymentEvent(data []byte) {
	var event events.PaymentEvent
	if err := json.Unmarshal(data, &event); err != nil {
		log.Printf("Error unmarshaling payment event: %v", err)
		return
	}

	handlers, exists := c.paymentHandlers[event.Type]
	if !exists {
		log.Printf("No handlers registered for payment event type: %s", event.Type)
		return
	}

	for _, handler := range handlers {
		if err := handler(&event); err != nil {
			log.Printf("Error handling payment event: %v", err)
		}
	}
}

// handleInvoiceEvent processes invoice events
func (c *Consumer) handleInvoiceEvent(data []byte) {
	var event events.InvoiceEvent
	if err := json.Unmarshal(data, &event); err != nil {
		log.Printf("Error unmarshaling invoice event: %v", err)
		return
	}

	handlers, exists := c.invoiceHandlers[event.Type]
	if !exists {
		log.Printf("No handlers registered for invoice event type: %s", event.Type)
		return
	}

	for _, handler := range handlers {
		if err := handler(&event); err != nil {
			log.Printf("Error handling invoice event: %v", err)
		}
	}
}

// handleTransactionEvent processes transaction events
func (c *Consumer) handleTransactionEvent(data []byte) {
	var event events.TransactionEvent
	if err := json.Unmarshal(data, &event); err != nil {
		log.Printf("Error unmarshaling transaction event: %v", err)
		return
	}

	handlers, exists := c.transactionHandlers[event.Type]
	if !exists {
		log.Printf("No handlers registered for transaction event type: %s", event.Type)
		return
	}

	for _, handler := range handlers {
		if err := handler(&event); err != nil {
			log.Printf("Error handling transaction event: %v", err)
		}
	}
}
