# 🛍️ E-commerce User Service

<div align="center">

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.4-brightgreen?style=for-the-badge&logo=spring-boot)
![Java](https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=openjdk)
![MongoDB](https://img.shields.io/badge/MongoDB-4.4+-green?style=for-the-badge&logo=mongodb)
![Apache Kafka](https://img.shields.io/badge/Apache%20Kafka-Latest-black?style=for-the-badge&logo=apache-kafka)
![Docker](https://img.shields.io/badge/Docker-Supported-blue?style=for-the-badge&logo=docker)

**A comprehensive microservice for user management in an e-commerce platform**

</div>

## 📋 Table of Contents

- [🌟 Features](#-features)
- [🏗️ Architecture](#️-architecture)
- [🛠️ Technology Stack](#️-technology-stack)
- [🚀 Quick Start](#-quick-start)
- [📊 Database Schema](#-database-schema)
- [🔒 Security](#-security)
- [📡 API Documentation](#-api-documentation)
- [🔄 Event-Driven Architecture](#-event-driven-architecture)
- [🐳 Docker Deployment](#-docker-deployment)
- [📈 Monitoring](#-monitoring)

## 🌟 Features

### Core Functionality
- ✅ **User Registration & Authentication** (JWT + OAuth2)
- ✅ **Role-Based Access Control** (RBAC)
- ✅ **User Profile Management**
- ✅ **Multi-Address Support** per user
- ✅ **OAuth2 Integration** (Google)
- ✅ **Event-Driven Communication** via Kafka
- ✅ **RESTful API** with comprehensive documentation
- ✅ **Distributed Logging** (ELK Stack)
- ✅ **Service Discovery** (Eureka)
- ✅ **Configuration Management** (Spring Cloud Config)

### Advanced Features
- 🔄 **Real-time Event Publishing** for user lifecycle
- 🛡️ **JWT Cookie-based Authentication**
- 📧 **Cross-service User Information Sharing**
- 🏠 **Address Management** with default address support
- 📊 **Comprehensive Monitoring** and Health Checks
- 🔧 **Hot Configuration Reload**

## 🏗️ Architecture

### System Architecture

```mermaid
graph TB
    subgraph "Client Layer"
        Web[Web Application]
        Mobile[Mobile App]
        Admin[Admin Panel]
    end

    subgraph "API Gateway"
        Gateway[Spring Cloud Gateway<br/>Port: 8080]
    end

    subgraph "Service Discovery"
        Eureka[Eureka Server<br/>Port: 8761]
    end

    subgraph "Configuration Management"
        ConfigServer[Config Server<br/>Port: 8888]
    end

    subgraph "User Service"
        UserApp[User Service<br/>Port: 8081]
        
        subgraph "Controllers"
            AuthCtrl[Auth Controller]
            UserCtrl[User Controller]
            RoleCtrl[Role Controller]
            AddressCtrl[Address Controller]
            OAuth2Ctrl[OAuth2 Controller]
        end
        
        subgraph "Services"
            UserSvc[User Service]
            RoleSvc[Role Service]
            AddressSvc[Address Service]
            EventSvc[Event Services]
        end
        
        subgraph "Security"
            JWT[JWT Utils]
            OAuth2[OAuth2 Config]
            Security[Security Config]
        end
    end

    subgraph "Data Layer"
        MongoDB[(MongoDB<br/>Port: 27017)]
    end

    subgraph "Message Broker"
        Kafka[Apache Kafka<br/>Port: 9092]
        
        subgraph "Topics"
            UserEvents[User Events]
            AddressEvents[Address Events]
            RoleEvents[Role Events]
            RequestResponse[Request/Response]
        end
    end

    subgraph "External Services"
        Google[Google OAuth2]
        ELK[ELK Stack<br/>Logging]
    end

    subgraph "Other Microservices"
        NotificationSvc[Notification Service]
        OrderSvc[Order Service]
        ProductSvc[Product Service]
    end

    %% Connections
    Web --> Gateway
    Mobile --> Gateway
    Admin --> Gateway
    
    Gateway --> UserApp
    UserApp --> Eureka
    UserApp --> ConfigServer
    UserApp --> MongoDB
    UserApp --> Kafka
    UserApp --> Google
    UserApp --> ELK
    
    Kafka --> NotificationSvc
    Kafka --> OrderSvc
    Kafka --> ProductSvc

    %% Styling
    classDef primary fill:#e1f5fe
    classDef secondary fill:#f3e5f5
    classDef database fill:#e8f5e8
    classDef external fill:#fff3e0
    
    class UserApp,Gateway,Eureka,ConfigServer primary
    class MongoDB,Kafka database
    class Google,ELK external
```

### Service Internal Architecture

```mermaid
graph TB
    subgraph "User Service Architecture"
        subgraph "Presentation Layer"
            REST[REST Controllers]
            Swagger[Swagger/OpenAPI]
        end
        
        subgraph "Business Layer"
            UserService[User Service]
            RoleService[Role Service]
            AddressService[Address Service]
            EventService[Kafka Event Service]
        end
        
        subgraph "Security Layer"
            AuthFilter[JWT Auth Filter]
            OAuth2Handler[OAuth2 Handler]
            SecurityConfig[Security Configuration]
        end
        
        subgraph "Data Access Layer"
            UserRepo[User Repository]
            RoleRepo[Role Repository]
            AddressRepo[Address Repository]
            EntityListeners[Entity Listeners]
        end
        
        subgraph "Infrastructure"
            KafkaProducer[Kafka Producer]
            KafkaConsumer[Kafka Consumer]
            MongoConfig[MongoDB Config]
            ConfigClient[Config Client]
        end
    end
    
    REST --> UserService
    REST --> RoleService
    REST --> AddressService
    
    UserService --> UserRepo
    RoleService --> RoleRepo
    AddressService --> AddressRepo
    
    EntityListeners --> EventService
    EventService --> KafkaProducer
    
    AuthFilter --> SecurityConfig
    OAuth2Handler --> SecurityConfig
    
    UserRepo --> MongoConfig
    KafkaProducer --> KafkaConsumer
```

## 🛠️ Technology Stack

<div align="center">

| Category | Technology | Version | Purpose |
|----------|------------|---------|---------|
| **Framework** | ![Spring Boot](https://img.shields.io/badge/-Spring%20Boot-6DB33F?style=flat-square&logo=spring-boot&logoColor=white) | 3.4.4 | Main Framework |
| **Language** | ![Java](https://img.shields.io/badge/-Java-ED8B00?style=flat-square&logo=openjdk&logoColor=white) | 21 | Programming Language |
| **Database** | ![MongoDB](https://img.shields.io/badge/-MongoDB-13aa52?style=flat-square&logo=mongodb&logoColor=white) | 4.4+ | Primary Database |
| **Message Broker** | ![Kafka](https://img.shields.io/badge/-Apache%20Kafka-000?style=flat-square&logo=apachekafka&logoColor=white) | Latest | Event Streaming |
| **Security** | ![Spring Security](https://img.shields.io/badge/-Spring%20Security-6DB33F?style=flat-square&logo=springsecurity&logoColor=white) | 6.x | Authentication & Authorization |
| **Documentation** | ![Swagger](https://img.shields.io/badge/-Swagger-85EA2D?style=flat-square&logo=swagger&logoColor=black) | 3.x | API Documentation |
| **Service Discovery** | ![Eureka](https://img.shields.io/badge/-Netflix%20Eureka-FF6B6B?style=flat-square) | Latest | Service Registry |
| **Configuration** | ![Spring Cloud](https://img.shields.io/badge/-Spring%20Cloud-6DB33F?style=flat-square&logo=spring&logoColor=white) | 2024.0.1 | Configuration Management |
| **Logging** | ![ELK](https://img.shields.io/badge/-ELK%20Stack-005571?style=flat-square&logo=elastic&logoColor=white) | Latest | Centralized Logging |
| **OAuth2** | ![Google](https://img.shields.io/badge/-Google%20OAuth2-4285F4?style=flat-square&logo=google&logoColor=white) | 2.0 | Social Authentication |

</div>

## 🚀 Quick Start

### Prerequisites

```bash
# Required Software
☑️ Java 21
☑️ Maven 3.8+
☑️ MongoDB 4.4+
☑️ Apache Kafka 2.8+
☑️ Docker & Docker Compose (optional)
```

### Installation Steps

1. **Clone the Repository**
```bash
git clone <repository-url>
cd User-Service
```

2. **Start Infrastructure Services**
```bash
# Start MongoDB
docker run -d --name mongodb -p 27017:27017 mongo:latest

# Start Kafka & Zookeeper
docker-compose up -d kafka zookeeper

# Start Eureka Server (if not running)
# Start Config Server (if not running)
```

3. **Configure Application**
```yaml
# application.yaml
spring:
  data:
    mongodb:
      host: localhost
      port: 27017
      database: User-service
  kafka:
    bootstrap-servers: localhost:9092
```

4. **Run the Application**
```bash
# Using Maven
./mvnw spring-boot:run

# Or using Java
./mvnw clean package
java -jar target/User-Service-0.0.1-SNAPSHOT.jar
```

5. **Initialize Roles** (One-time setup)
```bash
curl -X POST http://localhost:8081/api/users/roles/init
```

### Verification

- **Health Check**: http://localhost:8081/api/users/actuator/health
- **API Documentation**: http://localhost:8081/api/users/swagger-ui.html
- **Eureka Dashboard**: http://localhost:8761

## 📊 Database Schema

```mermaid
erDiagram
    User {
        string id PK
        string username UK
        string email UK
        string password
        UserStatus status
        datetime createdAt
        datetime updatedAt
    }
    
    Role {
        string id PK
        ERole name UK
        string description
        datetime createdAt
        datetime updatedAt
    }
    
    UserAddress {
        string id PK
        string userId FK
        AddressType addressType
        string street
        string city
        string state
        string country
        string zipCode
        boolean isDefault
        datetime createdAt
        datetime updatedAt
    }
    
    User }|--o{ Role : "user_roles"
    User ||--o{ UserAddress : "has"
```

### Enums

```mermaid
graph LR
    subgraph "UserStatus"
        US1[ACTIVE]
        US2[INACTIVE]
        US3[SUSPENDED]
    end
    
    subgraph "ERole"
        ER1[ROLE_USER]
        ER2[ROLE_MODERATOR]
        ER3[ROLE_ADMIN]
    end
    
    subgraph "AddressType"
        AT1[HOME]
        AT2[WORK]
        AT3[BILLING]
        AT4[SHIPPING]
    end
```

## 🔒 Security

### Authentication Flow

```mermaid
sequenceDiagram
    participant Client
    participant UserService
    participant MongoDB
    participant JWTUtils
    
    Note over Client,JWTUtils: Standard Login Flow
    Client->>UserService: POST /auth/signin
    UserService->>MongoDB: Validate credentials
    MongoDB-->>UserService: User details
    UserService->>JWTUtils: Generate JWT
    JWTUtils-->>UserService: JWT Token
    UserService-->>Client: Set JWT Cookie + User Info
    
    Note over Client,JWTUtils: Subsequent Requests
    Client->>UserService: API Request (with JWT cookie)
    UserService->>JWTUtils: Validate JWT
    JWTUtils-->>UserService: User details
    UserService-->>Client: API Response
```

### OAuth2 Flow

```mermaid
sequenceDiagram
    participant Client
    participant UserService
    participant Google
    participant MongoDB
    
    Client->>UserService: GET /oauth2/google
    UserService-->>Client: Redirect to Google
    Client->>Google: Login with Google
    Google-->>Client: Authorization Code
    Client->>UserService: /oauth2/callback/google?code=...
    UserService->>Google: Exchange code for token
    Google-->>UserService: Access Token + User Info
    UserService->>MongoDB: Create/Update user
    MongoDB-->>UserService: User details
    UserService-->>Client: Redirect with JWT
```

### Security Features

- 🔐 **JWT Token Authentication** with HTTP-only cookies
- 🛡️ **Role-Based Access Control** (RBAC)
- 🔑 **OAuth2 Integration** (Google)
- 🚫 **CORS Protection** with configurable origins
- 🔒 **Password Encryption** using BCrypt
- ⏰ **Token Expiration** and refresh handling
- 🛠️ **Method-level Security** with `@PreAuthorize`

## 📡 API Documentation

### Authentication Endpoints

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| `POST` | `/auth/signin` | User login | ❌ |
| `POST` | `/auth/signup` | User registration | ❌ |
| `POST` | `/auth/signout` | User logout | ✅ |

### User Management

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| `GET` | `/users` | Get all users | ✅ |
| `GET` | `/users/{id}` | Get user by ID | ✅ |
| `GET` | `/users/username/{username}` | Get user by username | ✅ |
| `GET` | `/users/email/{email}` | Get user by email | ✅ |
| `PUT` | `/users/{id}` | Update user | ✅ |
| `PATCH` | `/users/{id}/status/{status}` | Update user status | ✅ |
| `DELETE` | `/users/{id}` | Delete user | ✅ (Admin) |

### Role Management

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| `GET` | `/roles` | Get all roles | ✅ |
| `GET` | `/roles/{id}` | Get role by ID | ✅ |
| `POST` | `/roles` | Create role | ✅ |
| `PUT` | `/roles/{id}` | Update role | ✅ |
| `DELETE` | `/roles/{id}` | Delete role | ✅ |
| `POST` | `/roles/init` | Initialize default roles | ❌ |

### Address Management

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| `GET` | `/addresses` | Get all addresses | ✅ |
| `GET` | `/addresses/user/{userId}` | Get user addresses | ✅ |
| `GET` | `/addresses/user/{userId}/default` | Get default address | ✅ |
| `POST` | `/addresses` | Create address | ✅ |
| `PUT` | `/addresses/{id}` | Update address | ✅ |
| `PATCH` | `/addresses/{id}/default` | Set as default | ✅ |
| `DELETE` | `/addresses/{id}` | Delete address | ✅ |

### OAuth2 Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/oauth2/google` | Initiate Google OAuth2 |
| `GET` | `/oauth2/providers` | Get available providers |

## 🔄 Event-Driven Architecture

### Kafka Topics

```mermaid
graph TB
    subgraph "User Service Events"
        subgraph "User Events"
            UC[user-created]
            UU[user-updated]
            UD[user-deleted]
            USC[user-status-changed]
            URC[user-role-changed]
        end
        
        subgraph "Address Events"
            UAC[user-address-created]
            UAU[user-address-updated]
            UAD[user-address-deleted]
            UDAC[user-default-address-changed]
            UATC[user-address-type-changed]
        end
        
        subgraph "Role Events"
            RC[role-created]
            RU[role-updated]
            RD[role-deleted]
            RATU[role-assigned-to-user]
            RRFU[role-removed-from-user]
        end
        
        subgraph "Request/Response"
            UER[user-email-request]
            UERP[user-email-response]
            BUER[bulk-user-email-request]
            BUERP[bulk-user-email-response]
            UIR[user-info-request]
            UIRP[user-info-response]
            BUIR[bulk-user-info-request]
            BUIRP[bulk-user-info-response]
        end
    end
    
    subgraph "Consumer Services"
        NS[Notification Service]
        OS[Order Service]
        PS[Product Service]
        AS[Analytics Service]
    end
    
    UC --> NS
    UU --> NS
    UAC --> OS
    UAU --> OS
    USC --> AS
    
    UER --> UERP
    BUER --> BUERP
    UIR --> UIRP
    BUIR --> BUIRP
```

### Event Flow Example

```mermaid
sequenceDiagram
    participant User
    participant UserService
    participant MongoDB
    participant Kafka
    participant NotificationService
    participant EmailService
    
    User->>UserService: Create Account
    UserService->>MongoDB: Save User
    MongoDB-->>UserService: User Saved
    
    Note over UserService: Entity Listener Triggered
    UserService->>Kafka: Publish user-created event
    
    UserService-->>User: Registration Success
    
    Kafka->>NotificationService: Consume user-created event
    NotificationService->>EmailService: Send Welcome Email
    EmailService-->>NotificationService: Email Sent
    
    Note over NotificationService: Optional: Publish email-sent event
```

### Event Schema Examples

```json
// User Created Event
{
  "userId": "64f5a1b2c3d4e5f6a7b8c9d0",
  "username": "john_doe",
  "email": "john@example.com",
  "status": "ACTIVE",
  "roles": ["ROLE_USER"],
  "createdAt": "2024-01-15T10:30:00Z"
}

// Address Updated Event
{
  "addressId": "64f5a1b2c3d4e5f6a7b8c9d1",
  "userId": "64f5a1b2c3d4e5f6a7b8c9d0",
  "addressType": "HOME",
  "street": "123 Main St",
  "city": "New York",
  "state": "NY",
  "country": "USA",
  "zipCode": "10001",
  "isDefault": true,
  "updatedAt": "2024-01-15T10:35:00Z"
}
```

## 🐳 Docker Deployment

### Dockerfile
```dockerfile
FROM openjdk:21-jdk-slim

WORKDIR /app

COPY target/User-Service-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Docker Compose
```yaml
version: '3.8'
services:
  user-service:
    build: .
    ports:
      - "8081:8081"
    environment:
      - SPRING_DATA_MONGODB_HOST=mongodb
      - SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092
      - EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://eureka:8761/eureka/
    depends_on:
      - mongodb
      - kafka
      - eureka
    networks:
      - ecommerce-network

  mongodb:
    image: mongo:latest
    ports:
      - "27017:27017"
    volumes:
      - mongodb-data:/data/db
    networks:
      - ecommerce-network

  kafka:
    image: confluentinc/cp-kafka:latest
    ports:
      - "9092:9092"
    environment:
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092
    depends_on:
      - zookeeper
    networks:
      - ecommerce-network

  zookeeper:
    image: confluentinc/cp-zookeeper:latest
    ports:
      - "2181:2181"
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
    networks:
      - ecommerce-network

volumes:
  mongodb-data:

networks:
  ecommerce-network:
    driver: bridge
```

## 📈 Monitoring

### Health Checks

```mermaid
graph TB
    subgraph "Monitoring Stack"
        App[User Service]
        Actuator[Spring Actuator]
        
        subgraph "Metrics"
            Health[Health Check]
            Info[App Info]
            Metrics[JVM Metrics]
            Env[Environment]
        end
        
        subgraph "Logging"
            Logback[Logback]
            Kafka[Kafka Appender]
            ELK[ELK Stack]
        end
        
        subgraph "External Monitoring"
            Prometheus[Prometheus]
            Grafana[Grafana]
            AlertManager[Alert Manager]
        end
    end
    
    App --> Actuator
    Actuator --> Health
    Actuator --> Info
    Actuator --> Metrics
    Actuator --> Env
    
    App --> Logback
    Logback --> Kafka
    Kafka --> ELK
    
    Actuator --> Prometheus
    Prometheus --> Grafana
    Prometheus --> AlertManager
```

### Available Endpoints

| Endpoint | Description |
|----------|-------------|
| `/actuator/health` | Application health status |
| `/actuator/info` | Application information |
| `/actuator/metrics` | Application metrics |
| `/actuator/env` | Environment properties |
| `/actuator/loggers` | Logging configuration |

### Key Metrics to Monitor

- 📊 **API Response Times**
- 🔄 **Request Throughput**
- ❌ **Error Rates**
- 🗄️ **Database Connection Pool**
- 📨 **Kafka Producer/Consumer Metrics**
- 💾 **JVM Memory Usage**
- 🔒 **Authentication Success/Failure Rates**

---

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 📞 Support

- 📧 Email: support@ecommerce.com
- 📖 Documentation: [Wiki](./wiki)
- 🐛 Issues: [GitHub Issues](./issues)
- 💬 Discussions: [GitHub Discussions](./discussions)

---

<div align="center">

**Built with ❤️ for the E-commerce Platform**

![Made with Spring Boot](https://img.shields.io/badge/Made%20with-Spring%20Boot-6DB33F.svg?style=flat-square&logo=spring-boot)
![Powered by MongoDB](https://img.shields.io/badge/Powered%20by-MongoDB-13aa52.svg?style=flat-square&logo=mongodb)
![Event Streaming with Kafka](https://img.shields.io/badge/Event%20Streaming-Apache%20Kafka-000.svg?style=flat-square&logo=apache-kafka)

</div>