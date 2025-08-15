# 🛒 Cart Service - E-commerce Microservice

<div align="center">

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.4-brightgreen.svg?style=for-the-badge&logo=spring-boot)](https://spring.io/projects/spring-boot) 
[![Java](https://img.shields.io/badge/Java-17-orange.svg?style=for-the-badge&logo=openjdk)](https://www.oracle.com/java/) 
[![MongoDB](https://img.shields.io/badge/MongoDB-Latest-green.svg?style=for-the-badge&logo=mongodb)](https://www.mongodb.com/) 
[![Redis](https://img.shields.io/badge/Redis-Latest-red.svg?style=for-the-badge&logo=redis)](https://redis.io/) 
[![Apache Kafka](https://img.shields.io/badge/Apache%20Kafka-Latest-orange.svg?style=for-the-badge&logo=apache-kafka)](https://kafka.apache.org/)

[![Jenkins](https://img.shields.io/badge/Jenkins-CI%2FCD-blue.svg?style=for-the-badge&logo=jenkins)](https://jenkins.io/)
[![Docker](https://img.shields.io/badge/Docker-Containerized-blue.svg?style=for-the-badge&logo=docker)](https://www.docker.com/)
[![SonarQube](https://img.shields.io/badge/SonarQube-Quality%20Gate-blue.svg?style=for-the-badge&logo=sonarqube)](https://www.sonarqube.org/)
[![Trivy](https://img.shields.io/badge/Trivy-Security%20Scan-blue.svg?style=for-the-badge&logo=trivy)](https://trivy.dev/)

[![Build Status](https://img.shields.io/badge/Build-Passing-brightgreen.svg?style=for-the-badge&logo=github-actions)](https://jenkins.io/)
[![Code Coverage](https://img.shields.io/badge/Coverage%25-brightgreen.svg?style=for-the-badge&logo=codecov)](https://codecov.io/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg?style=for-the-badge&logo=opensource)](LICENSE)

</div>

A robust, scalable cart management microservice built with Spring Boot, featuring real-time event-driven architecture, intelligent caching, and comprehensive cart operations with automated CI/CD pipeline.

## 🏗️ Architecture Overview

```mermaid
graph TB
    subgraph "Cart Service Architecture"
        API[🎮 REST API Layer]
        SERVICE[⚙️ Service Layer]
        REPO[🗄️ Repository Layer]
        CACHE[🔴 Redis Cache Layer]
        DB[(🍃 MongoDB)]
        KAFKA[🔄 Kafka Events]
        
        API --> SERVICE
        SERVICE --> REPO
        SERVICE --> CACHE
        REPO --> DB
        SERVICE --> KAFKA
        
        subgraph "External Services"
            EUREKA[🔍 Eureka Discovery]
            CONFIG[⚙️ Config Server]
        end
        
        API -.-> EUREKA
        API -.-> CONFIG
    end
```

## 🚀 Technology Stack

<div align="center">

| Technology | Description | Version |
|------------|-------------|---------|
| <img src="https://img.shields.io/badge/Spring%20Boot-6DB33F?style=flat&logo=spring-boot&logoColor=white" width="100"> | Application Framework | 3.4.4 |
| <img src="https://img.shields.io/badge/MongoDB-47A248?style=flat&logo=mongodb&logoColor=white" width="100"> | Primary Database | Latest |
| <img src="https://img.shields.io/badge/Redis-DC382D?style=flat&logo=redis&logoColor=white" width="100"> | Caching Layer | Latest |
| <img src="https://img.shields.io/badge/Apache%20Kafka-231F20?style=flat&logo=apache-kafka&logoColor=white" width="100"> | Event Streaming | Latest |
| <img src="https://img.shields.io/badge/Spring%20Cloud-6DB33F?style=flat&logo=spring&logoColor=white" width="100"> | Service Discovery | 2024.0.1 |
| <img src="https://img.shields.io/badge/Swagger-85EA2D?style=flat&logo=swagger&logoColor=black" width="100"> | API Documentation | 2.7.0 |

</div>

## 🎯 Core Features

### 🛍️ Shopping Cart Management
- ✅ **Create/Read/Update/Delete** cart operations
- ✅ **Add/Remove items** with quantity management
- ✅ **Real-time price calculations** and totals
- ✅ **Cart expiration** and cleanup mechanisms

### 💾 Save for Later
- ✅ **Wishlist functionality** for delayed purchases
- ✅ **Move items** between cart and saved list
- ✅ **Bulk operations** for multiple items

### 🔄 Cart Synchronization
- ✅ **localStorage sync** for guest users
- ✅ **Conflict resolution** strategies
- ✅ **Cross-device synchronization**

### 📡 Event-Driven Architecture
- ✅ **Real-time notifications** via Kafka
- ✅ **Microservice communication** patterns
- ✅ **Event sourcing** for audit trails

## 🔄 CI/CD Pipeline with Jenkins

<div align="center">

[![Jenkins](https://img.shields.io/badge/Jenkins-Automated%20Pipeline-blue?style=for-the-badge&logo=jenkins)](https://jenkins.io/)
[![Docker Hub](https://img.shields.io/badge/Docker%20Hub-Registry-blue?style=for-the-badge&logo=docker)](https://hub.docker.com/)
[![SonarQube](https://img.shields.io/badge/SonarQube-Code%20Quality-blue?style=for-the-badge&logo=sonarqube)](https://sonarqube.org/)

</div>

```mermaid
graph LR
    subgraph "🚀 CI/CD Pipeline"
        A[📥 Checkout] --> B[🔨 Build]
        B --> C[🧪 Test]
        C --> D[🔍 SonarQube]
        D --> E[🚦 Quality Gate]
        E --> F[📦 Package]
        F --> G[🐳 Docker Build]
        G --> H[🛡️ Security Scan]
        H --> I[📤 Push Registry]
    end
```

### 🏗️ Pipeline Stages

| Stage | Tool | Duration | Status |
|-------|------|----------|--------|
| **📥 Checkout** | Git | ~30s | ✅ |
| **🔨 Build** | Maven 3.9.7 | ~2min | ✅ |
| **🧪 Tests** | JUnit + JaCoCo | ~3min | ✅ |
| **🔍 Code Analysis** | SonarQube | ~2min | ✅ |
| **📦 Package** | Maven | ~1min | ✅ |
| **🐳 Docker Build** | Docker | ~2min | ✅ |
| **🛡️ Security Scan** | Trivy | ~3min | ✅ |
| **📤 Registry Push** | Docker Hub | ~2min | ✅ |

### 🛠️ Jenkins Configuration

#### Required Credentials
- `yahya.zakaria-dockerhub` - Docker Hub authentication
- `GithubCredentials` - GitHub repository access
- `sonarqube` - SonarQube server configuration

#### Quality Gates
- **Code Coverage**: > 80%
- **Bugs**: 0 tolerance
- **Security Vulnerabilities**: 0 high/critical
- **Code Smells**: < 10 issues

## 🏛️ System Architecture

```mermaid
graph LR
    subgraph "Client Layer"
        WEB[🌐 Web App]
        MOBILE[📱 Mobile App]
    end
    
    subgraph "API Gateway"
        GATEWAY[🚪 API Gateway]
    end
    
    subgraph "Cart Service"
        CONTROLLER[🎮 Controllers]
        BUSINESS[⚙️ Business Logic]
        EVENTS[📡 Event Publishers]
    end
    
    subgraph "Data Layer"
        MONGODB[(🍃 MongoDB)]
        REDIS[(🔴 Redis Cache)]
    end
    
    subgraph "Message Broker"
        KAFKA[🔄 Apache Kafka]
    end
    
    subgraph "Service Discovery"
        EUREKA[🔍 Eureka Server]
    end
    
    WEB --> GATEWAY
    MOBILE --> GATEWAY
    GATEWAY --> CONTROLLER
    CONTROLLER --> BUSINESS
    BUSINESS --> MONGODB
    BUSINESS --> REDIS
    BUSINESS --> EVENTS
    EVENTS --> KAFKA
    CONTROLLER -.-> EUREKA
```

## 📊 Data Flow Architecture

```mermaid
sequenceDiagram
    participant Client
    participant CartController
    participant CartService
    participant MongoDB
    participant Redis
    participant Kafka
    
    Client->>CartController: Add Item to Cart
    CartController->>CartService: addItemToCart()
    CartService->>MongoDB: Save Cart Item
    CartService->>Redis: Update Cache
    CartService->>Kafka: Publish CartItemAdded Event
    MongoDB-->>CartService: Success
    Redis-->>CartService: Cache Updated
    Kafka-->>CartService: Event Published
    CartService-->>CartController: Updated Cart
    CartController-->>Client: Success Response
```

## 🗄️ Database Schema

```mermaid
erDiagram
    SHOPPING_CART {
        UUID id PK
        UUID userId FK
        DateTime createdAt
        DateTime updatedAt
        DateTime expiresAt
    }
    
    CART_ITEM {
        UUID id PK
        UUID cartId FK
        UUID productId FK
        int quantity
        decimal price
        DateTime addedAt
    }
    
    SAVED_FOR_LATER {
        UUID id PK
        UUID userId FK
        UUID productId FK
        DateTime savedAt
    }
    
    SHOPPING_CART ||--o{ CART_ITEM : contains
    SHOPPING_CART ||--|| USER : belongs_to
    SAVED_FOR_LATER ||--|| USER : belongs_to
```

## 🎪 Event Architecture

```mermaid
graph TD
    subgraph "Cart Events"
        CE1[📦 Cart Created]
        CE2[🔄 Cart Updated]
        CE3[🗑️ Cart Deleted]
        CE4[✅ Cart Checked Out]
        CE5[❌ Cart Abandoned]
    end
    
    subgraph "Cart Item Events"
        CIE1[➕ Item Added]
        CIE2[🔧 Item Updated]
        CIE3[➖ Item Removed]
        CIE4[🔢 Quantity Changed]
        CIE5[💰 Price Changed]
    end
    
    subgraph "Saved Items Events"
        SIE1[💾 Item Saved for Later]
        SIE2[🔄 Saved Item Moved to Cart]
        SIE3[🗑️ Saved Item Removed]
    end
    
    subgraph "Kafka Topics"
        KAFKA[🔄 Apache Kafka Broker]
    end
    
    CE1 --> KAFKA
    CE2 --> KAFKA
    CE3 --> KAFKA
    CE4 --> KAFKA
    CE5 --> KAFKA
    CIE1 --> KAFKA
    CIE2 --> KAFKA
    CIE3 --> KAFKA
    CIE4 --> KAFKA
    CIE5 --> KAFKA
    SIE1 --> KAFKA
    SIE2 --> KAFKA
    SIE3 --> KAFKA
```

## 🚀 Quick Start

### 📋 Prerequisites

<div align="center">

[![Java](https://img.shields.io/badge/Java-17%2B-orange?logo=openjdk)](https://openjdk.java.net/)
[![Docker](https://img.shields.io/badge/Docker-Latest-blue?logo=docker)](https://docker.com/)
[![Maven](https://img.shields.io/badge/Maven-3.6%2B-red?logo=apache-maven)](https://maven.apache.org/)
[![MongoDB](https://img.shields.io/badge/MongoDB-Latest-green?logo=mongodb)](https://mongodb.com/)
[![Redis](https://img.shields.io/badge/Redis-Latest-red?logo=redis)](https://redis.io/)

</div>

Before running the Cart Service, ensure you have the following installed:

- ☕ **Java 17** or higher
- 🐳 **Docker** (for running dependencies)
- 🔧 **Maven 3.6+**
- 🍃 **MongoDB** (port 27017)
- 🔴 **Redis** (port 6379)
- 🔄 **Apache Kafka** (port 9092)
- 🔍 **Eureka Discovery Server** (port 8761)

### 🐳 Docker Setup (Recommended)

```bash
# Start all dependencies with Docker Compose
docker run -d --name mongodb -p 27017:27017 mongo:latest
docker run -d --name redis -p 6379:6379 redis:latest
docker run -d --name kafka -p 9092:9092 \
  -e KAFKA_ZOOKEEPER_CONNECT=localhost:2181 \
  -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 \
  confluentinc/cp-kafka:latest
```

### 🏃‍♂️ Running the Service

1. **Clone the repository**
```bash
git clone <repository-url>
cd Cart-Service
```

2. **Build the application**
```bash
./mvnw clean compile
```

3. **Run the application**
```bash
./mvnw spring-boot:run
```

4. **Verify the service is running**
```bash
curl http://localhost:8087/api/carts/actuator/health
```

## 📚 API Documentation

### 🎯 Endpoints Overview

<div align="center">

[![Swagger](https://img.shields.io/badge/Swagger-API%20Docs-85EA2D?logo=swagger)](http://localhost:8087/api/carts/swagger-ui/index.html)
[![Postman](https://img.shields.io/badge/Postman-Collection-orange?logo=postman)](https://postman.com/)

</div>

The Cart Service provides RESTful APIs accessible at: `http://localhost:8087/api/carts`

### 🛒 Shopping Cart Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/{userId}` | Get cart by user ID |
| `POST` | `/{userId}/items` | Add item to cart |
| `PUT` | `/{userId}/items/{productId}` | Update item quantity |
| `DELETE` | `/{userId}/items/{productId}` | Remove item from cart |
| `GET` | `/{userId}/total` | Get cart total |
| `POST` | `/{userId}/checkout` | Checkout cart |
| `POST` | `/{userId}/sync` | Sync localStorage cart |

### 💾 Save for Later Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/{userId}/saved` | Get saved items |
| `POST` | `/{userId}/saved` | Save item for later |
| `DELETE` | `/{userId}/saved/{productId}` | Remove saved item |
| `POST` | `/{userId}/saved/{productId}/move-to-cart` | Move to cart |
| `GET` | `/{userId}/saved/count` | Get saved items count |

### 📖 Interactive API Documentation

Once the service is running, access the Swagger UI at:
**http://localhost:8087/api/carts/swagger-ui/index.html**

## 🔧 Configuration

### 📝 Application Configuration

```yaml
# Core Service Configuration
server:
  port: 8087
  servlet:
    context-path: /api/carts

spring:
  application:
    name: cart-service
  
  # MongoDB Configuration
  data:
    mongodb:
      database: Cart-service
      host: localhost
      port: 27017
  
  # Redis Configuration
  redis:
    host: localhost
    port: 6379
    timeout: 2000
  
  # Kafka Configuration
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
    consumer:
      group-id: cart-service-group
      auto-offset-reset: earliest

# Eureka Configuration
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
```

### 🎛️ Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `MONGODB_HOST` | MongoDB host | localhost |
| `MONGODB_PORT` | MongoDB port | 27017 |
| `REDIS_HOST` | Redis host | localhost |
| `REDIS_PORT` | Redis port | 6379 |
| `KAFKA_BROKERS` | Kafka bootstrap servers | localhost:9092 |
| `EUREKA_URL` | Eureka discovery URL | http://localhost:8761/eureka/ |

## 🔄 Kafka Event Topics

### 📡 Cart Events

```mermaid
graph LR
    subgraph "Cart Topics"
        CT1[cart-created]
        CT2[cart-updated]
        CT3[cart-deleted]
        CT4[cart-checked-out]
        CT5[cart-abandoned]
    end
    
    subgraph "Cart Item Topics"
        CIT1[cart-item-added]
        CIT2[cart-item-updated]
        CIT3[cart-item-removed]
        CIT4[cart-item-quantity-changed]
        CIT5[cart-item-price-changed]
    end
    
    subgraph "Saved Items Topics"
        SIT1[item-saved-for-later]
        SIT2[saved-item-moved-to-cart]
        SIT3[saved-item-removed]
    end
```

### 📨 Event Examples

#### Cart Item Added Event
```json
{
  "eventId": "uuid",
  "timestamp": "2024-01-01T10:00:00Z",
  "eventType": "CART_ITEM_ADDED",
  "userId": "user-uuid",
  "cartId": "cart-uuid",
  "productId": "product-uuid",
  "quantity": 2,
  "price": 29.99,
  "subtotal": 59.98
}
```

#### Cart Checkout Event
```json
{
  "eventId": "uuid",
  "timestamp": "2024-01-01T10:00:00Z",
  "eventType": "CART_CHECKED_OUT",
  "userId": "user-uuid",
  "cartId": "cart-uuid",
  "itemCount": 5,
  "totalAmount": 149.95
}
```

## 🏗️ Project Structure

```
Cart-Service/
├── 📁 src/main/java/com/Ecommerce/Cart/Service/
│   ├── 🎮 Controllers/           # REST Controllers
│   ├── ⚙️ Services/             # Business Logic
│   ├── 🗄️ Repositories/         # Data Access Layer
│   ├── 📦 Models/               # Domain Models
│   ├── 🎪 Events/               # Event Models
│   ├── 🔧 Config/               # Configuration Classes
│   ├── 👂 Listeners/            # Event Listeners
│   ├── 📬 Payload/              # Request/Response DTOs
│   ├── ⚠️ Exception/            # Exception Handling
│   └── 🕰️ ScheduledTasks/       # Scheduled Jobs
├── 📁 src/main/resources/
│   ├── 📄 application.yaml      # Main Configuration
│   └── 📄 bootstrap.yml         # Bootstrap Configuration
└── 📄 pom.xml                   # Maven Dependencies
```

## 🧪 Testing

<div align="center">

[![JUnit](https://img.shields.io/badge/JUnit-5-green?logo=junit5)](https://junit.org/)
[![JaCoCo](https://img.shields.io/badge/JaCoCo-Coverage-blue?logo=jacoco)](https://jacoco.org/)
[![Testcontainers](https://img.shields.io/badge/Testcontainers-Integration-blue?logo=docker)](https://testcontainers.org/)

</div>

### 🚀 Running Tests

```bash
# Run all tests
./mvnw test

# Run integration tests
./mvnw test -Dtest=**/*IntegrationTest

# Run with coverage
./mvnw test jacoco:report
```

### 📊 Testing Strategy

```mermaid
graph TD
    subgraph "Testing Pyramid"
        UT[🔬 Unit Tests]
        IT[🔗 Integration Tests]
        E2E[🌐 End-to-End Tests]
    end
    
    subgraph "Test Types"
        SERVICE[Service Layer Tests]
        REPO[Repository Tests]
        CONTROLLER[Controller Tests]
        KAFKA[Kafka Integration Tests]
    end
    
    UT --> SERVICE
    IT --> REPO
    IT --> KAFKA
    E2E --> CONTROLLER
```

## 📈 Performance & Monitoring

<div align="center">

[![Prometheus](https://img.shields.io/badge/Prometheus-Metrics-orange?logo=prometheus)](https://prometheus.io/)
[![Grafana](https://img.shields.io/badge/Grafana-Dashboard-orange?logo=grafana)](https://grafana.com/)
[![Jaeger](https://img.shields.io/badge/Jaeger-Tracing-blue?logo=jaeger)](https://jaegertracing.io/)

</div>

### 🎯 Key Metrics

- **Response Time**: < 100ms for cached requests
- **Throughput**: 1000+ requests/second
- **Cache Hit Ratio**: > 90%
- **Event Processing**: < 10ms latency

### 📊 Monitoring Endpoints

| Endpoint | Description |
|----------|-------------|
| `/actuator/health` | Health check |
| `/actuator/metrics` | Application metrics |
| `/actuator/info` | Application info |
| `/actuator/kafka` | Kafka metrics |

### 🔍 Observability Stack

```mermaid
graph LR
    subgraph "Monitoring"
        PROMETHEUS[📊 Prometheus]
        GRAFANA[📈 Grafana]
        JAEGER[🔍 Jaeger]
    end
    
    subgraph "Logging"
        ELK[📋 ELK Stack]
        LOGBACK[📝 Logback]
    end
    
    APP[Cart Service] --> PROMETHEUS
    APP --> JAEGER
    APP --> LOGBACK
    PROMETHEUS --> GRAFANA
    LOGBACK --> ELK
```

## 🚀 Deployment

### 🐳 Docker Deployment

<div align="center">

[![Docker](https://img.shields.io/badge/Docker-Containerized-blue?logo=docker)](https://docker.com/)
[![Docker Hub](https://img.shields.io/badge/Docker%20Hub-Registry-blue?logo=docker)](https://hub.docker.com/)

</div>

```dockerfile
FROM openjdk:17-jre-slim
COPY target/cart-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8087
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

```bash
# Build Docker image
docker build -t cart-service:latest .

# Run container
docker run -d \
  --name cart-service \
  -p 8087:8087 \
  -e MONGODB_HOST=mongodb \
  -e REDIS_HOST=redis \
  -e KAFKA_BROKERS=kafka:9092 \
  cart-service:latest
```

### ☸️ Kubernetes Deployment

<div align="center">

[![Kubernetes](https://img.shields.io/badge/Kubernetes-Orchestration-blue?logo=kubernetes)](https://kubernetes.io/)
[![Helm](https://img.shields.io/badge/Helm-Package%20Manager-blue?logo=helm)](https://helm.sh/)

</div>

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: cart-service
spec:
  replicas: 3
  selector:
    matchLabels:
      app: cart-service
  template:
    metadata:
      labels:
        app: cart-service
    spec:
      containers:
      - name: cart-service
        image: cart-service:latest
        ports:
        - containerPort: 8087
        env:
        - name: MONGODB_HOST
          value: "mongodb-service"
        - name: REDIS_HOST
          value: "redis-service"
        - name: KAFKA_BROKERS
          value: "kafka-service:9092"
```

## 🤝 Contributing

<div align="center">

[![GitHub](https://img.shields.io/badge/GitHub-Contribute-black?logo=github)](https://github.com/)
[![Conventional Commits](https://img.shields.io/badge/Conventional%20Commits-1.0.0-yellow?logo=conventionalcommits)](https://conventionalcommits.org/)

</div>

### 🛠️ Development Setup

1. **Fork the repository**
2. **Create a feature branch**
   ```bash
   git checkout -b feature/amazing-feature
   ```
3. **Make your changes**
4. **Add tests**
5. **Commit your changes**
   ```bash
   git commit -m 'Add some amazing feature'
   ```
6. **Push to the branch**
   ```bash
   git push origin feature/amazing-feature
   ```
7. **Open a Pull Request**

### 📋 Code Style

- Follow **Spring Boot** best practices
- Use **Lombok** for boilerplate code
- Write comprehensive **JavaDoc** comments
- Maintain **85%+** test coverage
- Follow **RESTful** API design principles

## 📜 API Examples

### 🛒 Add Item to Cart

```bash
curl -X POST http://localhost:8087/api/carts/123e4567-e89b-12d3-a456-426614174000/items \
  -H "Content-Type: application/json" \
  -d '{
    "productId": "456e4567-e89b-12d3-a456-426614174001",
    "quantity": 2,
    "price": 29.99
  }'
```

### 💾 Save Item for Later

```bash
curl -X POST http://localhost:8087/api/carts/123e4567-e89b-12d3-a456-426614174000/saved \
  -H "Content-Type: application/json" \
  -d '{
    "productId": "456e4567-e89b-12d3-a456-426614174001"
  }'
```

### 🔄 Sync Cart

```bash
curl -X POST http://localhost:8087/api/carts/123e4567-e89b-12d3-a456-426614174000/sync \
  -H "Content-Type: application/json" \
  -d '{
    "items": [
      {
        "productId": "456e4567-e89b-12d3-a456-426614174001",
        "quantity": 1,
        "price": 19.99,
        "addedAt": "2024-01-01T10:00:00Z"
      }
    ],
    "conflictStrategy": "SUM_QUANTITIES"
  }'
```

## 🔒 Security

<div align="center">

[![Spring Security](https://img.shields.io/badge/Spring%20Security-JWT-green?logo=spring-security)](https://spring.io/projects/spring-security)
[![OWASP](https://img.shields.io/badge/OWASP-Compliant-blue?logo=owasp)](https://owasp.org/)

</div>

### 🛡️ Security Features

- **JWT Authentication** integration ready
- **Input validation** with Bean Validation
- **SQL Injection** protection via MongoDB
- **Rate limiting** capabilities
- **CORS** configuration

### 🔐 Security Configuration

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    // Security configuration ready for JWT integration
}
```

## 📊 Performance Optimization

### ⚡ Caching Strategy

```mermaid
graph LR
    subgraph "Cache Layers"
        L1[L1: In-Memory Cache]
        L2[L2: Redis Cache]
        L3[L3: Database]
    end
    
    REQUEST[Client Request] --> L1
    L1 -->|Cache Miss| L2
    L2 -->|Cache Miss| L3
    L3 --> L2
    L2 --> L1
    L1 --> RESPONSE[Client Response]
```

### 🚀 Performance Tips

- **Use caching** for frequently accessed carts
- **Implement pagination** for large result sets
- **Optimize database queries** with proper indexing
- **Use async processing** for non-critical operations

## 🐛 Troubleshooting

### 🔍 Common Issues

#### MongoDB Connection Issues
```bash
# Check MongoDB status
docker ps | grep mongo
# View MongoDB logs
docker logs mongodb
```

#### Redis Connection Issues
```bash
# Test Redis connection
redis-cli ping
# Check Redis status
docker ps | grep redis
```

#### Kafka Issues
```bash
# List Kafka topics
kafka-topics.sh --list --bootstrap-server localhost:9092
# Check consumer groups
kafka-consumer-groups.sh --list --bootstrap-server localhost:9092
```

### 📋 Health Checks

```bash
# Service health
curl http://localhost:8087/api/carts/actuator/health

# Database connectivity
curl http://localhost:8087/api/carts/actuator/health/mongo

# Cache status
curl http://localhost:8087/api/carts/actuator/health/redis
```

## 📄 License

<div align="center">

[![MIT License](https://img.shields.io/badge/License-MIT-blue.svg?logo=opensource)](LICENSE)

</div>

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

<div align="center">

[![Spring](https://img.shields.io/badge/Spring-Team-green?logo=spring)](https://spring.io/)
[![MongoDB](https://img.shields.io/badge/MongoDB-Team-green?logo=mongodb)](https://mongodb.com/)
[![Redis](https://img.shields.io/badge/Redis-Team-red?logo=redis)](https://redis.io/)
[![Apache](https://img.shields.io/badge/Apache-Kafka%20Team-orange?logo=apache-kafka)](https://kafka.apache.org/)

</div>

- Spring Boot team for the amazing framework
- MongoDB team for the flexible database
- Redis team for the blazing-fast cache
- Apache Kafka team for the event streaming platform

---

<div align="center">

**🛒 Happy Shopping Cart Development! 🛒**

Made with ❤️ using Spring Boot

[![GitHub stars](https://img.shields.io/github/stars/username/cart-service?style=social)](https://github.com/username/cart-service/stargazers)
[![GitHub forks](https://img.shields.io/github/forks/username/cart-service?style=social)](https://github.com/username/cart-service/network/members)
[![GitHub watchers](https://img.shields.io/github/watchers/username/cart-service?style=social)](https://github.com/username/cart-service/watchers)

[⬆ Back to top](#-cart-service---e-commerce-microservice)

</div>
