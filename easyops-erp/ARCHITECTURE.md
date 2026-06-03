# EasyOps ERP System Architecture

## System Overview

EasyOps ERP is a microservices-based Enterprise Resource Planning system built with Spring Boot (backend) and React (frontend), designed for multi-tenancy and scalability.

---

## High-Level Architecture Diagram

```mermaid
graph TB
    subgraph "Client Layer"
        Browser[Web Browser]
    end

    subgraph "Frontend Layer"
        React[React Application<br/>Port: 3000<br/>Vite + TypeScript]
    end

    subgraph "API Gateway Layer"
        Gateway[API Gateway<br/>Port: 8081<br/>Spring Cloud Gateway<br/>Reactive WebFlux]
    end

    subgraph "Service Discovery"
        Eureka[Eureka Server<br/>Port: 8761<br/>Netflix Eureka]
    end

    subgraph "Microservices Layer"
        AuthService[Auth Service<br/>Port: 8083<br/>JWT + Sessions]
        UserMgmt[User Management<br/>Port: 8082<br/>User CRUD]
        RBAC[RBAC Service<br/>Port: 8084<br/>Roles & Permissions]
        OrgService[Organization Service<br/>Port: 8085<br/>Multi-tenancy]
    end

    subgraph "Database Layer"
        Postgres[(PostgreSQL<br/>Port: 5432<br/>Main Database)]
        MongoDB[(MongoDB<br/>Port: 27017<br/>Document Store)]
        Redis[(Redis<br/>Port: 6379<br/>Cache & Sessions)]
    end

    subgraph "Message Queue Layer"
        Kafka[Kafka<br/>Port: 9092<br/>Event Streaming]
        RabbitMQ[RabbitMQ<br/>Port: 5672, 15672<br/>Message Broker]
        Zookeeper[Zookeeper<br/>Port: 2181<br/>Kafka Coordinator]
    end

    subgraph "Search & Analytics"
        Elastic[Elasticsearch<br/>Port: 9200<br/>Full-text Search]
    end

    subgraph "Development Tools"
        Adminer[Adminer<br/>Port: 8080<br/>Database UI]
    end

    Browser --> React
    React --> Gateway
    Gateway --> Eureka
    Gateway --> AuthService
    Gateway --> UserMgmt
    Gateway --> RBAC
    Gateway --> OrgService
    
    AuthService --> Eureka
    UserMgmt --> Eureka
    RBAC --> Eureka
    OrgService --> Eureka
    
    AuthService --> Postgres
    AuthService --> Redis
    UserMgmt --> Postgres
    RBAC --> Postgres
    RBAC --> Redis
    OrgService --> Postgres
    OrgService --> Redis
    
    AuthService -.-> Kafka
    UserMgmt -.-> Kafka
    RBAC -.-> Kafka
    OrgService -.-> Kafka
    
    AuthService -.-> RabbitMQ
    UserMgmt -.-> RabbitMQ
    
    Kafka --> Zookeeper
    
    Adminer --> Postgres
    Adminer --> MongoDB
    
    style Browser fill:#e1f5ff
    style React fill:#61dafb
    style Gateway fill:#6db33f
    style Eureka fill:#ff6b6b
    style AuthService fill:#6db33f
    style UserMgmt fill:#6db33f
    style RBAC fill:#6db33f
    style OrgService fill:#6db33f
    style Postgres fill:#336791
    style MongoDB fill:#47a248
    style Redis fill:#dc382d
    style Kafka fill:#231f20
    style RabbitMQ fill:#ff6600
    style Elastic fill:#005571
```

---

## Detailed Component Architecture

```mermaid
graph LR
    subgraph "Frontend - React SPA"
        A1[Pages<br/>Dashboard, Login, Users]
        A2[Components<br/>Reusable UI]
        A3[Services<br/>API Clients]
        A4[State Management<br/>Context API]
        A5[Routing<br/>React Router]
    end

    subgraph "API Gateway - Spring Cloud"
        B1[Route Mapping]
        B2[Load Balancing]
        B3[Security Filter]
        B4[CORS Handling]
        B5[Rate Limiting]
    end

    subgraph "Auth Service"
        C1[JWT Generation]
        C2[Session Management]
        C3[Password Operations]
        C4[Login Attempts Tracking]
        C5[Password Reset]
    end

    subgraph "User Management Service"
        D1[User CRUD]
        D2[User Search]
        D3[User Validation]
        D4[Profile Management]
    end

    subgraph "RBAC Service"
        E1[Role Management]
        E2[Permission Management]
        E3[Authorization Checks]
        E4[Policy Enforcement]
    end

    subgraph "Organization Service"
        F1[Org Management]
        F2[Multi-tenancy]
        F3[Org Hierarchy]
        F4[Branch Management]
    end

    A3 --> B1
    B1 --> C1
    B1 --> D1
    B1 --> E1
    B1 --> F1
```

