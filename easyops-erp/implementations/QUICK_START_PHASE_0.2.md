# Quick Start Guide - Phase 0.2

## 🚀 Testing Authentication and RBAC Services

This guide provides step-by-step instructions to test the newly implemented Phase 0.2 services.

## Prerequisites

- Docker and Docker Compose installed
- A REST client (e.g., Postman, curl, or HTTPie)
- PostgreSQL running with initialized schema

## Step 1: Start the Services

```bash
cd easyops-erp

# Start all infrastructure services
docker-compose up -d postgres mongodb redis eureka

# Wait for services to be ready (about 30 seconds)
docker-compose ps

# Build the services (if not using Docker)
mvn clean install

# Start individual services (or use Docker Compose)
# Option 1: Using Maven (for development)
cd services/auth-service && mvn spring-boot:run &
cd services/rbac-service && mvn spring-boot:run &
cd services/user-management && mvn spring-boot:run &

# Option 2: Using Docker Compose
docker-compose up -d auth-service rbac-service user-management
```

## Step 2: Verify Services are Running

```bash
# Check Eureka Dashboard
curl http://localhost:8761

# Check service health
curl http://localhost:8083/actuator/health  # Auth Service
curl http://localhost:8084/actuator/health  # RBAC Service
curl http://localhost:8082/actuator/health  # User Management
```

## Step 3: Test Authentication Flow

### 3.1 Login with Default Admin User

```bash
# Login (username: admin, password: Admin123!)
curl -X POST http://localhost:8083/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "usernameOrEmail": "admin",
    "password": "Admin123!"
  }'
```

**Expected Response:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400000,
  "userId": "uuid-here",
  "username": "admin",
  "email": "admin@easyops.com",
  "firstName": "System",
  "lastName": "Administrator",
  "roles": [],
  "permissions": []
}
```

**Save the `accessToken` for subsequent requests!**

### 3.2 Validate Token

```bash
# Replace YOUR_ACCESS_TOKEN with the token from login
curl -X GET http://localhost:8083/api/auth/validate \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

**Expected Response:**
```json
{
  "valid": true
}
```

### 3.3 Refresh Token

```bash
# Replace YOUR_REFRESH_TOKEN with the refresh token from login
curl -X POST http://localhost:8083/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "YOUR_REFRESH_TOKEN"
  }'
```

### 3.4 Logout

```bash
curl -X POST http://localhost:8083/api/auth/logout \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

## Step 4: Test Password Reset Flow

### 4.1 Initiate Password Reset

```bash
curl -X POST http://localhost:8083/api/auth/password/reset \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@easyops.com"
  }'
```

**Check logs for reset token (in development mode):**
```bash
docker-compose logs auth-service | grep "Reset token"
```

### 4.2 Confirm Password Reset

```bash
# Replace RESET_TOKEN with token from logs
curl -X POST http://localhost:8083/api/auth/password/reset/confirm \
  -H "Content-Type: application/json" \
  -d '{
    "token": "RESET_TOKEN",
    "newPassword": "NewPassword123!"
  }'
```

## Step 5: Test RBAC Service

### 5.1 Get All Roles

```bash
curl -X GET http://localhost:8084/api/rbac/roles
```

**Expected Response:**
```json
{
  "content": [
    {
      "id": "uuid",
      "name": "System Administrator",
      "code": "SYSTEM_ADMIN",
      "description": "Full system access",
      "isSystemRole": true,
      "isActive": true,
      "permissions": [...]
    },
    ...
  ]
}
```

### 5.2 Get All Permissions

```bash
curl -X GET http://localhost:8084/api/rbac/permissions
```

### 5.3 Get System Roles

```bash
curl -X GET http://localhost:8084/api/rbac/roles/system
```

### 5.4 Create Custom Role

```bash
curl -X POST http://localhost:8084/api/rbac/roles \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Department Manager",
    "code": "DEPT_MANAGER",
    "description": "Department level management",
    "isActive": true,
    "permissionIds": []
  }'
```

### 5.5 Create Custom Permission

```bash
curl -X POST http://localhost:8084/api/rbac/permissions \
  -H "Content-Type: application/json" \
  -d '{
    "name": "View Reports",
    "code": "REPORT_VIEW",
    "resource": "reports",
    "action": "view",
    "description": "View system reports",
    "isActive": true
  }'
```

### 5.6 Assign Role to User

```bash
# Get user ID from login response, role ID from get roles
curl -X POST http://localhost:8084/api/rbac/authorization/users/roles \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "USER_UUID",
    "roleIds": ["ROLE_UUID_1", "ROLE_UUID_2"]
  }'
```

### 5.7 Get User Roles

```bash
curl -X GET http://localhost:8084/api/rbac/authorization/users/USER_UUID/roles
```

### 5.8 Get User Permissions

```bash
curl -X GET http://localhost:8084/api/rbac/authorization/users/USER_UUID/permissions
```

### 5.9 Check User Permission

```bash
curl -X POST http://localhost:8084/api/rbac/authorization/check \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "USER_UUID",
    "resource": "users",
    "action": "manage"
  }'
