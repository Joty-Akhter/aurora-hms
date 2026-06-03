# Rebuild API Gateway Script
# Run this script in PowerShell to rebuild the API Gateway service

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Rebuilding API Gateway Service" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# Stop the container
Write-Host "`nStopping API Gateway..." -ForegroundColor Yellow
docker stop easyops-api-gateway 2>$null

# Remove the container
Write-Host "Removing old container..." -ForegroundColor Yellow
docker rm easyops-api-gateway 2>$null

# Remove the old image to force rebuild
Write-Host "Removing old image..." -ForegroundColor Yellow
docker rmi easyops-erp-api-gateway 2>$null

# Rebuild and start
Write-Host "`nRebuilding API Gateway (this will take 2-3 minutes)..." -ForegroundColor Yellow
docker-compose up -d --build api-gateway

Write-Host "`n========================================" -ForegroundColor Green
Write-Host "Waiting for API Gateway to start..." -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green

Start-Sleep -Seconds 60

# Check status
Write-Host "`nChecking API Gateway status..." -ForegroundColor Yellow
docker ps --filter "name=easyops-api-gateway" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

Write-Host "`n========================================" -ForegroundColor Green
Write-Host "Testing endpoints..." -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green

Start-Sleep -Seconds 30

# Test health endpoint
Write-Host "`nTesting health endpoint..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "http://localhost:8081/actuator/health" -Method Get -TimeoutSec 5
    Write-Host "✅ Health endpoint: SUCCESS" -ForegroundColor Green
    $response | ConvertTo-Json
} catch {
    Write-Host "❌ Health endpoint: FAILED" -ForegroundColor Red
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
}

# Test auth health endpoint
Write-Host "`nTesting auth health endpoint..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "http://localhost:8081/api/auth/health" -Method Get -TimeoutSec 5
    Write-Host "✅ Auth health endpoint: SUCCESS" -ForegroundColor Green
    $response | ConvertTo-Json
} catch {
    Write-Host "❌ Auth health endpoint: FAILED" -ForegroundColor Red
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "Rebuild Complete!" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "`nYou can now test the login endpoint in auth.rest" -ForegroundColor Green
Write-Host "POST http://localhost:8081/api/auth/login" -ForegroundColor White

