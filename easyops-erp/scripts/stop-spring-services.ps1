# EasyOps ERP - Stop Spring Boot services launched via start-spring-services.ps1
#
# This script terminates any Spring Boot processes that were started with
# scripts/start-spring-services.ps1. It uses multiple methods to find processes:
# 1. Window titles (easyops-{service})
# 2. Port numbers (known service ports)
# 3. Command line patterns (spring-boot:run, com.easyops)
# 4. Java processes with matching characteristics

$ErrorActionPreference = "Continue"

# Resolve repository root
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RootDir = Resolve-Path (Join-Path $ScriptDir "..")
$LogDir = Join-Path $RootDir "logs\local-services"

# Known service ports
$ServicePorts = @(
    8761,  # eureka
    8081,  # api-gateway
    8082,  # user-management
    8083,  # auth-service
    8084,  # rbac-service
    8085,  # organization-service
    8086,  # notification-service
    8087,  # monitoring-service
    8088,  # accounting-service
    8089,  # (reserved)
    8090,  # ar-service
    8091,  # ap-service
    8092,  # bank-service
    8093,  # sales-service
    8094,  # inventory-service
    8095,  # purchase-service
    8096,  # crm-service
    8097,  # hr-service
    8098   # manufacturing-service
)

Write-Host "[STOP] Stopping EasyOps Spring Boot services..." -ForegroundColor Yellow
Write-Host ""

$stoppedCount = 0
$failedCount = 0
$foundPids = @()

# Method 1: Find processes by window title (easyops-{service})
Write-Host "[1/4] Checking for processes by window title..." -ForegroundColor Cyan
try {
    $windowProcs = Get-Process | Where-Object {
        $_.MainWindowTitle -like "easyops-*"
    }
    
    foreach ($proc in $windowProcs) {
        if ($proc.Id -notin $foundPids) {
            Write-Host "  - Found process by window title: $($proc.ProcessName) (PID: $($proc.Id))" -ForegroundColor Gray
            $foundPids += $proc.Id
        }
    }
} catch {
            Write-Host "  [WARN] Could not check window titles: $_" -ForegroundColor Yellow
}

# Method 2: Find processes by port numbers
Write-Host "[2/4] Checking for processes listening on service ports..." -ForegroundColor Cyan
try {
    foreach ($port in $ServicePorts) {
        $conns = Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction SilentlyContinue
        if ($conns) {
            foreach ($conn in $conns) {
                $processId = $conn.OwningProcess
                if ($processId -and $processId -notin $foundPids) {
                    try {
                        $proc = Get-Process -Id $processId -ErrorAction SilentlyContinue
                        if ($proc) {
                            Write-Host "  - Found process on port $port : $($proc.ProcessName) (PID: $processId)" -ForegroundColor Gray
                            $foundPids += $processId
                        }
                    } catch {
                        # Process may have exited
                    }
                }
            }
        }
    }
} catch {
    Write-Host "  ⚠️  Could not check ports: $_" -ForegroundColor Yellow
}

