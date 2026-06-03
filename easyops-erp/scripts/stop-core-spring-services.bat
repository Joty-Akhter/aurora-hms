@echo off
REM EasyOps ERP - Stop Core Spring Boot Services (Windows Wrapper)
REM Delegates to PowerShell implementation for robust process management.

setlocal
set "SCRIPT_DIR=%~dp0"
set "PS1=%SCRIPT_DIR%stop-core-spring-services.ps1"

if exist "%PS1%" (
  powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%PS1%"
  exit /b %errorlevel%
)

echo [ERROR] PowerShell script not found: %PS1%
exit /b 1

