@echo off
REM EasyOps ERP - Complete Deployment Script (Batch Version)

echo ========================================
echo   EasyOps ERP - Complete Deployment
echo ========================================
echo.

echo Stopping existing containers...
docker-compose down
echo.

echo Building and starting all services...
echo This will take several minutes...
echo.

docker-compose up -d --build

echo.
echo ========================================
echo Waiting for services to start...
echo ========================================
echo This may take 2-3 minutes...
echo.

timeout /t 120 /nobreak

echo.
echo ========================================
echo Deployment Complete!
echo ========================================
echo.
echo Access the application:
echo   Frontend: http://localhost:3000
echo   Login: admin@easyops.com
echo   Password: Admin123!
echo.
echo Service Dashboards:
echo   Eureka: http://localhost:8761
echo   Grafana: http://localhost:3001
echo.
echo Check service status:
echo   docker-compose ps
echo.
echo View logs:
echo   docker-compose logs -f
echo.
pause

