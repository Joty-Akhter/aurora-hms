# 🐳 Docker Complete Setup Guide

## 🚀 Start Everything with Docker

This guide will help you run the **complete EasyOps ERP system** using Docker Compose.

---

## ⚡ Quick Start (One Command)

```bash
cd easyops-erp
docker compose up -d
```

That's it! All services will start automatically.

### 🔹 Core Services Only

Need just the platform essentials (Postgres, Redis, Liquibase migrations, Adminer, Eureka, API Gateway)?

- macOS/Linux:
  ```bash
  ./start-core-services.sh
  ```
- Windows:
  ```powershell
  .\start-core-services.bat
  ```

---

## 📋 What Gets Started

The following services will be launched:

### **Infrastructure Services (Docker Only)**
- ✅ PostgreSQL (5432) – main relational database with persisted volume
- ✅ Redis (6379) – cache/session store used by auth and RBAC services
- ✅ Liquibase runner – applies schema migrations on startup
- ✅ Adminer (8080) – lightweight database UI
- ✅ Prometheus (9090) & Grafana (3001) – optional monitoring stack

> Optional components such as MongoDB, Kafka, Zookeeper, RabbitMQ, and Elasticsearch are available but disabled by default. Uncomment them in `docker-compose.yml` when you actually need them.

### **Core Platform Services**
- ✅ Eureka Server (8761) – service discovery
- ✅ API Gateway (8081) – unified API entry point
- ✅ Auth Service (8083) – authentication & tokens
- ✅ User Management (8082) – user provisioning
- ✅ RBAC Service (8084) – role based access control
- ✅ Organization Service (8085) – tenant management
- ✅ Notification Service (8086) – email notifications
- ✅ Monitoring Service (8087) – platform metrics aggregation

### **Business Microservices (Phase 1+)**
- ✅ Accounting (8088)
- ✅ Accounts Receivable (8090)
- ✅ Accounts Payable (8091)
- ✅ Bank & Cash Management (8092)
- ✅ Sales (8093)
- ✅ Inventory (8094)
- ✅ Purchase (8095)
- ✅ HR (8096)
- ✅ CRM (8097)
- ✅ Manufacturing (8098)

### **Frontend Application**
- ✅ React Frontend (3000)

**Total: 20+ containers (depending on optional stacks)**

---

## 🔧 Step-by-Step Instructions

### **1. Prerequisites**

Ensure you have:
- ✅ Docker Desktop (or Docker Engine) running
- ✅ Docker Compose v2 (`docker compose`) available
- ✅ At least 12GB RAM available (more is better for full stack)
- ✅ Ports 3000, 3001, 5432, 6379, 8080-8098, 8761, 9090 free

Check Docker is running:
```bash
docker --version
docker compose version
docker ps
```

### **2. Clone/Navigate to Project**

```bash
cd easyops-erp
```

### **3. Start All Services**

```bash
# Start in detached mode (background)
docker compose up -d

# Or start with logs visible
docker compose up
```

### **4. Wait for Services to Initialize**

```bash
# Watch the logs
docker compose logs -f

# Or check specific service
docker compose logs -f frontend
docker compose logs -f auth-service
```

**Wait about 2-3 minutes** for all services to fully start.

### **5. Verify Services Are Running**

```bash
# Check all containers
docker compose ps

# Should show all services as "Up"
```

### **6. Access the Application**

Open your browser and go to:
```
http://localhost:3000
```

Login with:
```
Username: admin
Password: Admin123!
```

---

## 🎯 Service URLs

| Service | URL | Description |
|---------|-----|-------------|
| **Frontend** | http://localhost:3000 | Main web UI |
| **API Gateway** | http://localhost:8081 | API entry point |
| **Eureka Dashboard** | http://localhost:8761 | Service registry |
| **Adminer** | http://localhost:8080 | Database UI |
| **Monitoring (Grafana)** | http://localhost:3001 | Observability dashboards |
| **Prometheus** | http://localhost:9090 | Metrics store |

