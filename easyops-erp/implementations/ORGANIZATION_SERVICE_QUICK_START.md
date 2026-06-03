# 🚀 Organization Service - Quick Start Guide

## Overview
The Organization Service provides comprehensive multi-tenancy and organization management for EasyOps ERP.

## 🏁 Getting Started

### 1. Start the Services

```bash
# Navigate to project directory
cd easyops-erp

# Start all services using Docker Compose
docker-compose up -d

# Check service status
docker-compose ps

# View organization service logs
docker-compose logs -f organization-service
```

### 2. Verify Service Health

```bash
# Check organization service health
curl http://localhost:8085/actuator/health

# Check via API Gateway
curl http://localhost:8081/api/organizations/health

# Verify Eureka registration
curl http://localhost:8761/eureka/apps/ORGANIZATION-SERVICE
```

## 📝 API Examples

### Organization Management

#### Create Organization
```bash
curl -X POST http://localhost:8081/api/organizations \
  -H "Content-Type: application/json" \
  -H "X-User-Id: <your-user-id>" \
  -d '{
    "code": "ACME",
    "name": "ACME Corporation",
    "legalName": "ACME Corp LLC",
    "email": "info@acme.com",
    "phone": "+1-555-0100",
    "website": "https://acme.com",
    "industry": "Technology",
    "businessType": "Corporation",
    "taxId": "12-3456789",
    "currency": "USD",
    "timezone": "America/New_York",
    "locale": "en-US",
    "addressLine1": "123 Main Street",
    "city": "New York",
    "state": "NY",
    "postalCode": "10001",
    "country": "US",
    "subscriptionPlan": "PROFESSIONAL",
    "maxUsers": 100,
    "maxStorage": 107374182400
  }'
```

#### Get Organization by ID
```bash
curl http://localhost:8081/api/organizations/{organization-id}
```

#### Get Organization by Code
```bash
curl http://localhost:8081/api/organizations/code/ACME
```

#### Update Organization
```bash
curl -X PUT http://localhost:8081/api/organizations/{organization-id} \
  -H "Content-Type: application/json" \
  -H "X-User-Id: <your-user-id>" \
  -d '{
    "name": "ACME Corporation Updated",
    "phone": "+1-555-0200"
  }'
```

#### Suspend Organization
```bash
curl -X PATCH http://localhost:8081/api/organizations/{organization-id}/suspend
```

#### Activate Organization
```bash
curl -X PATCH http://localhost:8081/api/organizations/{organization-id}/activate
```

### Department Management

#### Create Department
```bash
curl -X POST http://localhost:8081/api/organizations/{org-id}/departments \
  -H "Content-Type: application/json" \
  -d '{
    "code": "SALES",
    "name": "Sales Department",
    "description": "Responsible for sales operations",
    "type": "DEPARTMENT",
    "costCenter": "CC-SALES-001"
  }'
```

#### Create Sub-Department
```bash
curl -X POST http://localhost:8081/api/organizations/{org-id}/departments \
  -H "Content-Type: application/json" \
  -d '{
    "code": "SALES-NA",
    "name": "North America Sales",
    "parentDepartmentId": "{parent-dept-id}",
    "type": "DIVISION"
  }'
```

#### Get Department Tree
```bash
curl http://localhost:8081/api/organizations/{org-id}/departments/tree
```

Response example:
```json
[
  {
    "id": "uuid",
    "code": "SALES",
    "name": "Sales Department",
    "children": [
      {
        "id": "uuid",
        "code": "SALES-NA",
        "name": "North America Sales",
        "children": []
      }
    ]
  }
]
```

### Location Management

#### Create Location
```bash
curl -X POST http://localhost:8081/api/organizations/{org-id}/locations \
  -H "Content-Type: application/json" \
  -d '{
    "code": "HQ",
    "name": "Headquarters",
    "type": "HEADQUARTERS",
    "addressLine1": "123 Main Street",
    "city": "New York",
    "state": "NY",
    "postalCode": "10001",
    "country": "US",
    "phone": "+1-555-0100",
    "email": "hq@acme.com",
    "timezone": "America/New_York",
    "operatingHours": {
      "monday": "9:00-17:00",
      "tuesday": "9:00-17:00",
      "wednesday": "9:00-17:00",
      "thursday": "9:00-17:00",
      "friday": "9:00-17:00"
    },
    "coordinates": {
      "latitude": 40.7128,
      "longitude": -74.0060
    }
  }'
```

#### Get Locations by Type
```bash
curl http://localhost:8081/api/organizations/{org-id}/locations/type/BRANCH
```

### Settings Management

#### Set Organization Setting (Encrypted)
```bash
curl -X PUT http://localhost:8081/api/organizations/{org-id}/settings \
  -H "Content-Type: application/json" \
  -d '{
    "key": "api_secret_key",
    "value": "my-secret-api-key-12345",
    "type": "string",
    "encrypted": true
  }'
```

#### Set Organization Setting (Plain)
```bash
curl -X PUT http://localhost:8081/api/organizations/{org-id}/settings \
  -H "Content-Type: application/json" \
  -d '{
    "key": "default_language",
    "value": "en-US",
    "type": "string",
    "encrypted": false
  }'
```

#### Get All Settings
```bash
curl http://localhost:8081/api/organizations/{org-id}/settings
```

Response:
```json
{
  "api_secret_key": "my-secret-api-key-12345",
  "default_language": "en-US",
  "theme": "dark"
}
```

#### Get Specific Setting
```bash
curl http://localhost:8081/api/organizations/{org-id}/settings/default_language
```

### Invitation Management

