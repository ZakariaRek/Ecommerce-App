# Multi-stage build for Go application
FROM golang:1.24-alpine AS builder

# Set working directory
WORKDIR /app

# Install dependencies for building
RUN apk add --no-cache git ca-certificates tzdata

# Copy go.mod and go.sum first for better caching
COPY go.mod go.sum ./
RUN go mod download

# Copy source code (only the directories that exist)
COPY internal/ internal/
COPY main.go .
COPY .env* ./

# Build the application with optimizations
RUN CGO_ENABLED=0 GOOS=linux GOARCH=amd64 go build \
    -ldflags='-w -s -extldflags "-static"' \
    -a -installsuffix cgo \
    -o payment-service main.go

# Create minimal production image
FROM alpine:3.19

# Install ca-certificates and curl for health checks
RUN apk --no-cache add ca-certificates curl tzdata

# Create non-root user for security
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

# Set working directory
WORKDIR /app

# Copy the binary from builder stage
COPY --from=builder --chown=appuser:appgroup /app/payment-service .

# Copy environment file if exists
COPY --from=builder --chown=appuser:appgroup /app/.env* ./

# Switch to non-root user
USER appuser:appgroup

# Expose port (using the port from your .env file)
EXPOSE 8089

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8089/health || exit 1

# Run the application
ENTRYPOINT ["./payment-service"]