# EasyOps ERP - Local Spring Boot launcher (PowerShell)
# Starts each microservice via mvnw with the provided profile (default: local).
#
# Prerequisites:
#   1. Core infrastructure (Postgres/Redis) should already be running.
#      The docker-based start-core-services.ps1 script is the easiest way to achieve this.
#   2. Java 21+ and Maven Wrapper dependencies available (mvnw will download as required).
#   3. Ports 8761 (Eureka) and 8081 (API Gateway) must be free for local services.

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

# Default log pattern for consistent structured logs across services
$DefaultLoggingPatternConsole = "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
$DefaultLoggingPatternFile = "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"

if (-not $env:LOGGING_PATTERN_CONSOLE) {
    $env:LOGGING_PATTERN_CONSOLE = $DefaultLoggingPatternConsole
}
if (-not $env:LOGGING_PATTERN_FILE) {
    $env:LOGGING_PATTERN_FILE = $DefaultLoggingPatternFile
}

# Ensure directories exist
if (-not (Test-Path $LogDir)) {
    New-Item -ItemType Directory -Path $LogDir -Force | Out-Null
}
if (-not (Test-Path $PidDir)) {
    New-Item -ItemType Directory -Path $PidDir -Force | Out-Null
}

# Ensure Eureka clients register with a host reachable from Docker containers.
# Always use localhost so Docker containers can reach services.
# Docker containers can resolve localhost even if host ping/resolution fails.
if (-not $env:EUREKA_INSTANCE_HOSTNAME) {
    $env:EUREKA_INSTANCE_HOSTNAME = "localhost"
}
$env:EUREKA_INSTANCE_PREFER_IP_ADDRESS = "false"

Write-Host "[INFO] Using localhost for Eureka registrations (Docker containers can reach it)" -ForegroundColor Cyan

# Update this list to match the services you want to boot locally.
# Order matters when services depend on one another.
# Eureka must start first, followed by API Gateway, then all other services.
$DefaultServices = @(
    "eureka",
    "api-gateway",
    "user-management",
    "auth-service",
    "rbac-service",
    "organization-service",
    "notification-service",
    "monitoring-service",
    "accounting-service",
    "ar-service",
    "ap-service",
    "bank-service",
    "sales-service",
    "inventory-service",
    "purchase-service",
    "crm-service",
    "hr-service",
    "manufacturing-service"
)

# Services managed by Docker via start-core-services.ps1
# NOTE: eureka and api-gateway are now run locally, not via Docker
$CoreManagedServices = @(
    "frontend",
    "adminer",
    "postgres",
    "redis",
    "prometheus",
    "grafana"
)

# Determine which services to start
if ($env:SERVICES_OVERRIDE) {
    $Services = $env:SERVICES_OVERRIDE -split '[,\s]+' | Where-Object { $_ }
} else {
    $Services = $DefaultServices
}

# Filter out core-managed services
$FilteredServices = @()
foreach ($service in $Services) {
    if ($CoreManagedServices -contains $service) {
        Write-Host "[SKIP]  Skipping $service because it is managed by start-core-services (Docker)." -ForegroundColor Yellow
    } else {
        $FilteredServices += $service
    }
}

if ($FilteredServices.Count -eq 0) {
    Write-Host "[WARN]  No Spring services to launch (all requested services are handled by Docker)." -ForegroundColor Yellow
    exit 0
}

$Services = $FilteredServices

# Display startup information
Write-Host ""
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  EasyOps ERP Spring Boot Launcher (PowerShell)" -ForegroundColor Cyan
Write-Host "------------------------------------------------------------" -ForegroundColor Cyan
Write-Host "  Root directory : $RootDir"
Write-Host "  Maven command  : $MavenCmd"
Write-Host "  Spring profile : $SpringProfile"
Write-Host "  Log directory  : $LogDir"
Write-Host "  Services       : $($Services -join ', ')"
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""

# Bootstrap Maven wrapper (quiet check)
Push-Location $RootDir
try {
    & $MavenCmd -N -q --version | Out-Null 2>&1
} catch {
    & $MavenCmd -N --version | Out-Null 2>&1
} finally {
    Pop-Location
}

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
$EurekaStarted = $false
$ApiGatewayStarted = $false
$StartedProcesses = @()

