# EasyOps ERP - API Testing

This directory contains REST Client files for testing EasyOps ERP APIs using VS Code.

## Prerequisites

1. **Install VS Code Extension**: [REST Client by Huachao Mao](https://marketplace.visualstudio.com/items?itemName=humao.rest-client)
2. **Start Docker Services**: 
   ```bash
   cd easyops-erp
   docker-compose up -d
   ```
3. **Wait for Services**: Services need 2-3 minutes to become healthy after startup

## Available REST Files

### `auth.rest` - Authentication API Tests
Tests for the Authentication Service including:
- Login/Logout
- Token validation and refresh
- Password change and reset
- Health checks

## How to Use

1. Open any `.rest` file in VS Code
2. You'll see `Send Request` links above each API call
3. Click `Send Request` to execute the API call
4. Response appears in a split pane to the right

## Quick Test

1. Open `auth.rest`
2. Find the "Login with Admin User" section
3. Click `Send Request` above it
4. You should receive a 200 OK response with JWT tokens

## Default Credentials

```
Username: admin
Password: Admin123!
Email: admin@easyops.com
```

## Service URLs

| Service | URL | Description |
|---------|-----|-------------|
| Frontend | http://localhost:3000 | React Web App |
| API Gateway | http://localhost:8081 | Main API Entry Point |
| Auth Service | http://localhost:8083 | Direct Auth Service |
| User Service | http://localhost:8082 | User Management |
| Eureka | http://localhost:8761 | Service Registry |
| Adminer | http://localhost:8080 | Database UI |

## Response Status Codes

- `200 OK` - Success
- `400 Bad Request` - Invalid input
- `401 Unauthorized` - Authentication required or failed
- `403 Forbidden` - Insufficient permissions
- `404 Not Found` - Resource not found
- `500 Internal Server Error` - Server error
- `502 Bad Gateway` - Service not ready (wait a bit)

## Tips

### Extract Variables from Response
```http
# @name login
POST {{apiUrl}}/login
Content-Type: application/json

{
  "username": "admin",
  "password": "Admin123!"
}

### Use extracted token
@token = {{login.response.body.$.accessToken}}

GET {{apiUrl}}/validate
Authorization: Bearer {{token}}
```

### Check Service Status
```bash
# View all services
docker-compose ps

# Check logs
docker-compose logs -f auth-service

# Restart a service
docker-compose restart auth-service
```

### Common Issues

**502 Bad Gateway**
- Services still starting up
- Wait 2-3 minutes after `docker-compose up -d`
- Check: `docker-compose logs -f api-gateway`

**Connection Refused**
- Services not running
- Run: `docker-compose up -d`
- Check: `docker-compose ps`

**401 Unauthorized**
- Token expired (24 hours)
- Get new token by logging in again
- Check token is included in Authorization header

**No Admin User**
- Database not initialized
- Run: Check `docker-compose logs postgres`
- Initialize: Database tables should be created automatically

## Database Access

Access PostgreSQL using Adminer:
1. Go to http://localhost:8080
2. System: PostgreSQL
3. Server: postgres
4. Username: easyops
5. Password: easyops123
6. Database: easyops

## Testing Workflow

1. **Health Check** - Verify services are up
2. **Login** - Get JWT tokens
3. **Validate Token** - Verify token is valid
4. **Test Endpoints** - Use token for authenticated requests
5. **Logout** - Invalidate token

## Need Help?

- Check service logs: `docker-compose logs -f [service-name]`
- Check service status: `docker-compose ps`
- Restart services: `docker-compose restart`
- Full restart: `docker-compose down && docker-compose up -d`

## Next Steps

Create additional `.rest` files for other services:
- `users.rest` - User Management API
- `rbac.rest` - Roles and Permissions API
- `organizations.rest` - Organization Management API

