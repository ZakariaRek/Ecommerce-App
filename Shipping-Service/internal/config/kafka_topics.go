package config

import (
	"log"
	"strings"
)

// Event type constants
const (
	EventCreated = "created"
	EventUpdated = "updated"
	EventChanged = "changed"
	EventDeleted = "deleted"
)

// Entity type constants
const (
	EntityShipping = "shipping"
	EntityTracking = "tracking"
)

// KafkaTopicsConfig holds configuration for Kafka topics
type KafkaTopicsConfig struct {
	Brokers        []string
	ShippingTopics map[string]string
	TrackingTopics map[string]string
	ConsumerGroup  string
	EnableConsumer bool
	ConsumerTopics []string
}

// LoadKafkaTopicsConfig loads Kafka topics configuration from environment
func LoadKafkaTopicsConfig() *KafkaTopicsConfig {
	brokersStr := getEnv("KAFKA_BROKERS", "localhost:9092")
	brokers := strings.Split(brokersStr, ",")

	// Create topic maps based on environment variables or defaults
	// Format follows: {entity}-{event} (e.g., shipping-created, tracking-updated)
	shippingTopics := map[string]string{
		EventCreated: getEnv("KAFKA_SHIPPING_CREATED_TOPIC", "shipping-created"),
		EventUpdated: getEnv("KAFKA_SHIPPING_UPDATED_TOPIC", "shipping-updated"),
		EventChanged: getEnv("KAFKA_SHIPPING_STATUS_CHANGED_TOPIC", "shipping-status-changed"),
		EventDeleted: getEnv("KAFKA_SHIPPING_DELETED_TOPIC", "shipping-deleted"),
	}

	trackingTopics := map[string]string{
		EventCreated: getEnv("KAFKA_TRACKING_CREATED_TOPIC", "tracking-created"),
		EventUpdated: getEnv("KAFKA_TRACKING_UPDATED_TOPIC", "tracking-updated"),
		EventDeleted: getEnv("KAFKA_TRACKING_DELETED_TOPIC", "tracking-deleted"),
	}

	// Generate default consumer topics list
	defaultConsumerTopics := []string{}

	// Add shipping topics
	for _, topic := range shippingTopics {
		defaultConsumerTopics = append(defaultConsumerTopics, topic)
	}

	// Add tracking topics
	for _, topic := range trackingTopics {
		defaultConsumerTopics = append(defaultConsumerTopics, topic)
	}

	// Get consumer topics from environment or use defaults
	consumerTopicsStr := getEnv("KAFKA_CONSUMER_TOPICS", strings.Join(defaultConsumerTopics, ","))

	return &KafkaTopicsConfig{
		Brokers:        brokers,
		ShippingTopics: shippingTopics,
		TrackingTopics: trackingTopics,
		ConsumerGroup:  getEnv("KAFKA_CONSUMER_GROUP", "shipping-service-group"),
		EnableConsumer: getEnvAsBool("KAFKA_ENABLE_CONSUMER", true),
		ConsumerTopics: strings.Split(consumerTopicsStr, ","),
	}
}

// LogTopicConfiguration logs the Kafka topic configuration
func (c *KafkaTopicsConfig) LogTopicConfiguration() {
	log.Println("Kafka Topic Configuration:")
	log.Println("==========================")
	log.Println("Shipping Topics:")
	for event, topic := range c.ShippingTopics {
		log.Printf("  %s: %s", event, topic)
	}

	log.Println("Tracking Topics:")
	for event, topic := range c.TrackingTopics {
		log.Printf("  %s: %s", event, topic)
	}

	log.Printf("Consumer Group: %s", c.ConsumerGroup)
	log.Printf("Enable Consumer: %t", c.EnableConsumer)
	log.Printf("Consumer Topics: %s", strings.Join(c.ConsumerTopics, ", "))
}
