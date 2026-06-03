# EasyOps ERP - Complete Deployment Script
# This script builds and deploys all services to Docker

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  EasyOps ERP - Complete Deployment" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Check if Docker is running
Write-Host "Checking Docker status..." -ForegroundColor Yellow
$dockerStatus = docker info 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Docker is not running!" -ForegroundColor Red
    Write-Host "Please start Docker Desktop and try again." -ForegroundColor Red
    exit 1
}
Write-Host "✓ Docker is running" -ForegroundColor Green
Write-Host ""

# Navigate to project directory
$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $projectRoot
Write-Host "Project directory: $projectRoot" -ForegroundColor Cyan
Write-Host ""

# Function to check service health
function Wait-ForService {
    param(
        [string]$ServiceName,
        [string]$Url,
        [int]$MaxAttempts = 30,
        [int]$DelaySeconds = 2
    )
    
    Write-Host "Waiting for $ServiceName to be healthy..." -ForegroundColor Yellow
    $attempt = 0
    while ($attempt -lt $MaxAttempts) {
        try {
            $response = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 2 -ErrorAction SilentlyContinue
            if ($response.StatusCode -eq 200) {
                Write-Host "✓ $ServiceName is healthy" -ForegroundColor Green
                return $true
            }
        } catch {
            # Service not ready yet
        }
        $attempt++
        Start-Sleep -Seconds $DelaySeconds
        Write-Host "." -NoNewline
    }
    Write-Host ""
    Write-Host "⚠ $ServiceName health check timeout (may still be starting)" -ForegroundColor Yellow
    return $false
}

# Stop any existing containers
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Step 1: Stopping existing containers" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
docker-compose down
Write-Host ""

# Clean up old images (optional - uncomment if needed)
# Write-Host "Cleaning up old images..." -ForegroundColor Yellow
# docker image prune -f
# Write-Host ""

# Build and start infrastructure services
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Step 2: Starting Infrastructure" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Building and starting PostgreSQL and Redis..." -ForegroundColor Yellow
docker-compose up -d --build postgres redis adminer

Write-Host "Waiting for database to initialize (30 seconds)..." -ForegroundColor Yellow
Start-Sleep -Seconds 30
Write-Host "✓ Infrastructure ready" -ForegroundColor Green
Write-Host ""

# Start Eureka
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Step 3: Starting Service Discovery" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Building and starting Eureka..." -ForegroundColor Yellow
docker-compose up -d --build eureka

Wait-ForService "Eureka" "http://localhost:8761"
Write-Host ""

# Start API Gateway
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Step 4: Starting API Gateway" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Building and starting API Gateway..." -ForegroundColor Yellow
docker-compose up -d --build api-gateway

Wait-ForService "API Gateway" "http://localhost:8081/actuator/health"
Write-Host ""

# Start Phase 0.1 Services
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Step 5: Starting Core Services (Phase 0.1)" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Building and starting User Management and Auth Service..." -ForegroundColor Yellow
docker-compose up -d --build user-management auth-service

Write-Host "Waiting for services to start (20 seconds)..." -ForegroundColor Yellow
Start-Sleep -Seconds 20
Write-Host "✓ Core services started" -ForegroundColor Green
Write-Host ""

# Start Phase 0.2 Services
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Step 6: Starting RBAC & Organization (Phase 0.2)" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Building and starting RBAC and Organization services..." -ForegroundColor Yellow
docker-compose up -d --build rbac-service organization-service

Write-Host "Waiting for services to start (20 seconds)..." -ForegroundColor Yellow
Start-Sleep -Seconds 20
Write-Host "✓ RBAC and Organization services started" -ForegroundColor Green
Write-Host ""

# Start Phase 0.3 Services
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Step 7: Starting Notification & Monitoring (Phase 0.3)" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Building and starting Notification and Monitoring services..." -ForegroundColor Yellow
docker-compose up -d --build notification-service monitoring-service

Write-Host "Waiting for services to start (20 seconds)..." -ForegroundColor Yellow
Start-Sleep -Seconds 20
Write-Host "✓ Notification and Monitoring services started" -ForegroundColor Green
Write-Host ""

# Start Phase 1.1 Service
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Step 8: Starting Accounting Service (Phase 1.1)" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Building and starting Accounting Service..." -ForegroundColor Yellow
docker-compose up -d --build accounting-service

Write-Host "Waiting for service to start (30 seconds)..." -ForegroundColor Yellow
Start-Sleep -Seconds 30
Write-Host "✓ Accounting service started" -ForegroundColor Green
Write-Host ""

