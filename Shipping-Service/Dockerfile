# Multi-stage Dockerfile for Shipping Service
FROM golang:1.24-alpine AS builder

# Set working directory
WORKDIR /app

# Install git and ca-certificates for fetching dependencies
RUN apk update && apk add --no-cache git ca-certificates curl make

# Copy go mod files first for better caching
COPY go.mod go.sum ./

# Download dependencies
RUN go mod download && go mod verify

# Copy source code
COPY . .

# Build the application with optimizations
RUN CGO_ENABLED=0 GOOS=linux GOARCH=amd64 go build \
    -a -installsuffix cgo \
    -ldflags="-w -s -X main.version=$(date +%Y%m%d_%H%M%S)" \
    -o shipping-service main.go

# Verify the binary was created
RUN ls -la shipping-service

# Final stage - minimal runtime image
FROM alpine:latest

# Install ca-certificates and curl for health checks
RUN apk update && apk add --no-cache ca-certificates curl tzdata && \
    update-ca-certificates

# Set timezone to UTC
RUN cp /usr/share/zoneinfo/UTC /etc/localtime && echo "UTC" > /etc/timezone

# Create non-root user for security
RUN addgroup -g 1001 appgroup && \
    adduser -u 1001 -G appgroup -s /bin/sh -D appuser

# Set working directory
WORKDIR /app

# Copy the binary from builder stage
COPY --from=builder /app/shipping-service .

# Copy configuration files if they exist
COPY --from=builder /app/.env* ./

# Create logs directory
RUN mkdir -p /app/logs

# Change ownership of all files to appuser
RUN chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Expose the service port
EXPOSE 8085

# Add health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
    CMD curl -f http://localhost:8085/health || exit 1

# Add labels for better image management
LABEL maintainer="Your Name <your.email@example.com>"
LABEL version="1.0.0"
LABEL description="Enhanced Shipping Service for E-commerce Platform"
LABEL service="shipping-service"

# Run the application
CMD ["./shipping-service"]