# Method 3: Find processes by command line patterns
Write-Host "[3/4] Checking for processes by command line patterns..." -ForegroundColor Cyan
try {
    $serviceNames = @(
        "eureka", "api-gateway", "user-management", "auth-service", "rbac-service",
        "organization-service", "notification-service", "monitoring-service",
        "accounting-service", "ar-service", "ap-service", "bank-service",
        "sales-service", "inventory-service", "purchase-service", "crm-service",
        "hr-service", "manufacturing-service"
    )
    
    # Get all Java processes
    $javaProcs = Get-CimInstance Win32_Process -Filter "Name = 'java.exe' OR Name = 'javaw.exe'" -ErrorAction SilentlyContinue
    
    foreach ($proc in $javaProcs) {
        $cmdLine = $proc.CommandLine
        if ($cmdLine) {
            # Check for Spring Boot patterns - look for com.easyops package or spring-boot argfile
            $isSpringBoot = $false
            $serviceName = "unknown"
            
            # Check for com.easyops.*Application pattern (most reliable)
            if ($cmdLine -match 'com\.easyops\.([a-z-]+)\.([A-Za-z]+Application)') {
                $isSpringBoot = $true
                $packageName = $matches[1]
                # Map package names to service names
                $packageToService = @{
                    'eureka' = 'eureka'
                    'gateway' = 'api-gateway'
                    'usermanagement' = 'user-management'
                    'auth' = 'auth-service'
                    'rbac' = 'rbac-service'
                    'organization' = 'organization-service'
                    'notification' = 'notification-service'
                    'monitoring' = 'monitoring-service'
                    'accounting' = 'accounting-service'
                    'ar' = 'ar-service'
                    'ap' = 'ap-service'
                    'bank' = 'bank-service'
                    'sales' = 'sales-service'
                    'inventory' = 'inventory-service'
                    'purchase' = 'purchase-service'
                    'crm' = 'crm-service'
                    'hr' = 'hr-service'
                    'manufacturing' = 'manufacturing-service'
                }
                if ($packageToService.ContainsKey($packageName)) {
                    $serviceName = $packageToService[$packageName]
                } else {
                    $serviceName = $packageName
                }
            }
            # Also check for spring-boot argfile pattern
            elseif ($cmdLine -match 'spring-boot-\d+\.argfile') {
                $isSpringBoot = $true
                # Try to identify service from command line
                foreach ($svc in $serviceNames) {
                    if ($cmdLine -match $svc) {
                        $serviceName = $svc
                        break
                    }
                }
            }
            # Check for spring-boot:run (Maven wrapper)
            elseif ($cmdLine -match 'spring-boot:run' -or ($cmdLine -match 'easyops-erp' -and $cmdLine -match 'services')) {
                $isSpringBoot = $true
                foreach ($svc in $serviceNames) {
                    if ($cmdLine -match $svc) {
                        $serviceName = $svc
                        break
                    }
                }
            }
            
            if ($isSpringBoot) {
                $processId = $proc.ProcessId
                if ($processId -notin $foundPids) {
                    Write-Host "  - Found Spring Boot process: $serviceName (PID: $processId)" -ForegroundColor Gray
                    $foundPids += $processId
                }
            }
        }
    }
} catch {
    Write-Host "  [WARN] Could not check command lines: $_" -ForegroundColor Yellow
}

# Method 4: Find Maven wrapper processes
Write-Host "[4/4] Checking for Maven wrapper processes..." -ForegroundColor Cyan
try {
    $mavenProcs = Get-CimInstance Win32_Process -Filter "Name = 'java.exe' OR Name = 'javaw.exe'" -ErrorAction SilentlyContinue | Where-Object {
        $_.CommandLine -match 'maven-wrapper\.jar' -and $_.CommandLine -match 'spring-boot:run'
    }
    
    foreach ($proc in $mavenProcs) {
        $processId = $proc.ProcessId
        if ($processId -notin $foundPids) {
            Write-Host "  - Found Maven wrapper process (PID: $processId)" -ForegroundColor Gray
            $foundPids += $processId
        }
    }
} catch {
    Write-Host "  [WARN] Could not check Maven processes: $_" -ForegroundColor Yellow
}

# Method 5: Fallback - find all Java processes with com.easyops in command line
Write-Host "[5/5] Fallback: Checking all Java processes for com.easyops..." -ForegroundColor Cyan
try {
    $allJavaProcs = Get-CimInstance Win32_Process -Filter "Name = 'java.exe' OR Name = 'javaw.exe'" -ErrorAction SilentlyContinue
    foreach ($proc in $allJavaProcs) {
            if ($proc.CommandLine -and $proc.CommandLine -match 'com\.easyops\.') {
            $processId = $proc.ProcessId
            if ($processId -notin $foundPids) {
                Write-Host "  - Found com.easyops process (PID: $processId)" -ForegroundColor Gray
                $foundPids += $processId
            }
        }
    }
} catch {
    Write-Host "  [WARN] Could not check all Java processes: $_" -ForegroundColor Yellow
}

