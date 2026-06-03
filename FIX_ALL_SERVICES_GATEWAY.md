# Fix: API Gateway 500 Errors - Eureka Hostname Configuration

## Problem
All Spring Boot services running locally are registering with Eureka using IP addresses (e.g., `172.18.144.1`) instead of hostnames. The API Gateway running in Docker containers cannot reach these IP addresses, causing 500 Internal Server Error.

## Root Cause
- Services are using `prefer-ip-address: true` which makes them register with IP addresses
- Docker containers can't reach the host machine's IP directly
- Services should register with `` so Docker containers can reach them

## Services Affected
- ✅ **auth-service** (port 8083) - Fixed
- ✅ **rbac-service** (port 8084) - Fixed  
- ⚠️ **All other services** - Need same fix

## Solution Applied

### 1. Updated Configuration Files

**auth-service** (`easyops-erp/services/auth-service/src/main/resources/application.yml`):
```yaml
eureka:
  instance:
    hostname: ${EUREKA_INSTANCE_HOSTNAME:}
    prefer-ip-address: ${EUREKA_INSTANCE_PREFER_IP_ADDRESS:false}
```

**rbac-service** (`easyops-erp/services/rbac-service/src/main/resources/application.yml`):
```yaml
eureka:
  instance:
    hostname: ${EUREKA_INSTANCE_HOSTNAME:}
    prefer-ip-address: ${EUREKA_INSTANCE_PREFER_IP_ADDRESS:false}
```

### 2. Restart Scripts Created

- `start-auth-service.bat` - Restarts auth-service with correct config
- `start-rbac-service.bat` - Restarts RBAC service with correct config

## How to Fix Each Service

### Option 1: Restart Individual Service (Recommended)

For **auth-service**:
```batch
.\start-auth-service.bat
```

For **rbac-service**:
```batch
.\start-rbac-service.bat
```

### Option 2: Restart All Services

Stop all services, then restart using the start script:
```batch
cd easyops-erp
.\scripts\start-spring-services.bat
```

The start script should automatically set the correct environment variables if `` is resolvable.

## Verify Fix

After restarting a service, verify it's registered correctly:

```powershell
# Check Eureka registration
curl.exe -s http://localhost:8761/eureka/apps/AUTH-SERVICE | Select-String -Pattern "hostName"
curl.exe -s http://localhost:8761/eureka/apps/RBAC-SERVICE | Select-String -Pattern "hostName"

# Should show: <hostName></hostName>
```

## Test API Gateway Connectivity

```powershell
# Test auth service via gateway
curl.exe -X POST http://localhost:8081/api/auth/login `
  -H "Content-Type: application/json" `
  -d '{\"usernameOrEmail\":\"admin\",\"password\":\"Admin123!\"}'

# Test RBAC service via gateway  
curl.exe -X GET http://localhost:8081/api/rbac/authorization/users/39915ce7-e756-43ee-aa80-3dcf53abd199/roles
```

## Apply Same Fix to Other Services

If you encounter 500 errors with other services (user-management, organization-service, etc.), apply the same configuration change:

1. Update `application.yml`:
   ```yaml
   eureka:
     instance:
       hostname: ${EUREKA_INSTANCE_HOSTNAME:}
       prefer-ip-address: ${EUREKA_INSTANCE_PREFER_IP_ADDRESS:false}
   ```

2. Restart the service with environment variables:
   ```batch
   set EUREKA_INSTANCE_HOSTNAME=
   set EUREKA_INSTANCE_PREFER_IP_ADDRESS=false
   mvnw spring-boot:run -Dspring-boot.run.profiles=local
   ```

## Notes

- The configuration change is in `application.yml` files
- Services need to be **restarted** to pick up the new defaults
- If `` doesn't resolve, the start script will fall back to using the host IP
- All services should register with `` for Docker container connectivity

