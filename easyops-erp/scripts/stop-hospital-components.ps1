# EasyOps ERP - Stop Hospital Components
# Terminates only Hospital-related business services (Inventory, Accounting, HR, Hospital, Hospital Pharmacy).

$ErrorActionPreference = "Continue"

# Resolve repository root
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RootDir = Resolve-Path (Join-Path $ScriptDir "..")

# Hospital components ports (align with scripts/start-hospital-components.ps1)
$HospitalPorts = @(
    8094, # inventory-service
    8088, # accounting-service
    8096, # hr-service
    8100, # hospital-service
    8111, # hospital-billing-service
    8110, # hospital-pharmacy-service
    8090, # hospital-card-management-service
    8095, # hospital-corporate-and-discount-service
    8092, # hospital-clinical-orders-service
    8093  # hospital-scheduling-service
)

$HospitalServices = @(
    "inventory-service",
    "accounting-service",
    "hr-service",
    "hospital-service",
    "hospital-billing-service",
    "hospital-pharmacy-service",
    "hospital-card-management-service",
    "hospital-corporate-and-discount-service",
    "hospital-clinical-orders-service",
    "hospital-scheduling-service"
)

Write-Host "[STOP] Stopping EasyOps Hospital components..." -ForegroundColor Yellow
Write-Host ""

$foundPids = @()

# Method 1: Find processes by port numbers
foreach ($port in $HospitalPorts) {
    try {
        $conns = Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction SilentlyContinue
        if ($conns) {
            foreach ($conn in $conns) {
                $processId = $conn.OwningProcess
                if ($processId -and $processId -notin $foundPids) {
                    $proc = Get-Process -Id $processId -ErrorAction SilentlyContinue
                    if ($proc) {
                        Write-Host "  - Found process on port $port (PID: $processId)" -ForegroundColor Gray
                        $foundPids += $processId
                    }
                }
            }
        }
    } catch {}
}

# Method 2: Find processes by window title
foreach ($service in $HospitalServices) {
    try {
        $windowProcs = Get-Process | Where-Object {
            $_.MainWindowTitle -like "easyops-$service*"
        }
        foreach ($proc in $windowProcs) {
            if ($proc.Id -notin $foundPids) {
                Write-Host "  - Found process by window title: $($proc.ProcessName) (PID: $($proc.Id))" -ForegroundColor Gray
                $foundPids += $proc.Id
            }
        }
    } catch {}
}

# Method 3: Find processes by command line patterns
try {
    $javaProcs = Get-CimInstance Win32_Process -Filter "Name = 'java.exe' OR Name = 'javaw.exe'" -ErrorAction SilentlyContinue
    foreach ($proc in $javaProcs) {
        $cmdLine = $proc.CommandLine
        if ($cmdLine) {
            foreach ($service in $HospitalServices) {
                if ($cmdLine -match $service -and ($cmdLine -match "spring-boot:run" -or $cmdLine -match "com\.easyops")) {
                    if ($proc.ProcessId -notin $foundPids) {
                        Write-Host "  - Found process by command line: $service (PID: $($proc.ProcessId))" -ForegroundColor Gray
                        $foundPids += $proc.ProcessId
                    }
                }
            }
        }
    }
} catch {}

if ($foundPids.Count -eq 0) {
    Write-Host "[INFO] No running Hospital components detected." -ForegroundColor Cyan
    exit 0
}

Write-Host ""
Write-Host "Stopping $($foundPids.Count) process(es)..." -ForegroundColor Yellow

$stoppedCount = 0
foreach ($p in $foundPids) {
    try {
        Stop-Process -Id $p -Force -ErrorAction Stop
        Write-Host "  [OK] PID ${p} stopped." -ForegroundColor Green
        $stoppedCount++
    } catch {
        Write-Host "  [ERROR] Failed to stop PID ${p}: $($_.Exception.Message)" -ForegroundColor Red
    }
}

# Cleanup remaining cmd windows for these services
try {
    $cmdProcs = Get-Process cmd -ErrorAction SilentlyContinue | Where-Object {
        foreach ($service in $HospitalServices) {
            if ($_.MainWindowTitle -like "easyops-$service*") { return $true }
        }
        return $false
    }
    foreach ($proc in $cmdProcs) {
        Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue
    }
} catch {}

Write-Host ""
Write-Host "Done. Stopped $stoppedCount hospital-related process(es)." -ForegroundColor Green
