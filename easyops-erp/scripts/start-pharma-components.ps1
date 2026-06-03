# EasyOps ERP - Start Pharma Components
# Starts Inventory, Accounting, HR, and Pharma services locally via Maven.
#
# Prerequisites:
#   1. Core infrastructure (Postgres/Redis) should already be running.
#   2. Core Spring services (Eureka, Gateway, Auth, RBAC) should already be running.
#   3. Java 21+ and Maven Wrapper dependencies available.

$ErrorActionPreference = "Stop"

# Resolve repository root
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RootDir = Resolve-Path (Join-Path $ScriptDir "..")

# Maven wrapper
if (-not $env:MAVEN_CMD) {
    if (Test-Path (Join-Path $RootDir "mvnw.cmd")) {
        $MavenCmd = Join-Path $RootDir "mvnw.cmd"
    } else {
        $MavenCmd = Join-Path $RootDir "mvnw"
    }
} else {
    $MavenCmd = $env:MAVEN_CMD
}

# Active Spring profile
if ($env:SPRING_PROFILE) {
    $SpringProfile = $env:SPRING_PROFILE
} else {
    $SpringProfile = "local"
}

# Log directory
if ($env:LOG_DIR) {
    $LogDir = $env:LOG_DIR
} else {
    $LogDir = Join-Path $RootDir "logs\local-services"
}
$PidDir = Join-Path $LogDir "pids"

# Ensure directories exist
if (-not (Test-Path $LogDir)) {
    New-Item -ItemType Directory -Path $LogDir -Force | Out-Null
}
if (-not (Test-Path $PidDir)) {
    New-Item -ItemType Directory -Path $PidDir -Force | Out-Null
}

# Ensure Eureka clients register with a host reachable from Docker containers.
if (-not $env:EUREKA_INSTANCE_HOSTNAME) {
    $env:EUREKA_INSTANCE_HOSTNAME = "localhost"
}
$env:EUREKA_INSTANCE_PREFER_IP_ADDRESS = "false"

# Pharma components to start (bank-service required for deposit bank account dropdown; organization-service is now part of core-spring services)
$PharmaServices = @(
    "inventory-service",
    "accounting-service",
    "bank-service",
    "hr-service",
    "pharma-service"
)

# Display startup information
Write-Host ""
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  EasyOps ERP Pharma Components Launcher (PowerShell)" -ForegroundColor Cyan
Write-Host "------------------------------------------------------------" -ForegroundColor Cyan
Write-Host "  Root directory : $RootDir"
Write-Host "  Maven command  : $MavenCmd"
Write-Host "  Spring profile : $SpringProfile"
Write-Host "  Log directory  : $LogDir"
Write-Host "  Services       : $($PharmaServices -join ', ')"
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""

# Function to wait for service health
function Wait-ForService {
    param(
        [string]$ServiceName,
        [string]$HealthUrl,
        [int]$MaxRetries = 60
    )
    
    Write-Host "[WAIT]  Waiting for $ServiceName ($HealthUrl)..." -ForegroundColor Yellow
    $retries = $MaxRetries
    while ($retries -gt 0) {
        try {
            $response = Invoke-WebRequest -Uri $HealthUrl -UseBasicParsing -TimeoutSec 2 -ErrorAction Stop
            if ($response.StatusCode -eq 200) {
                Write-Host "[OK]    $ServiceName is healthy" -ForegroundColor Green
                return $true
            }
        } catch {
            # Service not ready yet
        }
        Start-Sleep -Seconds 2
        $retries--
    }
    
    Write-Host "[WARN]  $ServiceName did not become healthy within timeout" -ForegroundColor Yellow
    return $false
}

# Launch each service
$StartedProcesses = @()
$FailedServices = @()