# Start Phase 1.2 Services
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Step 9: Starting AR/AP/Bank Services (Phase 1.2)" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Building and starting AR, AP, and Bank services..." -ForegroundColor Yellow
docker-compose up -d --build ar-service ap-service bank-service

Write-Host "Waiting for services to start (40 seconds)..." -ForegroundColor Yellow
Start-Sleep -Seconds 40
Write-Host "✓ AR, AP, and Bank services started" -ForegroundColor Green
Write-Host ""

# Start Frontend
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Step 10: Starting Frontend" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Building and starting Frontend (this may take a few minutes)..." -ForegroundColor Yellow
docker-compose up -d --build frontend

Write-Host "Waiting for frontend to build and start (60 seconds)..." -ForegroundColor Yellow
Start-Sleep -Seconds 60
Write-Host "✓ Frontend started" -ForegroundColor Green
Write-Host ""

# Start Monitoring Stack
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Step 11: Starting Monitoring Stack" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Starting Prometheus and Grafana..." -ForegroundColor Yellow
docker-compose up -d prometheus grafana

Write-Host "✓ Monitoring stack started" -ForegroundColor Green
Write-Host ""

# Display status
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Deployment Status" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "Checking service status..." -ForegroundColor Yellow
docker-compose ps
Write-Host ""

# Display access information
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Access Information" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Frontend Application:" -ForegroundColor Green
Write-Host "  URL: http://localhost:3000" -ForegroundColor White
Write-Host "  Login: admin@easyops.com" -ForegroundColor White
Write-Host "  Password: Admin123!" -ForegroundColor White
Write-Host ""
Write-Host "Service Dashboards:" -ForegroundColor Green
Write-Host "  Eureka: http://localhost:8761" -ForegroundColor White
Write-Host "  Grafana: http://localhost:3001 (admin/admin)" -ForegroundColor White
Write-Host "  Prometheus: http://localhost:9090" -ForegroundColor White
Write-Host "  Adminer (DB): http://localhost:8080" -ForegroundColor White
Write-Host ""
Write-Host "Swagger UIs:" -ForegroundColor Green
Write-Host "  API Gateway: http://localhost:8081/swagger-ui.html" -ForegroundColor White
Write-Host "  Accounting: http://localhost:8088/swagger-ui.html" -ForegroundColor White
Write-Host "  AR Service: http://localhost:8090/swagger-ui.html" -ForegroundColor White
Write-Host "  AP Service: http://localhost:8091/swagger-ui.html" -ForegroundColor White
Write-Host "  Bank Service: http://localhost:8092/swagger-ui.html" -ForegroundColor White
Write-Host ""

# Final health checks
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Health Checks" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$services = @(
    @{Name="API Gateway"; Url="http://localhost:8081/actuator/health"},
    @{Name="Accounting Service"; Url="http://localhost:8088/actuator/health"},
    @{Name="AR Service"; Url="http://localhost:8090/actuator/health"},
    @{Name="AP Service"; Url="http://localhost:8091/actuator/health"},
    @{Name="Bank Service"; Url="http://localhost:8092/actuator/health"}
)

foreach ($service in $services) {
    try {
        $response = Invoke-WebRequest -Uri $service.Url -UseBasicParsing -TimeoutSec 5 -ErrorAction Stop
        if ($response.StatusCode -eq 200) {
            Write-Host "✓ $($service.Name) is healthy" -ForegroundColor Green
        } else {
            Write-Host "⚠ $($service.Name) returned status $($response.StatusCode)" -ForegroundColor Yellow
        }
    } catch {
        Write-Host "⚠ $($service.Name) is not responding yet (may still be starting)" -ForegroundColor Yellow
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Deployment Complete!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Next Steps:" -ForegroundColor Yellow
Write-Host "1. Wait 1-2 minutes for all services to fully start" -ForegroundColor White
Write-Host "2. Open http://localhost:3000 in your browser" -ForegroundColor White
Write-Host "3. Login with: admin@easyops.com / Admin123!" -ForegroundColor White
Write-Host "4. Explore the 7 accounting pages!" -ForegroundColor White
Write-Host ""
Write-Host "To view logs: docker-compose logs -f" -ForegroundColor White
Write-Host "To check Eureka: http://localhost:8761" -ForegroundColor White
Write-Host ""
Write-Host "Deployment script completed successfully! 🚀" -ForegroundColor Green

