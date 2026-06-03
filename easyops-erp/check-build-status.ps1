# Check Docker Build Status

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Docker Build Status Check" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Check if Docker is building
$buildProcess = Get-Process | Where-Object { $_.ProcessName -like "*docker*" }

if ($buildProcess) {
    Write-Host "✓ Docker is running" -ForegroundColor Green
} else {
    Write-Host "⚠ Docker may not be running" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Checking built images..." -ForegroundColor Yellow
Write-Host ""

# List EasyOps images
$images = docker images | Select-String "easyops"

if ($images) {
    Write-Host "Built images:" -ForegroundColor Green
    docker images | Select-String "easyops" | ForEach-Object { Write-Host $_ -ForegroundColor White }
    Write-Host ""
    
    $imageCount = ($images | Measure-Object).Count
    Write-Host "Total images built: $imageCount / 13" -ForegroundColor Cyan
    
    if ($imageCount -ge 13) {
        Write-Host ""
        Write-Host "========================================" -ForegroundColor Green
        Write-Host "  ✓ BUILD COMPLETE!" -ForegroundColor Green
        Write-Host "========================================" -ForegroundColor Green
        Write-Host ""
        Write-Host "Ready to deploy! Run:" -ForegroundColor Yellow
        Write-Host "  docker compose up -d" -ForegroundColor White
        Write-Host ""
        Write-Host "Or use the deployment script:" -ForegroundColor Yellow
        Write-Host "  .\deploy-all.ps1" -ForegroundColor White
    } else {
        Write-Host ""
        Write-Host "Build is still in progress..." -ForegroundColor Yellow
        Write-Host "Run this script again in a few minutes." -ForegroundColor White
    }
} else {
    Write-Host "No EasyOps images found yet." -ForegroundColor Yellow
    Write-Host "Build is still in progress or hasn't started." -ForegroundColor White
    Write-Host ""
    Write-Host "To check build progress:" -ForegroundColor Yellow
    Write-Host "  docker compose build" -ForegroundColor White
}

Write-Host ""