```

**Expected Response:**
```json
{
  "authorized": true
}
```

## Step 6: Test User Management Service

### 6.1 Create New User

```bash
curl -X POST http://localhost:8082/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "testuser@easyops.com",
    "password": "Test123!",
    "firstName": "Test",
    "lastName": "User",
    "phone": "+1234567890"
  }'
```

### 6.2 Login with New User

```bash
curl -X POST http://localhost:8083/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "usernameOrEmail": "testuser",
    "password": "Test123!"
  }'
```

### 6.3 Get All Users

```bash
curl -X GET http://localhost:8082/api/users
```

## Step 7: Integration Testing

### 7.1 Complete User Workflow

```bash
# 1. Create a user
USER_RESPONSE=$(curl -X POST http://localhost:8082/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john.doe",
    "email": "john.doe@easyops.com",
    "password": "John123!",
    "firstName": "John",
    "lastName": "Doe"
  }')

USER_ID=$(echo $USER_RESPONSE | jq -r '.id')

# 2. Get USER role
ROLE_RESPONSE=$(curl -X GET http://localhost:8084/api/rbac/roles/code/USER)
ROLE_ID=$(echo $ROLE_RESPONSE | jq -r '.id')

# 3. Assign role to user
curl -X POST http://localhost:8084/api/rbac/authorization/users/roles \
  -H "Content-Type: application/json" \
  -d "{
    \"userId\": \"$USER_ID\",
    \"roleIds\": [\"$ROLE_ID\"]
  }"

# 4. Login as the user
curl -X POST http://localhost:8083/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "usernameOrEmail": "john.doe",
    "password": "John123!"
  }'

# 5. Check user permissions
curl -X GET http://localhost:8084/api/rbac/authorization/users/$USER_ID/permissions
```

## Step 8: Monitor Services

### 8.1 Check Eureka Dashboard

Open browser: http://localhost:8761

You should see:
- EUREKA-SERVER
- API-GATEWAY
- USER-MANAGEMENT-SERVICE
- AUTH-SERVICE
- RBAC-SERVICE

### 8.2 Check Service Metrics

```bash
# Auth Service metrics
curl http://localhost:8083/actuator/metrics

# RBAC Service metrics
curl http://localhost:8084/actuator/metrics

# Prometheus metrics
curl http://localhost:8083/actuator/prometheus
```

### 8.3 Check Redis Cache

```bash
# Connect to Redis
docker-compose exec redis redis-cli

# View cached keys
KEYS *

# View a specific cache entry
GET "roles::ROLE_UUID"

# Clear cache
FLUSHALL
```

## Common Issues and Solutions

### Issue 1: Services Not Starting

```bash
# Check logs
docker-compose logs -f auth-service
docker-compose logs -f rbac-service

# Restart services
docker-compose restart auth-service rbac-service
```

### Issue 2: Database Connection Errors

```bash
# Check PostgreSQL is running
docker-compose ps postgres

# Verify database initialization
docker-compose exec postgres psql -U easyops -d easyops -c "\dt auth.*"
docker-compose exec postgres psql -U easyops -d easyops -c "\dt rbac.*"
```

### Issue 3: Token Validation Fails

- Ensure the JWT secret matches across services
- Check token hasn't expired (24 hours for access tokens)
- Verify Redis is running for session management

### Issue 4: Permission Denied

- Verify user has been assigned appropriate roles
- Check role has required permissions
- Clear Redis cache and retry

## Testing with Postman

1. Import the collection (if available)
2. Set environment variables:
   - `BASE_URL`: http://localhost:8083
   - `RBAC_URL`: http://localhost:8084
   - `USER_URL`: http://localhost:8082
3. Run the "Login" request first
4. Token will be automatically set for other requests

## Performance Testing

```bash
# Install Apache Bench
apt-get install apache2-utils

# Test login endpoint
ab -n 100 -c 10 -p login.json -T application/json http://localhost:8083/api/auth/login

# Test role retrieval
ab -n 1000 -c 50 http://localhost:8084/api/rbac/roles

# Monitor Redis cache hit rate
docker-compose exec redis redis-cli info stats
```

## Next Steps

1. ✅ Test all authentication endpoints
2. ✅ Test RBAC functionality
3. ✅ Verify caching is working
4. ✅ Test with multiple concurrent users
5. ⏭️  Implement frontend integration
6. ⏭️  Add comprehensive test suite
7. ⏭️  Deploy to staging environment

## Additional Resources

- [Phase 0.2 Implementation Details](PHASE_0.2_IMPLEMENTATION.md)
- [API Documentation](docs/api/)
- [Architecture Documentation](IMPLEMENTATION.md)
- [Database Schema](infrastructure/docker/postgres/init.sql)

## Support

For issues or questions:
1. Check service logs: `docker-compose logs [service-name]`
2. Verify service health: `curl http://localhost:PORT/actuator/health`
3. Check Eureka dashboard: http://localhost:8761
4. Review implementation docs: PHASE_0.2_IMPLEMENTATION.md

---

**Happy Testing! 🚀**

