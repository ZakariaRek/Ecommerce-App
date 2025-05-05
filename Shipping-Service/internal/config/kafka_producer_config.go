package config

import (
	"time"

	"github.com/IBM/sarama"
)

// KafkaProducerConfig holds configuration for kafka producers
type KafkaProducerConfig struct {
	Brokers         []string
	Topic           string
	RequiredAcks    sarama.RequiredAcks
	Compression     sarama.CompressionCodec
	FlushFrequency  time.Duration
	FlushMessages   int
	RetryMax        int
	RetryBackoff    time.Duration
	ReturnSuccesses bool
	ReturnErrors    bool
}

// NewDefaultProducerConfig creates a KafkaProducerConfig with sensible defaults
func NewDefaultProducerConfig(brokers []string, topic string) *KafkaProducerConfig {
	return &KafkaProducerConfig{
		Brokers:         brokers,
		Topic:           topic,
		RequiredAcks:    sarama.WaitForLocal,
		Compression:     sarama.CompressionSnappy,
		FlushFrequency:  500 * time.Millisecond,
		FlushMessages:   10,
		RetryMax:        3,
		RetryBackoff:    100 * time.Millisecond,
		ReturnSuccesses: true,
		ReturnErrors:    true,
	}
}

// CreateProducer creates a kafka producer using the provided configuration
func CreateProducer(config *KafkaProducerConfig) (sarama.AsyncProducer, error) {
	saramaConfig := sarama.NewConfig()
	saramaConfig.Producer.RequiredAcks = config.RequiredAcks
	saramaConfig.Producer.Compression = config.Compression
	saramaConfig.Producer.Flush.Frequency = config.FlushFrequency
	saramaConfig.Producer.Flush.Messages = config.FlushMessages
	saramaConfig.Producer.Retry.Max = config.RetryMax
	saramaConfig.Producer.Retry.Backoff = config.RetryBackoff
	saramaConfig.Producer.Return.Successes = config.ReturnSuccesses
	saramaConfig.Producer.Return.Errors = config.ReturnErrors

	producer, err := sarama.NewAsyncProducer(config.Brokers, saramaConfig)
	if err != nil {
		return nil, err
	}

	// Start monitoring the error channel if error handling is enabled
	if config.ReturnErrors {
		go func() {
			for err := range producer.Errors() {
				// Log the error (you can replace this with proper logging)
				println("Failed to produce message:", err.Error())
			}
		}()
	}

	return producer, nil
}
