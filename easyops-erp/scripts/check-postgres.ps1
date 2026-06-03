# EasyOps ERP - PostgreSQL Health Check Script
# Checks PostgreSQL connection and provides troubleshooting guidance

$ErrorActionPreference = "Continue"

Write-Host ""
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  PostgreSQL Health Check" -ForegroundColor Cyan
Write-Host "------------------------------------------------------------" -ForegroundColor Cyan
Write-Host ""

# Check if PostgreSQL container is running
Write-Host "[CHECK] Checking PostgreSQL container status..." -ForegroundColor Yellow
$postgresContainer = docker ps --filter "name=postgres" --format "{{.Names}}" 2>&1

if ($LASTEXITCODE -eq 0 -and $postgresContainer -match "postgres") {
    Write-Host "[OK]     PostgreSQL container is running: $postgresContainer" -ForegroundColor Green
    
    # Check container health
    $health = docker inspect --format='{{.State.Health.Status}}' postgres 2>&1
    if ($health -match "healthy") {
        Write-Host "[OK]     Container health status: $health" -ForegroundColor Green
    } elseif ($health -match "starting") {
        Write-Host "[WARN]   Container is starting (health check in progress)..." -ForegroundColor Yellow
    } else {
        Write-Host "[WARN]   Container health status: $health" -ForegroundColor Yellow
    }
} else {
    Write-Host "[ERROR]  PostgreSQL container is not running!" -ForegroundColor Red
    Write-Host ""
    Write-Host "  Solution: Start PostgreSQL using:" -ForegroundColor Yellow
    Write-Host "    .\scripts\start-core-services.ps1" -ForegroundColor Cyan
    Write-Host ""
    exit 1
}

Write-Host ""
Write-Host "[CHECK] Testing PostgreSQL connection..." -ForegroundColor Yellow

# Try to connect to PostgreSQL
$connectionTest = docker exec postgres psql -U easyops -d easyops -c "SELECT version();" 2>&1

if ($LASTEXITCODE -eq 0) {
    Write-Host "[OK]     PostgreSQL connection successful!" -ForegroundColor Green
    Write-Host ""
    Write-Host "  PostgreSQL is ready to accept connections." -ForegroundColor Green
    Write-Host ""
    exit 0
} else {
    Write-Host "[ERROR]  PostgreSQL connection failed!" -ForegroundColor Red
    Write-Host ""
    Write-Host "  Error details:" -ForegroundColor Yellow
    Write-Host "  $connectionTest" -ForegroundColor Red
    Write-Host ""
    
    # Check for recovery mode
    if ($connectionTest -match "recovery mode" -or $connectionTest -match "recovery") {
        Write-Host "  ⚠️  Database is in RECOVERY MODE" -ForegroundColor Red
        Write-Host ""
        Write-Host "  This usually happens when PostgreSQL is recovering from a crash or improper shutdown." -ForegroundColor Yellow
        Write-Host ""
        Write-Host "  Solutions:" -ForegroundColor Cyan
        Write-Host "  1. Wait for recovery to complete (usually takes 1-5 minutes)" -ForegroundColor White
        Write-Host "  2. Check PostgreSQL logs:" -ForegroundColor White
        Write-Host "     docker logs postgres --tail 50" -ForegroundColor Cyan
        Write-Host ""
        Write-Host "  3. If recovery is stuck, restart PostgreSQL:" -ForegroundColor White
        Write-Host "     docker restart postgres" -ForegroundColor Cyan
        Write-Host "     (Wait 30-60 seconds for PostgreSQL to start)" -ForegroundColor Gray
        Write-Host ""
        Write-Host "  4. If restart doesn't help, recreate the container:" -ForegroundColor White
        Write-Host "     docker stop postgres" -ForegroundColor Cyan
        Write-Host "     docker rm postgres" -ForegroundColor Cyan
        Write-Host "     .\scripts\start-core-services.ps1" -ForegroundColor Cyan
        Write-Host ""
    } else {
        Write-Host "  Troubleshooting steps:" -ForegroundColor Cyan
        Write-Host "  1. Check PostgreSQL logs:" -ForegroundColor White
        Write-Host "     docker logs postgres --tail 50" -ForegroundColor Cyan
        Write-Host ""
        Write-Host "  2. Restart PostgreSQL:" -ForegroundColor White
        Write-Host "     docker restart postgres" -ForegroundColor Cyan
        Write-Host ""
        Write-Host "  3. Verify database credentials in application.yml" -ForegroundColor White
        Write-Host ""
    }
    
    exit 1
}

