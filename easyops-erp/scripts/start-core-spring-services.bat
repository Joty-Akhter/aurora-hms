@echo off
REM EasyOps ERP - Start Core Spring Boot Services (Eureka, Gateway, User Management, Auth, RBAC, Communication)
REM Starts only the fundamental infrastructure microservices locally via Maven.
REM
REM Prerequisites:
REM   1. Core infrastructure (Postgres/Redis/Kafka) should already be running.
REM      The docker-based start-core-services.bat script is the easiest way to achieve this.
REM   2. Java 21+ and Maven Wrapper dependencies available.
REM   3. Ports 8761 (Eureka), 8081 (Gateway), 8082 (User Management), 8083 (Auth), 8084 (RBAC), 8085 (Organization), 8094 (Communication) must be free.

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
if not defined EUREKA_INSTANCE_HOSTNAME (
  set "EUREKA_INSTANCE_HOSTNAME=localhost"
)
set "EUREKA_INSTANCE_PREFER_IP_ADDRESS=false"

REM --- Core services to start in order ----------------------------------------
set "CORE_SERVICES=eureka api-gateway user-management auth-service rbac-service organization-service communication-service"

REM --- Maven wrapper bootstrap -------------------------------------------------
echo ============================================================
echo   EasyOps ERP Core Services Launcher (Windows)
echo ------------------------------------------------------------
echo   Root directory : %ROOT_DIR%
echo   Maven command  : %MAVEN_CMD%
echo   Spring profile : %PROFILE%
echo   Log directory  : %LOG_DIR%
echo   Services       : %CORE_SERVICES%
echo ============================================================
echo.

pushd "%ROOT_DIR%" >nul
call "%MAVEN_CMD%" -N -q --version >nul 2>&1
if errorlevel 1 (
  call "%MAVEN_CMD%" -N --version >nul 2>&1
)
popd >nul

