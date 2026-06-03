@echo off
REM EasyOps ERP - Start Hospital Components (Windows)
REM
REM NOTE:
REM The PowerShell implementation (`start-hospital-components.ps1`) is the canonical launcher.
REM This .bat file delegates to it for consistency.

setlocal
set "SCRIPT_DIR=%~dp0"
set "PS1=%SCRIPT_DIR%start-hospital-components.ps1"

if exist "%PS1%" (
  powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%PS1%"
  exit /b %errorlevel%
)

echo [ERROR] Missing launcher: "%PS1%"
exit /b 1