# Remove duplicates and filter out invalid PIDs
$foundPids = $foundPids | Sort-Object -Unique | Where-Object { $_ -gt 0 }

if ($foundPids.Count -eq 0) {
    Write-Host ""
    Write-Host "[INFO] No running Spring Boot services detected." -ForegroundColor Cyan
    exit 0
}

Write-Host ""
Write-Host "Found $($foundPids.Count) process(es) to stop:" -ForegroundColor Yellow

# Stop each process
foreach ($processId in $foundPids) {
    try {
        $proc = Get-Process -Id $processId -ErrorAction Stop
        
        # Try to identify service name from process
        $serviceName = $proc.ProcessName
        try {
            $procInfo = Get-CimInstance Win32_Process -Filter "ProcessId = $processId" -ErrorAction SilentlyContinue
            if ($procInfo -and $procInfo.CommandLine) {
                $cmdLine = $procInfo.CommandLine
                # Use single quotes to avoid PowerShell parsing issues with regex
                if ($cmdLine -match 'services[\\/]([a-z-]+)[\\/]') {
                    $serviceName = $matches[1]
                } elseif ($cmdLine -match 'easyops-([a-z-]+)') {
                    $serviceName = $matches[1]
                }
            }
        } catch {
            # Use default process name
        }
        
        Write-Host "  - Stopping $serviceName (PID: $processId)..." -ForegroundColor Yellow -NoNewline
        
        # Try to stop the process
        $stopped = $false
        try {
            # First try graceful shutdown
            Stop-Process -Id $processId -ErrorAction Stop
            
            # Wait a bit to see if it stops gracefully
            Start-Sleep -Milliseconds 1000
            
            # Check if still running
            try {
                $stillRunning = Get-Process -Id $processId -ErrorAction Stop
                # Still running, force kill
                Write-Host " (force killing)" -ForegroundColor Red -NoNewline
                Stop-Process -Id $processId -Force -ErrorAction Stop
                Start-Sleep -Milliseconds 500
            } catch {
                # Process stopped successfully
            }
            
            # Verify it's stopped
            try {
                $verify = Get-Process -Id $processId -ErrorAction Stop
                Write-Host " [WARN: Still running]" -ForegroundColor Yellow
                $failedCount++
            } catch {
                # Process is stopped
                Write-Host " [OK]" -ForegroundColor Green
                $stopped = $true
                $stoppedCount++
            }
        } catch {
            Write-Host " [ERROR: $($_.Exception.Message)]" -ForegroundColor Red
            $failedCount++
        }
    } catch {
        Write-Host " [SKIP] (not found or already stopped)" -ForegroundColor Gray
        # Process may have already exited
    }
}

Write-Host ""
if ($stoppedCount -gt 0) {
    Write-Host "[OK] Done. Stopped: $stoppedCount process(es)." -ForegroundColor Green
}
if ($failedCount -gt 0) {
    Write-Host "[WARN] Failed to stop: $failedCount process(es)." -ForegroundColor Yellow
}
if ($stoppedCount -eq 0 -and $failedCount -eq 0) {
    Write-Host "[INFO] No active Spring services were running." -ForegroundColor Cyan
}

# Also try to stop any remaining cmd.exe processes with "easyops-" window titles
Write-Host ""
Write-Host "Cleaning up command windows..." -ForegroundColor Cyan
try {
    $cmdProcs = Get-Process cmd -ErrorAction SilentlyContinue | Where-Object {
        try {
            $_.MainWindowTitle -like "easyops-*"
        } catch {
            $false
        }
    }
    
    foreach ($proc in $cmdProcs) {
        try {
            Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue
            Write-Host "  - Stopped cmd.exe window: $($proc.MainWindowTitle)" -ForegroundColor Gray
        } catch {
            # Already stopped
        }
    }
} catch {
    # Ignore errors
}

Write-Host ""
Write-Host "All Spring Boot services have been stopped." -ForegroundColor Green