### **Adminer Database Access**
- URL: http://localhost:8080
- System: PostgreSQL
- Server: `postgres`
- Username: `easyops`
- Password: `easyops123`
- Database: `easyops`

> Need RabbitMQ, Kafka, or Elasticsearch? Uncomment those services in `docker-compose.yml` before running `docker compose up`.

---

## 📊 Monitoring Commands

### **Check Status**
```bash
# View all running containers
docker compose ps

# View resource usage
docker stats

# Check specific service health
docker compose exec frontend curl http://localhost:3000
docker compose exec api-gateway curl http://localhost:8081/actuator/health
```

### **View Logs**
```bash
# All services
docker compose logs

# Follow logs (real-time)
docker compose logs -f

# Specific service
docker compose logs -f frontend
docker compose logs -f auth-service
docker compose logs -f postgres

# Last 100 lines
docker compose logs --tail=100
```

### **Service Health Checks**
```bash
# Check PostgreSQL
docker compose exec postgres pg_isready -U easyops

# Check Redis
docker compose exec redis redis-cli ping

# Check Auth Service
curl http://localhost:8083/api/auth/health
```

---

## 🔄 Management Commands

### **Restart Services**
```bash
# Restart all services
docker compose restart

# Restart specific service
docker compose restart frontend
docker compose restart auth-service
```

### **Stop Services**
```bash
# Stop all (keep data)
docker compose stop

# Stop specific service
docker compose stop frontend
```

### **Start Stopped Services**
```bash
# Start all
docker compose start

# Start specific service
docker compose start frontend
```

### **Rebuild Services**
```bash
# Rebuild all services
docker compose up -d --build

# Rebuild specific service
docker compose up -d --build frontend

# Force rebuild (no cache)
docker compose build --no-cache frontend
```

---

## 🗑️ Cleanup Commands

### **Stop and Remove Containers**
```bash
# Stop and remove containers (keep data)
docker compose down

# Stop and remove containers + volumes (DELETES DATA!)
docker compose down -v

# Stop and remove containers + images
docker compose down --rmi all
```

### **Clean Up Everything**
```bash
# Complete cleanup (DELETES EVERYTHING!)
docker compose down -v --rmi all --remove-orphans

# Remove unused Docker resources
docker system prune -a
```

---

## 🐛 Troubleshooting

### **Issue 1: Services Won't Start**

```bash
# Check Docker is running
docker ps

# Check logs for errors
docker compose logs

# Restart Docker daemon
sudo systemctl restart docker  # Linux
# Or restart Docker Desktop on Mac/Windows
```

### **Issue 2: Port Already in Use**

```bash
# Find process using port 3000
lsof -i :3000  # Mac/Linux
netstat -ano | findstr :3000  # Windows

# Kill the process or change port in docker-compose.yml
```

### **Issue 3: Frontend Won't Build**

```bash
# Rebuild with no cache
docker compose build --no-cache frontend

# Check frontend logs
docker compose logs frontend

# Enter container to debug
docker compose exec frontend sh
```

### **Issue 4: Database Not Initializing**

```bash
# Remove volumes and restart
docker compose down -v
docker compose up -d postgres redis

# Wait for initialization
docker compose logs -f postgres
```

### **Issue 5: Services Can't Connect**

```bash
# Check network
docker network ls
docker network inspect easyops_easyops-network

# Restart all services
docker compose restart
```

### **Issue 6: Out of Memory**

```bash
# Check Docker memory usage
docker stats

# Increase Docker memory limit in Docker Desktop settings
# Recommended: At least 8GB RAM for Docker
```

---

## 🔍 Advanced Commands

### **Enter a Container**
```bash
# Enter frontend container
docker compose exec frontend sh

# Enter backend container
docker compose exec auth-service sh

# Enter database container
docker compose exec postgres psql -U easyops -d easyops
```

