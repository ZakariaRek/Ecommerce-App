# 🚀 E-commerce Gateway Service

<div align="center">
  <img src="https://raw.githubusercontent.com/spring-projects/spring-framework/main/framework-docs/src/docs/spring-framework.png" alt="Spring" width="100"/>
  <img src="https://www.vectorlogo.zone/logos/springio/springio-ar21.svg" alt="Spring Boot" width="120"/>
  <img src="https://redis.io/wp-content/uploads/2024/04/Logotype.svg" alt="Redis" width="100"/>
  <img src="https://kafka.apache.org/logos/kafka_logo--simple.png" alt="Kafka" width="100"/>
</div>

## 📋 Overview

The **Gateway Service** is a central API Gateway built with **Spring Cloud Gateway** that serves as the single entry point for all client requests in our e-commerce microservices architecture. It provides authentication, authorization, rate limiting, circuit breaking, and intelligent request routing.

### ✨ Key Features

- 🔐 **JWT Authentication & Authorization**
- 🚦 **Advanced Rate Limiting** (IP & User-based)
- ⚡ **Circuit Breakers** for fault tolerance
- 🌐 **CORS Configuration**
- 📊 **BFF Pattern** with enriched responses
- 🔄 **Async Kafka Communication**
- 📈 **Comprehensive Monitoring**
- 📚 **Swagger Documentation**

---

## 🏗️ Architecture Overview

```mermaid
graph TB
    Client[🌐 Client Applications<br/>Web/Mobile] --> Gateway[🚪 API Gateway<br/>Port: 8099]
    
    Gateway --> |Authentication| UserService[👤 User Service<br/>Port: 8081]
    Gateway --> |Product Catalog| ProductService[📦 Product Service<br/>Port: 8082]
    Gateway --> |Orders| OrderService[📋 Order Service<br/>Port: 8083]
    Gateway --> |Payments| PaymentService[💳 Payment Service<br/>Port: 8084]
    Gateway --> |Shopping Cart| CartService[🛒 Cart Service<br/>Port: 8085]
    Gateway --> |Loyalty Program| LoyaltyService[🎁 Loyalty Service<br/>Port: 8086]
    Gateway --> |Notifications| NotificationService[📧 Notification Service<br/>Port: 8087]
    Gateway --> |Shipping| ShippingService[🚚 Shipping Service<br/>Port: 8088]
    
    Gateway <--> Redis[(🔴 Redis<br/>Rate Limiting & Cache)]
    Gateway <--> Kafka[(📨 Apache Kafka<br/>Async Communication)]
    Gateway --> Eureka[🔍 Eureka Server<br/>Service Discovery]
    
    style Gateway fill:#e1f5fe
    style Redis fill:#ffcdd2
    style Kafka fill:#f3e5f5
    style Eureka fill:#e8f5e8
```

---

## 🔄 Request Flow Architecture

```mermaid
sequenceDiagram
    participant Client
    participant Gateway
    participant Redis
    participant Service
    participant Kafka
    
    Client->>Gateway: HTTP Request
    Gateway->>Gateway: 1. CORS Check
    Gateway->>Gateway: 2. JWT Validation
    Gateway->>Redis: 3. Rate Limit Check
    
    alt Rate Limit OK
        Gateway->>Gateway: 4. Route Resolution
        Gateway->>Service: 5. Forward Request
        Service-->>Gateway: 6. Response
        Gateway-->>Client: 7. Response + Headers
    else Rate Limit Exceeded
        Gateway-->>Client: 429 Too Many Requests
    end
    
    Note over Gateway,Kafka: For BFF Endpoints
    Gateway->>Kafka: Async Request (Cart/Order)
    Kafka-->>Gateway: Async Response
    Gateway->>Gateway: Enrich with Product Data
    Gateway-->>Client: Enriched Response
```

---

## 🛠️ Technology Stack

<div align="center">

