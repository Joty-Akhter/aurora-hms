# Fix: API Gateway 500 Error with Auth Service

## Problem
The API Gateway (running in Docker) returns a 500 error when trying to reach the auth-service because:
- Auth service registered with Eureka using IP `172.18.144.1:8083`
- Docker containers can't reach this IP from within the Docker network
- Error: `NoRouteToHostException: null: /172.18.144.1:8083`

## Solution
The auth service needs to register with Eureka using `host.docker.internal` as the hostname so Docker containers can reach it.

## Configuration Fixed
✅ Updated `application.yml` to use:
- `hostname: ${EUREKA_INSTANCE_HOSTNAME:host.docker.internal}` (default)
- `prefer-ip-address: ${EUREKA_INSTANCE_PREFER_IP_ADDRESS:false}` (use hostname, not IP)

## Steps to Apply the Fix

### Option 1: Restart Auth Service with Environment Variables (Recommended)

1. **Stop the currently running auth-service** (if started via script):
   ```powershell
   # Find and stop the Java process on port 8083
   $process = Get-NetTCPConnection -LocalPort 8083 | Select-Object -First 1 -ExpandProperty OwningProcess
   Stop-Process -Id $process -Force
   ```

2. **Restart with correct environment variables**:
   ```powershell
   cd easyops-erp\services\auth-service
   $env:EUREKA_INSTANCE_HOSTNAME = "host.docker.internal"
   $env:EUREKA_INSTANCE_PREFER_IP_ADDRESS = "false"
   $env:SPRING_PROFILES_ACTIVE = "local"
   mvnw spring-boot:run -Dspring-boot.run.profiles=local
   ```

### Option 2: Use the Start Script (Recommended)

The `start-spring-services.bat` script should automatically set these variables. Just restart the service:

1. **Stop all Spring services**:
   ```powershell
   # Kill any Java processes running Spring Boot services
   Get-Process java -ErrorAction SilentlyContinue | Where-Object { (Get-WmiObject Win32_Process -Filter "ProcessId = $($_.Id)").CommandLine -like "*spring-boot:run*" } | Stop-Process -Force
   ```

2. **Restart using the script**:
   ```powershell
   cd easyops-erp
   .\scripts\start-spring-services.bat
   ```

### Option 3: Verify Registration After Restart

After restarting, verify the auth service registered correctly:

```powershell
# Check Eureka registration
curl.exe -s http://localhost:8761/eureka/apps/AUTH-SERVICE | Select-String -Pattern "hostName|ipAddr" 

# Should show: <hostName>host.docker.internal</hostName>
```

### Option 4: Test the Gateway Endpoint

After restart, test if the gateway can reach the auth service:

```powershell
# This should now work!
curl.exe -X POST http://localhost:8081/api/auth/login -H "Content-Type: application/json" -d @login-request.json
```

## Expected Result

After restarting with the correct configuration:
- ✅ Auth service registers with Eureka using `host.docker.internal:8083`
- ✅ API Gateway can reach the auth service
- ✅ Frontend login works through the API Gateway
- ✅ No more 500 errors

## Quick Test Command

```powershell
# Test direct auth service (should work)
curl.exe -X POST http://localhost:8083/api/auth/login -H "Content-Type: application/json" -d '{\"usernameOrEmail\":\"admin\",\"password\":\"Admin123!\"}'

# Test via API Gateway (should work after restart)
curl.exe -X POST http://localhost:8081/api/auth/login -H "Content-Type: application/json" -d '{\"usernameOrEmail\":\"admin\",\"password\":\"Admin123!\"}'
```

## Notes

- The configuration change is already in `application.yml`
- The service just needs to be restarted to pick up the new defaults
- If `host.docker.internal` doesn't resolve on your system, the script will fall back to using your host IP, which might still have connectivity issues