### **Run Commands in Containers**
```bash
# Run npm command in frontend
docker compose exec frontend npm run build

# Run Maven command in backend
docker compose exec auth-service mvn clean install

# Execute SQL in PostgreSQL
docker compose exec postgres psql -U easyops -d easyops -c "SELECT * FROM users.users;"
```

### **Copy Files**
```bash
# Copy file from container to host
docker cp easyops-frontend:/app/package.json ./

# Copy file from host to container
docker cp ./file.txt easyops-frontend:/app/
```

---

## 📝 Environment Variables

Edit these in `docker-compose.yml` for each service:

### **Frontend**
```yaml
environment:
  - VITE_API_BASE_URL=http://localhost:8081
  - VITE_APP_NAME=EasyOps ERP
  - VITE_APP_VERSION=1.0.0
```

### **Backend Services**
```yaml
environment:
  - SPRING_PROFILES_ACTIVE=dev
  - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/easyops
  - JWT_SECRET=your-secret-key
```

---

## 🚀 Production Deployment

### **Build for Production**
```bash
# Build all services
docker compose -f docker-compose.prod.yml build

# Start production services
docker compose -f docker-compose.prod.yml up -d
```

### **Export Images**
```bash
# Save images
docker save -o easyops-frontend.tar easyops-frontend:latest
docker save -o easyops-backend.tar easyops-auth-service:latest

# Load images on another machine
docker load -i easyops-frontend.tar
```

---

## 📊 Performance Monitoring

### **Check Resource Usage**
```bash
# Real-time stats
docker stats

# Check disk usage
docker system df

# Container inspect
docker inspect easyops-frontend
```

### **View Container Metrics**
```bash
# CPU and memory
docker stats --no-stream

# Detailed container info
docker inspect easyops-frontend | grep -i memory
```

---

## ✅ Health Check

Create a script to check all services:

```bash
#!/bin/bash
echo "Checking EasyOps ERP Services..."

echo "Frontend: $(curl -s http://localhost:3000 && echo '✅' || echo '❌')"
echo "API Gateway: $(curl -s http://localhost:8081/actuator/health && echo '✅' || echo '❌')"
echo "Auth Service: $(curl -s http://localhost:8083/actuator/health && echo '✅' || echo '❌')"
echo "RBAC Service: $(curl -s http://localhost:8084/actuator/health && echo '✅' || echo '❌')"
echo "Eureka: $(curl -s http://localhost:8761 && echo '✅' || echo '❌')"
```

Save as `health-check.sh` and run:
```bash
chmod +x health-check.sh
./health-check.sh
```

---

## 🎯 Quick Reference

### **Daily Commands**
```bash
# Start
docker compose up -d

# Stop
docker compose stop

# Restart
docker compose restart

# Logs
docker compose logs -f

# Status
docker compose ps
```

### **Development Commands**
```bash
# Rebuild service
docker compose up -d --build frontend

# View logs
docker compose logs -f frontend

# Enter container
docker compose exec frontend sh
```

### **Cleanup Commands**
```bash
# Stop and remove
docker compose down

# Full cleanup
docker compose down -v --rmi all
```

---

## 🎉 Success!

If you see:
- ✅ All containers running (`docker compose ps`)
- ✅ Frontend accessible at http://localhost:3000
- ✅ Can login with admin/Admin123!
- ✅ Dashboard shows statistics

**Congratulations!** Your EasyOps ERP is running in Docker! 🚀

---

## 📞 Need Help?

1. Check logs: `docker compose logs -f`
2. Check status: `docker compose ps`
3. Restart: `docker compose restart`
4. Full reset: `docker compose down -v && docker compose up -d`

---

**For more details, see:**
- `QUICKSTART.md` - Quick start guide
- `FRONTEND_GUIDE.md` - Frontend documentation
- `COMPLETE_IMPLEMENTATION.md` - Full system overview

