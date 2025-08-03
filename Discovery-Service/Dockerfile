# Use OpenJDK 21 as base image for compatibility
FROM openjdk:21-jdk-slim

# Set working directory
WORKDIR /app

# Add metadata
LABEL maintainer="E-commerce Team"
LABEL application="discovery-service"
LABEL version="1.0.0"
LABEL description="Netflix Eureka Discovery Service for E-commerce Microservices"

# Create non-root user for security
RUN groupadd -r ecommerce && useradd -r -g ecommerce ecommerce

# Install necessary packages
RUN apt-get update && apt-get install -y \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Copy the JAR file
COPY target/*.jar discovery-service.jar

# Change ownership to non-root user
RUN chown ecommerce:ecommerce discovery-service.jar

# Switch to non-root user
USER ecommerce

# Expose the Eureka server port
EXPOSE 8761

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8761/actuator/health || exit 1

# Set JVM options for optimal performance
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -XX:+UseStringDeduplication"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar discovery-service.jar"]