foreach ($service in $Services) {
    $ModuleDir = Join-Path $RootDir "services\$service"
    
    if (-not (Test-Path (Join-Path $ModuleDir "pom.xml"))) {
        Write-Host "[SKIP]   $service (module directory not found)" -ForegroundColor Yellow
        continue
    }
    
    $LogFile = Join-Path $LogDir "$service.log"
    Write-Host "[START]  $service  (profile=$SpringProfile)" -ForegroundColor Green
    Write-Host "         log: $LogFile"
    
    # Build Maven command arguments
    # Use absolute path for Maven wrapper
    $mavenCmdPath = $MavenCmd
    if (-not [System.IO.Path]::IsPathRooted($MavenCmd)) {
        $mavenCmdPath = Join-Path $RootDir $MavenCmd
    }
    
    # JVM arguments to suppress Java 25 warnings about restricted methods
    # This allows native access for Maven dependencies (jansi, tomcat, etc.)
    $jvmArgs = "--enable-native-access=ALL-UNNAMED"
    
    $mavenArgs = @(
        "spring-boot:run",
        "-Dspring-boot.run.profiles=$SpringProfile",
        "-Dspring-boot.run.jvmArguments=`"$jvmArgs`"",
        "-DskipTests=true",
        "-Deureka.instance.hostname=$env:EUREKA_INSTANCE_HOSTNAME",
        "-Deureka.instance.preferIpAddress=$env:EUREKA_INSTANCE_PREFER_IP_ADDRESS"
    )
    
    if ($env:SPRING_BOOT_EXTRAS) {
        $mavenArgs += $env:SPRING_BOOT_EXTRAS -split '\s+'
    }
    
    # Build full command for cmd.exe
    # Escape quotes properly for cmd.exe
    $mavenCmdEscaped = $mavenCmdPath -replace '"', '""'
    $logFileEscaped = $LogFile -replace '"', '""'
    $moduleDirEscaped = $ModuleDir -replace '"', '""'
    
    # Build the command that will be executed.
    # IMPORTANT: wrap the whole command in quotes when passing to `cmd /c` so `&&` is parsed by the *inner* cmd,
    # not the outer `cmd.exe /c start ...` invocation.
    $cmdLine = "cd /d ""$moduleDirEscaped"" && set SPRING_PROFILES_ACTIVE=$SpringProfile && ""$mavenCmdEscaped"" $($mavenArgs -join ' ') >> ""$logFileEscaped"" 2>&1"
    $cmdLineForCmd = "`"$cmdLine`""

    # Start service in background using cmd.exe start command
    try {
        $process = Start-Process -FilePath "cmd.exe" `
            -ArgumentList "/c", "start", "`"easyops-$service`"", "/b", "cmd", "/c", $cmdLineForCmd `
            -WindowStyle Hidden `
            -PassThru
        
        Write-Host "[OK]    $service launch command issued." -ForegroundColor Green
        $StartedProcesses += @{
            Service = $service
            Process = $process
            LogFile = $LogFile
        }
        
        Start-Sleep -Seconds 2
        
        # Wait for Eureka before starting other services
        if ($service -eq "eureka" -and -not $EurekaStarted) {
            $EurekaStarted = $true
            Wait-ForService -ServiceName "Eureka" -HealthUrl "http://localhost:8761/actuator/health" -MaxRetries 60
            Write-Host ""
        }
        
        # Wait for API Gateway before starting remaining services
        if ($service -eq "api-gateway" -and -not $ApiGatewayStarted) {
            $ApiGatewayStarted = $true
            Wait-ForService -ServiceName "API Gateway" -HealthUrl "http://localhost:8081/actuator/health" -MaxRetries 60
            Write-Host ""
        }
    } catch {
        Write-Host "[ERROR] Failed to launch $service (see $LogFile)" -ForegroundColor Red
        Write-Host "        Error: $_" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "All services launched in the background." -ForegroundColor Cyan
Write-Host "Check log files under $LogDir for startup progress." -ForegroundColor Cyan
Write-Host "Press Ctrl+C to exit this script (services continue to run)." -ForegroundColor Yellow
Write-Host ""

# Keep script running and handle cleanup on exit
try {
    # Wait for user interrupt
    while ($true) {
        Start-Sleep -Seconds 1
    }
} finally {
    # Cleanup on exit
    Write-Host ""
    Write-Host "[STOP]  Stopping Spring Boot services..." -ForegroundColor Yellow
    
    foreach ($item in $StartedProcesses) {
        $proc = $item.Process
        if ($proc -and -not $proc.HasExited) {
            Write-Host "  - $($item.Service) (pid=$($proc.Id))" -ForegroundColor Yellow
            try {
                Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue
            } catch {
                # Process may have already exited
            }
        }
    }
}
