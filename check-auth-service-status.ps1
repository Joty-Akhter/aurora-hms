# Check if auth-service is running and registered correctly with Eureka

Write-Host "Checking auth-service status..." -ForegroundColor Cyan
Write-Host ""

# Check if service is running on port 8083
$connection = Get-NetTCPConnection -LocalPort 8083 -ErrorAction SilentlyContinue | Select-Object -First 1
if ($connection) {
    Write-Host "Auth-service is running on port 8083" -ForegroundColor Green
} else {
    Write-Host "Auth-service is NOT running on port 8083" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "Checking Eureka registration..." -ForegroundColor Cyan

# Check Eureka registration
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8761/eureka/apps/AUTH-SERVICE" -UseBasicParsing -ErrorAction Stop
    $content = $response.Content
    
    if ($content -match '<hostName>(.*?)</hostName>') {
        $hostname = $matches[1]
        Write-Host "   Registered hostname: $hostname" -ForegroundColor Yellow
        
        if ($hostname -eq "localhost") {
            Write-Host "Auth-service is registered correctly with localhost" -ForegroundColor Green
        } elseif ($hostname -match '^\d+\.\d+\.\d+\.\d+$') {
            Write-Host "Auth-service is registered with IP address instead of hostname" -ForegroundColor Yellow
            Write-Host "This may cause connectivity issues with Docker containers" -ForegroundColor Yellow
        } else {
            Write-Host "Auth-service is registered with: $hostname" -ForegroundColor Cyan
        }
    } else {
        Write-Host "Could not find hostname in Eureka registration" -ForegroundColor Yellow
    }
    
    if ($content -match '<status>(.*?)</status>') {
        $status = $matches[1]
        $statusColor = if ($status -eq "UP") { "Green" } else { "Red" }
        Write-Host "   Status: $status" -ForegroundColor $statusColor
    }
} catch {
    Write-Host "Could not connect to Eureka or service not registered" -ForegroundColor Red
    Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "Testing direct auth service endpoint..." -ForegroundColor Cyan
try {
    $health = Invoke-RestMethod -Uri "http://localhost:8083/api/auth/health" -ErrorAction Stop
    Write-Host "Direct auth service endpoint is responding" -ForegroundColor Green
} catch {
    Write-Host "Direct auth service endpoint is NOT responding" -ForegroundColor Red
}

Write-Host ""
Write-Host "Testing API Gateway endpoint..." -ForegroundColor Cyan
try {
    $body = @{
        usernameOrEmail = "admin"
        password = "Admin123!"
    } | ConvertTo-Json
    
    $response = Invoke-RestMethod -Uri "http://localhost:8081/api/auth/login" -Method POST -Body $body -ContentType "application/json" -ErrorAction Stop
    Write-Host "API Gateway can reach auth-service! Login successful." -ForegroundColor Green
    $tokenPreview = $response.accessToken.Substring(0, [Math]::Min(50, $response.accessToken.Length))
    Write-Host "   Access token received: $tokenPreview..." -ForegroundColor Gray
} catch {
    Write-Host "API Gateway cannot reach auth-service" -ForegroundColor Red
    Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Red
    if ($_.Exception.Response -and $_.Exception.Response.StatusCode -eq 500) {
        Write-Host "   This is likely the NoRouteToHost error - check Eureka registration hostname" -ForegroundColor Yellow
    }
}
