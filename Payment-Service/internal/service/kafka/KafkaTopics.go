package kafka

import (
	"log"
	"os"
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
	EntityPayment     = "payment"
	EntityInvoice     = "invoice"
	EntityTransaction = "transaction"
)

// KafkaTopicsConfig holds configuration for Kafka topics
type KafkaTopicsConfig struct {
	Brokers           []string
	PaymentTopics     map[string]string
	InvoiceTopics     map[string]string
	TransactionTopics map[string]string
	ConsumerGroup     string
	EnableConsumer    bool
	ConsumerTopics    []string
}

// LoadKafkaTopicsConfig loads Kafka topics configuration from environment
func LoadKafkaTopicsConfig() *KafkaTopicsConfig {
	brokersStr := getEnv("KAFKA_BROKERS", "localhost:9092")
	brokers := strings.Split(brokersStr, ",")

	// Create topic maps based on environment variables or defaults
	// Format follows: {entity}-{event} (e.g., payment-created, invoice-updated)
	paymentTopics := map[string]string{
		EventCreated: getEnv("KAFKA_PAYMENT_CREATED_TOPIC", "payment-created"),
		EventUpdated: getEnv("KAFKA_PAYMENT_UPDATED_TOPIC", "payment-updated"),
		EventChanged: getEnv("KAFKA_PAYMENT_STATUS_CHANGED_TOPIC", "payment-status-changed"),
		EventDeleted: getEnv("KAFKA_PAYMENT_DELETED_TOPIC", "payment-deleted"),
	}

	invoiceTopics := map[string]string{
		EventCreated: getEnv("KAFKA_INVOICE_CREATED_TOPIC", "invoice-created"),
		EventUpdated: getEnv("KAFKA_INVOICE_UPDATED_TOPIC", "invoice-updated"),
		EventChanged: getEnv("KAFKA_INVOICE_DUE_DATE_CHANGED_TOPIC", "invoice-due-date-changed"),
		EventDeleted: getEnv("KAFKA_INVOICE_DELETED_TOPIC", "invoice-deleted"),
	}

	transactionTopics := map[string]string{
		EventCreated: getEnv("KAFKA_TRANSACTION_CREATED_TOPIC", "transaction-created"),
		EventUpdated: getEnv("KAFKA_TRANSACTION_UPDATED_TOPIC", "transaction-updated"),
		EventChanged: getEnv("KAFKA_TRANSACTION_STATUS_CHANGED_TOPIC", "transaction-status-changed"),
		EventDeleted: getEnv("KAFKA_TRANSACTION_DELETED_TOPIC", "transaction-deleted"),
	}

	// Generate default consumer topics list
	defaultConsumerTopics := []string{}

	// Add payment topics
	for _, topic := range paymentTopics {
		defaultConsumerTopics = append(defaultConsumerTopics, topic)
	}

	// Add invoice topics
	for _, topic := range invoiceTopics {
		defaultConsumerTopics = append(defaultConsumerTopics, topic)
	}

	// Add transaction topics
	for _, topic := range transactionTopics {
		defaultConsumerTopics = append(defaultConsumerTopics, topic)
	}

	// Get consumer topics from environment or use defaults
	consumerTopicsStr := getEnv("KAFKA_CONSUMER_TOPICS", strings.Join(defaultConsumerTopics, ","))

	return &KafkaTopicsConfig{
		Brokers:           brokers,
		PaymentTopics:     paymentTopics,
		InvoiceTopics:     invoiceTopics,
		TransactionTopics: transactionTopics,
		ConsumerGroup:     getEnv("KAFKA_CONSUMER_GROUP", "payment-service-group"),
		EnableConsumer:    getEnvAsBool("KAFKA_ENABLE_CONSUMER", true),
		ConsumerTopics:    strings.Split(consumerTopicsStr, ","),
	}
}

// LogTopicConfiguration logs the Kafka topic configuration
func (c *KafkaTopicsConfig) LogTopicConfiguration() {
	log.Println("Kafka Topic Configuration:")
	log.Println("==========================")
	log.Println("Payment Topics:")
	for event, topic := range c.PaymentTopics {
		log.Printf("  %s: %s", event, topic)
	}

	log.Println("Invoice Topics:")
	for event, topic := range c.InvoiceTopics {
		log.Printf("  %s: %s", event, topic)
	}

	log.Println("Transaction Topics:")
	for event, topic := range c.TransactionTopics {
		log.Printf("  %s: %s", event, topic)
	}

	log.Printf("Consumer Group: %s", c.ConsumerGroup)
	log.Printf("Enable Consumer: %t", c.EnableConsumer)
	log.Printf("Consumer Topics: %s", strings.Join(c.ConsumerTopics, ", "))
}

// Helper functions
func getEnv(key, defaultValue string) string {
	if value, exists := os.LookupEnv(key); exists {
		return value
	}
	return defaultValue
}

func getEnvAsBool(key string, defaultValue bool) bool {
	strValue := getEnv(key, "")
	if strValue == "" {
		return defaultValue
	}
	return strValue == "true" || strValue == "1" || strValue == "yes"
}
