# 🔧 Config Service - Microservices Configuration Server

<div align="center">

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.4-6DB33F?style=for-the-badge&logo=spring-boot)
![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2024.0.1-6DB33F?style=for-the-badge&logo=spring)
![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk)
![Maven](https://img.shields.io/badge/Maven-3.9.9-C71A36?style=for-the-badge&logo=apache-maven)
![Docker](https://img.shields.io/badge/Docker-Ready-2496ED?style=for-the-badge&logo=docker)

**Centralized Configuration Management for E-commerce Microservices**

</div>

## 📋 Table of Contents

- [🏗️ Architecture Overview](#️-architecture-overview)
- [✨ Features](#-features)
- [🔧 Prerequisites](#-prerequisites)
- [🚀 Getting Started](#-getting-started)
- [📁 Project Structure](#-project-structure)
- [⚙️ Configuration](#️-configuration)
- [🔌 Service Integration](#-service-integration)
- [📊 Monitoring & Health](#-monitoring--health)
- [🐳 Docker Support](#-docker-support)
- [🤝 Contributing](#-contributing)

## 🏗️ Architecture Overview

```mermaid
graph TB
    subgraph "Client Applications"
        WEB[Web Application<br/>:3000]
        MOBILE[Mobile App<br/>:3001]
        ADMIN[Admin Panel<br/>:5173]
    end

    subgraph "Service Discovery"
        EUREKA[Eureka Server<br/>:8761]
    end

    subgraph "Configuration Management"
        CONFIG[Config Server<br/>:8888]
        REPO[(Config Repository<br/>File System)]
    end

    subgraph "Microservices"
        USER[User Service<br/>:8081]
        PRODUCT[Product Service<br/>:8082]
        PAYMENT[Payment Service<br/>:8089]
        SHIPPING[Shipping Service<br/>:8083]
    end

    subgraph "Data Layer"
        DB1[(PostgreSQL<br/>User DB)]
        DB2[(PostgreSQL<br/>Product DB)]
        DB3[(PostgreSQL<br/>Payment DB)]
        DB4[(PostgreSQL<br/>Shipping DB)]
    end

    subgraph "Message Broker"
        KAFKA[Apache Kafka<br/>:9092]
    end

    subgraph "Monitoring"
        ZIPKIN[Zipkin Tracing<br/>:9411]
    end

    WEB --> USER
    WEB --> PRODUCT
    MOBILE --> USER
    ADMIN --> PRODUCT

    CONFIG --> REPO
    USER --> CONFIG
    PRODUCT --> CONFIG
    PAYMENT --> CONFIG
    SHIPPING --> CONFIG

    USER --> EUREKA
    PRODUCT --> EUREKA
    PAYMENT --> EUREKA
    SHIPPING --> EUREKA
    CONFIG --> EUREKA

    USER --> DB1
    PRODUCT --> DB2
    PAYMENT --> DB3
    SHIPPING --> DB4

    USER --> KAFKA
    PRODUCT --> KAFKA
    PAYMENT --> KAFKA
    SHIPPING --> KAFKA

    USER --> ZIPKIN
    PRODUCT --> ZIPKIN
    PAYMENT --> ZIPKIN
    SHIPPING --> ZIPKIN
    CONFIG --> ZIPKIN

    classDef configService fill:#e1f5fe,stroke:#01579b,stroke-width:3px
    classDef microservice fill:#f3e5f5,stroke:#4a148c,stroke-width:2px
    classDef database fill:#e8f5e8,stroke:#1b5e20,stroke-width:2px
    classDef client fill:#fff3e0,stroke:#e65100,stroke-width:2px

    class CONFIG configService
    class USER,PRODUCT,PAYMENT,SHIPPING microservice
    class DB1,DB2,DB3,DB4 database
    class WEB,MOBILE,ADMIN client
```

## ✨ Features

### 🎯 Core Features
- **Centralized Configuration Management** - Single source of truth for all microservice configurations
- **Environment-Specific Configs** - Support for development, staging, and production environments
- **Dynamic Configuration Refresh** - Hot-reload configurations without service restart
- **Service Discovery Integration** - Seamless integration with Netflix Eureka
- **Distributed Tracing** - Built-in Zipkin integration for request tracking

### 🔒 Security & CORS
- **CORS Configuration** - Pre-configured for multiple client origins
- **Secure Property Management** - Environment-based property overrides
- **Health Check Endpoints** - Comprehensive health monitoring

### 📊 Observability
- **Actuator Endpoints** - Health, info, and metrics exposure
- **Distributed Tracing** - Request tracking across services
- **Custom Metrics** - Application-specific monitoring

## 🔧 Prerequisites

Before running the Config Service, ensure you have:

| Technology | Version | Purpose |
|------------|---------|---------|
| ![Java](https://img.shields.io/badge/Java-21+-ED8B00?style=flat-square&logo=openjdk) | 21+ | Runtime Environment |
| ![Maven](https://img.shields.io/badge/Maven-3.6+-C71A36?style=flat-square&logo=apache-maven) | 3.6+ | Build Tool |
| ![Git](https://img.shields.io/badge/Git-Latest-F05032?style=flat-square&logo=git) | Latest | Version Control |
| ![Docker](https://img.shields.io/badge/Docker-Optional-2496ED?style=flat-square&logo=docker) | Latest | Containerization |

## 🚀 Getting Started

### 📥 Clone the Repository

```bash
git clone <repository-url>
cd Config-Service
```

### 🏃‍♂️ Run with Maven

```bash
# Clean and compile
./mvnw clean compile

# Run the application
./mvnw spring-boot:run
```

### 🐳 Run with Docker

```bash
# Build Docker image
docker build -t config-service .

# Run container
docker run -p 8888:8888 config-service
```

### 🧪 Verify Installation

```bash
# Check health endpoint
curl http://localhost:8888/actuator/health

# Get application configuration
curl http://localhost:8888/application/default
```

## 📁 Project Structure

```
Config-Service/
├── 📁 src/
│   ├── 📁 main/
│   │   ├── 📁 java/com/Ecommerce/Config_Service/
│   │   │   └── 📄 ConfigServiceApplication.java
│   │   └── 📁 resources/
│   │       └── 📄 application.yaml
│   └── 📁 test/
├── 📁 config-repo/                    # Configuration Repository
│   ├── 📄 application.yml            # Global configurations
│   ├── 📄 user-service.yml          # User service config
│   ├── 📄 product-service.yaml      # Product service config
│   ├── 📄 payment-service.yml       # Payment service config
│   └── 📄 shipping-service.yml      # Shipping service config
├── 🐳 Dockerfile
├── 📋 compose.yaml
└── 📄 pom.xml
```

## ⚙️ Configuration

### 🌐 Server Configuration

```yaml
server:
  port: 8888                          # Config server port

spring:
  application:
    name: config-server
  cloud:
    config:
      server:
        git:
          uri: file:./config-repo     # Local file system
          default-label: main
```

### 🔍 Service Discovery

```yaml
eureka:
  instance:
    preferIpAddress: true
    instanceId: ${spring.application.name}:${server.port}
    hostname: localhost
  client:
    registerWithEureka: true
    fetchRegistry: true
    serviceUrl:
      defaultZone: http://localhost:8761/eureka/
```

### 🌍 CORS Configuration

The global CORS configuration supports multiple client origins:

```yaml
cors:
  configuration:
    allowed-origins:
      - "http://localhost:3000"       # React App
      - "http://localhost:5173"       # Vite Dev Server
      - "http://localhost:8761"       # Eureka Dashboard
      - "http://localhost:8099"       # API Gateway
    allowed-methods:
      - "GET"
      - "POST"
      - "PUT" 
      - "DELETE"
      - "OPTIONS"
      - "PATCH"
```

## 🔌 Service Integration

### Configuration Flow

```mermaid
sequenceDiagram
    participant MS as Microservice
    participant CS as Config Server
    participant R as Config Repository
    participant E as Eureka Server

    Note over MS,E: Service Startup Sequence
    
    MS->>E: 1. Register with Eureka
    E-->>MS: Registration confirmed
    
    MS->>CS: 2. Request configuration
    Note over MS,CS: GET /{service-name}/{profile}
    
    CS->>R: 3. Fetch configuration
    R-->>CS: Return config files
    
    CS-->>MS: 4. Send configuration
    Note over CS,MS: YAML/JSON response
    
    MS->>MS: 5. Initialize with config
    
    Note over MS,E: Runtime Configuration Refresh
    
    MS->>CS: 6. Refresh configuration
    Note over MS,CS: POST /actuator/refresh
    
    CS->>R: 7. Check for updates
    R-->>CS: Updated configuration
    
    CS-->>MS: 8. New configuration
    MS->>MS: 9. Apply new config
```

### 📝 Service Configuration Examples

#### Payment Service
```yaml
server:
  port: 8089

datasource:
  host: localhost
  port: 5432
  username: postgres
  password: ${DB_PASSWORD:defaultpass}
  database: payment_system

kafka:
  brokers: localhost:9092
  topics:
    payment:
      created: payment-created
      updated: payment-updated
```

#### Shipping Service
```yaml
server:
  port: 8083

shipping:
  default-carrier: Standard Shipping
  estimated-delivery-days: 3
  cost:
    base-rate: 10.99
    express-multiplier: 2.0
```

## 📊 Monitoring & Health

### 🏥 Health Endpoints

| Endpoint | Description | Example |
|----------|-------------|---------|
| `/actuator/health` | Service health status | `{"status":"UP"}` |
| `/actuator/info` | Application information | Service metadata |
| `/actuator/refresh` | Refresh configuration | Trigger config reload |

### 📈 Metrics & Tracing

```bash
# Check all actuator endpoints
curl http://localhost:8888/actuator

# View health details
curl http://localhost:8888/actuator/health

# Access Zipkin tracing
open http://localhost:9411
```

## 🐳 Docker Support

### 🏗️ Multi-Stage Dockerfile

```dockerfile
FROM ubuntu:latest
LABEL authors="DELL"

ENTRYPOINT ["top", "-b"]
```

### 🐙 Docker Compose

```yaml
services:
  zipkin:
    image: 'openzipkin/zipkin:latest'
    ports:
      - '9411:9411'
      
  config-server:
    build: .
    ports:
      - '8888:8888'
    depends_on:
      - zipkin
```

## 🔄 Configuration Management Workflow

```mermaid
gitGraph
    commit id: "Initial config"
    branch development
    checkout development
    commit id: "Add user-service config"
    commit id: "Add payment-service config"
    checkout main
    merge development
    commit id: "Production release"
    branch hotfix
    checkout hotfix
    commit id: "Fix CORS issue"
    checkout main
    merge hotfix
    commit id: "Hotfix deployed"
```

## 🧪 Testing

### Unit Tests
```bash
./mvnw test
```

### Integration Tests
```bash
./mvnw integration-test
```

### Configuration Validation
```bash
# Test service configuration retrieval
curl http://localhost:8888/user-service/default
curl http://localhost:8888/payment-service/development
curl http://localhost:8888/shipping-service/production
```

## 🚀 Deployment

### 📦 Production Deployment

1. **Build the application**
   ```bash
   ./mvnw clean package -DskipTests
   ```

2. **Create Docker image**
   ```bash
   docker build -t config-service:1.0.0 .
   ```

3. **Deploy to production**
   ```bash
   docker run -d \
     --name config-service \
     -p 8888:8888 \
     -e SPRING_PROFILES_ACTIVE=production \
     config-service:1.0.0
   ```

### 🌍 Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `SERVER_PORT` | Server port | `8888` |
| `EUREKA_URL` | Eureka server URL | `http://localhost:8761/eureka` |
| `CONFIG_REPO_URI` | Configuration repository URI | `file:./config-repo` |
| `SPRING_PROFILES_ACTIVE` | Active profile | `default` |

## 🤝 Contributing

We welcome contributions! Please follow these steps:

1. **Fork the repository**
2. **Create a feature branch**
   ```bash
   git checkout -b feature/amazing-feature
   ```
3. **Commit your changes**
   ```bash
   git commit -m 'Add amazing feature'
   ```
4. **Push to the branch**
   ```bash
   git push origin feature/amazing-feature
   ```
5. **Open a Pull Request**

### 📝 Code Style
- Follow Java naming conventions
- Add appropriate comments and documentation
- Include unit tests for new features
- Update configuration examples as needed

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🆘 Support

- 📧 **Email**: support@ecommerce.com
- 💬 **Slack**: #config-service
- 📖 **Documentation**: [Wiki](wiki)
- 🐛 **Issues**: [GitHub Issues](issues)

---

<div align="center">

**Made with ❤️ by the E-commerce Team**

![Spring Boot](https://img.shields.io/badge/Powered%20by-Spring%20Boot-6DB33F?style=flat-square&logo=spring-boot)
![Microservices](https://img.shields.io/badge/Architecture-Microservices-FF6B6B?style=flat-square)
![Cloud Native](https://img.shields.io/badge/Cloud-Native-4ECDC4?style=flat-square)

</div>