foreach ($service in $PharmaServices) {
    $ModuleDir = Join-Path $RootDir "services\$service"
    
    if (-not (Test-Path (Join-Path $ModuleDir "pom.xml"))) {
        Write-Host "[SKIP]   $service (module directory not found)" -ForegroundColor Yellow
        continue
    }
    
    $LogFile = Join-Path $LogDir "$service.log"
    Write-Host "[START]  $service  (profile=$SpringProfile)" -ForegroundColor Green
    Write-Host "         log: $LogFile"
    
    # Build Maven command arguments
    $mavenCmdPath = $MavenCmd
    if (-not [System.IO.Path]::IsPathRooted($MavenCmd)) {
        $mavenCmdPath = Join-Path $RootDir $MavenCmd
    }
    
    $jvmArgs = "--enable-native-access=ALL-UNNAMED"
    
    $mavenArgs = @(
        "clean",
        "spring-boot:run",
        "-Dspring-boot.run.profiles=$SpringProfile",
        "-Dspring-boot.run.jvmArguments=`"$jvmArgs`"",
        "-Dspring.redis.host=localhost",
        "-Dspring.data.redis.host=localhost",
        "-DskipTests=true",
        "-Deureka.instance.hostname=$env:EUREKA_INSTANCE_HOSTNAME",
        "-Deureka.instance.preferIpAddress=$env:EUREKA_INSTANCE_PREFER_IP_ADDRESS"
    )
    
    if ($env:SPRING_BOOT_EXTRAS) {
        $mavenArgs += $env:SPRING_BOOT_EXTRAS -split '\s+'
    }
    
    # Build command string for cmd.exe
    $mavenCmdEscaped = $mavenCmdPath -replace '"', '""'
    $logFileEscaped = $LogFile -replace '"', '""'
    $moduleDirEscaped = $ModuleDir -replace '"', '""'
    
    # Build the command that will be executed.
    # IMPORTANT: wrap the whole command in quotes when passing to `cmd /c` so `&&` is parsed by the *inner* cmd,
    # not the outer `cmd.exe /c start ...` invocation.
    $cmdLine = "cd /d ""$moduleDirEscaped"" && set SPRING_PROFILES_ACTIVE=$SpringProfile && set SPRING_REDIS_HOST=localhost && ""$mavenCmdEscaped"" $($mavenArgs -join ' ') >> ""$logFileEscaped"" 2>&1"
    $cmdLineForCmd = "`"$cmdLine`""
    
    try {
        $process = Start-Process -FilePath "cmd.exe" `
            -ArgumentList "/c", "start", "`"easyops-$service`"", "/b", "cmd", "/c", $cmdLineForCmd `
            -WindowStyle Hidden `
            -PassThru
        
        Write-Host "[...]   $service process running, waiting for health..." -ForegroundColor Cyan
        $StartedProcesses += @{
            Service = $service
            Process = $process
            LogFile = $LogFile
        }
        
        Start-Sleep -Seconds 2
        
        # Health check ports
        $port = switch ($service) {
            "organization-service" { 8085 }
            "inventory-service" { 8094 }
            "accounting-service" { 8088 }
            "bank-service" { 8092 }
            "hr-service" { 8096 }
            "pharma-service" { 8095 }
            Default { 0 }
        }

        if ($port -gt 0) {
            $healthy = Wait-ForService -ServiceName $service -HealthUrl "http://localhost:$port/actuator/health" -MaxRetries 60
            if (-not $healthy) {
                $FailedServices += $service
            }
            Write-Host ""
        }
    } catch {
        Write-Host "[ERROR] Failed to launch $service (see $LogFile)" -ForegroundColor Red
        Write-Host "        Error: $_" -ForegroundColor Red
    }
}

if ($FailedServices.Count -gt 0) {
    Write-Host ""
    Write-Host "[ERROR] One or more services failed to start:" -ForegroundColor Red
    foreach ($s in $FailedServices) {
        $logPath = Join-Path $LogDir "$s.log"
        Write-Host "   - $s (check $logPath)" -ForegroundColor Red
    }
    Write-Host ""
    exit 1
}

Write-Host ""
Write-Host "Pharma components launched in the background." -ForegroundColor Cyan
Write-Host "Check log files under $LogDir for startup progress." -ForegroundColor Cyan
Write-Host ""
Write-Host "Service URLs:" -ForegroundColor Cyan
Write-Host "  - Organization: http://localhost:8085" -ForegroundColor White
Write-Host "  - Inventory:    http://localhost:8094" -ForegroundColor White
Write-Host "  - Accounting:   http://localhost:8088" -ForegroundColor White
Write-Host "  - Bank:         http://localhost:8092" -ForegroundColor White
Write-Host "  - HR:           http://localhost:8096" -ForegroundColor White
Write-Host "  - Pharma:       http://localhost:8095" -ForegroundColor White
Write-Host ""
Write-Host "Press Ctrl+C to exit this script (services continue to run)." -ForegroundColor Yellow
Write-Host ""

# Keep script running
try {
    while ($true) {
        Start-Sleep -Seconds 1
    }
} finally {
    Write-Host ""
    Write-Host "[STOP]  Stopping services..." -ForegroundColor Yellow
    
    foreach ($item in $StartedProcesses) {
        $proc = $item.Process
        if ($proc -and -not $proc.HasExited) {
            Write-Host "  - $($item.Service) (pid=$($proc.Id))" -ForegroundColor Yellow
            try {
                Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue
            } catch {}
        }
    }
}