---

## Database Schema Architecture

```mermaid
erDiagram
    USERS ||--o{ USER_ORGANIZATIONS : belongs_to
    ORGANIZATIONS ||--o{ USER_ORGANIZATIONS : has
    USERS ||--o{ USER_ROLES : has
    ROLES ||--o{ USER_ROLES : assigned_to
    ROLES ||--o{ ROLE_PERMISSIONS : has
    PERMISSIONS ||--o{ ROLE_PERMISSIONS : granted_to
    USERS ||--o{ LOGIN_ATTEMPTS : tracks
    USERS ||--o{ USER_SESSIONS : has
    USERS ||--o{ PASSWORD_RESET_TOKENS : requests
    
    USERS {
        uuid id PK
        string username UK
        string email UK
        string password_hash
        string first_name
        string last_name
        string phone
        boolean is_active
        boolean is_verified
        timestamp created_at
        timestamp updated_at
    }
    
    ORGANIZATIONS {
        uuid id PK
        string name
        string code UK
        string description
        string domain
        boolean is_active
        timestamp created_at
    }
    
    ROLES {
        uuid id PK
        string name
        string code UK
        string description
        boolean is_system_role
        boolean is_active
    }
    
    PERMISSIONS {
        uuid id PK
        string name
        string code UK
        string resource
        string action
        string description
    }
    
    USER_SESSIONS {
        uuid id PK
        uuid user_id FK
        string session_token
        string ip_address
        string user_agent
        timestamp expires_at
        timestamp created_at
    }
    
    LOGIN_ATTEMPTS {
        uuid id PK
        uuid user_id FK
        string ip_address
        boolean successful
        timestamp attempted_at
    }
```

---

## Service Communication Patterns

```mermaid
sequenceDiagram
    participant Browser
    participant React
    participant Gateway
    participant Eureka
    participant Auth
    participant User
    participant RBAC
    participant Redis
    participant Postgres

    Browser->>React: User Login
    React->>Gateway: POST /api/auth/login
    Gateway->>Eureka: Discover Auth Service
    Eureka-->>Gateway: Auth Service Location
    Gateway->>Auth: Forward Login Request
    Auth->>Postgres: Validate Credentials
    Postgres-->>Auth: User Data
    Auth->>Redis: Store Session
    Auth->>Auth: Generate JWT
    Auth-->>Gateway: JWT Token + User Info
    Gateway-->>React: Login Response
    React-->>Browser: Display Dashboard

    Note over Browser,Postgres: Subsequent Authenticated Requests
    
    Browser->>React: Get User List
    React->>Gateway: GET /api/users (with JWT)
    Gateway->>Gateway: Validate JWT
    Gateway->>Eureka: Discover User Service
    Eureka-->>Gateway: User Service Location
    Gateway->>User: Forward Request
    User->>RBAC: Check Permissions
    RBAC->>Redis: Get Cached Permissions
    Redis-->>RBAC: Permission Data
    RBAC-->>User: Authorization Result
    User->>Postgres: Query Users
    Postgres-->>User: User List
    User-->>Gateway: User Response
    Gateway-->>React: User Data
    React-->>Browser: Display Users
```

---

## Technology Stack

### Frontend
- **Framework**: React 18
- **Language**: TypeScript
- **Build Tool**: Vite
- **UI**: Modern CSS with responsive design
- **State Management**: React Context API
- **HTTP Client**: Axios
- **Routing**: React Router

### Backend Microservices
- **Framework**: Spring Boot 3.3.3
- **Language**: Java 21
- **Service Discovery**: Netflix Eureka
- **API Gateway**: Spring Cloud Gateway (Reactive)
- **Security**: Spring Security + JWT
- **Data Access**: Spring Data JPA
- **Validation**: Jakarta Bean Validation
- **Caching**: Spring Cache with Redis

### Databases
- **PostgreSQL 17**: Primary relational database
  - Schemas: users, auth, rbac, admin, system, audit
- **MongoDB 7**: Document storage for unstructured data
- **Redis 7**: Caching and session management

