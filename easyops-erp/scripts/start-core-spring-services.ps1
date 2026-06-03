# EasyOps ERP - Start Core Spring Boot Services (Eureka, Gateway, User Management, Auth, RBAC, Communication)
# Starts only the fundamental infrastructure microservices locally via Maven.
#
# Startup order: Eureka is started and must be healthy first (discovery). Then API Gateway,
# User Management, Auth, and RBAC are started in parallel so total time is much lower than
# starting them one-by-one. start-pharma-components feels faster because it only starts
# business services (which tend to boot quicker) and does not run log-tailing jobs.
#
# Prerequisites:
#   1. Core infrastructure (Postgres/Redis) should already be running.
#      The docker-based start-core-services.ps1 script is the easiest way to achieve this.
#   2. Java 21+ and Maven Wrapper dependencies available (mvnw will download as required).
#   3. Ports 8761 (Eureka), 8081 (Gateway), 8082 (User Management), 8083 (Auth), 8084 (RBAC), 8085 (Organization), and 8094 (Communication) must be free.

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

Write-Host "[INFO] Using localhost for Eureka registrations (Docker containers can reach it)" -ForegroundColor Cyan

# Core services: Eureka first (required for discovery), then Gateway/Auth/RBAC/UserMgmt/Organization can start in parallel
$CoreServices = @(
    "eureka",
    "api-gateway",
    "user-management",
    "auth-service",
    "rbac-service",
    "organization-service",
    "communication-service"
)
# Services that depend on Eureka - we start these in parallel after Eureka is healthy to reduce total startup time
$ServicesAfterEureka = @("api-gateway", "user-management", "auth-service", "rbac-service", "organization-service", "communication-service")

# Display startup information
Write-Host ""
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  EasyOps ERP Core Services Launcher (PowerShell)" -ForegroundColor Cyan
Write-Host "------------------------------------------------------------" -ForegroundColor Cyan
Write-Host "  Root directory : $RootDir"
Write-Host "  Maven command  : $MavenCmd"
Write-Host "  Spring profile : $SpringProfile"
Write-Host "  Log directory  : $LogDir"
Write-Host "  Services       : $($CoreServices -join ', ')"
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
$StartedProcesses = @()

foreach ($service in $CoreServices) {
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
    
    # Build command string for cmd.exe
    # Escape quotes properly for cmd.exe
    $mavenCmdEscaped = $mavenCmdPath -replace '"', '""'
    $logFileEscaped = $LogFile -replace '"', '""'
    $moduleDirEscaped = $ModuleDir -replace '"', '""'
    
    # Build the command that will be executed.
    # IMPORTANT: wrap the whole command in quotes when passing to `cmd /c` so `&&` is parsed by the *inner* cmd,
    # not the outer `cmd.exe /c start ...` invocation.
    $cmdLine = "cd /d ""$moduleDirEscaped"" && set SPRING_PROFILES_ACTIVE=$SpringProfile"
    if ($service -eq "communication-service") {
        $cmdLine += " && if not defined COMMUNICATION_LIQUIBASE_ENABLED set COMMUNICATION_LIQUIBASE_ENABLED=true"
        $cmdLine += " && if not defined COMMUNICATION_PHASE3_KAFKA_ENABLED set COMMUNICATION_PHASE3_KAFKA_ENABLED=true"
    }
    $cmdLine += " && ""$mavenCmdEscaped"" $($mavenArgs -join ' ') >> ""$logFileEscaped"" 2>&1"
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
        
        # Start background job to tail and display log file
        $tailJob = Start-Job -ScriptBlock {
            param($LogFilePath, $ServiceName)
            $lastPosition = 0
            $maxWaitTime = 300  # Stop after 5 minutes of no updates
            $noUpdateCount = 0
            
            while ($noUpdateCount -lt $maxWaitTime) {
                if (Test-Path $LogFilePath) {
                    try {
                        $file = Get-Item $LogFilePath -ErrorAction Stop
                        if ($file.Length -gt $lastPosition) {
                            $stream = [System.IO.File]::OpenRead($LogFilePath)
                            $stream.Position = $lastPosition
                            $reader = New-Object System.IO.StreamReader($stream)
                            
                            while ($null -ne ($line = $reader.ReadLine())) {
                                $color = 'Gray'
                                if ($line -match 'ERROR|Exception|Failed|FATAL|Error') { $color = 'Red' }
                                elseif ($line -match 'WARN|Warning') { $color = 'Yellow' }
                                elseif ($line -match 'INFO|Started|Tomcat started|Netty started|JVM running') { $color = 'Green' }
                                elseif ($line -match 'DEBUG') { $color = 'DarkGray' }
                                
                                Write-Host "[$ServiceName] $line" -ForegroundColor $color
                            }
                            
                            $lastPosition = $stream.Position
                            $reader.Close()
                            $stream.Close()
                            $noUpdateCount = 0
                        } else {
                            $noUpdateCount++
                        }
                    } catch {
                        # File might be locked, wait a bit
                        $noUpdateCount++
                    }
                } else {
                    $noUpdateCount++
                }
                Start-Sleep -Milliseconds 500
            }
        } -ArgumentList $LogFile, $service
        
        # Store job reference for cleanup
        $StartedProcesses[-1].TailJob = $tailJob
        
        # Only wait for Eureka here so the next services can start; others are waited for in parallel below
        if ($service -eq "eureka") {
            Wait-ForService -ServiceName "Eureka" -HealthUrl "http://localhost:8761/actuator/health" -MaxRetries 60
            Write-Host ""
        }
    } catch {
        Write-Host "[ERROR] Failed to launch $service (see $LogFile)" -ForegroundColor Red
        Write-Host "        Error: $_" -ForegroundColor Red
    }
}

