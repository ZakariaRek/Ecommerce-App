// internal/logger/elk_logger.go
package logger

import (
	"encoding/json"
	"fmt"
	"os"
	"time"

	"github.com/IBM/sarama"
	"github.com/ZakariaRek/Ecommerce-App/Shipping-Service/internal/config"
	"github.com/sirupsen/logrus"
)

// ELKLogger provides structured logging with ELK Stack integration
type ELKLogger struct {
	*logrus.Logger
	kafkaProducer sarama.AsyncProducer
	serviceName   string
	environment   string
	kafkaTopic    string
}

// LogEntry represents a structured log entry for ELK
type LogEntry struct {
	Timestamp   string                 `json:"@timestamp"`
	Level       string                 `json:"level"`
	Message     string                 `json:"message"`
	ServiceName string                 `json:"service_name"`
	Environment string                 `json:"environment"`
	Logger      string                 `json:"logger"`
	Thread      string                 `json:"thread"`
	Fields      map[string]interface{} `json:"fields,omitempty"`
	StackTrace  string                 `json:"stack_trace,omitempty"`
	TraceID     string                 `json:"trace_id,omitempty"`
	SpanID      string                 `json:"span_id,omitempty"`
}

// KafkaHook sends logs to Kafka for ELK consumption
type KafkaHook struct {
	producer    sarama.AsyncProducer
	topic       string
	serviceName string
	environment string
}

// NewELKLogger creates a new logger with ELK Stack integration
func NewELKLogger(cfg *config.Config) (*ELKLogger, error) {
	logger := logrus.New()

	// Set JSON formatter for structured logging
	logger.SetFormatter(&logrus.JSONFormatter{
		TimestampFormat: "2006-01-02T15:04:05.000Z07:00",
		FieldMap: logrus.FieldMap{
			logrus.FieldKeyTime:  "@timestamp",
			logrus.FieldKeyLevel: "level",
			logrus.FieldKeyMsg:   "message",
		},
	})

	// Set log level based on environment
	if cfg.Environment == "development" {
		logger.SetLevel(logrus.DebugLevel)
	} else {
		logger.SetLevel(logrus.InfoLevel)
	}

	// Create ELK logger instance
	elkLogger := &ELKLogger{
		Logger:      logger,
		serviceName: "shipping-service",
		environment: cfg.Environment,
		kafkaTopic:  getEnv("ELK_KAFKA_TOPIC", "app-logs"),
	}

	// Set up Kafka producer for log shipping if enabled
	enableKafkaLogging := getEnvAsBool("ENABLE_KAFKA_LOGGING", true)
	if enableKafkaLogging && len(cfg.KafkaBrokers) > 0 {
		if err := elkLogger.setupKafkaLogging(cfg.KafkaBrokers); err != nil {
			logger.WithError(err).Warn("Failed to setup Kafka logging, continuing with console logging only")
		} else {
			logger.Info("ELK Kafka logging enabled")
		}
	}

	// Add service-specific fields to all logs
	elkLogger.Logger = elkLogger.Logger.WithFields(logrus.Fields{
		"service_name": elkLogger.serviceName,
		"environment":  elkLogger.environment,
		"version":      getEnv("SERVICE_VERSION", "1.0.0"),
	}).Logger

	return elkLogger, nil
}

// setupKafkaLogging configures Kafka producer for log shipping
func (e *ELKLogger) setupKafkaLogging(brokers []string) error {
	config := sarama.NewConfig()
	config.Producer.Return.Successes = false // Don't wait for acknowledgments for logs
	config.Producer.Return.Errors = true
	config.Producer.Retry.Max = 3
	config.Producer.RequiredAcks = sarama.WaitForLocal
	config.Producer.Compression = sarama.CompressionSnappy
	config.Producer.Flush.Frequency = 1 * time.Second // Batch logs every second

	producer, err := sarama.NewAsyncProducer(brokers, config)
	if err != nil {
		return fmt.Errorf("failed to create Kafka producer for logging: %w", err)
	}

	e.kafkaProducer = producer

	// Monitor producer errors
	go func() {
		for err := range producer.Errors() {
			fmt.Printf("Failed to send log to Kafka: %v\n", err)
		}
	}()

	// Add Kafka hook to logger
	kafkaHook := &KafkaHook{
		producer:    producer,
		topic:       e.kafkaTopic,
		serviceName: e.serviceName,
		environment: e.environment,
	}

	e.Logger.AddHook(kafkaHook)

	return nil
}

