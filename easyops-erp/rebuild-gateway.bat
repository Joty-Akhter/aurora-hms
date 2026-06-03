@echo off
REM Rebuild API Gateway Script for Windows Command Prompt

echo ========================================
echo Rebuilding API Gateway Service
echo ========================================

REM Stop the container
echo.
echo Stopping API Gateway...
docker stop easyops-api-gateway 2>nul

REM Remove the container
echo Removing old container...
docker rm easyops-api-gateway 2>nul

REM Remove the old image to force rebuild
echo Removing old image...
docker rmi easyops-erp-api-gateway 2>nul

REM Rebuild and start
echo.
echo Rebuilding API Gateway (this will take 2-3 minutes)...
docker-compose up -d --build api-gateway

echo.
echo ========================================
echo Waiting for API Gateway to start...
echo ========================================
timeout /t 90 /nobreak

REM Check status
echo.
echo Checking API Gateway status...
docker ps --filter "name=easyops-api-gateway"

echo.
echo ========================================
echo Rebuild Complete!
echo ========================================
echo.
echo You can now test the login endpoint in auth.rest
echo POST http://localhost:8081/api/auth/login

pause

