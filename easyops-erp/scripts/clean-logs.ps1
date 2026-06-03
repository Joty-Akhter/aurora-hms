# EasyOps ERP - Clean log files
# Deletes all log files from logs/local-services directory
# Optionally stops services first if log files are locked

param(
    [switch]$StopServicesFirst = $false
)

$ErrorActionPreference = "Continue"

# Resolve repository root
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RootDir = Resolve-Path (Join-Path $ScriptDir "..")
$LogDir = Join-Path $RootDir "logs\local-services"

Write-Host "[CLEAN] Cleaning log files from: $LogDir" -ForegroundColor Cyan
Write-Host ""

# Check if services are running and log files are locked
$logFiles = Get-ChildItem -Path $LogDir -Filter "*.log" -ErrorAction SilentlyContinue
$lockedFiles = @()

if ($logFiles) {
    foreach ($file in $logFiles) {
        try {
            $stream = [System.IO.File]::Open($file.FullName, 'Open', 'ReadWrite', 'None')
            $stream.Close()
        } catch {
            $lockedFiles += $file
        }
    }
}

if ($lockedFiles.Count -gt 0 -and -not $StopServicesFirst) {
    Write-Host "[WARN] $($lockedFiles.Count) log file(s) are locked (services are running)." -ForegroundColor Yellow
    Write-Host ""
    Write-Host "To delete locked log files, either:" -ForegroundColor Cyan
    Write-Host "  1. Stop services first: .\scripts\stop-spring-services.ps1" -ForegroundColor White
    Write-Host "  2. Use -StopServicesFirst flag: .\scripts\clean-logs.ps1 -StopServicesFirst" -ForegroundColor White
    Write-Host ""
    $response = Read-Host "Stop services now and delete logs? (Y/N)"
    if ($response -eq 'Y' -or $response -eq 'y') {
        $StopServicesFirst = $true
    } else {
        Write-Host "[INFO] Cancelled. Log files not deleted." -ForegroundColor Yellow
        exit 0
    }
}

if ($StopServicesFirst -and $lockedFiles.Count -gt 0) {
    Write-Host ""
    Write-Host "[STOP] Stopping Spring Boot services first..." -ForegroundColor Yellow
    & (Join-Path $ScriptDir "stop-spring-services.ps1")
    Write-Host ""
    Write-Host "[WAIT] Waiting 2 seconds for file handles to release..." -ForegroundColor Cyan
    Start-Sleep -Seconds 2
    Write-Host ""
}

if (-not (Test-Path $LogDir)) {
    Write-Host "[INFO] Log directory does not exist: $LogDir" -ForegroundColor Yellow
    exit 0
}

# Get all log files (refresh list after stopping services)
$logFiles = Get-ChildItem -Path $LogDir -Filter "*.log" -ErrorAction SilentlyContinue

if ($logFiles.Count -eq 0) {
    Write-Host "[INFO] No log files found to delete." -ForegroundColor Cyan
    exit 0
}

Write-Host "Found $($logFiles.Count) log file(s) to delete:" -ForegroundColor Yellow

$deletedCount = 0
$failedCount = 0

foreach ($file in $logFiles) {
    try {
        Write-Host "  - Deleting: $($file.Name) ($([math]::Round($file.Length / 1MB, 2)) MB)" -ForegroundColor Gray
        Remove-Item -Path $file.FullName -Force -ErrorAction Stop
        $deletedCount++
    } catch {
        Write-Host "  [ERROR] Failed to delete $($file.Name): $_" -ForegroundColor Red
        $failedCount++
    }
}

Write-Host ""
if ($deletedCount -gt 0) {
    Write-Host "[OK] Deleted: $deletedCount file(s)" -ForegroundColor Green
}
if ($failedCount -gt 0) {
    Write-Host "[WARN] Failed to delete: $failedCount file(s)" -ForegroundColor Yellow
}

# Also clean PID files if they exist
$pidDir = Join-Path $LogDir "pids"
if (Test-Path $pidDir) {
    $pidFiles = Get-ChildItem -Path $pidDir -Filter "*.pid" -ErrorAction SilentlyContinue
    if ($pidFiles.Count -gt 0) {
        Write-Host ""
        Write-Host "Cleaning PID files..." -ForegroundColor Cyan
        foreach ($file in $pidFiles) {
            try {
                Remove-Item -Path $file.FullName -Force -ErrorAction Stop
            } catch {
                # Ignore errors
            }
        }
        Write-Host "[OK] Cleaned $($pidFiles.Count) PID file(s)" -ForegroundColor Green
    }
}

Write-Host ""
Write-Host "Log cleanup completed." -ForegroundColor Green