### Message Queues
- **Apache Kafka**: Event streaming and async communication
- **RabbitMQ**: Traditional message brokering

### Search & Analytics
- **Elasticsearch 8.11**: Full-text search and analytics

### DevOps & Tools
- **Docker**: Containerization
- **Docker Compose**: Local orchestration
- **Adminer**: Database management UI
- **Maven**: Build automation

---

## Security Architecture

```mermaid
graph TB
    subgraph "Security Layers"
        L1[HTTPS/TLS]
        L2[CORS Policy]
        L3[API Gateway Security]
        L4[JWT Validation]
        L5[RBAC Authorization]
        L6[Data Encryption]
    end

    subgraph "Authentication Flow"
        A1[Login Request]
        A2[Credential Validation]
        A3[JWT Generation]
        A4[Session Creation]
        A5[Response with Token]
    end

    subgraph "Authorization Flow"
        B1[Extract JWT]
        B2[Validate Token]
        B3[Get User Permissions]
        B4[Check Resource Access]
        B5[Allow/Deny Request]
    end

    L1 --> L2 --> L3 --> L4 --> L5 --> L6
    A1 --> A2 --> A3 --> A4 --> A5
    B1 --> B2 --> B3 --> B4 --> B5
```

### Security Features
- **JWT Authentication**: Stateless token-based auth
- **Session Management**: Redis-backed sessions
- **Password Security**: BCrypt hashing
- **Login Attempt Tracking**: Brute force protection
- **Password Reset**: Secure token-based reset
- **RBAC**: Fine-grained permission control
- **CORS**: Configurable cross-origin policies
- **Rate Limiting**: API throttling (planned)

---

## Data Flow Architecture

```mermaid
flowchart LR
    subgraph "Data Sources"
        UI[User Input]
        API[External APIs]
        Events[System Events]
    end

    subgraph "Ingestion"
        Gateway[API Gateway]
        Kafka[Kafka Topics]
    end

    subgraph "Processing"
        Services[Microservices]
        Cache[Redis Cache]
    end

    subgraph "Storage"
        PG[(PostgreSQL)]
        MG[(MongoDB)]
        ES[(Elasticsearch)]
    end

    subgraph "Output"
        Response[API Response]
        Notifications[Notifications]
        Analytics[Analytics]
    end

    UI --> Gateway
    API --> Gateway
    Events --> Kafka
    
    Gateway --> Services
    Kafka --> Services
    
    Services --> Cache
    Services --> PG
    Services --> MG
    Services --> ES
    
    Cache --> Response
    PG --> Response
    Services --> Response
    Services --> Notifications
    ES --> Analytics
```

---

## Deployment Architecture

```mermaid
graph TB
    subgraph "Docker Network: easyops-network"
        subgraph "Application Tier"
            FE[Frontend Container<br/>Node 20 Alpine]
            GW[API Gateway Container<br/>Maven + JDK 21]
            EU[Eureka Container<br/>Maven + JDK 21]
            AS[Auth Service Container<br/>Maven + JDK 21]
            US[User Service Container<br/>Maven + JDK 21]
            RS[RBAC Service Container<br/>Maven + JDK 21]
            OS[Org Service Container<br/>Maven + JDK 21]
        end
        
        subgraph "Data Tier"
            PG[PostgreSQL Container]
            MG[MongoDB Container]
            RD[Redis Container]
        end
        
        subgraph "Messaging Tier"
            KF[Kafka Container]
            ZK[Zookeeper Container]
            RB[RabbitMQ Container]
        end
        
        subgraph "Search Tier"
            ES[Elasticsearch Container]
        end
        
        subgraph "Tools"
            AD[Adminer Container]
        end
    end

    FE --> GW
    GW --> EU
    GW --> AS
    GW --> US
    GW --> RS
    GW --> OS
    
    AS --> PG
    US --> PG
    RS --> PG
    OS --> PG
    
    AS --> RD
    RS --> RD
    OS --> RD
    
    KF --> ZK
    
    AD --> PG
    AD --> MG
```

### Container Details