// Fire sends log entry to Kafka (implements logrus.Hook interface)
func (hook *KafkaHook) Fire(entry *logrus.Entry) error {
	if hook.producer == nil {
		return nil
	}

	// Create structured log entry for ELK
	logEntry := LogEntry{
		Timestamp:   entry.Time.Format("2006-01-02T15:04:05.000Z07:00"),
		Level:       entry.Level.String(),
		Message:     entry.Message,
		ServiceName: hook.serviceName,
		Environment: hook.environment,
		Logger:      "shipping-service",
		Thread:      "main",
		Fields:      make(map[string]interface{}),
	}

	// Add custom fields
	for key, value := range entry.Data {
		if key != "service_name" && key != "environment" && key != "version" {
			logEntry.Fields[key] = value
		}
	}

	// Handle error stack traces
	if err, ok := entry.Data[logrus.ErrorKey]; ok {
		if errorObj, isError := err.(error); isError {
			logEntry.StackTrace = errorObj.Error()
		}
	}

	// Serialize to JSON
	jsonData, err := json.Marshal(logEntry)
	if err != nil {
		return err
	}

	// Send to Kafka asynchronously
	message := &sarama.ProducerMessage{
		Topic: hook.topic,
		Value: sarama.ByteEncoder(jsonData),
		Key:   sarama.StringEncoder(hook.serviceName),
	}

	select {
	case hook.producer.Input() <- message:
		// Message queued successfully
	default:
		// Producer input channel is full, skip this log entry
		return fmt.Errorf("kafka producer input channel is full")
	}

	return nil
}

// Levels returns the log levels this hook should be fired for
func (hook *KafkaHook) Levels() []logrus.Level {
	return logrus.AllLevels
}

// WithRequestID adds request ID to log context
func (e *ELKLogger) WithRequestID(requestID string) *logrus.Entry {
	return e.Logger.WithField("request_id", requestID)
}

// WithUserID adds user ID to log context
func (e *ELKLogger) WithUserID(userID string) *logrus.Entry {
	return e.Logger.WithField("user_id", userID)
}

// WithOrderID adds order ID to log context
func (e *ELKLogger) WithOrderID(orderID string) *logrus.Entry {
	return e.Logger.WithField("order_id", orderID)
}

// WithShippingID adds shipping ID to log context
func (e *ELKLogger) WithShippingID(shippingID string) *logrus.Entry {
	return e.Logger.WithField("shipping_id", shippingID)
}

// WithOperation adds operation name to log context
func (e *ELKLogger) WithOperation(operation string) *logrus.Entry {
	return e.Logger.WithField("operation", operation)
}

// LogHTTPRequest logs HTTP request details
func (e *ELKLogger) LogHTTPRequest(method, path, userAgent, remoteAddr string, statusCode int, duration time.Duration) {
	e.Logger.WithFields(logrus.Fields{
		"http_method":   method,
		"http_path":     path,
		"http_status":   statusCode,
		"http_duration": duration.Milliseconds(),
		"user_agent":    userAgent,
		"remote_addr":   remoteAddr,
		"request_type":  "http",
	}).Info("HTTP request processed")
}

// LogKafkaEvent logs Kafka event publishing
func (e *ELKLogger) LogKafkaEvent(topic, eventType, entityID string, success bool, err error) {
	fields := logrus.Fields{
		"kafka_topic":    topic,
		"event_type":     eventType,
		"entity_id":      entityID,
		"event_category": "kafka",
		"success":        success,
	}

	if err != nil {
		fields["error"] = err.Error()
		e.Logger.WithFields(fields).Error("Failed to publish Kafka event")
	} else {
		e.Logger.WithFields(fields).Info("Kafka event published successfully")
	}
}

// LogDatabaseOperation logs database operations
func (e *ELKLogger) LogDatabaseOperation(operation, table, entityID string, duration time.Duration, err error) {
	fields := logrus.Fields{
		"db_operation":   operation,
		"db_table":       table,
		"entity_id":      entityID,
		"db_duration":    duration.Milliseconds(),
		"operation_type": "database",
	}

	if err != nil {
		fields["error"] = err.Error()
		e.Logger.WithFields(fields).Error("Database operation failed")
	} else {
		e.Logger.WithFields(fields).Debug("Database operation completed")
	}
}

// LogServiceOperation logs general service operations
func (e *ELKLogger) LogServiceOperation(service, operation string, success bool, duration time.Duration, metadata map[string]interface{}) {
	fields := logrus.Fields{
		"service":        service,
		"operation":      operation,
		"success":        success,
		"duration":       duration.Milliseconds(),
		"operation_type": "service",
	}

	// Add metadata
	for key, value := range metadata {
		fields[key] = value
	}

	if success {
		e.Logger.WithFields(fields).Info("Service operation completed")
	} else {
		e.Logger.WithFields(fields).Error("Service operation failed")
	}
}

// Close gracefully closes the logger and its resources
func (e *ELKLogger) Close() error {
	if e.kafkaProducer != nil {
		return e.kafkaProducer.Close()
	}
	return nil
}

// Helper functions
func getEnv(key, defaultValue string) string {
	if value, exists := os.LookupEnv(key); exists {
		return value
	}
	return defaultValue
}

func getEnvAsBool(key string, defaultValue bool) bool {
	value := getEnv(key, "")
	if value == "" {
		return defaultValue
	}
	return value == "true" || value == "1" || value == "yes"
}