#### Send Invitation
```bash
curl -X POST http://localhost:8081/api/organizations/{org-id}/invitations \
  -H "Content-Type: application/json" \
  -H "X-User-Id: <inviter-user-id>" \
  -d '{
    "email": "newuser@acme.com",
    "role": "MEMBER"
  }'
```

Response:
```json
{
  "id": "uuid",
  "organizationId": "uuid",
  "email": "newuser@acme.com",
  "role": "MEMBER",
  "invitedBy": "uuid",
  "status": "PENDING",
  "expiresAt": "2024-01-20T12:00:00Z",
  "createdAt": "2024-01-13T12:00:00Z"
}
```

#### Get Invitation by Token
```bash
curl http://localhost:8081/api/organizations/{org-id}/invitations/token/{token}
```

#### Accept Invitation
```bash
curl -X POST http://localhost:8081/api/organizations/{org-id}/invitations/accept/{token}
```

#### Get All Invitations for Organization
```bash
curl http://localhost:8081/api/organizations/{org-id}/invitations
```

#### Cancel Invitation
```bash
curl -X DELETE http://localhost:8081/api/organizations/{org-id}/invitations/{invitation-id}
```

## 🔧 Common Use Cases

### Use Case 1: Multi-Tenant SaaS Setup

```bash
# 1. Create organization for new tenant
ORG_RESPONSE=$(curl -X POST http://localhost:8081/api/organizations \
  -H "Content-Type: application/json" \
  -d '{"code":"CLIENT1","name":"Client Company","email":"admin@client.com"}')

ORG_ID=$(echo $ORG_RESPONSE | jq -r '.id')

# 2. Create departments
curl -X POST http://localhost:8081/api/organizations/$ORG_ID/departments \
  -H "Content-Type: application/json" \
  -d '{"code":"SALES","name":"Sales"}'

curl -X POST http://localhost:8081/api/organizations/$ORG_ID/departments \
  -H "Content-Type: application/json" \
  -d '{"code":"SUPPORT","name":"Support"}'

# 3. Set organization settings
curl -X PUT http://localhost:8081/api/organizations/$ORG_ID/settings \
  -H "Content-Type: application/json" \
  -d '{"key":"theme","value":"dark","type":"string"}'

# 4. Invite users
curl -X POST http://localhost:8081/api/organizations/$ORG_ID/invitations \
  -H "Content-Type: application/json" \
  -d '{"email":"user1@client.com","role":"ADMIN"}'
```

### Use Case 2: Organization Structure Setup

```bash
# Create main departments
SALES_ID=$(curl -X POST http://localhost:8081/api/organizations/{org-id}/departments \
  -H "Content-Type: application/json" \
  -d '{"code":"SALES","name":"Sales"}' | jq -r '.id')

# Create sub-departments
curl -X POST http://localhost:8081/api/organizations/{org-id}/departments \
  -H "Content-Type: application/json" \
  -d "{\"code\":\"SALES-NA\",\"name\":\"North America Sales\",\"parentDepartmentId\":\"$SALES_ID\"}"

curl -X POST http://localhost:8081/api/organizations/{org-id}/departments \
  -H "Content-Type: application/json" \
  -d "{\"code\":\"SALES-EU\",\"name\":\"Europe Sales\",\"parentDepartmentId\":\"$SALES_ID\"}"

# Get tree structure
curl http://localhost:8081/api/organizations/{org-id}/departments/tree
```

## 🐛 Troubleshooting

### Service Not Starting
```bash
# Check logs
docker-compose logs organization-service

# Check dependencies
docker-compose ps postgres redis eureka

# Restart service
docker-compose restart organization-service
```

### Database Connection Issues
```bash
# Check PostgreSQL
docker-compose exec postgres psql -U easyops -d easyops -c "SELECT 1;"

# Verify schema
docker-compose exec postgres psql -U easyops -d easyops -c "\dt admin.*"
```

### Eureka Registration Issues
```bash
# Check Eureka
curl http://localhost:8761/eureka/apps

# Force re-registration
docker-compose restart organization-service
```

## 📊 Monitoring

### Health Check
```bash
curl http://localhost:8085/actuator/health
```

### Metrics
```bash
curl http://localhost:8085/actuator/metrics
curl http://localhost:8085/actuator/metrics/http.server.requests
```

### Prometheus Metrics
```bash
curl http://localhost:8085/actuator/prometheus
```

## 🔐 Security Notes

### Encrypted Settings
- Sensitive settings are encrypted using AES
- Encryption happens automatically when `encrypted: true`
- Decryption is transparent when retrieving settings
- In production, use proper key management (AWS KMS, HashiCorp Vault)

### Invitation Tokens
- Tokens are UUIDs (secure random)
- Tokens expire after 7 days
- One-time use (status changes to ACCEPTED)
- Automatic cleanup of expired invitations

## 📚 Additional Resources

- **Requirements:** `requirements/administrative/organization_multi_tenancy_requirements.md`
- **Implementation Summary:** `PHASE_0.2_ORGANIZATION_COMPLETE.md`
- **API Gateway Config:** `services/api-gateway/src/main/resources/application.yml`
- **Service Config:** `services/organization-service/src/main/resources/application.yml`

## 🎯 Next Steps

1. **Frontend Development**
   - Create organization management UI
   - Build department tree visualization
   - Implement settings management interface

2. **Integration**
   - Add organization context to JWT tokens
   - Implement tenant-scoped RBAC
   - Add organization filtering to other services

3. **Testing**
   - Write integration tests
   - Performance testing
   - Security testing

---

**Happy Coding! 🚀**

