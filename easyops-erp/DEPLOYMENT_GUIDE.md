# EasyOps ERP - Complete Deployment Guide

## 🚀 **Quick Deploy - All Services**

This guide will help you build and deploy all EasyOps ERP services to Docker.

---

## 📋 **What Will Be Deployed**

### **Total Services: 12 Microservices**

| Service | Port | Phase | Description |
|---------|------|-------|-------------|
| postgres | 5432 | Infrastructure | Database |
| redis | 6379 | Infrastructure | Cache |
| eureka | 8761 | 0.1 | Service Discovery |
| api-gateway | 8081 | 0.1 | API Gateway |
| user-management | 8082 | 0.1 | User Management |
| auth-service | 8083 | 0.1 | Authentication |
| rbac-service | 8084 | 0.2 | RBAC |
| organization-service | 8085 | 0.2 | Multi-tenancy |
| notification-service | 8086 | 0.3 | Notifications |
| monitoring-service | 8087 | 0.3 | Monitoring |
| accounting-service | 8088 | 1.1 | GL & CoA |
| ar-service | 8090 | 1.2 | Accounts Receivable |
| ap-service | 8091 | 1.2 | Accounts Payable |
| bank-service | 8092 | 1.2 | Bank Management |
| frontend | 3000 | UI | React Frontend |

---

## 🔧 **Prerequisites**

- ✅ Docker Desktop installed and running
- ✅ At least 8GB RAM allocated to Docker
- ✅ Ports 3000, 5432, 6379, 8080-8092, 8761, 9090, 3001 available

---

## 🚀 **Deployment Steps**

### **Step 1: Navigate to Project Directory**

```powershell
cd C:\workspace\together\EasyOps\easyops-erp
```

### **Step 2: Start Infrastructure Services First**

```powershell
# Start database and cache
docker-compose up -d postgres redis

# Wait for them to be healthy (30 seconds)
Start-Sleep -Seconds 30
```

### **Step 3: Start Core Services**

```powershell
# Start Eureka (service discovery)
docker-compose up -d eureka

# Wait for Eureka to be ready (30 seconds)
Start-Sleep -Seconds 30

# Start API Gateway
docker-compose up -d api-gateway

# Wait for Gateway to be ready (20 seconds)
Start-Sleep -Seconds 20
```

### **Step 4: Start All Microservices**

```powershell
# Start all application services
docker-compose up -d user-management auth-service rbac-service organization-service notification-service monitoring-service accounting-service ar-service ap-service bank-service

# Wait for services to start (60 seconds)
Start-Sleep -Seconds 60
```

### **Step 5: Start Frontend**

```powershell
# Start frontend
docker-compose up -d frontend

# Wait for frontend to build (60 seconds)
Start-Sleep -Seconds 60
```

### **Step 6: Start Monitoring Stack (Optional)**

```powershell
# Start Prometheus and Grafana
docker-compose up -d prometheus grafana adminer
```

---

## 🎯 **One-Command Deployment**

Use this single command to deploy everything:

```powershell
# Build and start all services
docker-compose up -d --build

# This will:
# 1. Build all Docker images
# 2. Start all services in correct order
# 3. Wait for dependencies
```

---

## ✅ **Verify Deployment**

### **Check Service Status**

```powershell
# View all running containers
docker-compose ps

# Should show all services as "running" or "healthy"
```

### **Check Service Health**

```powershell
# Eureka Dashboard
curl http://localhost:8761

# API Gateway Health
curl http://localhost:8081/actuator/health

# All registered services
# Visit: http://localhost:8761
```

### **Test Each Service**

```powershell
# Infrastructure
curl http://localhost:8761                    # Eureka
curl http://localhost:8081/actuator/health    # API Gateway

# Core Services
curl http://localhost:8082/actuator/health    # User Management
curl http://localhost:8083/actuator/health    # Auth Service
curl http://localhost:8084/actuator/health    # RBAC Service
curl http://localhost:8085/actuator/health    # Organization Service

# Phase 0.3 Services
curl http://localhost:8086/actuator/health    # Notification Service
curl http://localhost:8087/actuator/health    # Monitoring Service

# Phase 1.1 Service
curl http://localhost:8088/actuator/health    # Accounting Service

# Phase 1.2 Services
curl http://localhost:8090/actuator/health    # AR Service
curl http://localhost:8091/actuator/health    # AP Service
curl http://localhost:8092/actuator/health    # Bank Service
```

---

## 🌐 **Access the Application**

### **Frontend Application**
- **URL**: http://localhost:3000
- **Login**: admin@easyops.com
- **Password**: Admin123!

### **Service Dashboards**
- **Eureka Dashboard**: http://localhost:8761
- **API Gateway Swagger**: http://localhost:8081/swagger-ui.html
- **Grafana**: http://localhost:3001 (admin/admin)
- **Prometheus**: http://localhost:9090
- **Adminer (DB)**: http://localhost:8080