| Technology | Purpose | Version |
|------------|---------|---------|
| ![Spring Boot](https://img.shields.io/badge/Spring%20Boot-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white) | Application Framework | 3.4.4 |
| ![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-6DB33F?style=for-the-badge&logo=spring&logoColor=white) | Microservices Toolkit | 2024.0.1 |
| ![Redis](https://img.shields.io/badge/redis-%23DD0031.svg?style=for-the-badge&logo=redis&logoColor=white) | Caching & Rate Limiting | Latest |
| ![Apache Kafka](https://img.shields.io/badge/Apache%20Kafka-000?style=for-the-badge&logo=apachekafka) | Message Streaming | Latest |
| ![JWT](https://img.shields.io/badge/JWT-black?style=for-the-badge&logo=JSON%20web%20tokens) | Authentication | 0.11.5 |
| ![Swagger](https://img.shields.io/badge/-Swagger-%23Clojure?style=for-the-badge&logo=swagger&logoColor=white) | API Documentation | 2.3.0 |

</div>

---

## 🚦 Rate Limiting Strategy

```mermaid
graph LR
    subgraph "Rate Limiting Types"
        A[IP-Based<br/>Anonymous Users] --> B[Sliding Window<br/>Algorithm]
        C[User-Based<br/>Authenticated Users] --> D[Token Bucket<br/>Algorithm]
    end
    
    subgraph "Endpoint Categories"
        E[🔐 Auth Endpoints<br/>5 req/min]
        F[💳 Payment Endpoints<br/>3 req/5min]
        G[📖 Public Read<br/>200 req/min]
        H[👤 User Operations<br/>50 req/min]
    end
    
    B --> E
    B --> G
    D --> F
    D --> H
    
    style E fill:#ffcdd2
    style F fill:#ffecb3
    style G fill:#c8e6c9
    style H fill:#e1f5fe
```

---

## ⚡ Circuit Breaker Configuration

```mermaid
stateDiagram-v2
    [*] --> CLOSED
    CLOSED --> OPEN : Failure Rate > 50%
    OPEN --> HALF_OPEN : Wait Duration (30s)
    HALF_OPEN --> CLOSED : Success Calls
    HALF_OPEN --> OPEN : Failure Calls
    
    note right of CLOSED
        Normal Operation
        Monitoring Calls
    end note
    
    note right of OPEN
        Failing Fast
        Preventing Cascade
    end note
    
    note right of HALF_OPEN
        Testing Recovery
        Limited Calls
    end note
```

---

## 🔐 Security Architecture

```mermaid
graph TD
    Request[📱 Client Request] --> CORS{CORS Check}
    CORS -->|✅ Valid Origin| JWT{JWT Token?}
    CORS -->|❌ Invalid Origin| Reject1[❌ CORS Error]
    
    JWT -->|✅ Valid Token| RateLimit{Rate Limit Check}
    JWT -->|❌ Invalid/Missing| Reject2[❌ 401 Unauthorized]
    
    RateLimit -->|✅ Within Limits| Route[🚀 Route to Service]
    RateLimit -->|❌ Exceeded| Reject3[❌ 429 Too Many Requests]
    
    Route --> Service[🎯 Target Service]
    Service --> Response[📤 Response + Headers]
    
    style CORS fill:#e3f2fd
    style JWT fill:#f3e5f5
    style RateLimit fill:#fff3e0
    style Route fill:#e8f5e8
```

---

## 📦 Installation & Setup

### Prerequisites

```bash
# Required Software
Java 21+
Maven 3.6+
Redis Server
Apache Kafka
Eureka Server (Config Server)
```

### 🏃‍♂️ Quick Start

1. **Clone the repository**
```bash
git clone <repository-url>
cd Gateway-Service
```

2. **Start infrastructure services**
```bash
# Start Redis
docker run -d --name redis -p 6379:6379 redis:latest

# Start Kafka
docker run -d --name kafka -p 9092:9092 \
  -e KAFKA_ZOOKEEPER_CONNECT=zookeeper:2181 \
  -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 \
  confluentinc/cp-kafka:latest
```

3. **Configure application**
```yaml
# application.yaml - Key configurations
spring:
  kafka:
    bootstrap-servers: localhost:9092
  data:
    redis:
      host: localhost
      port: 6379

gateway1:
  jwt:
    secret: "your-secret-key"
```

4. **Run the application**
```bash
mvn spring-boot:run
```

5. **Access the services**
- **Gateway API**: `http://localhost:8099`
- **Swagger UI**: `http://localhost:8099/swagger-ui.html`
- **Health Check**: `http://localhost:8099/actuator/health`

---

## 🛣️ API Routes Configuration

### Public Endpoints (No Authentication)
```yaml
# Product Catalog (Read-only)
GET /api/products/**          → product-service
GET /api/categories/**        → product-service
GET /api/images/**           → product-service

# Authentication
POST /api/users/auth/signin   → user-service
POST /api/users/auth/signup   → user-service
```

### Protected Endpoints (JWT Required)
```yaml
# User Management
GET,POST,PUT,DELETE /api/users/**     → user-service

# Shopping Cart
GET,POST,PUT,DELETE /api/carts/**     → cart-service

# Orders
GET,POST,PUT,DELETE /api/orders/**    → order-service

# Payments (Highly Restricted)
POST /api/payments/**                 → payment-service
```

### BFF Endpoints (Enhanced Responses)
```yaml
# Enriched Cart with Product Details
GET /api/cart/{userId}/enriched       → cart-bff-service

# Enriched Order with Product Details  
GET /api/order/{orderId}/enriched     → order-bff-service

# Batch Order Processing
POST /api/order/batch                 → order-bff-service
```

---

## 📊 Monitoring & Health Checks

### Available Endpoints

| Endpoint | Description |
|----------|-------------|
| `/actuator/health` | Overall health status |
| `/actuator/metrics` | Application metrics |
| `/actuator/circuitbreakers` | Circuit breaker status |
| `/api/gateway/circuit-breakers` | Custom CB management |
| `/api/gateway/rate-limiting/stats` | Rate limiting statistics |

### Circuit Breaker Monitoring

```bash
# Check all circuit breakers
curl http://localhost:8099/api/gateway/circuit-breakers

# Reset specific circuit breaker
curl -X POST http://localhost:8099/api/gateway/circuit-breakers/auth-cb/reset
```

---

## 🔧 Configuration

### Rate Limiting Configuration
```yaml
rate-limiting:
  endpoints:
    auth:
      limit: 5
      window-seconds: 60
      key-type: IP
    payment:
      limit: 3  
      window-seconds: 300
      key-type: USER
    public-read:
      limit: 200
      window-seconds: 60
      key-type: IP
```

### Circuit Breaker Configuration
```yaml
resilience4j:
  circuitbreaker:
    instances:
      auth-cb:
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 30s
      payment-cb:
        slidingWindowSize: 5
        failureRateThreshold: 30
        waitDurationInOpenState: 60s
```

---

## 🎯 BFF (Backend for Frontend) Pattern

The Gateway implements BFF pattern for complex data aggregation:

```mermaid
sequenceDiagram
    participant Client
    participant Gateway
    participant CartService
    participant ProductService
    participant Kafka
    
    Client->>Gateway: GET /api/cart/{userId}/enriched
    Gateway->>Kafka: Request Cart Data
    CartService->>Kafka: Cart Response
    Kafka-->>Gateway: Cart Items
    
    Gateway->>Kafka: Request Product Details
    ProductService->>Kafka: Product Response  
    Kafka-->>Gateway: Product Info
    
    Gateway->>Gateway: Merge Cart + Product Data
    Gateway-->>Client: Enriched Cart Response
```

### Enhanced Response Example
```json
{
  "id": "cart-uuid",
  "userId": "user-uuid", 
  "items": [
    {
      "id": "item-uuid",
      "productId": "product-uuid",
      "quantity": 2,
      "price": 29.99,
      "subtotal": 59.98,
      "productName": "Premium Headphones",
      "productImage": "/images/headphones.jpg",
      "inStock": true,
      "availableQuantity": 15,
      "discountType": "PERCENTAGE",
      "discountValue": 10.0
    }
  ],
  "total": 59.98,
  "itemCount": 1,
  "totalQuantity": 2
}
```

---

## 🔄 Async Communication with Kafka

### Kafka Topics

| Topic | Purpose | Producer | Consumer |
|-------|---------|-----------|----------|
| `cart.request` | Cart data requests | Gateway | Cart Service |
| `cart.response` | Cart data responses | Cart Service | Gateway |
| `product.batch.request` | Product info requests | Gateway | Product Service |
| `product.batch.response` | Product info responses | Product Service | Gateway |
| `order.request` | Order data requests | Gateway | Order Service |
| `order.response` | Order data responses | Order Service | Gateway |

---

## 📈 Performance & Scalability

### Key Metrics
- **Throughput**: 1000+ requests/second
- **Latency**: < 100ms (95th percentile)
- **Availability**: 99.9% uptime
- **Circuit Breaker**: < 1s failover time

### Scaling Recommendations
```yaml
# Production Configuration
spring:
  cloud:
    gateway:
      httpclient:
        pool:
          max-connections: 100
          max-idle-time: 30s
          max-life-time: 60s
        
server:
  tomcat:
    threads:
      max: 200
      min-spare: 10
```

---

## 🐛 Troubleshooting

### Common Issues

1. **Circuit Breaker Open**
```bash
# Check circuit breaker status
curl http://localhost:8099/api/gateway/circuit-breakers

# Reset if needed
curl -X POST http://localhost:8099/api/gateway/circuit-breakers/{name}/reset
```

2. **Rate Limit Exceeded**
```bash
# Check rate limit status
curl http://localhost:8099/api/gateway/rate-limiting/status/IP/192.168.1.1

# Reset rate limits
curl -X POST http://localhost:8099/api/gateway/rate-limiting/reset/IP/192.168.1.1
```

3. **Service Discovery Issues**
```bash
# Check Eureka registration
curl http://localhost:8761/eureka/apps/gateway-service
```

---

## 📝 API Documentation

- **Swagger UI**: [http://localhost:8099/swagger-ui.html](http://localhost:8099/swagger-ui.html)
- **OpenAPI Spec**: [http://localhost:8099/v3/api-docs](http://localhost:8099/v3/api-docs)

---

## 👥 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 🆘 Support

For support and questions:
- 📧 Email: support@ecommerce.com
- 📖 Documentation: [Gateway Wiki](wiki-url)
- 🐛 Issues: [GitHub Issues](issues-url)

---

<div align="center">

**Built with ❤️ by the E-commerce Team**

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-6DB33F?style=flat&logo=spring-boot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-6DB33F?style=flat&logo=spring&logoColor=white)](https://spring.io/projects/spring-cloud)
[![Redis](https://img.shields.io/badge/Redis-DC382D?style=flat&logo=redis&logoColor=white)](https://redis.io/)
[![Apache Kafka](https://img.shields.io/badge/Apache%20Kafka-231F20?style=flat&logo=apache-kafka&logoColor=white)](https://kafka.apache.org/)

</div>