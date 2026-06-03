# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**EasyOps ERP** is a full-stack, microservices-based Enterprise Resource Planning system with healthcare modules. The main application lives in `easyops-erp/`.

## Build & Run Commands

### Backend (Java 21 + Maven)

All backend commands run from `easyops-erp/`:

```bash
# Build all services
./mvnw clean install -DskipTests

# Run all tests (requires Docker for TestContainers)
./mvnw clean test

# Test a single service
./mvnw -pl services/auth-service test
./mvnw -pl services/user-management test
./mvnw -pl services/rbac-service test
```

### Frontend (React + Vite)

```bash
cd easyops-erp/frontend
npm install
npm run dev          # Dev server at http://localhost:3000
npm run build        # Production build
npm run lint         # ESLint
npm run type-check   # TypeScript check
```

### Local Development Stack

```bash
# Start infrastructure (postgres, redis, adminer)
cd easyops-erp
docker-compose up -d postgres redis adminer

# Then start Spring services manually or via script
./scripts/start-spring-services.sh     # Linux/Mac
./scripts/start-spring-services.bat    # Windows

# Or start full stack
./scripts/dev-start.sh
./scripts/dev-stop.sh
```

**Default credentials:** `admin` / `Admin123!`

## Service Ports

| Service | Port |
|---------|------|
| Frontend | 3000 |
| API Gateway | 8081 |
| Eureka Dashboard | 8761 |
| User Management | 8082 |
| Auth Service | 8083 |
| RBAC Service | 8084 |
| Organization Service | 8085 |
| PostgreSQL | 5432 |
| Redis | 6379 |
| Adminer (DB UI) | 8080 |

## Architecture

### Request Flow

```
Browser → API Gateway (8081) → Eureka (discovery) → Microservice → PostgreSQL/Redis
```

All client requests go through **Spring Cloud Gateway**. Services register with **Netflix Eureka** for dynamic discovery. Authentication uses **JWT tokens** validated at the gateway level; authorization uses RBAC checked per-service.

### Backend Service Structure

28 microservices under `easyops-erp/services/`, organized by domain:

- **Platform:** `eureka`, `api-gateway`, `auth-service`, `rbac-service`, `user-management`, `organization-service`
- **ERP:** `accounting-service`, `ar-service`, `ap-service`, `bank-service`, `sales-service`, `inventory-service`, `purchase-service`, `hr-service`, `crm-service`, `manufacturing-service`, `pharma-service`
- **Healthcare:** `hospital-service`, `hospital-billing-service`, `hospital-pharmacy-service`, `hospital-scheduling-service`, `hospital-card-management-service`, `hospital-clinical-orders-service`, `hospital-corporate-and-discount-service`

Each service has its own PostgreSQL schema. The root `easyops-erp/pom.xml` is the parent POM managing all service modules.

### Frontend Structure

```
easyops-erp/frontend/src/
├── components/     # Reusable UI components
├── pages/          # Route-level page components (organized by domain)
├── services/       # Axios API client modules
├── contexts/       # React Context providers (auth, theme, etc.)
├── types/          # TypeScript interfaces and type definitions
└── hooks/          # Custom React hooks
```

The frontend uses **React Context API** for state (no Redux). API calls go through service modules in `src/services/` using Axios. The active module preset is set via `VITE_MODULE_PRESET` in `.env` (currently `alien-pharma`).

### Database

- **PostgreSQL 17** — primary database, one schema per service domain
- **Redis 7** — distributed caching and session management
- **Liquibase** — database migrations in `easyops-erp/database-versioning/`

Run migrations: `cd easyops-erp/database-versioning && liquibase update`

### Security Model

1. Client authenticates via `auth-service` → receives JWT
2. JWT included in `Authorization: Bearer <token>` header
3. API Gateway validates JWT on every request
4. Services check RBAC permissions via `rbac-service`

## Key Tech Versions

- Java 21, Spring Boot 3.3.3, Spring Cloud 2023.0.3
- React 19, TypeScript 5.3.3, Vite 5, Material-UI 5
- PostgreSQL 17, Redis 7
- TestContainers 1.19.3 (integration tests require Docker)