REM --- Launch each service -----------------------------------------------------
for %%S in (%CORE_SERVICES%) do (
  set "SERVICE=%%~S"
  set "MODULE_DIR=%ROOT_DIR%\services\!SERVICE!"
  if exist "!MODULE_DIR!\pom.xml" (
    set "LOG_FILE=%LOG_DIR%\!SERVICE!.log"
    echo [START] !SERVICE!  (profile=%PROFILE%)
    echo          log: !LOG_FILE!

    set "SERVICE_CMD=cd /d ""!MODULE_DIR!"" && "
    set "SERVICE_CMD=!SERVICE_CMD!set SPRING_PROFILES_ACTIVE=%PROFILE% && "
    if "!SERVICE!"=="communication-service" (
      set "SERVICE_CMD=!SERVICE_CMD!set COMMUNICATION_LIQUIBASE_ENABLED=true && "
      set "SERVICE_CMD=!SERVICE_CMD!set COMMUNICATION_PHASE3_KAFKA_ENABLED=true && "
    )
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
    if "!SERVICE!"=="eureka" (
      echo [WAIT]  Waiting for Eureka to become healthy...
      powershell -NoProfile -Command "foreach ($i in 1..60) { try { if ((Invoke-WebRequest -UseBasicParsing 'http://localhost:8761/actuator/health').StatusCode -eq 200) { exit 0 } } catch { } Start-Sleep 2 } exit 1"
      if errorlevel 1 (
        echo [WARN] Eureka did not become healthy within timeout
      ) else (
        echo [OK]    Eureka is healthy
      )
      echo.
    )
    
    REM Wait for API Gateway after starting it
    if "!SERVICE!"=="api-gateway" (
      echo [WAIT]  Waiting for API Gateway to become healthy...
      powershell -NoProfile -Command "foreach ($i in 1..60) { try { if ((Invoke-WebRequest -UseBasicParsing 'http://localhost:8081/actuator/health').StatusCode -eq 200) { exit 0 } } catch { } Start-Sleep 2 } exit 1"
      if errorlevel 1 (
        echo [WARN] API Gateway did not become healthy within timeout
      ) else (
        echo [OK]    API Gateway is healthy
      )
      echo.
    )

    REM Wait for User Management Service after starting it
    if "!SERVICE!"=="user-management" (
      echo [WAIT]  Waiting for User Management Service to become healthy...
      powershell -NoProfile -Command "foreach ($i in 1..60) { try { if ((Invoke-WebRequest -UseBasicParsing 'http://localhost:8082/actuator/health').StatusCode -eq 200) { exit 0 } } catch { } Start-Sleep 2 } exit 1"
      if errorlevel 1 (
        echo [WARN] User Management Service did not become healthy within timeout
      ) else (
        echo [OK]    User Management Service is healthy
      )
      echo.
    )

    REM Wait for Auth Service after starting it
    if "!SERVICE!"=="auth-service" (
      echo [WAIT]  Waiting for Auth Service to become healthy...
      powershell -NoProfile -Command "foreach ($i in 1..60) { try { if ((Invoke-WebRequest -UseBasicParsing 'http://localhost:8083/actuator/health').StatusCode -eq 200) { exit 0 } } catch { } Start-Sleep 2 } exit 1"
      if errorlevel 1 (
        echo [WARN] Auth Service did not become healthy within timeout
      ) else (
        echo [OK]    Auth Service is healthy
      )
      echo.
    )

    REM Wait for RBAC Service after starting it
    if "!SERVICE!"=="rbac-service" (
      echo [WAIT]  Waiting for RBAC Service to become healthy...
      powershell -NoProfile -Command "foreach ($i in 1..60) { try { if ((Invoke-WebRequest -UseBasicParsing 'http://localhost:8084/actuator/health').StatusCode -eq 200) { exit 0 } } catch { } Start-Sleep 2 } exit 1"
      if errorlevel 1 (
        echo [WARN] RBAC Service did not become healthy within timeout
      ) else (
        echo [OK]    RBAC Service is healthy
      )
      echo.
    )

    REM Wait for Organization Service after starting it
    if "!SERVICE!"=="organization-service" (
      echo [WAIT]  Waiting for Organization Service to become healthy...
      powershell -NoProfile -Command "foreach ($i in 1..60) { try { if ((Invoke-WebRequest -UseBasicParsing 'http://localhost:8085/actuator/health').StatusCode -eq 200) { exit 0 } } catch { } Start-Sleep 2 } exit 1"
      if errorlevel 1 (
        echo [WARN] Organization Service did not become healthy within timeout
      ) else (
        echo [OK]    Organization Service is healthy
      )
      echo.
    )

    REM Wait for Communication Service after starting it
    if "!SERVICE!"=="communication-service" (
      echo [WAIT]  Waiting for Communication Service to become healthy...
      powershell -NoProfile -Command "foreach ($i in 1..60) { try { if ((Invoke-WebRequest -UseBasicParsing 'http://localhost:8094/actuator/health').StatusCode -eq 200) { exit 0 } } catch { } Start-Sleep 2 } exit 1"
      if errorlevel 1 (
        echo [WARN] Communication Service did not become healthy within timeout
      ) else (
        echo [OK]    Communication Service is healthy
      )
      echo.
    )
  ) else (
    echo [SKIP]   !SERVICE! (module directory not found)
  )
)

echo.
echo Core services launched in the background.
echo Check log files under %LOG_DIR% for startup progress.
echo.
echo Service URLs:
echo   - Eureka:      http://localhost:8761
echo   - API Gateway: http://localhost:8081
echo   - User Management Service: http://localhost:8082
echo   - Auth Service: http://localhost:8083
echo   - RBAC Service: http://localhost:8084
echo   - Organization Service: http://localhost:8085
echo   - Communication Service: http://localhost:8094
echo.
echo Press Ctrl+C to exit this script (services continue to run).

:END

endlocal
