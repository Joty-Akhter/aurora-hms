@echo off
REM EasyOps ERP - Windows Spring Boot launcher
REM Mirrors start-spring-services.sh for Git Bash/Command Prompt users.
REM
REM IMPORTANT: This script runs ALL Spring Boot services LOCALLY via Maven (mvnw spring-boot:run).
REM This includes Eureka and API Gateway - they are NOT run in Docker.
REM Only infrastructure services (Postgres, Redis, Frontend, etc.) run in Docker via start-core-services.bat
REM
REM Prerequisites:
REM   1. Core infrastructure (Postgres/Redis) should already be running.
REM      The docker-based start-core-services.bat script is the easiest way to achieve this.
REM   2. Java 21+ and Maven Wrapper dependencies available (mvnw will download as required).
REM   3. Ports 8761 (Eureka) and 8081 (API Gateway) must be free for local services.

setlocal enabledelayedexpansion

REM --- Resolve repository root -------------------------------------------------
set "SCRIPT_DIR=%~dp0"
for %%I in ("%SCRIPT_DIR%..") do set "ROOT_DIR=%%~fI"

REM --- Maven wrapper -----------------------------------------------------------
if not defined MAVEN_CMD (
  if exist "%ROOT_DIR%\mvnw.cmd" (
    set "MAVEN_CMD=%ROOT_DIR%\mvnw.cmd"
  ) else (
    set "MAVEN_CMD=%ROOT_DIR%\mvnw"
  )
)

REM --- Active Spring profile ---------------------------------------------------
if not defined SPRING_PROFILE (
  set "SPRING_PROFILE=local"
)
set "PROFILE=%SPRING_PROFILE%"

REM --- Log directory -----------------------------------------------------------
if not defined LOG_DIR (
  set "LOG_DIR=%ROOT_DIR%\logs\local-services"
)
set "PID_DIR=%LOG_DIR%\pids"

REM --- Default logging pattern -------------------------------------------------
set "DEFAULT_LOGGING_PATTERN_CONSOLE=%%d{yyyy-MM-dd HH:mm:ss.SSS} [%%thread] %%-5level %%logger{36} - %%msg%%n"
set "DEFAULT_LOGGING_PATTERN_FILE=%%d{yyyy-MM-dd HH:mm:ss.SSS} [%%thread] %%-5level %%logger{36} - %%msg%%n"

if not defined LOGGING_PATTERN_CONSOLE (
  set "LOGGING_PATTERN_CONSOLE=%DEFAULT_LOGGING_PATTERN_CONSOLE%"
)
if not defined LOGGING_PATTERN_FILE (
  set "LOGGING_PATTERN_FILE=%DEFAULT_LOGGING_PATTERN_FILE%"
)

if not exist "%LOG_DIR%" (
  mkdir "%LOG_DIR%"
)
if not exist "%PID_DIR%" (
  mkdir "%PID_DIR%"
)

REM --- Eureka hostname resolution ----------------------------------------------
REM Always use localhost for Eureka registration so Docker containers can reach services
REM Docker containers can resolve localhost even if host ping fails
if not defined EUREKA_INSTANCE_HOSTNAME (
  set "EUREKA_INSTANCE_HOSTNAME=localhost"
)
set "EUREKA_INSTANCE_PREFER_IP_ADDRESS=false"
set "DEFAULT_SERVICES=eureka api-gateway user-management auth-service rbac-service organization-service notification-service monitoring-service accounting-service ar-service ap-service bank-service sales-service inventory-service purchase-service crm-service hr-service manufacturing-service"
set "CORE_MANAGED_SERVICES=frontend adminer postgres redis prometheus grafana"
if not defined SERVICES_OVERRIDE (
  set "SERVICES=%DEFAULT_SERVICES%"
) else (
  set "SERVICES=%SERVICES_OVERRIDE%"
)

set "FILTERED_SERVICES="
for %%S in (%SERVICES%) do (
  set "SERVICE=%%~S"
  set "SKIP=0"
  for %%C in (%CORE_MANAGED_SERVICES%) do (
    if /I "%%~C"=="!SERVICE!" set "SKIP=1"
  )
  if "!SKIP!"=="1" (
    echo [SKIP]   !SERVICE! is managed by start-core-services (Docker).
  ) else (
    if defined FILTERED_SERVICES (
      set "FILTERED_SERVICES=!FILTERED_SERVICES! !SERVICE!"
    ) else (
      set "FILTERED_SERVICES=!SERVICE!"
    )
  )
)

if defined FILTERED_SERVICES (
  set "SERVICES=!FILTERED_SERVICES!"
) else (
  echo [WARN] No Spring services to launch; all requested services are managed by Docker.
  goto END
)

