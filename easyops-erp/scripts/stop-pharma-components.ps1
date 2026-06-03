# EasyOps ERP - Stop Pharma Components
# Terminates only Pharma-related business services (Inventory, Accounting, HR, Bank, Pharma).
# Method 1: PID files (from start-pharma-components.sh or compatible launchers).
# Method 2–4: Port / window title / command line (only when available, no errors).

$ErrorActionPreference = "Continue"

# Resolve repository root
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RootDir = Resolve-Path (Join-Path $ScriptDir "..")
$PidDir = Join-Path $RootDir "logs\local-services\pids"

# Pharma components ports (align with scripts/start-pharma-components.ps1)
$PharmaPorts = @(
    8094, # inventory-service
    8088, # accounting-service
    8096, # hr-service
    8095  # pharma-service
)

$PharmaServices = @(
    "inventory-service",
    "accounting-service",
    "hr-service",
    "bank-service",
    "pharma-service"
)

Write-Host "[STOP] Stopping EasyOps Pharma components..." -ForegroundColor Yellow
Write-Host ""

$foundPids = @()

# Method 1: PID files (primary; works when started via start-pharma-components.sh or any launcher that writes PIDs)
if (Test-Path $PidDir) {
    foreach ($service in $PharmaServices) {
        $pidFile = Join-Path $PidDir "$service.pid"
        if (Test-Path $pidFile) {
            try {
                $pid = [int](Get-Content $pidFile -Raw).Trim()
                $proc = Get-Process -Id $pid -ErrorAction SilentlyContinue
                if ($proc) {
                    Write-Host "  - Stopping $service from PID file (PID: $pid)" -ForegroundColor Gray
                    $foundPids += $pid
                }
                Remove-Item $pidFile -Force -ErrorAction SilentlyContinue
            } catch {}
        }
    }
}

# Method 2: Find processes by port numbers (requires Get-NetTCPConnection; skip if not available)
try {
    $null = Get-Command Get-NetTCPConnection -ErrorAction Stop
    foreach ($port in $PharmaPorts) {
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
} catch {}

# Method 3: Find processes by window title
foreach ($service in $PharmaServices) {
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

# Method 4: Find processes by command line patterns (requires CIM; skip if not available)
try {
    $javaProcs = Get-CimInstance Win32_Process -Filter "Name = 'java.exe' OR Name = 'javaw.exe'" -ErrorAction SilentlyContinue
    foreach ($proc in $javaProcs) {
        $cmdLine = $proc.CommandLine
        if ($cmdLine) {
            foreach ($service in $PharmaServices) {
                if ($cmdLine -match [regex]::Escape($service) -and ($cmdLine -match "spring-boot:run" -or $cmdLine -match "com\.easyops")) {
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
    Write-Host "[INFO] No running Pharma components detected." -ForegroundColor Cyan
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
        foreach ($service in $PharmaServices) {
            if ($_.MainWindowTitle -like "easyops-$service*") { return $true }
        }
        return $false
    }
    foreach ($proc in $cmdProcs) {
        Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue
    }
} catch {}

Write-Host ""
Write-Host "Done. Stopped $stoppedCount pharma-related process(es)." -ForegroundColor Green
