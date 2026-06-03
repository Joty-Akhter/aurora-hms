# Cleanup script for legacy SQL files (hms.sql, lab.sql)
# Removes: CREATE DATABASE, ALTER DATABASE, CREATE USER, ALTER ROLE, file paths
# Adds: Header with navigation and usage notes
# Run from: requirements/module-hospital/
# NOTE: Run only on fresh SQL Server exports. Current hms.sql and lab.sql are already cleaned.

$ErrorActionPreference = "Stop"
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $scriptDir

$hmsHeader = @"
/*
================================================================================
LEGACY REFERENCE: WebHospitalMDB (HMS) - Schema Only
================================================================================
Database: WebHospitalMDB (SQL Server)
Purpose: Reference for requirements gap analysis. Use to extract table structures,
         functions, and business logic. NOT for direct migration - use for
         understanding legacy capabilities only.
See: LEGACY-SQL-REVIEW-AND-REQUIREMENTS-PLAN.md

Removed: CREATE DATABASE, ALTER DATABASE, CREATE USER, ALTER ROLE, file paths
================================================================================
*/

USE [WebHospitalMDB]
GO

"@

$labHeader = @"
/*
================================================================================
LEGACY REFERENCE: LabDb - Schema Only
================================================================================
Database: LabDb (SQL Server)
Purpose: Reference for lab/diagnostic requirements. Use to extract table structures,
         views, and workflows. NOT for direct migration.
See: LEGACY-SQL-REVIEW-AND-REQUIREMENTS-PLAN.md

Removed: CREATE DATABASE, ALTER DATABASE, CREATE USER, ALTER ROLE, file paths
================================================================================
*/

USE [LabDb]
GO

"@

# --- Clean hms.sql ---
Write-Host "Processing hms.sql..."
$hmsPath = Join-Path $scriptDir "hms.sql"
$hmsBackup = Join-Path $scriptDir "hms.sql.bak"
$hmsClean = Join-Path $scriptDir "hms-clean.sql"

Copy-Item $hmsPath $hmsBackup -Force

$lines = Get-Content $hmsPath
# Remove lines 1-91 (database + user setup), keep from line 92
$keepLines = @($lines[91..($lines.Count-1)])
# Remove trailing USE [master] and ALTER DATABASE block
$cut = $keepLines.Count
for ($i = $keepLines.Count - 1; $i -ge 0; $i--) {
    if ($keepLines[$i] -match "^\s*USE\s+\[master\]\s*$") { $cut = $i; break }
}
if ($cut -lt $keepLines.Count) { $keepLines = $keepLines[0..($cut-1)] }
$result = $hmsHeader + ($keepLines -join "`n")
Set-Content $hmsClean -Value $result -NoNewline
Write-Host "  Created: hms-clean.sql"

# --- Clean lab.sql ---
Write-Host "Processing lab.sql..."
$labPath = Join-Path $scriptDir "lab.sql"
$labBackup = Join-Path $scriptDir "lab.sql.bak"
$labClean = Join-Path $scriptDir "lab-clean.sql"

Copy-Item $labPath $labBackup -Force

$lines = Get-Content $labPath
$keepLines = @($lines[104..($lines.Count-1)])
# Remove trailing USE [master] and ALTER DATABASE block
$cut = $keepLines.Count
for ($i = $keepLines.Count - 1; $i -ge 0; $i--) {
    if ($keepLines[$i] -match "^\s*USE\s+\[master\]\s*$") { $cut = $i; break }
}
if ($cut -lt $keepLines.Count) { $keepLines = $keepLines[0..($cut-1)] }
$result = $labHeader + ($keepLines -join "`n")
Set-Content $labClean -Value $result -NoNewline
Write-Host "  Created: lab-clean.sql"

Write-Host ""
Write-Host "Done. Clean files: hms-clean.sql, lab-clean.sql"
Write-Host "Backups: hms.sql.bak, lab.sql.bak"
Write-Host "To replace originals: Move-Item hms-clean.sql hms.sql -Force; Move-Item lab-clean.sql lab.sql -Force"
