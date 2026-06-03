# Fix API Gateway 401/403 Errors - Step by Step Guide

## 🔴 Problem
Getting CSRF errors (403) or Unauthorized (401) when testing `/api/auth/login` endpoint.

## ✅ Solution
The API Gateway needs to be rebuilt to load the new `SecurityConfig.java` that disables CSRF protection.

---

## 📋 **STEP-BY-STEP REBUILD INSTRUCTIONS**

### **Step 1: Close Any Log Viewers**
- Close any terminal windows showing `docker-compose logs`
- Close any Docker Desktop log viewers
- This prevents file locks

### **Step 2: Open Fresh PowerShell**
- Press `Windows + X`
- Select "Windows PowerShell" or "Terminal"
- **Run as Administrator** (right-click → Run as Administrator)

### **Step 3: Navigate to Project**
```powershell
cd C:\workspace\together\EasyOps\easyops-erp
```

### **Step 4: Stop API Gateway**
```powershell
docker stop easyops-api-gateway
```
**Expected output:** `easyops-api-gateway`

### **Step 5: Remove Container**
```powershell
docker rm easyops-api-gateway
```
**Expected output:** `easyops-api-gateway`

### **Step 6: Remove Old Image (Force Rebuild)**
```powershell
docker rmi easyops-erp-api-gateway
```
**Expected output:** `Untagged: easyops-erp-api-gateway:latest`

### **Step 7: Rebuild and Start**
```powershell
docker-compose up -d --build api-gateway
```

**Expected output:**
```
[+] Building 120.0s
...
✔ Container easyops-api-gateway Started
```

**This step takes 2-3 minutes!** ⏱️

### **Step 8: Wait for Startup**
```powershell
Start-Sleep -Seconds 90
```

### **Step 9: Check Status**
```powershell
docker-compose ps api-gateway
```

**Expected output:**
```
NAME                 STATUS                    PORTS
easyops-api-gateway  Up X minutes (healthy)    0.0.0.0:8081->8081/tcp
```

Wait until you see **(healthy)** in the status!

### **Step 10: View Logs (Optional)**
```powershell
docker-compose logs --tail=50 api-gateway
```

Look for:
```
Started ApiGatewayApplication in X seconds
```

---

## 🧪 **TEST AFTER REBUILD**

Open `easyops-erp/api/auth.rest` in VS Code and test:

### Test 1: API Gateway Health
```http
GET http://localhost:8081/actuator/health
```
**Expected:** ✅ `200 OK` (NOT 401)

### Test 2: Auth Health via Gateway
```http
GET http://localhost:8081/api/auth/health
```
**Expected:** ✅ `200 OK` with `{"status":"UP","service":"Authentication Service"}`

### Test 3: Login
```http
POST http://localhost:8081/api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "Admin123!"
}
```
**Expected:** ✅ `200 OK` with JWT tokens (NO MORE CSRF ERROR!)

---

## 🔍 **VERIFY THE FIX WAS APPLIED**

After rebuild, you can verify the SecurityConfig is loaded:

### Check Container Logs
```powershell
docker-compose logs api-gateway | Select-String "SecurityConfig"
```

If you see references to SecurityConfig or SecurityWebFilterChain, the new config is loaded!

---

## 🆘 **If Rebuild Still Doesn't Work**

### Option A: Nuclear Rebuild
```powershell
# Stop ALL services
docker-compose down

# Remove ALL old images
docker-compose down --rmi all

# Rebuild and start everything
docker-compose up -d --build
```

**Wait 5-10 minutes for all services to build and start**

### Option B: Direct Docker Commands
```powershell
# Build the image manually
docker build -t easyops-erp-api-gateway -f services/api-gateway/Dockerfile.dev .

# Remove old container
docker rm -f easyops-api-gateway

# Run new container
docker run -d --name easyops-api-gateway `
  --network easyops_easyops-network `
  -p 8081:8081 `
  -e SPRING_PROFILES_ACTIVE=dev `
  -e EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://eureka:8761/eureka `
  easyops-erp-api-gateway
```

---

## ✅ **SUCCESS INDICATORS**

After successful rebuild, you should see:

1. ✅ No CSRF errors (403)
2. ✅ No Unauthorized errors (401) on public endpoints
3. ✅ Login returns JWT tokens
4. ✅ Health checks return 200 OK

---

## 📊 **Before vs After Rebuild**

| Test | Before Rebuild | After Rebuild |
|------|----------------|---------------|
| Health Check | 401 Unauthorized | 200 OK |
| Auth Health | 401 Unauthorized | 200 OK |
| Login | 403 CSRF Error | 200 OK + Tokens |

---

## 💡 **Why This Happens**

Docker containers are **immutable snapshots**:
- ✅ Source code updated → `SecurityConfig.java` created
- ❌ Running container → Still has old compiled code
- 🔄 Solution → Must rebuild to compile and load new code

---

## 📞 **Need Help?**

Check these:
1. Container is running: `docker ps | findstr api-gateway`
2. Service logs: `docker-compose logs -f api-gateway`
3. Build logs: Look for "BUILD SUCCESS" in output
4. Time since creation: Container created date should be recent

---

**Follow Steps 1-9 above in a fresh PowerShell window to rebuild!**