# Wait for the services that boot in parallel (after Eureka)
$HealthChecks = @(
    @{ Name = "API Gateway"; Url = "http://localhost:8081/actuator/health" },
    @{ Name = "User Management Service"; Url = "http://localhost:8082/actuator/health" },
    @{ Name = "Auth Service"; Url = "http://localhost:8083/actuator/health" },
    @{ Name = "RBAC Service"; Url = "http://localhost:8084/actuator/health" },
    @{ Name = "Organization Service"; Url = "http://localhost:8085/actuator/health" },
    @{ Name = "Communication Service"; Url = "http://localhost:8094/actuator/health" }
)
Write-Host "[WAIT]  Waiting for Gateway, User Management, Auth, RBAC, Organization, and Communication (started in parallel)..." -ForegroundColor Cyan
foreach ($check in $HealthChecks) {
    Wait-ForService -ServiceName $check.Name -HealthUrl $check.Url -MaxRetries 60
    Write-Host ""
}

Write-Host ""
Write-Host "Core services launched in the background." -ForegroundColor Cyan
Write-Host "Check log files under $LogDir for startup progress." -ForegroundColor Cyan
Write-Host ""
Write-Host "Service URLs:" -ForegroundColor Cyan
Write-Host "  - Eureka:      http://localhost:8761" -ForegroundColor White
Write-Host "  - API Gateway: http://localhost:8081" -ForegroundColor White
Write-Host "  - User Management Service: http://localhost:8082" -ForegroundColor White
Write-Host "  - Auth Service: http://localhost:8083" -ForegroundColor White
Write-Host "  - RBAC Service: http://localhost:8084" -ForegroundColor White
Write-Host "  - Organization Service: http://localhost:8085" -ForegroundColor White
Write-Host "  - Communication Service: http://localhost:8094" -ForegroundColor White
Write-Host ""
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
    Write-Host "[STOP]  Stopping core services and log tailing..." -ForegroundColor Yellow
    
    foreach ($item in $StartedProcesses) {
        # Stop log tailing job
        if ($item.TailJob) {
            try {
                Stop-Job -Job $item.TailJob -ErrorAction SilentlyContinue
                Remove-Job -Job $item.TailJob -Force -ErrorAction SilentlyContinue
            } catch {
                # Job may have already stopped
            }
        }
        
        # Stop process
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