REM --- Maven wrapper bootstrap -------------------------------------------------
echo ============================================================
echo   EasyOps ERP Spring Boot Launcher (Windows)
echo ------------------------------------------------------------
echo   Root directory : %ROOT_DIR%
echo   Maven command  : %MAVEN_CMD%
echo   Spring profile : %PROFILE%
echo   Log directory  : %LOG_DIR%
echo   Services       : %SERVICES%
echo ============================================================
echo.

pushd "%ROOT_DIR%" >nul
call "%MAVEN_CMD%" -N -q --version >nul 2>&1
if errorlevel 1 (
  call "%MAVEN_CMD%" -N --version >nul 2>&1
)
popd >nul

REM --- Launch each service -----------------------------------------------------
set "EUREKA_STARTED=0"
set "API_GATEWAY_STARTED=0"

for %%S in (%SERVICES%) do (
  set "SERVICE=%%~S"
  set "MODULE_DIR=%ROOT_DIR%\services\!SERVICE!"
  if exist "!MODULE_DIR!\pom.xml" (
    set "LOG_FILE=%LOG_DIR%\!SERVICE!.log"
    echo [START] !SERVICE!  (profile=%PROFILE%)
    echo          log: !LOG_FILE!

    set "SERVICE_CMD=cd /d ""!MODULE_DIR!"" && "
    set "SERVICE_CMD=!SERVICE_CMD!set SPRING_PROFILES_ACTIVE=%PROFILE% && "
    set "SERVICE_CMD=!SERVICE_CMD!call ""%MAVEN_CMD%"" clean >nul 2>&1 && "
    set "SERVICE_CMD=!SERVICE_CMD!""%MAVEN_CMD%"" spring-boot:run "
    set "SERVICE_CMD=!SERVICE_CMD!-Dspring-boot.run.profiles=%PROFILE% "
    set "SERVICE_CMD=!SERVICE_CMD!-Dspring-boot.run.jvmArguments=--enable-native-access=ALL-UNNAMED "
    set "SERVICE_CMD=!SERVICE_CMD!-DskipTests=true "
    set "SERVICE_CMD=!SERVICE_CMD!-Deureka.instance.hostname=%EUREKA_INSTANCE_HOSTNAME% "
    set "SERVICE_CMD=!SERVICE_CMD!-Deureka.instance.preferIpAddress=%EUREKA_INSTANCE_PREFER_IP_ADDRESS%"

    if defined SPRING_BOOT_EXTRAS (
      set "SERVICE_CMD=!SERVICE_CMD! %SPRING_BOOT_EXTRAS%"
    )

    set "SERVICE_CMD=!SERVICE_CMD! >> ""!LOG_FILE!"" 2>&1"

    start "easyops-!SERVICE!" /b cmd /c "!SERVICE_CMD!"

    if errorlevel 1 (
      echo [ERROR] Failed to launch !SERVICE!  (see !LOG_FILE!)
    ) else (
      echo [OK]    !SERVICE! launch command issued.
      timeout /t 2 /nobreak >nul
    )
    
    REM Wait for Eureka before starting other services
    if "!SERVICE!"=="eureka" if "!EUREKA_STARTED!"=="0" (
      set "EUREKA_STARTED=1"
      echo [WAIT]  Waiting for Eureka to become healthy...
      powershell -NoProfile -Command "foreach ($i in 1..60) { try { if ((Invoke-WebRequest -UseBasicParsing 'http://localhost:8761/actuator/health').StatusCode -eq 200) { exit 0 } } catch { } Start-Sleep 2 } exit 1"
      if errorlevel 1 (
        echo [WARN] Eureka did not become healthy within timeout
      ) else (
        echo [OK]    Eureka is healthy
      )
      echo.
    )
    
    REM Wait for API Gateway before starting remaining services
    if "!SERVICE!"=="api-gateway" if "!API_GATEWAY_STARTED!"=="0" (
      set "API_GATEWAY_STARTED=1"
      echo [WAIT]  Waiting for API Gateway to become healthy...
      powershell -NoProfile -Command "foreach ($i in 1..60) { try { if ((Invoke-WebRequest -UseBasicParsing 'http://localhost:8081/actuator/health').StatusCode -eq 200) { exit 0 } } catch { } Start-Sleep 2 } exit 1"
      if errorlevel 1 (
        echo [WARN] API Gateway did not become healthy within timeout
      ) else (
        echo [OK]    API Gateway is healthy
      )
      echo.
    )
  ) else (
    echo [SKIP]   !SERVICE! (module directory not found)
  )
)

echo.
echo All services launched in the background.
echo Check log files under %LOG_DIR% for startup progress.
echo Press Ctrl+C to exit this script (services continue to run).

:END

endlocal

