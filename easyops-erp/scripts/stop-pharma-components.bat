@echo off
REM EasyOps ERP - Stop Pharma Components (Windows)
REM 1. Tries PowerShell implementation (port/window/command-line + PID files).
REM 2. If PowerShell missing or fails, falls back to PID files + taskkill (e.g. when started via .sh).

setlocal enabledelayedexpansion
set "SCRIPT_DIR=%~dp0"
set "ROOT_DIR=%SCRIPT_DIR%.."
set "PID_DIR=%ROOT_DIR%\logs\local-services\pids"
set "PS1=%SCRIPT_DIR%stop-pharma-components.ps1"

REM Prefer PowerShell if available and script exists
if exist "%PS1%" (
  powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%PS1%" 2>nul
  if %errorlevel% equ 0 exit /b 0
)

REM Fallback: stop using PID files (same layout as .sh; no dependency on pgrep/lsof/PowerShell)
echo [STOP] Stopping EasyOps Pharma components (PID files)...
set STOPPED=0
if exist "%PID_DIR%" (
  for %%s in (organization-service inventory-service accounting-service hr-service pharma-service) do (
    set "PF=%PID_DIR%\%%s.pid"
    if exist "!PF!" (
      set /p PID=<"!PF!"
      if defined PID (
        taskkill /PID !PID! /F >nul 2>&1
        if !errorlevel! equ 0 (
          echo   - Stopped %%s ^(PID: !PID!^)
          set /a STOPPED+=1
        )
        del /f /q "!PF!" >nul 2>&1
      )
    )
  )
)
echo Done. Stopped !STOPPED! pharma-related process(es).
exit /b 0