| Service | Base Image | Exposed Port | Health Check |
|---------|-----------|--------------|--------------|
| Frontend | node:20-alpine | 3000 | HTTP on / |
| API Gateway | maven:3.9-temurin-21 | 8081 | Actuator /health |
| Eureka | maven:3.9-temurin-21 | 8761 | Actuator /health |
| Auth Service | maven:3.9-temurin-21 | 8083 | Actuator /health |
| User Management | maven:3.9-temurin-21 | 8082 | Actuator /health |
| RBAC Service | maven:3.9-temurin-21 | 8084 | Actuator /health |
| Org Service | maven:3.9-temurin-21 | 8085 | Actuator /health |
| PostgreSQL | postgres:17 | 5432 | pg_isready |
| MongoDB | mongo:7 | 27017 | mongosh ping |
| Redis | redis:7-alpine | 6379 | redis-cli ping |
| Kafka | confluentinc/cp-kafka:7.3.3 | 9092 | kafka-topics list |
| Zookeeper | confluentinc/cp-zookeeper | 2181 | nc -z |
| RabbitMQ | rabbitmq:3-management | 5672, 15672 | rabbitmq-diagnostics |
| Elasticsearch | elasticsearch:8.11.0 | 9200 | curl /_cluster/health |
| Adminer | adminer | 8080 | N/A |

---

## Scalability & Performance

### Horizontal Scaling
- **Stateless Services**: All microservices are stateless
- **Service Discovery**: Eureka enables dynamic scaling
- **Load Balancing**: Client-side load balancing via Ribbon
- **Session Externalization**: Redis for distributed sessions

### Caching Strategy
- **L1 Cache**: Application-level caching
- **L2 Cache**: Redis distributed cache
- **Cache Patterns**: 
  - Cache-aside for user data
  - Write-through for sessions
  - TTL-based expiration

### Performance Optimizations
- **Database Connection Pooling**: HikariCP
- **Async Processing**: Kafka for event-driven operations
- **Reactive Programming**: WebFlux in API Gateway
- **Index Optimization**: Proper database indexes
- **Query Optimization**: JPA query optimization

---

## Monitoring & Observability

```mermaid
graph LR
    subgraph "Metrics Collection"
        A[Spring Actuator]
        B[Micrometer]
        C[Prometheus Endpoints]
    end

    subgraph "Logging"
        D[Logback]
        E[JSON Logging]
        F[Centralized Logs]
    end

    subgraph "Health Checks"
        G[Liveness Probes]
        H[Readiness Probes]
        I[Dependency Checks]
    end

    A --> B --> C
    D --> E --> F
    G --> H --> I
```

### Monitoring Capabilities
- **Health Endpoints**: `/actuator/health` on all services
- **Metrics**: CPU, memory, request rates, response times
- **Tracing**: Distributed tracing ready (Sleuth/Zipkin integration planned)
- **Logging**: Structured JSON logging
- **Alerts**: Health check failures trigger restarts

---

## Future Enhancements

### Planned Modules
1. **Accounting Module**: Complete accounting functionality
2. **Inventory Module**: Warehouse and inventory management
3. **Sales Module**: Sales order processing
4. **Purchase Module**: Procurement management
5. **Manufacturing Module**: Production planning
6. **HR Module**: Human resources management
7. **CRM Module**: Customer relationship management

### Infrastructure Enhancements
- **Service Mesh**: Istio for advanced traffic management
- **CI/CD**: Jenkins/GitLab CI pipelines
- **Monitoring**: Prometheus + Grafana dashboards
- **Tracing**: Zipkin/Jaeger integration
- **API Documentation**: OpenAPI/Swagger
- **Testing**: Increased test coverage with TestContainers

---

## Quick Start

See [DOCKER_START.md](DOCKER_START.md) for complete Docker setup instructions.

```bash
# Start all services
cd easyops-erp
docker-compose up -d

# Check status
docker-compose ps

# View logs
docker-compose logs -f

# Access applications
# Frontend: http://localhost:3000
# Eureka: http://localhost:8761
# Adminer: http://localhost:8080
```

---

## Development Workflow

```mermaid
graph LR
    A[Code Changes] --> B[Local Testing]
    B --> C[Docker Build]
    C --> D[Integration Tests]
    D --> E[Code Review]
    E --> F[Merge to Main]
    F --> G[Deploy]
```

---

## Contact & Support

For architecture questions or system design discussions, please refer to:
- [IMPLEMENTATION.md](IMPLEMENTATION.md) - Implementation details
- [QUICKSTART.md](QUICKSTART.md) - Quick start guide
- [TESTING_GUIDE.md](TESTING_GUIDE.md) - Testing guidelines

---

**Version**: 1.0.0 (Phase 0.2)  
**Last Updated**: October 2025  
**Architecture**: Microservices with Spring Cloud  
**Status**: Active Development

