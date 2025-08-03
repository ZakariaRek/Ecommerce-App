// internal/middleware/logging.go
package middleware

import (
	"net/http"
	"time"

	"github.com/ZakariaRek/Ecommerce-App/Shipping-Service/internal/logger"
)

// ResponseWriter wrapper to capture status code
type responseWriter struct {
	http.ResponseWriter
	statusCode int
}

// WriteHeader captures the status code
func (rw *responseWriter) WriteHeader(code int) {
	rw.statusCode = code
	rw.ResponseWriter.WriteHeader(code)
}

// LoggingMiddleware creates a middleware for HTTP request logging
func LoggingMiddleware(elkLogger *logger.ELKLogger) func(http.Handler) http.Handler {
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			start := time.Now()

			// Wrap the response writer to capture status code
			wrapped := &responseWriter{
				ResponseWriter: w,
				statusCode:     http.StatusOK, // Default status code
			}

			// Generate request ID for tracing
			requestID := generateRequestID()

			// Add request ID to response headers
			wrapped.Header().Set("X-Request-ID", requestID)

			// Add request ID to context for downstream use
			ctx := r.Context()
			r = r.WithContext(ctx)

			// Process the request
			next.ServeHTTP(wrapped, r)

			// Calculate duration
			duration := time.Since(start)

			// Log the request
			elkLogger.LogHTTPRequest(
				r.Method,
				r.URL.Path,
				r.UserAgent(),
				r.RemoteAddr,
				wrapped.statusCode,
				duration,
			)

			// Add additional context logging for specific endpoints
			if wrapped.statusCode >= 400 {
				elkLogger.WithRequestID(requestID).WithFields(map[string]interface{}{
					"query_params": r.URL.Query(),
					"content_type": r.Header.Get("Content-Type"),
				}).Warn("HTTP request completed with error status")
			}
		})
	}
}

// generateRequestID creates a simple request ID
func generateRequestID() string {
	return time.Now().Format("20060102150405") + "-" + randomString(6)
}

// randomString generates a random string of specified length
func randomString(length int) string {
	const charset = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
	b := make([]byte, length)
	for i := range b {
		b[i] = charset[time.Now().UnixNano()%int64(len(charset))]
	}
	return string(b)
}
