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
	"github.com/ZakariaRek/Ecommerce-App/Shipping-Service/internal/events"
)

// Consumer represents a Kafka consumer
type Consumer struct {
	ready            chan bool
	consumerGroup    sarama.ConsumerGroup
	topics           []string
	shippingHandlers map[events.ShippingEventType][]ShippingEventHandler
	trackingHandlers map[events.TrackingEventType][]TrackingEventHandler
	wg               sync.WaitGroup
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
		ready:            make(chan bool),
		consumerGroup:    consumerGroup,
		topics:           topics,
		shippingHandlers: make(map[events.ShippingEventType][]ShippingEventHandler),
		trackingHandlers: make(map[events.TrackingEventType][]TrackingEventHandler),
	}

	return consumer, nil
}

// ShippingEventHandler defines a function to handle shipping events
type ShippingEventHandler func(event *events.ShippingEvent) error

// TrackingEventHandler defines a function to handle tracking events
type TrackingEventHandler func(event *events.TrackingEvent) error

// RegisterShippingEventHandler registers a handler for shipping events
func (c *Consumer) RegisterShippingEventHandler(eventType events.ShippingEventType, handler ShippingEventHandler) {
	c.shippingHandlers[eventType] = append(c.shippingHandlers[eventType], handler)
}

// RegisterTrackingEventHandler registers a handler for tracking events
func (c *Consumer) RegisterTrackingEventHandler(eventType events.TrackingEventType, handler TrackingEventHandler) {
	c.trackingHandlers[eventType] = append(c.trackingHandlers[eventType], handler)
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
		case strings.Contains(topicLower, "shipping"):
			c.handleShippingEvent(message.Value)
		case strings.Contains(topicLower, "tracking"):
			c.handleTrackingEvent(message.Value)
		}

		session.MarkMessage(message, "")
	}
	return nil
}

// handleShippingEvent processes shipping events
func (c *Consumer) handleShippingEvent(data []byte) {
	var event events.ShippingEvent
	if err := json.Unmarshal(data, &event); err != nil {
		log.Printf("Error unmarshaling shipping event: %v", err)
		return
	}

	handlers, exists := c.shippingHandlers[event.Type]
	if !exists {
		log.Printf("No handlers registered for shipping event type: %s", event.Type)
		return
	}

	for _, handler := range handlers {
		if err := handler(&event); err != nil {
			log.Printf("Error handling shipping event: %v", err)
		}
	}
}

// handleTrackingEvent processes tracking events
func (c *Consumer) handleTrackingEvent(data []byte) {
	var event events.TrackingEvent
	if err := json.Unmarshal(data, &event); err != nil {
		log.Printf("Error unmarshaling tracking event: %v", err)
		return
	}

	handlers, exists := c.trackingHandlers[event.Type]
	if !exists {
		log.Printf("No handlers registered for tracking event type: %s", event.Type)
		return
	}

	for _, handler := range handlers {
		if err := handler(&event); err != nil {
			log.Printf("Error handling tracking event: %v", err)
		}
	}
}
