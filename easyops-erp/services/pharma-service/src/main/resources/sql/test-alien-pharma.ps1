# PowerShell script to test alien-pharma.sql
# This script validates and optionally tests the SQL script

param(
    [string]$DatabaseHost = "localhost",
    [int]$DatabasePort = 5432,
    [string]$DatabaseName = "easyops",
    [string]$DatabaseUser = "easyops",
    [string]$DatabasePassword = "easyops123",
    [switch]$ValidateOnly,
    [switch]$Execute
)

$ErrorActionPreference = "Stop"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Alien Pharma SQL Script Tester" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$scriptPath = Join-Path $PSScriptRoot "alien-pharma.sql"
$validatePath = Join-Path $PSScriptRoot "validate-alien-pharma.sql"

if (-not (Test-Path $scriptPath)) {
    Write-Host "ERROR: alien-pharma.sql not found at $scriptPath" -ForegroundColor Red
    exit 1
}

# Check if psql is available
$psqlPath = Get-Command psql -ErrorAction SilentlyContinue
if (-not $psqlPath) {
    Write-Host "WARNING: psql not found in PATH" -ForegroundColor Yellow
    Write-Host "You can test the script using:" -ForegroundColor Yellow
    Write-Host "  1. Docker: docker exec -i easyops-postgres psql -U $DatabaseUser -d $DatabaseName < $scriptPath" -ForegroundColor Yellow
    Write-Host "  2. pgAdmin or another database client" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Or install PostgreSQL client tools" -ForegroundColor Yellow
    exit 0
}

# Set PGPASSWORD environment variable
$env:PGPASSWORD = $DatabasePassword

try {
    if ($ValidateOnly) {
        Write-Host "Running validation script..." -ForegroundColor Green
        if (Test-Path $validatePath) {
            $validateOutput = & psql -h $DatabaseHost -p $DatabasePort -U $DatabaseUser -d $DatabaseName -f $validatePath 2>&1
            Write-Host $validateOutput
        } else {
            Write-Host "Validation script not found, skipping..." -ForegroundColor Yellow
        }
    }
    
    if ($Execute) {
        Write-Host ""
        Write-Host "WARNING: This will DELETE all organizations and related data!" -ForegroundColor Red
        Write-Host "Press Ctrl+C to cancel, or Enter to continue..." -ForegroundColor Yellow
        Read-Host
        
        Write-Host "Executing alien-pharma.sql..." -ForegroundColor Green
        $output = & psql -h $DatabaseHost -p $DatabasePort -U $DatabaseUser -d $DatabaseName -f $scriptPath 2>&1
        Write-Host $output
        
        if ($LASTEXITCODE -eq 0) {
            Write-Host ""
            Write-Host "✓ Script executed successfully!" -ForegroundColor Green
            Write-Host ""
            Write-Host "Verifying results..." -ForegroundColor Cyan
            
            # Verify organization was created
            $orgCheck = & psql -h $DatabaseHost -p $DatabasePort -U $DatabaseUser -d $DatabaseName -t -c "SELECT COUNT(*) FROM admin.organizations WHERE code = 'ALIEN_PHARMA';" 2>&1
            if ($orgCheck.Trim() -eq "1") {
                Write-Host "✓ Alien Pharma organization created" -ForegroundColor Green
            } else {
                Write-Host "✗ Alien Pharma organization not found" -ForegroundColor Red
            }
            
            # Verify products were created
            $productCheck = & psql -h $DatabaseHost -p $DatabasePort -U $DatabaseUser -d $DatabaseName -t -c "SELECT COUNT(*) FROM inventory.products WHERE organization_id = (SELECT id FROM admin.organizations WHERE code = 'ALIEN_PHARMA');" 2>&1
            Write-Host "✓ Products created: $($productCheck.Trim())" -ForegroundColor Green
            
            # Verify admin user has roles
            $roleCheck = & psql -h $DatabaseHost -p $DatabasePort -U $DatabaseUser -d $DatabaseName -t -c "SELECT COUNT(*) FROM rbac.user_roles ur JOIN users.users u ON u.id = ur.user_id WHERE u.username = 'admin';" 2>&1
            Write-Host "✓ Admin user roles: $($roleCheck.Trim())" -ForegroundColor Green
        } else {
            Write-Host ""
            Write-Host "✗ Script execution failed!" -ForegroundColor Red
            exit 1
        }
    } else {
        Write-Host ""
        Write-Host "To execute the script, use: -Execute" -ForegroundColor Yellow
        Write-Host "Example: .\test-alien-pharma.ps1 -ValidateOnly -Execute" -ForegroundColor Yellow
    }
} finally {
    # Clear password from environment
    Remove-Item Env:\PGPASSWORD -ErrorAction SilentlyContinue
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
