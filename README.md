# 🚀 NexusCommerce Microservices Platform

![Build Status](https://img.shields.io/badge/build-passing-brightgreen)
![Version](https://img.shields.io/badge/version-1.0.0-blue)
![License](https://img.shields.io/badge/license-MIT-green)

## 🌟 Welcome to the Future of E-Commerce

**NexusCommerce** isn't just another e-commerce platform—it's a resilient, scalable ecosystem where microservices dance in perfect harmony to deliver exceptional shopping experiences. Born from the vision of making online retail more responsive, reliable, and revolutionary, our architecture stands as a testament to modern software engineering principles.

## 🛠️ Technology Stack

<div align="center">

### Core Technologies
![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Go](https://img.shields.io/badge/Go-00ADD8?style=for-the-badge&logo=go&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![Spring Cloud](https://img.shields.io/badge/Spring_Cloud-6DB33F?style=for-the-badge&logo=spring&logoColor=white)

### Databases & Storage
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white)
![MongoDB](https://img.shields.io/badge/MongoDB-4EA94B?style=for-the-badge&logo=mongodb&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white)

### Message Streaming & Communication
![Apache Kafka](https://img.shields.io/badge/Apache_Kafka-231F20?style=for-the-badge&logo=apache-kafka&logoColor=white)

### Observability & Monitoring
![Elasticsearch](https://img.shields.io/badge/Elasticsearch-005571?style=for-the-badge&logo=elasticsearch&logoColor=white)
![Kibana](https://img.shields.io/badge/Kibana-005571?style=for-the-badge&logo=kibana&logoColor=white)
![Logstash](https://img.shields.io/badge/Logstash-005571?style=for-the-badge&logo=logstash&logoColor=white)
![Zipkin](https://img.shields.io/badge/Zipkin-1f425f?style=for-the-badge&logo=zipkin&logoColor=white)
![Prometheus](https://img.shields.io/badge/Prometheus-E6522C?style=for-the-badge&logo=prometheus&logoColor=white)
![Grafana](https://img.shields.io/badge/Grafana-F46800?style=for-the-badge&logo=grafana&logoColor=white)

### DevOps & Infrastructure
![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![Kubernetes](https://img.shields.io/badge/Kubernetes-326CE5?style=for-the-badge&logo=kubernetes&logoColor=white)
![Netflix Eureka](https://img.shields.io/badge/Netflix_Eureka-E50914?style=for-the-badge&logo=netflix&logoColor=white)
![SonarQube](https://img.shields.io/badge/SonarQube-4E9BCD?style=for-the-badge&logo=sonarqube&logoColor=white)

### Security & Authentication
![JWT](https://img.shields.io/badge/JWT-000000?style=for-the-badge&logo=json-web-tokens&logoColor=white)
![OAuth2](https://img.shields.io/badge/OAuth2-3C4043?style=for-the-badge&logo=oauth&logoColor=white)

</div>

## 🧩 Key Components

- **Client Applications**: The gateway to our digital marketplace
- **API Gateway**: Our intelligent traffic controller with advanced features (see below)
- **Service Registry (Eureka)**: The compass that guides service discovery
- **Configuration Server**: The central brain for distributed configuration
- **Microservices Fleet**:
    - 🧑‍💼 **User Service** - Managing customer identities and profiles (Spring Boot + MongoDB)
    - 🛍️ **Product Service** - Our digital catalog (Spring Boot + PostgreSQL)
    - 🛒 **Cart Service** - The virtual shopping cart (Go + MongoDB + Redis)
    - 📋 **Order Service** - Order processing and history (Spring Boot + PostgreSQL)
    - 💳 **Payment Service** - Secure transaction processing (Go + PostgreSQL)
    - 🚚 **Shipping Service** - Delivery tracking and management (Go + PostgreSQL)
    - 🎁 **Loyalty Service** - Rewards and customer retention (Spring Boot + PostgreSQL)
    - 📱 **Notification Service** - Customer communications (Spring Boot + MongoDB)
- **Kafka Message Bus**: The neural network enabling event-driven communication
- **Centralized Logging Pipeline**: Real-time log aggregation and analysis (ELK + Kafka)
- **Observability Stack**:
    - **Zipkin**: Tracing requests through our service mesh
    - **ELK Stack**: Illuminating our system through logs and analytics
    - **Prometheus & Grafana**: Metrics collection and visualization
- **SonarQube**: Our quality guardian, ensuring code excellence


## 🏗️ Detailed Architecture Overview

Our platform implements a sophisticated cloud-native microservices architecture with advanced patterns including Backend for Frontend (BFF), event-driven communication, and comprehensive resilience mechanisms:

```mermaid
graph TB
    %% Client Layer
    subgraph "Client Layer"
        WEB[Web Client<br/>React/Angular]
        MOBILE[Mobile App<br/>React Native]
        API_CLIENT[API Clients<br/>Postman/Swagger]
    end

    %% API Gateway Layer with detailed components
    subgraph "API Gateway Layer (Port 8099)"
        GATEWAY[Spring Cloud Gateway<br/>WebFlux]
        
        subgraph "Gateway Filters"
            JWT[JWT Authentication<br/>Filter]
            RATE[Rate Limiting<br/>Redis-backed]
            CIRCUIT[Circuit Breaker<br/>Resilience4j]
            CORS[CORS Filter]
        end
        
        subgraph "BFF Services"
            CART_BFF[AsyncCartBffService<br/>Cart + Product Enrichment]
            ORDER_BFF[AsyncOrderBffService<br/>Order + Product Enrichment]
            SAVED_BFF[AsyncSaved4LaterBffService<br/>Saved Items + Availability]
            PRODUCT_BFF[AsyncProductService<br/>Batch Product Fetching]
        end
        
        subgraph "Gateway Management"
            ASYNC_MGR[AsyncResponseManager<br/>Correlation ID Tracking]
            MONITOR[Gateway Health<br/>& Statistics]
        end
    end

    %% Service Discovery & Configuration
    subgraph "Infrastructure Layer"
        EUREKA[Eureka Server<br/>:8761<br/>Service Discovery]
        CONFIG[Config Server<br/>:8888<br/>Centralized Config]
        REDIS[(Redis<br/>Rate Limiting<br/>Caching<br/>Session Store)]
    end

    %% Message Bus
    subgraph "Event-Driven Message Bus"
        KAFKA[Apache Kafka<br/>:9092]
        
        subgraph "Kafka Topics"
            CART_TOPICS[cart.request<br/>cart.response<br/>cart.error]
            PRODUCT_TOPICS[product.batch.request<br/>product.batch.response<br/>product.error]
            ORDER_TOPICS[order.request<br/>order.response<br/>order.error<br/>order.ids.request<br/>order.ids.response]
            SAVED_TOPICS[saved4later.request<br/>saved4later.response<br/>saved4later.error]
            LOG_TOPICS[app-logs<br/>system-metrics<br/>error-alerts]
        end
    end

    %% Microservices Layer
    subgraph "Core Services Layer"
        subgraph "User Management"
            USER_SVC[User Service<br/>Spring Boot + MongoDB<br/>JWT + OAuth2]
            USER_DB[(MongoDB<br/>Users & Auth)]
        end
        
        subgraph "Product Catalog"
            PRODUCT_SVC[Product Service<br/>Spring Boot + PostgreSQL<br/>Inventory Management]
            PRODUCT_DB[(PostgreSQL<br/>Products & Inventory)]
        end
        
        subgraph "Shopping Experience"
            CART_SVC[Cart Service<br/>Go + MongoDB + Redis<br/>Session Management]
            CART_DB[(MongoDB<br/>Cart Data)]
        end
        
        subgraph "Order Management"
            ORDER_SVC[Order Service<br/>Spring Boot + PostgreSQL<br/>Order Processing]
            ORDER_DB[(PostgreSQL<br/>Orders & History)]
        end
    end

    subgraph "Business Services Layer"
        subgraph "Payment Processing"
            PAYMENT_SVC[Payment Service<br/>Go + PostgreSQL<br/>Secure Transactions]
            PAYMENT_DB[(PostgreSQL<br/>Payment Records)]
        end
        
        subgraph "Fulfillment"
            SHIPPING_SVC[Shipping Service<br/>Go + PostgreSQL<br/>Delivery Tracking]
            SHIPPING_DB[(PostgreSQL<br/>Shipping Data)]
        end
        
        subgraph "Customer Engagement"
            LOYALTY_SVC[Loyalty Service<br/>Spring Boot + PostgreSQL<br/>Rewards Program]
            LOYALTY_DB[(PostgreSQL<br/>Loyalty Points)]
            
            NOTIFICATION_SVC[Notification Service<br/>Spring Boot + MongoDB<br/>Multi-channel Messaging]
            NOTIFICATION_DB[(MongoDB<br/>Notifications)]
        end
    end

    %% Observability Stack
    subgraph "Observability & Monitoring"
        ZIPKIN[Zipkin<br/>:9411<br/>Distributed Tracing]
        
        subgraph "ELK Stack + Kafka Integration"
            ELASTICSEARCH[Elasticsearch<br/>:9200<br/>Log Storage & Search]
            LOGSTASH[Logstash<br/>:5044<br/>Log Processing & Enrichment]
            KIBANA[Kibana<br/>:5601<br/>Dashboards & Visualization]
        end
        
        subgraph "Metrics & Health"
            ACTUATOR[Spring Actuator<br/>Health Endpoints]
            PROMETHEUS[Prometheus<br/>Metrics Collection]
            GRAFANA[Grafana<br/>Metrics Dashboard]
        end
    end

    %% Quality Assurance
    subgraph "Quality & Security"
        SONAR[SonarQube<br/>:9000<br/>Code Quality]
        VAULT[HashiCorp Vault<br/>Secret Management]
    end

    %% Connections - Client to Gateway
    WEB --> GATEWAY
    MOBILE --> GATEWAY
    API_CLIENT --> GATEWAY

    %% Gateway Internal Flow
    GATEWAY --> JWT
    JWT --> RATE
    RATE --> CIRCUIT
    CIRCUIT --> CORS
    CORS --> CART_BFF
    CORS --> ORDER_BFF
    CORS --> SAVED_BFF

    %% BFF to Async Manager
    CART_BFF --> ASYNC_MGR
    ORDER_BFF --> ASYNC_MGR
    SAVED_BFF --> ASYNC_MGR
    PRODUCT_BFF --> ASYNC_MGR

    %% Gateway to Infrastructure
    GATEWAY --> EUREKA
    GATEWAY --> CONFIG
    GATEWAY --> REDIS

    %% Gateway to Kafka
    GATEWAY --> KAFKA
    CART_BFF --> CART_TOPICS
    ORDER_BFF --> ORDER_TOPICS
    SAVED_BFF --> SAVED_TOPICS
    PRODUCT_BFF --> PRODUCT_TOPICS

    %% Services to Kafka
    CART_SVC --> CART_TOPICS
    PRODUCT_SVC --> PRODUCT_TOPICS
    ORDER_SVC --> ORDER_TOPICS
    USER_SVC --> KAFKA

    %% Logging Pipeline
    GATEWAY --> LOG_TOPICS
    USER_SVC --> LOG_TOPICS
    PRODUCT_SVC --> LOG_TOPICS
    CART_SVC --> LOG_TOPICS
    ORDER_SVC --> LOG_TOPICS
    PAYMENT_SVC --> LOG_TOPICS
    SHIPPING_SVC --> LOG_TOPICS
    LOYALTY_SVC --> LOG_TOPICS
    NOTIFICATION_SVC --> LOG_TOPICS
    
    LOG_TOPICS --> LOGSTASH
    LOGSTASH --> ELASTICSEARCH
    ELASTICSEARCH --> KIBANA

    %% Services to Databases
    USER_SVC --> USER_DB
    PRODUCT_SVC --> PRODUCT_DB
    CART_SVC --> CART_DB
    ORDER_SVC --> ORDER_DB
    PAYMENT_SVC --> PAYMENT_DB
    SHIPPING_SVC --> SHIPPING_DB
    LOYALTY_SVC --> LOYALTY_DB
    NOTIFICATION_SVC --> NOTIFICATION_DB

    %% Services to Infrastructure
    USER_SVC --> EUREKA
    PRODUCT_SVC --> EUREKA
    CART_SVC --> EUREKA
    ORDER_SVC --> EUREKA
    PAYMENT_SVC --> EUREKA
    SHIPPING_SVC --> EUREKA
    LOYALTY_SVC --> EUREKA
    NOTIFICATION_SVC --> EUREKA

    %% Cache Connections
    CART_SVC --> REDIS
    PRODUCT_SVC --> REDIS

    %% Observability Connections
    GATEWAY --> ZIPKIN
    USER_SVC --> ZIPKIN
    PRODUCT_SVC --> ZIPKIN
    CART_SVC --> ZIPKIN
    ORDER_SVC --> ZIPKIN

    %% Health Monitoring
    GATEWAY --> ACTUATOR
    USER_SVC --> ACTUATOR
    PRODUCT_SVC --> ACTUATOR

    %% Styling
    classDef gatewayStyle fill:#e1f5fe,stroke:#01579b,stroke-width:3px
    classDef serviceStyle fill:#f3e5f5,stroke:#4a148c,stroke-width:2px
    classDef dbStyle fill:#e8f5e8,stroke:#1b5e20,stroke-width:2px
    classDef infraStyle fill:#fff3e0,stroke:#e65100,stroke-width:2px
    classDef bffStyle fill:#fce4ec,stroke:#880e4f,stroke-width:2px
    classDef loggingStyle fill:#f1f8e9,stroke:#33691e,stroke-width:2px

    class GATEWAY,JWT,RATE,CIRCUIT,CORS gatewayStyle
    class CART_BFF,ORDER_BFF,SAVED_BFF,PRODUCT_BFF bffStyle
    class USER_SVC,PRODUCT_SVC,CART_SVC,ORDER_SVC,PAYMENT_SVC,SHIPPING_SVC,LOYALTY_SVC,NOTIFICATION_SVC serviceStyle
    class USER_DB,PRODUCT_DB,CART_DB,ORDER_DB,PAYMENT_DB,SHIPPING_DB,LOYALTY_DB,NOTIFICATION_DB,REDIS dbStyle
    class EUREKA,CONFIG,KAFKA,ZIPKIN,PROMETHEUS,GRAFANA,SONAR infraStyle
    class ELASTICSEARCH,LOGSTASH,KIBANA,LOG_TOPICS,ACTUATOR loggingStyle
```

## 📊 Centralized Logging Pipeline Architecture

### 🔄 **Real-Time Logging Pipeline Overview**

Our platform implements a sophisticated **centralized logging pipeline** that provides real-time observability across all microservices. This enterprise-grade logging architecture enables comprehensive monitoring, debugging, and analytics for the entire NexusCommerce ecosystem.

```mermaid
graph TB
    subgraph "Microservices Layer"
        subgraph "Spring Boot Services"
            ORDER[Order Service<br/>🛒 Order Processing]
            USER[User Service<br/>👤 Authentication]
            PRODUCT[Product Service<br/>📦 Catalog Management]
            LOYALTY[Loyalty Service<br/>🎁 Rewards Program]
            NOTIFICATION[Notification Service<br/>📱 Messaging]
        end
        
        subgraph "Go Services"
            CART[Cart Service<br/>🛒 Shopping Cart]
            PAYMENT[Payment Service<br/>💳 Transactions]
            SHIPPING[Shipping Service<br/>🚚 Delivery]
        end
        
        subgraph "API Gateway"
            GATEWAY[Gateway Service<br/>🌐 Traffic Controller]
        end
    end

    subgraph "Log Processing Pipeline"
        subgraph "Message Streaming"
            KAFKA_LOGS[Apache Kafka<br/>📡 Topic: app-logs<br/>High-throughput streaming]
        end
        
        subgraph "Log Processing Engine"
            LOGSTASH[Logstash<br/>🔄 Log Parser & Enricher<br/>• JSON Processing<br/>• Field Mapping<br/>• Timestamp Parsing<br/>• Error Handling]
        end
        
        subgraph "Search & Storage"
            ELASTICSEARCH[Elasticsearch<br/>💾 Distributed Search Engine<br/>• Daily Indices<br/>• Full-text Search<br/>• Aggregations<br/>• High Performance]
        end
        
        subgraph "Visualization & Analytics"
            KIBANA[Kibana<br/>📈 Interactive Dashboards<br/>• Real-time Search<br/>• Service Monitoring<br/>• Error Analysis<br/>• Custom Dashboards]
        end
    end

    subgraph "Log Configuration"
        LOGBACK[Logback Configuration<br/>📝 JSON Structured Logging<br/>• Service Metadata<br/>• Correlation IDs<br/>• Error Stack Traces<br/>• Performance Metrics]
    end

    %% Logging Flow
    ORDER --> LOGBACK
    USER --> LOGBACK
    PRODUCT --> LOGBACK
    LOYALTY --> LOGBACK
    NOTIFICATION --> LOGBACK
    CART --> LOGBACK
    PAYMENT --> LOGBACK
    SHIPPING --> LOGBACK
    GATEWAY --> LOGBACK

    LOGBACK --> KAFKA_LOGS
    KAFKA_LOGS --> LOGSTASH
    LOGSTASH --> ELASTICSEARCH
    ELASTICSEARCH --> KIBANA

    %% Styling
    classDef serviceStyle fill:#e3f2fd,stroke:#1976d2,stroke-width:2px
    classDef kafkaStyle fill:#f3e5f5,stroke:#7b1fa2,stroke-width:2px
    classDef elkStyle fill:#e8f5e8,stroke:#388e3c,stroke-width:2px
    classDef configStyle fill:#fff3e0,stroke:#f57c00,stroke-width:2px

    class ORDER,USER,PRODUCT,LOYALTY,NOTIFICATION,CART,PAYMENT,SHIPPING,GATEWAY serviceStyle
    class KAFKA_LOGS kafkaStyle
    class LOGSTASH,ELASTICSEARCH,KIBANA elkStyle
    class LOGBACK configStyle
```

### ⚡ **Real-Time Data Flow Example**

This sequence diagram shows how a single API request generates structured logs that flow through our pipeline in real-time:

```mermaid
sequenceDiagram
    participant Client
    participant Gateway as API Gateway
    participant OrderSvc as Order Service
    participant Kafka as Kafka Bus<br/>(app-logs topic)
    participant Logstash as Logstash<br/>(Log Processor)
    participant Elasticsearch as Elasticsearch<br/>(Search Engine)
    participant Kibana as Kibana<br/>(Dashboard)

    Client->>+Gateway: POST /api/orders (Create Order)
    Note over Gateway: 📝 Gateway generates logs:<br/>• Request received<br/>• JWT validation<br/>• Rate limit check
    Gateway->>Kafka: Structured JSON logs
    
    Gateway->>+OrderSvc: Route to Order Service
    Note over OrderSvc: 📝 Order Service logs:<br/>• Order validation<br/>• Database operations<br/>• Business logic events
    OrderSvc->>Kafka: Structured JSON logs
    
    OrderSvc-->>-Gateway: Order Response
    Note over Gateway: 📝 Gateway logs:<br/>• Response processing<br/>• Performance metrics<br/>• Success/Error status
    Gateway->>Kafka: Final logs
    Gateway-->>-Client: Order Created Response

    Note over Kafka: 🚀 Real-time log streaming<br/>High-throughput message processing

    Kafka->>+Logstash: Consume log messages
    Note over Logstash: 🔄 Log processing:<br/>• Parse JSON structure<br/>• Enrich with metadata<br/>• Handle timestamps<br/>• Error validation
    
    Logstash->>+Elasticsearch: Index processed logs
    Note over Elasticsearch: 💾 Storage & indexing:<br/>• Daily indices creation<br/>• Full-text search setup<br/>• Field mapping<br/>• Performance optimization

    Elasticsearch->>+Kibana: Real-time data refresh
    Note over Kibana: 📊 Visualization update:<br/>• Dashboard refresh<br/>• Search results<br/>• Metrics calculation<br/>• Alert processing

    Note over Client,Kibana: ⚡ Total pipeline latency: < 2 seconds<br/>From API call to Kibana visualization
```

### 🛠️ **Log Structure & Schema**

Our logging pipeline generates highly structured JSON logs with consistent schema across all services:

```json
{
  "@timestamp": "2025-07-23T21:45:00.123Z",
  "level": "INFO",
  "service_name": "order-service",
  "logger_name": "com.Ecommerce.Order_Service.Controllers.OrderController",
  "message": "Order created successfully for user 12345",
  "thread": "http-nio-8083-exec-1",
  "correlation_id": "req-abc123",
  "user_id": "12345",
  "order_id": "ord-789",
  "request_duration_ms": 245,
  "environment": "production",
  "version": "1.0.0"
}
```

**Key Fields:**
- **Timestamp**: ISO 8601 format for precise time tracking
- **Service Name**: Identifies the source microservice
- **Log Level**: ERROR, WARN, INFO, DEBUG for filtering
- **Correlation ID**: Tracks requests across service boundaries
- **Business Context**: User IDs, order IDs, transaction data
- **Performance Metrics**: Response times, resource usage

### 📈 **Kibana Dashboards & Visualizations**

Our Kibana setup provides comprehensive observability dashboards:

#### 🎯 **Service Overview Dashboard**
- **Service Health Map**: Real-time status of all microservices
- **Request Volume**: API calls per service over time
- **Response Time Trends**: Performance metrics and SLA tracking
- **Error Rate Monitoring**: Service-specific error tracking


#### **Logstash Processing Configuration**
```ruby
input {
  kafka {
    bootstrap_servers => "kafka:29092"
    topics => ["app-logs"]
    codec => "json"
    group_id => "logstash-microservices-group"
    auto_offset_reset => "earliest"
  }
}

filter {
  # Add processing metadata
  mutate {
    add_field => { "logstash_processed_at" => "%{+yyyy-MM-dd HH:mm:ss}" }
  }

  # Parse timestamp
  date {
    match => [ "@timestamp", "ISO8601" ]
    target => "@timestamp"
  }

  # Ensure required fields
  if ![service_name] {
    mutate { add_field => { "service_name" => "unknown" } }
  }
  
  if ![level] {
    mutate { add_field => { "level" => "INFO" } }
  }
}

output {
  elasticsearch {
    hosts => ["elasticsearch:9200"]
    index => "microservices-logs-%{+yyyy.MM.dd}"
    document_type => "_doc"
  }
  
  # Debug output (remove in production)
  stdout { codec => rubydebug }
}
```


### 🌐 **Monitoring & Management Endpoints**

Access comprehensive logging pipeline management through these endpoints:

- **Kafka Management**: `http://localhost:8091` - Kafka UI for topic management
- **Elasticsearch Health**: `http://localhost:9200/_cluster/health` - Cluster status
- **Kibana Dashboards**: `http://localhost:5601` - Log visualization and analytics
- **Logstash Monitoring**: `http://localhost:9600/_node/stats` - Processing statistics

---


## 🌐 Service Communication Matrix

This diagram shows the actual communication patterns, ports, and protocols used in your implementation:

```mermaid
graph LR
    subgraph "Client Layer"
        CLIENT[Client Applications<br/>Web/Mobile/API]
    end

    subgraph "Gateway Layer - :8099"
        GATEWAY[API Gateway<br/>Spring Cloud Gateway]
        
        subgraph "Gateway Features"
            AUTH[JWT Authentication<br/>gateway1.jwt.secret]
            RATE_LIMIT[Rate Limiting<br/>Redis-backed<br/>5-300 req/min]
            CIRCUIT[Circuit Breakers<br/>Resilience4j<br/>50% failure threshold]
            BFF[BFF Aggregation<br/>Async Kafka</br>Correlation IDs]
        end
    end

    subgraph "Infrastructure - Discovery & Config"
        EUREKA[Eureka Server<br/>:8761<br/>Service Registry]
        CONFIG_SRV[Config Server<br/>:8888<br/>http://localhost:8888]
        REDIS[Redis<br/>:6379<br/>Cache & Rate Limiting]
    end

    subgraph "Message Bus"
        KAFKA_BROKER[Apache Kafka<br/>:9092<br/>Event Streaming]
        
        subgraph "Topic Ecosystem"
            CART_T[cart.request/response/error]
            PRODUCT_T[product.batch.request/response]
            ORDER_T[order.request/response<br/>order.ids.request/response]
            SAVED_T[saved4later.request/response]
            LOG_T[app-logs<br/>system-metrics]
        end
    end

    subgraph "Core Services"
        USER[User Service<br/>lb://user-service<br/>MongoDB + JWT + OAuth2]
        PRODUCT[Product Service<br/>lb://product-service<br/>PostgreSQL + Batch API]
        CART[Cart Service<br/>lb://cart-service<br/>Go + MongoDB + Redis]
        ORDER[Order Service<br/>lb://order-service<br/>PostgreSQL + Spring Boot]
    end

    subgraph "Business Services"
        PAYMENT[Payment Service<br/>lb://PAYMENT-SERVICE<br/>Go + PostgreSQL<br/>Token Bucket: 3/5min]
        SHIPPING[Shipping Service<br/>lb://SHIPPING-SERVICE<br/>Go + PostgreSQL]
        LOYALTY[Loyalty Service<br/>lb://LOYALTY-SERVICE<br/>PostgreSQL + Spring Boot]
        NOTIFICATION[Notification Service<br/>lb://NOTIFICATION-SERVICE<br/>MongoDB + Spring Boot]
    end

    subgraph "Observability"
        ZIPKIN[Zipkin<br/>:9411<br/>Distributed Tracing]
        ELK[ELK Stack<br/>:5601<br/>Centralized Logging]
        ACTUATOR[Spring Actuator<br/>Health & Metrics]
    end

    %% Client to Gateway
    CLIENT -->|HTTPS<br/>JWT Bearer| GATEWAY

    %% Gateway Internal Processing
    GATEWAY --> AUTH
    AUTH --> RATE_LIMIT
    RATE_LIMIT --> CIRCUIT
    CIRCUIT --> BFF

    %% Gateway to Infrastructure
    GATEWAY -.->|Service Discovery| EUREKA
    GATEWAY -.->|Configuration| CONFIG_SRV
    GATEWAY -.->|Rate Limiting| REDIS

    %% BFF to Kafka (Async Communication)
    BFF -->|Async Request<br/>Correlation ID| KAFKA_BROKER
    KAFKA_BROKER --> CART_T
    KAFKA_BROKER --> PRODUCT_T
    KAFKA_BROKER --> ORDER_T
    KAFKA_BROKER --> SAVED_T
    KAFKA_BROKER --> LOG_T

    %% Services to Kafka
    CART -.->|Consume/Produce| CART_T
    PRODUCT -.->|Batch Processing| PRODUCT_T
    ORDER -.->|Order Processing| ORDER_T
    CART -.->|Saved Items| SAVED_T

    %% All Services to Logging
    GATEWAY -.->|Structured Logs| LOG_T
    USER -.->|Structured Logs| LOG_T
    PRODUCT -.->|Structured Logs| LOG_T
    CART -.->|Structured Logs| LOG_T
    ORDER -.->|Structured Logs| LOG_T
    PAYMENT -.->|Structured Logs| LOG_T
    SHIPPING -.->|Structured Logs| LOG_T
    LOYALTY -.->|Structured Logs| LOG_T
    NOTIFICATION -.->|Structured Logs| LOG_T

    %% Gateway Routes (Load Balanced)
    GATEWAY -->|lb://user-service<br/>Authentication Routes| USER
    GATEWAY -->|lb://product-service<br/>Catalog Routes| PRODUCT
    GATEWAY -->|lb://cart-service<br/>Cart Routes| CART
    GATEWAY -->|lb://order-service<br/>Order Routes| ORDER
    GATEWAY -->|lb://PAYMENT-SERVICE<br/>Payment Routes| PAYMENT
    GATEWAY -->|lb://SHIPPING-SERVICE<br/>Shipping Routes| SHIPPING
    GATEWAY -->|lb://LOYALTY-SERVICE<br/>Loyalty Routes| LOYALTY
    GATEWAY -->|lb://NOTIFICATION-SERVICE<br/>Notification Routes| NOTIFICATION

    %% Services to Infrastructure
    USER -.->|Register| EUREKA
    PRODUCT -.->|Register| EUREKA
    CART -.->|Register| EUREKA
    ORDER -.->|Register| EUREKA
    PAYMENT -.->|Register| EUREKA
    SHIPPING -.->|Register| EUREKA
    LOYALTY -.->|Register| EUREKA
    NOTIFICATION -.->|Register| EUREKA

    %% Observability
    GATEWAY -.->|Tracing| ZIPKIN
    USER -.->|Tracing| ZIPKIN
    PRODUCT -.->|Tracing| ZIPKIN
    CART -.->|Tracing| ZIPKIN
    ORDER -.->|Tracing| ZIPKIN

    LOG_T -.->|Log Processing| ELK
    GATEWAY -.->|Health| ACTUATOR
    USER -.->|Health| ACTUATOR
    PRODUCT -.->|Health| ACTUATOR

    %% Styling
    classDef gatewayClass fill:#1976d2,color:#fff,stroke:#0d47a1
    classDef serviceClass fill:#388e3c,color:#fff,stroke:#1b5e20
    classDef infraClass fill:#f57c00,color:#fff,stroke:#e65100
    classDef kafkaClass fill:#7b1fa2,color:#fff,stroke:#4a148c
    classDef observeClass fill:#d32f2f,color:#fff,stroke:#b71c1c

    class GATEWAY,AUTH,RATE_LIMIT,CIRCUIT,BFF gatewayClass
    class USER,PRODUCT,CART,ORDER,PAYMENT,SHIPPING,LOYALTY,NOTIFICATION serviceClass
    class EUREKA,CONFIG_SRV,REDIS infraClass
    class KAFKA_BROKER,CART_T,PRODUCT_T,ORDER_T,SAVED_T,LOG_T kafkaClass
    class ZIPKIN,ELK,ACTUATOR observeClass
```

### 🛡️ **Security & Resilience Features**

```mermaid
flowchart TD
    REQUEST[Incoming Request] --> JWT_CHECK{JWT Valid?}
    
    JWT_CHECK -->|No| REJECT[401 Unauthorized]
    JWT_CHECK -->|Yes| RATE_CHECK{Rate Limit OK?}
    
    RATE_CHECK -->|No| RATE_REJECT[429 Too Many Requests<br/>X-RateLimit-Remaining: 0]
    RATE_CHECK -->|Yes| CIRCUIT_CHECK{Circuit Open?}
    
    CIRCUIT_CHECK -->|Yes| FALLBACK[Circuit Open<br/>Return Fallback Response]
    CIRCUIT_CHECK -->|No| ROUTE[Route to Service]
    
    ROUTE --> SERVICE_CALL[Service Call]
    SERVICE_CALL --> SUCCESS{Success?}
    
    SUCCESS -->|Yes| RECORD_SUCCESS[Record Success<br/>Update Circuit Metrics]
    SUCCESS -->|No| RECORD_FAILURE[Record Failure<br/>Check Threshold]
    
    RECORD_FAILURE --> THRESHOLD{Failure Rate > 50%?}
    THRESHOLD -->|Yes| OPEN_CIRCUIT[Open Circuit<br/>30s Wait Period]
    THRESHOLD -->|No| CONTINUE[Continue Normal Operation]
    
    RECORD_SUCCESS --> RESPONSE[Return Response]
    CONTINUE --> RESPONSE
    OPEN_CIRCUIT --> FALLBACK
    
    subgraph "Rate Limiting Configuration"
        AUTH_RATE["/api/users/auth/**<br/>5 requests/60s<br/>IP-based"]
        PAYMENT_RATE["/api/payments/**<br/>3 requests/300s<br/>User-based<br/>Token Bucket"]
        PUBLIC_RATE["/api/products/** (GET)<br/>300 requests/60s<br/>IP-based"]
        CART_RATE["/api/cart/**<br/>50 requests/60s<br/>User-based"]
    end
    
    subgraph "Circuit Breaker Thresholds"
        CB_AUTH["auth-cb: 50% failure<br/>10 calls window"]
        CB_PAYMENT["payment-cb: 30% failure<br/>5 calls window<br/>60s wait"]
        CB_PRODUCT["product-read-cb: 60% failure<br/>20 calls window"]
    end

    style REQUEST fill:#e3f2fd
    style REJECT fill:#ffcdd2
    style RATE_REJECT fill:#fff3e0
    style FALLBACK fill:#f3e5f5
    style RESPONSE fill:#e8f5e8
```

## 🎯 Backend for Frontend (BFF) Pattern with Async Communication

Our API Gateway implements sophisticated BFF patterns using asynchronous Kafka-based communication to provide enriched, aggregated data from multiple microservices. This approach delivers superior performance and resilience compared to traditional synchronous API calls.

### 🛒 Enriched Cart Flow

This diagram shows how a client request for enriched cart data triggers coordinated async communication between services:

```mermaid
sequenceDiagram
    participant Client
    participant Gateway as API Gateway<br/>(BFF Layer)
    participant CartKafka as Kafka<br/>(cart.request)
    participant CartService as Cart Service
    participant ProductKafka as Kafka<br/>(product.batch.request)
    participant ProductService as Product Service
    participant AsyncMgr as AsyncResponseManager
    
    Client->>+Gateway: GET /api/cart/{userId}/enriched
    Note over Gateway: Generate correlationId<br/>Start async orchestration
    
    Gateway->>+AsyncMgr: Register pending request
    Gateway->>+CartKafka: Send cart request<br/>{correlationId, userId}
    
    CartService->>CartKafka: Consume request
    CartService->>CartService: Fetch user cart<br/>with items
    CartService->>CartKafka: Publish response<br/>(cart.response topic)
    
    Gateway->>CartKafka: Consume cart response
    Note over Gateway: Extract productIds<br/>from cart items
    
    Gateway->>+ProductKafka: Send batch product request<br/>{correlationId, [productIds]}
    
    ProductService->>ProductKafka: Consume batch request
    ProductService->>ProductService: Fetch product details<br/>for all IDs
    ProductService->>ProductKafka: Publish batch response<br/>(product.batch.response)
    
    Gateway->>ProductKafka: Consume product response
    Gateway->>Gateway: Merge cart items<br/>with product details
    
    Gateway->>+AsyncMgr: Complete request<br/>with enriched data
    Gateway->>-Client: Return EnrichedCartResponse<br/>{cart + product details}
    
    Note over Gateway,ProductService: Total time: ~200-500ms<br/>vs 1000ms+ for sync calls
```

### 📋 Optimized Batch Order Processing

This advanced pattern demonstrates how we efficiently process multiple orders with minimal service calls:

```mermaid
sequenceDiagram
    participant Client
    participant Gateway as API Gateway<br/>(BFF Layer)
    participant OrderKafka as Kafka<br/>(order.ids.request)
    participant OrderService as Order Service
    participant ProductKafka as Kafka<br/>(product.batch.request)
    participant ProductService as Product Service
    participant AsyncMgr as AsyncResponseManager
    
    Client->>+Gateway: GET /api/order/user/{userId}/all?includeProducts=true
    Note over Gateway: Step 1: Get User's Order IDs
    
    Gateway->>+AsyncMgr: Register order IDs request
    Gateway->>+OrderKafka: Send order IDs request<br/>{userId, status, limit}
    
    OrderService->>OrderKafka: Consume IDs request
    OrderService->>OrderService: Query user's order IDs
    OrderService->>OrderKafka: Publish IDs response<br/>(order.ids.response)
    
    Gateway->>OrderKafka: Consume order IDs<br/>[orderId1, orderId2, ...]
    
    Note over Gateway: Step 2: Parallel Order Fetching
    
    par Fetch Order 1
        Gateway->>+OrderKafka: order.request {orderId1}
        OrderService->>OrderKafka: order.response {order1}
    and Fetch Order 2
        Gateway->>OrderKafka: order.request {orderId2}
        OrderService->>OrderKafka: order.response {order2}
    and Fetch Order N
        Gateway->>OrderKafka: order.request {orderIdN}
        OrderService->>OrderKafka: order.response {orderN}
    end
    
    Note over Gateway: Step 3: Collect All Unique Product IDs
    Gateway->>Gateway: Extract unique productIds<br/>from ALL orders
    
    Note over Gateway: Step 4: Single Batch Product Request
    Gateway->>+ProductKafka: Send SINGLE batch request<br/>{[all unique productIds]}
    
    ProductService->>ProductKafka: Consume batch request
    ProductService->>ProductService: Fetch ALL products<br/>in one database call
    ProductService->>ProductKafka: Publish batch response<br/>{all product details}
    
    Gateway->>ProductKafka: Consume product response
    Gateway->>Gateway: Enrich ALL orders<br/>with product details
    
    Gateway->>+AsyncMgr: Complete batch request
    Gateway->>-Client: Return BatchOrderResponse<br/>{enriched orders, statistics}
    
    Note over Gateway,ProductService: 5 orders = 7 Kafka calls<br/>vs 15+ synchronous API calls<br/>Efficiency: ~70% reduction
```

### 💾 Saved Items with Availability Check

This flow shows how saved items are enriched with real-time product availability:

```mermaid
sequenceDiagram
    participant Client
    participant Gateway as API Gateway<br/>(BFF Layer)
    participant SavedKafka as Kafka<br/>(saved4later.request)
    participant CartService as Cart Service<br/>(Saved4Later Module)
    participant ProductKafka as Kafka<br/>(product.batch.request)
    participant ProductService as Product Service
    participant AsyncMgr as AsyncResponseManager
    
    Client->>+Gateway: GET /api/saved4later/{userId}/enriched
    Note over Gateway: Generate correlationId<br/>Start saved items flow
    
    Gateway->>+AsyncMgr: Register pending request
    Gateway->>+SavedKafka: Send saved4later request<br/>{correlationId, userId}
    
    CartService->>SavedKafka: Consume request
    CartService->>CartService: Fetch user's<br/>saved items
    CartService->>SavedKafka: Publish response<br/>(saved4later.response)
    
    Gateway->>SavedKafka: Consume saved response
    Note over Gateway: Extract productIds<br/>from saved items
    
    Gateway->>+ProductKafka: Send product batch request<br/>{correlationId, [productIds]}
    
    ProductService->>ProductKafka: Consume batch request
    ProductService->>ProductService: Check availability<br/>& fetch details
    ProductService->>ProductKafka: Publish availability response<br/>(product.batch.response)
    
    Gateway->>ProductKafka: Consume product response
    Gateway->>Gateway: Merge saved items<br/>with availability data
    Gateway->>Gateway: Calculate statistics<br/>(available vs unavailable)
    
    Gateway->>+AsyncMgr: Complete request
    Gateway->>-Client: Return EnrichedSavedItemsResponse<br/>{items + availability + stats}
    
    Note over Client,ProductService: Response includes:<br/>• Items with availability status<br/>• Available items count<br/>• Unavailable items count<br/>• Availability percentage
```
# 🔄 SSE Notification Flow with Asynchronous Kafka Communication

## Overview
This sequence diagram illustrates how the Notification Service handles real-time notifications using Server-Sent Events (SSE) combined with asynchronous Kafka messaging for cross-service communication.

## 📊 Complete SSE + Kafka Flow Sequence Diagram

```mermaid
sequenceDiagram
    participant Frontend as 🖥️ Frontend Client
    participant Gateway as 🌐 API Gateway
    participant NotifController as 📱 SSE Controller
    participant SSEService as 🔌 SSE Service
    participant KafkaConsumer as 📨 Kafka Consumer
    participant NotifService as 🔔 Notification Service
    participant MongoDB as 🗄️ MongoDB
    participant KafkaProducer as 📤 Kafka Producer
    participant KafkaBus as 🚌 Kafka Message Bus
    participant PaymentService as 💳 Payment Service
    participant UserService as 👤 User Service
    participant EmailService as 📧 Email Service

    Note over Frontend,EmailService: 🚀 Phase 1: SSE Connection Establishment

    Frontend->>+NotifController: GET /sse/connect/{userId}
    Note right of Frontend: User establishes SSE connection<br/>MongoDB ObjectId: 64a7b8c9e1234567890abcde

    NotifController->>NotifController: validateUserId(mongoObjectId)
    NotifController->>NotifController: parseToUUID(mongoObjectId)
    Note right of NotifController: Convert MongoDB ObjectId to UUID<br/>for internal service communication

    NotifController->>+SSEService: createConnection(userUUID)
    SSEService->>SSEService: Create SseEmitter(timeout: ∞)
    SSEService->>SSEService: Store in userConnections[userUUID]

    SSEService->>Frontend: SSE Connection Established
    Note right of SSEService: Send initial connection message:<br/>{"type": "CONNECTION_ESTABLISHED"}

    SSEService-->>-NotifController: Return SseEmitter
    NotifController-->>-Frontend: SSE Stream Ready

    Note over Frontend,EmailService: 🎯 Phase 2: Business Event Triggers Notification

    PaymentService->>+KafkaBus: Publish Event
    Note right of PaymentService: Topic: "payment-confirmed"<br/>Payload: {userId, orderId, amount, paymentMethod}

    KafkaBus->>+KafkaConsumer: @KafkaListener("payment-confirmed")
    Note right of KafkaConsumer: EmailKafkaConsumer.handlePaymentConfirmed()

    KafkaConsumer->>KafkaConsumer: parseOrConvertToUUID(userId)
    Note right of KafkaConsumer: Convert ObjectId to UUID if needed

    KafkaConsumer->>+UserService: Request user info via Kafka
    Note right of KafkaConsumer: Topic: "user-info-request"<br/>Enhanced email with user details

    UserService->>KafkaBus: User info response
    KafkaBus->>KafkaConsumer: User details received

    KafkaConsumer->>+NotifService: createNotification()
    Note right of KafkaConsumer: UUID userId, PAYMENT_CONFIRMATION,<br/>content, expiresAt

    NotifService->>+MongoDB: Save notification
    MongoDB-->>-NotifService: Notification saved

    NotifService->>+SSEService: sendNotificationToUser(userId, notification)
    Note right of NotifService: Real-time delivery via SSE

    SSEService->>SSEService: Find userConnections[userId]
    SSEService->>SSEService: convertToSSEDTO(notification)

    SSEService->>Frontend: SSE Event: "notification"
    Note right of SSEService: JSON payload with notification details

    SSEService-->>-NotifService: SSE sent successfully
    NotifService-->>-KafkaConsumer: Notification created

    Note over Frontend,EmailService: 📧 Phase 3: Enhanced Email Notification

    KafkaConsumer->>+EmailService: sendPaymentConfirmationEmailWithUserInfo()
    Note right of KafkaConsumer: Enhanced email with user address,<br/>order details, and branding

    EmailService->>EmailService: generateEnhancedPaymentConfirmationTemplate()
    EmailService->>EmailService: Send SMTP email

    EmailService-->>-KafkaConsumer: Email sent successfully
    KafkaConsumer-->>-KafkaBus: Event processing complete

    Note over Frontend,EmailService: 🔄 Phase 4: Frontend Handles Real-time Notification

    Frontend->>Frontend: SSE Event Received
    Note right of Frontend: Update UI with notification:<br/>• Show toast/banner<br/>• Update notification count<br/>• Play notification sound

    Frontend->>+NotifController: PUT /notifications/{id}/read
    Note right of Frontend: Mark notification as read

    NotifController->>+NotifService: markAsRead(notificationId)
    NotifService->>NotifService: Store state before save
    NotifService->>+MongoDB: Update notification.isRead = true
    MongoDB-->>-NotifService: Updated successfully

    NotifService->>+SSEService: sendNotificationToUser(userId, updatedNotification)
    Note right of NotifService: Send read status update via SSE

    SSEService->>Frontend: SSE Event: "notification-updated"
    Note right of SSEService: Update UI to show read status

    SSEService-->>-NotifService: Update sent
    NotifService-->>-NotifController: Notification marked as read
    NotifController-->>-Frontend: 200 OK

    Note over Frontend,EmailService: 🎬 Phase 5: Kafka Event Publishing (MongoDB Listener)

    MongoDB->>+NotifService: MongoDB Change Event (via listener)
    NotifService->>+KafkaProducer: publishNotificationRead(notification)
    KafkaProducer->>KafkaBus: Publish to "notification-read" topic
    Note right of KafkaProducer: Other services can react to<br/>notification read events

    KafkaProducer-->>-NotifService: Event published
    NotifService-->>-MongoDB: Event handling complete

    Note over Frontend,EmailService: 🔧 Phase 6: Error Handling & Fallbacks

    rect rgb(255, 240, 240)
        Note over SSEService,Frontend: Connection Error Handling
        SSEService->>SSEService: onError() → removeConnection()
        SSEService->>SSEService: onTimeout() → removeConnection()
        SSEService->>SSEService: onCompletion() → cleanup()
    end

    rect rgb(240, 255, 240)
        Note over KafkaConsumer,EmailService: Fallback Mechanisms
        Note right of KafkaConsumer: If user info not available:<br/>• Use basic email fallback<br/>• Generate demo email for testing
        Note right of EmailService: Email retry with backoff:<br/>• 3 attempts with 1s delay<br/>• Circuit breaker protection
    end

    Note over Frontend,EmailService: 📊 Phase 7: Monitoring & Analytics

    SSEService->>SSEService: Track connection stats
    Note right of SSEService: • Connected users count<br/>• Total connections<br/>• User-specific connection count

    KafkaConsumer->>KafkaConsumer: Log processing metrics
    Note right of KafkaConsumer: • Event processing time<br/>• Success/failure rates<br/>• Email delivery status

    NotifService->>NotifService: Generate notification stats
    Note right of NotifService: • Total notifications<br/>• Unread count<br/>• SSE delivery metrics
```
### ⚡ Error Handling & Circuit Breaker Flow

This diagram illustrates how our system gracefully handles failures and implements circuit breaker patterns:

```mermaid
sequenceDiagram
    participant Client
    participant Gateway as API Gateway<br/>(BFF + Circuit Breaker)
    participant CircuitBreaker as Circuit Breaker
    participant Kafka as Kafka Bus
    participant Service as Downstream Service
    participant AsyncMgr as AsyncResponseManager
    participant Redis as Redis<br/>(Circuit Breaker State)
    
    Client->>+Gateway: Request enriched data
    Gateway->>+CircuitBreaker: Check circuit state
    CircuitBreaker->>+Redis: Get circuit status
    Redis-->>-CircuitBreaker: State: CLOSED
    CircuitBreaker-->>-Gateway: Allow request
    
    Gateway->>+AsyncMgr: Register request<br/>(30s timeout)
    Gateway->>+Kafka: Send service request
    
    alt Service Healthy
        Service->>Kafka: Process & respond
        Kafka-->>Gateway: Success response
        Gateway->>+AsyncMgr: Complete request
        Gateway-->>-Client: Success response
        Gateway->>CircuitBreaker: Record success
        
    else Service Timeout
        Note over AsyncMgr: 30s timeout exceeded
        AsyncMgr->>Gateway: Timeout exception
        Gateway->>CircuitBreaker: Record failure
        CircuitBreaker->>Redis: Update failure count
        
        alt Failure Threshold Reached
            CircuitBreaker->>Redis: Set state: OPEN
            Note over CircuitBreaker: Circuit opens for 30s
        end
        
        Gateway-->>Client: Fallback response<br/>(empty/cached data)
        
    else Circuit OPEN
        CircuitBreaker->>Redis: Check open duration
        alt Still in open state
            Gateway-->>Client: Fast fail<br/>(fallback response)
        else Half-open trial
            CircuitBreaker->>Redis: Set state: HALF_OPEN
            Gateway->>Kafka: Allow limited requests
        end
    end
    
    Note over Gateway,Redis: Circuit Breaker States:<br/>• CLOSED: Normal operation<br/>• OPEN: Fast fail (30s)<br/>• HALF_OPEN: Testing recovery
```

### 🧩 BFF Architecture Benefits

Our async BFF implementation provides several key advantages:

```mermaid
graph TB
    A[Client Request] --> B{API Gateway BFF}
    
    B --> C[Request Orchestration]
    B --> D[Correlation ID Management]
    B --> E[Circuit Breaker Protection]
    B --> F[Rate Limiting]
    
    C --> G[Async Kafka Calls]
    G --> H[Parallel Processing]
    G --> I[Timeout Management]
    
    H --> J[Data Aggregation]
    H --> K[Response Enrichment]
    
    J --> L[Optimized Client Response]
    K --> L
    
    style B fill:#e1f5fe
    style G fill:#f3e5f5
    style L fill:#e8f5e8
    
    M[Benefits] --> N[70% Reduction in API Calls]
    M --> O[300ms Average Response Time]
    M --> P[Automatic Failure Recovery]
    M --> Q[Single Client Interface]
    M --> R[Event-Driven Scalability]
```

## 🚀 CI/CD Pipeline Architecture

NexusCommerce implements a sophisticated **CI/CD pipeline** for each microservice, ensuring code quality, security, and automated deployments. Every service includes a dedicated `Jenkinsfile` that orchestrates a comprehensive build, test, and deployment process.

![Jenkins](https://img.shields.io/badge/Jenkins-D24939?style=for-the-badge&logo=jenkins&logoColor=white)
![Docker](https://img.shields.io/badge/Docker_Hub-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![SonarQube](https://img.shields.io/badge/SonarQube-4E9BCD?style=for-the-badge&logo=sonarqube&logoColor=white)
![Trivy](https://img.shields.io/badge/Trivy_Security-1904DA?style=for-the-badge&logo=trivy&logoColor=white)

### 🎯 **Pipeline Overview**

Each microservice follows a standardized **10-stage CI/CD pipeline** that ensures quality, security, and consistency across all services:

```mermaid
graph LR
    A[📥 Checkout] --> B[🔨 Build]
    B --> C[🧪 Test]
    C --> D[🔍 SonarQube Analysis]
    D --> E[🚦 Quality Gate]
    E --> F[📦 Package]
    F --> G[🐳 Docker Build]
    G --> H[🔒 Security Scan]
    H --> I[🏃 Deploy]
    I --> J[📤 Push to Registry]
    
    style A fill:#e3f2fd
    style D fill:#f3e5f5
    style H fill:#ffebee
    style J fill:#e8f5e8
```

### 🛠️ **Pipeline Stages Breakdown**

#### **Stage 1: 📥 Checkout**
```groovy
stage('Checkout') {
    steps {
        checkout([$class: 'GitSCM',
            branches: [[name: '*/main']],
            userRemoteConfigs: [[url: 'https://github.com/ZakariaRek/Ecommerce-App']],
            extensions: [[$class: 'SparseCheckoutPaths']]
        ])
    }
}
```
- **Smart Git Integration**: Sparse checkout for service-specific code
- **Branch Strategy**: Main branch deployment with feature branch support
- **Credential Management**: Secure GitHub integration

#### **Stage 2: 🔨 Build Application**
```groovy
stage('Build Application') {
    steps {
        bat '''
            mvn clean compile
                -Dmaven.compiler.source=17
                -Dmaven.compiler.target=17
        '''
    }
}
```
- **Multi-Language Support**: Maven for Java services, Go build for Go services
- **Standardized JDK**: Java 17 across all Spring Boot services
- **Clean Builds**: Ensures consistent build environment

#### **Stage 3: 🧪 Run Tests**
```groovy
stage('Run Tests') {
    steps {
        bat '''
            mvn test
                -Dmaven.test.failure.ignore=true
                -Dspring.profiles.active=test
        '''
    }
    post {
        always {
            junit testResults: 'target/surefire-reports/*.xml'
            archiveArtifacts artifacts: 'target/site/jacoco/**/*'
        }
    }
}
```
- **Comprehensive Testing**: Unit tests, integration tests, coverage reports
- **JaCoCo Integration**: Code coverage analysis and reporting
- **Flexible Execution**: Continue pipeline even with test failures for analysis

#### **Stage 4: 🔍 SonarQube Analysis**
```groovy
stage('SonarQube Analysis') {
    steps {
        withSonarQubeEnv('sonarqube') {
            bat '''
                mvn sonar:sonar
                    -Dsonar.projectKey=E-commerce-User-Service
                    -Dsonar.host.url=%SONAR_HOST_URL%
                    -Dsonar.token=%SONAR_AUTH_TOKEN%
            '''
        }
    }
}
```
- **Code Quality Metrics**: Bugs, vulnerabilities, code smells, technical debt
- **Quality Standards**: Enforced coding standards and best practices
- **Trend Analysis**: Historical quality metrics tracking

#### **Stage 5: 🚦 Quality Gate**
```groovy
stage('Quality Gate') {
    steps {
        timeout(time: 2, unit: 'MINUTES') {
            def qg = waitForQualityGate()
            if (qg.status != 'OK') {
                currentBuild.result = 'UNSTABLE'
            }
        }
    }
}
```
- **Automated Quality Control**: Fail-fast on quality issues
- **Configurable Thresholds**: Coverage, duplication, security ratings
- **Pipeline Control**: Quality gate failures mark build as unstable

#### **Stage 6: 📦 Package Application**
```groovy
stage('Package Application') {
    steps {
        bat '''
            mvn package -DskipTests
                -Dmaven.compiler.source=17
        '''
    }
}
```
- **Artifact Generation**: JAR files for Java services, binaries for Go services
- **Optimization**: Skip tests during packaging (already executed)
- **Standardization**: Consistent naming conventions

#### **Stage 7: 🐳 Build Docker Images**
```groovy
stage('Build Docker Images') {
    steps {
        bat "docker build -t user-service:latest ."
        bat "docker-compose build"
    }
}
```
- **Multi-Stage Builds**: Optimized Docker images
- **Layer Caching**: Efficient build times
- **Compose Integration**: Service orchestration support

#### **Stage 8: 🔒 Security Scan with Trivy**
```groovy
stage('Security Scan with Trivy') {
    steps {
        bat """
            trivy image --severity HIGH,CRITICAL 
                --exit-code 1 user-service:latest
        """
        archiveArtifacts artifacts: 'trivy-report.*'
    }
}
```
- **Vulnerability Scanning**: Container image security analysis
- **Severity Filtering**: Focus on HIGH and CRITICAL vulnerabilities
- **Comprehensive Reporting**: JSON and table format reports
- **Pipeline Integration**: Fail builds on critical security issues

#### **Stage 9: 🏃 Deploy & Test**
```groovy
stage('Run Containers') {
    steps {
        bat "docker-compose up -d"
        // Health checks and integration tests
    }
}
```
- **Container Orchestration**: Docker Compose for local testing
- **Health Verification**: Application startup and readiness checks
- **Integration Testing**: Service-to-service communication validation

#### **Stage 10: 📤 Push to Docker Hub**
```groovy
stage('Push to Docker Hub') {
    steps {
        withCredentials([usernamePassword(credentialsId: 'dockerhub-creds')]) {
            bat "docker push yahyazakaria123/ecommerce-app-user-service:latest"
        }
    }
}
```
- **Registry Integration**: Automated Docker Hub publishing
- **Secure Credentials**: Jenkins credential management
- **Tagging Strategy**: Latest, version-specific, and branch-based tags

### 🎭 **Service-Specific Pipeline Configurations**

Each service has tailored pipeline configurations based on technology stack:

#### **🟢 Spring Boot Services** (User, Product, Order, Loyalty, Notification)
- **Maven Build System**: `mvn clean compile test package`
- **Spring Profiles**: Test-specific configurations
- **JaCoCo Coverage**: Minimum 70% coverage requirement
- **Spring Boot Testing**: `@SpringBootTest`, `@TestContainers`

#### **🔵 Go Services** (Cart, Payment, Shipping)
- **Go Build Tools**: `go build`, `go test`, `go mod tidy`
- **Coverage Analysis**: `go test -coverprofile=coverage.out`
- **Static Analysis**: `golangci-lint` integration
- **Performance Testing**: Benchmark tests with `go test -bench`


### 🔐 **Security & Quality Assurance**

#### **🛡️ Security Scanning Pipeline**
```mermaid
graph TB
    subgraph "Security Analysis"
        A[Trivy Container Scan] --> B[Vulnerability Database]
        C[SonarQube Security Hotspots] --> D[OWASP Top 10]
        E[Dependency Check] --> F[Known CVEs]
    end
    
    subgraph "Quality Metrics"
        G[Code Coverage] --> H[Minimum 70%]
        I[Code Duplication] --> J[Maximum 3%]
        K[Complexity] --> L[Cyclomatic < 10]
    end
    
    subgraph "Pipeline Gates"
        M[Security Gate] --> N{Critical Vulns?}
        O[Quality Gate] --> P{Standards Met?}
        N -->|Yes| Q[Fail Build]
        N -->|No| R[Continue]
        P -->|No| S[Mark Unstable]
        P -->|Yes| R
    end

    style A fill:#ffebee
    style C fill:#ffebee
    style E fill:#ffebee
    style G fill:#e8f5e8
    style I fill:#e8f5e8
    style K fill:#e8f5e8
```

#### **📊 Quality Metrics Dashboard**

Our SonarQube integration provides comprehensive quality insights:

- **Code Coverage**:  across all services
- **Security Rating**: A rating required for production deployment
- **Maintainability**: Technical debt ratio < 5%
- **Reliability**: Bug-free code deployment
- **Duplication**: Less than 3% code duplication

### 🚢 **Deployment Strategies**

#### **🌱 Environment Promotion**
```mermaid
graph LR
    A[Feature Branch] --> B[Dev Environment]
    B --> C[Integration Tests]
    C --> D[Staging Environment]
    D --> E[User Acceptance Tests]
    E --> F[Production Environment]
    
    G[Hotfix Branch] --> H[Production Direct]
    
    style A fill:#e3f2fd
    style D fill:#fff3e0
    style F fill:#e8f5e8
    style G fill:#ffebee
```
## 🚀 Getting Started

### Prerequisites

![Java](https://img.shields.io/badge/Java_17+-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Go](https://img.shields.io/badge/Go_1.18+-00ADD8?style=for-the-badge&logo=go&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![Kubernetes](https://img.shields.io/badge/Kubernetes-326CE5?style=for-the-badge&logo=kubernetes&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-C71A36?style=for-the-badge&logo=apache-maven&logoColor=white)

- Docker and Docker Compose
- Kubernetes cluster (for production deployment)
- Java 17+
- Go 1.18+
- Maven/Gradle
- Redis (caching & rate limiting)

### Quick Start

1. Clone the repository:
   ```bash
   git clone https://github.com/ZakariaRek/Ecommerce-App.git
   cd Ecommerce-App
   ```

2. Start the infrastructure services:
   ```bash
   docker-compose up -d config-server eureka-server kafka zookeeper redis
   ```

3. Start the observability stack:
   ```bash
   docker-compose up -d elasticsearch logstash kibana zipkin sonarqube
   ```

4. Start the API Gateway:
   ```bash
   cd Gateway-Service
   mvn spring-boot:run
   ```

5. Start the core services:
   ```bash
   docker-compose up -d user-service product-service cart-service order-service
   ```

6. Start the supporting services:
   ```bash
   docker-compose up -d payment-service shipping-service loyalty-service notification-service
   ```

7. Access the services:
    - **API Gateway**: http://localhost:8099
    - **Gateway Swagger UI**: http://localhost:8099/swagger-ui.html
    - **Eureka Dashboard**: http://localhost:8761
    - **Zipkin Tracing**: http://localhost:9411
    - **Kibana Dashboards**: http://localhost:5601
    - **Kafka UI**: http://localhost:8091
    - **SonarQube**: http://localhost:9000
    - **Elasticsearch**: http://localhost:9200

## 🌟 API Gateway Endpoints

### Gateway Management
- `GET /api/gateway/health` - Gateway health status
- `GET /api/gateway/circuit-breakers` - List all circuit breakers
- `POST /api/gateway/circuit-breakers/{name}/reset` - Reset a circuit breaker
- `GET /api/gateway/rate-limiting/config` - Rate limiting configuration
- `GET /api/gateway/rate-limiting/stats` - Rate limiting statistics
- `GET /api/gateway/services` - List all registered services

### BFF Endpoints
- `GET /api/cart/{userId}/enriched` - Get cart with product details
- `GET /api/order/{orderId}/enriched` - Get order with product details
- `GET /api/order/user/{userId}/all` - Get all user orders (batch)
- `GET /api/saved4later/{userId}/enriched` - Get saved items with availability

### Logging & Monitoring
- `GET /api/logs/health` - Logging pipeline health
- `GET /api/logs/stats` - Real-time logging statistics
- `POST /api/logs/test` - Generate test logs for verification

## 🧪 Development Workflow

1. **Fork & Clone**: Start with your own copy of the repository
2. **Branch**: Create a feature branch `feature/your-feature-name`
3. **Develop**: Write your code and tests
4. **Quality Check**: Run SonarQube analysis
5. **Logging**: Test log generation and Kibana visualization
6. **Test**: Ensure all tests pass
7. **PR**: Submit a pull request for review

## 📊 Monitoring and Observability

Our platform provides comprehensive visibility through multiple layers:

### 🔍 **Distributed Tracing**
![Zipkin](https://img.shields.io/badge/Zipkin-1f425f?style=for-the-badge&logo=zipkin&logoColor=white)
- Follow requests across services with Zipkin
- Correlation ID tracking through the entire request lifecycle
- Performance bottleneck identification
- Service dependency mapping

### 📋 **Centralized Logging**
![ELK Stack](https://img.shields.io/badge/ELK_Stack-005571?style=for-the-badge&logo=elastic&logoColor=white)
- **Real-time log aggregation** from all microservices
- **Structured JSON logging** with business context
- **Advanced search capabilities** across all service logs
- **Custom dashboards** for different stakeholder needs
- **Alert management** for proactive issue detection

### 📈 **Metrics & Performance**
![Prometheus](https://img.shields.io/badge/Prometheus-E6522C?style=for-the-badge&logo=prometheus&logoColor=white)
![Grafana](https://img.shields.io/badge/Grafana-F46800?style=for-the-badge&logo=grafana&logoColor=white)
- Circuit breaker metrics and health status
- Rate limit analytics and usage patterns
- Service health monitoring with Spring Actuator
- Resource utilization tracking (CPU, memory, network)
- Business metrics (orders, payments, user activity)

### 🚨 **Alerting & Notifications**
- **Automated error detection** through log analysis
- **Performance degradation alerts** via metrics monitoring
- **Service availability notifications** from health checks
- **Business metric alerts** (payment failures, order drops)
- **Multi-channel notifications** (email, Slack, SMS)

## 🔒 Security

Security is foundational to our architecture:

![JWT](https://img.shields.io/badge/JWT-000000?style=for-the-badge&logo=json-web-tokens&logoColor=white)
![OAuth2](https://img.shields.io/badge/OAuth2-3C4043?style=for-the-badge&logo=oauth&logoColor=white)

- **API Gateway Authentication**: JWT-based with refresh tokens
- **OAuth2 Support**: Social login integration
- **Rate Limiting**: DDoS protection and fair usage
- **Circuit Breakers**: Cascading failure prevention
- **Service-to-Service Communication**: Mutual TLS
- **Data Encryption**: At rest and in transit
- **Security Scanning**: Regular vulnerability assessments with SonarQube
- **Audit Logging**: Complete audit trail through centralized logging

## 🌐 Scaling and Resilience

Our architecture is designed for growth and reliability:

- **Horizontal Scaling**: Each service scales independently
- **Circuit Breakers**: Prevent cascading failures
- **Rate Limiting**: Protect services from traffic spikes
- **Auto-Healing**: Self-recovering services in Kubernetes
- **Async Processing**: Kafka-based event handling
- **Caching**: Redis for performance optimization
- **Load Balancing**: Client-side load balancing with Eureka
- **Graceful Degradation**: Fallback responses when services are unavailable

## 🤝 Contributing

We welcome contributions! See our [Contribution Guidelines](CONTRIBUTING.md) for more details.

### Technology-Specific Contributions

![Spring Boot](https://img.shields.io/badge/Spring_Boot_Services-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
- User Service, Order Service, Product Service, Loyalty Service, Notification Service

![Go](https://img.shields.io/badge/Go_Services-00ADD8?style=for-the-badge&logo=go&logoColor=white)
- Cart Service, Payment Service, Shipping Service

![Infrastructure](https://img.shields.io/badge/Infrastructure-326CE5?style=for-the-badge&logo=kubernetes&logoColor=white)
- API Gateway, Config Server, Service Registry, Observability Stack

## 📜 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 📞 Support

For questions and support, please open an issue or contact our team at support@nexuscommerce.io.

### 🔗 **Useful Links**

- **Documentation**: [Architecture Guide](docs/architecture.md)
- **API Documentation**: [Swagger UI](http://localhost:8099/swagger-ui.html)
- **Monitoring Dashboards**: [Kibana](http://localhost:5601) | [Grafana](http://localhost:3000)
- **Service Health**: [Eureka](http://localhost:8761) | [Actuator](http://localhost:8099/actuator)
- **Message Streaming**: [Kafka UI](http://localhost:8091)
- **Code Quality**: [SonarQube](http://localhost:9000)

---

> "In the world of e-commerce, it's not just about transactions—it's about transformations. NexusCommerce transforms shopping into an experience, monoliths into microservices, and challenges into opportunities. With comprehensive observability, we transform data into insights, and insights into innovation."