### **Individual Service Swagger UIs**
- **User Management**: http://localhost:8082/swagger-ui.html
- **Auth Service**: http://localhost:8083/swagger-ui.html
- **RBAC Service**: http://localhost:8084/swagger-ui.html
- **Organization Service**: http://localhost:8085/swagger-ui.html
- **Notification Service**: http://localhost:8086/swagger-ui.html
- **Monitoring Service**: http://localhost:8087/swagger-ui.html
- **Accounting Service**: http://localhost:8088/swagger-ui.html
- **AR Service**: http://localhost:8090/swagger-ui.html
- **AP Service**: http://localhost:8091/swagger-ui.html
- **Bank Service**: http://localhost:8092/swagger-ui.html

---

## 📊 **View Logs**

### **View All Logs**
```powershell
docker-compose logs -f
```

### **View Specific Service Logs**
```powershell
# Follow logs for specific service
docker-compose logs -f ar-service
docker-compose logs -f accounting-service
docker-compose logs -f frontend
```

### **View Last 100 Lines**
```powershell
docker-compose logs --tail=100 ar-service
```

---

## 🔄 **Common Operations**

### **Restart a Service**
```powershell
docker-compose restart ar-service
```

### **Rebuild a Service**
```powershell
docker-compose up -d --build ar-service
```

### **Stop All Services**
```powershell
docker-compose down
```

### **Stop and Remove Volumes (Fresh Start)**
```powershell
docker-compose down -v
```

### **View Resource Usage**
```powershell
docker stats
```

---

## 🐛 **Troubleshooting**

### **Service Won't Start**

1. **Check logs**:
   ```powershell
   docker-compose logs service-name
   ```

2. **Check if port is in use**:
   ```powershell
   netstat -ano | findstr :8090
   ```

3. **Rebuild the service**:
   ```powershell
   docker-compose up -d --build service-name
   ```

### **Database Connection Issues**

```powershell
# Check if postgres is running
docker-compose ps postgres

# Restart postgres
docker-compose restart postgres

# Check postgres logs
docker-compose logs postgres
```

### **Frontend Not Loading**

```powershell
# Check frontend logs
docker-compose logs frontend

# Rebuild frontend
docker-compose up -d --build frontend

# Clear browser cache (Ctrl + Shift + R)
```

### **Services Not Registering with Eureka**

1. Wait 60 seconds for registration
2. Check Eureka dashboard: http://localhost:8761
3. Restart the service:
   ```powershell
   docker-compose restart service-name
   ```

### **Out of Memory**

```powershell
# Increase Docker memory in Docker Desktop settings
# Recommended: 8GB minimum

# Restart Docker Desktop after changing
```

---

## 🧹 **Cleanup**

### **Stop All Services**
```powershell
docker-compose down
```

### **Remove All Data (Fresh Start)**
```powershell
docker-compose down -v
docker system prune -a --volumes
```

### **Remove Unused Images**
```powershell
docker image prune -a
```

---

## 📈 **Performance Optimization**

### **Allocate More Resources**
1. Open Docker Desktop
2. Go to Settings → Resources
3. Increase:
   - Memory: 8GB minimum
   - CPUs: 4+ cores
   - Disk: 60GB+

### **Enable BuildKit**
```powershell
# Set environment variable
$env:DOCKER_BUILDKIT=1
$env:COMPOSE_DOCKER_CLI_BUILD=1

# Then rebuild
docker-compose up -d --build
```

---

## 🔐 **Security Notes**

### **Default Credentials**

⚠️ **Change these in production!**

- **Admin User**: admin@easyops.com / Admin123!
- **Database**: easyops / easyops123
- **Eureka**: admin / admin
- **Grafana**: admin / admin

### **Update Credentials**

1. **Database**: Update in docker-compose.yml
2. **Admin User**: Use the frontend to change password
3. **Eureka**: Update in docker-compose.yml
4. **JWT Secret**: Update in api-gateway environment

---

## 📋 **Health Check Endpoints**

All services expose these endpoints:

- `/actuator/health` - Service health status
- `/actuator/info` - Service information
- `/actuator/metrics` - Service metrics
- `/actuator/prometheus` - Prometheus metrics

---

## 🎯 **Success Criteria**

✅ All services show as "running" in `docker-compose ps`
✅ Eureka dashboard shows 11 registered services
✅ Frontend loads at http://localhost:3000
✅ Can login with admin credentials
✅ Can navigate to all 7 accounting pages
✅ No errors in service logs

---

## 🚀 **Next Steps After Deployment**

1. **Login to Frontend**: http://localhost:3000
2. **Create Test Data**:
   - Organizations
   - Users
   - Chart of Accounts
   - Customers/Vendors
   - Test Invoices/Bills
3. **Explore Features**:
   - All 7 accounting pages
   - User management
   - Role-based access
4. **Monitor System**:
   - Grafana dashboards
   - Prometheus metrics
   - Service logs

---

## 📞 **Support**

If you encounter issues:
1. Check this troubleshooting guide
2. Review service logs
3. Check Docker Desktop resources
4. Verify port availability

---

**Deployment script ready! Run the commands to deploy your complete ERP system!** 🚀

