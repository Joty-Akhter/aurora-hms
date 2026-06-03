# Build script for AP Service with improved Maven dependency resolution
# This script provides multiple fallback strategies for Maven dependency issues

param(
    [switch]$Verbose
)

function Write-ColorOutput($ForegroundColor) {
    $fc = $host.UI.RawUI.ForegroundColor
    $host.UI.RawUI.ForegroundColor = $ForegroundColor
    if ($args) {
        Write-Output $args
    } else {
        $input | Write-Output
    }
    $host.UI.RawUI.ForegroundColor = $fc
}

function Run-MavenBuild {
    param(
        [string]$Strategy,
        [string]$Description,
        [scriptblock]$Command
    )
    
    Write-ColorOutput Green "=== Strategy: $Description ==="
    Write-ColorOutput Yellow "Running: $Strategy"
    Write-Output ""
    
    try {
        & $Command
        if ($LASTEXITCODE -eq 0) {
            Write-ColorOutput Green "✅ Build successful with strategy: $Description"
            return $true
        } else {
            Write-ColorOutput Red "❌ Build failed with strategy: $Description"
            return $false
        }
    } catch {
        Write-ColorOutput Red "❌ Build failed with strategy: $Description - Error: $($_.Exception.Message)"
        return $false
    }
}

Write-ColorOutput Cyan "=== EasyOps AP Service Build Script ==="
Write-ColorOutput Cyan "This script will attempt to build the AP service with multiple fallback strategies"
Write-Output ""

# Strategy 1: Standard build with retry
$Strategy1 = {
    mvn clean package -pl services/ap-service -am -DskipTests spring-boot:repackage `
        --batch-mode `
        --show-version `
        --errors `
        --fail-at-end `
        --no-transfer-progress
}

# Strategy 2: Build with offline mode disabled and update snapshots
$Strategy2 = {
    mvn clean package -pl services/ap-service -am -DskipTests spring-boot:repackage `
        --batch-mode `
        --show-version `
        --errors `
        --fail-at-end `
        --no-transfer-progress `
        --update-snapshots
}

# Strategy 3: Build with dependency resolution in offline mode first, then online
$Strategy3 = {
    Write-ColorOutput Yellow "Attempting offline dependency resolution..."
    mvn dependency:resolve -pl services/ap-service --offline
    Write-ColorOutput Yellow "Now attempting online build..."
    mvn clean package -pl services/ap-service -am -DskipTests spring-boot:repackage `
        --batch-mode `
        --show-version `
        --errors `
        --fail-at-end `
        --no-transfer-progress
}

# Strategy 4: Build with increased timeout and retry settings
$Strategy4 = {
    $env:MAVEN_OPTS = "-Xmx2048m -XX:MaxPermSize=512m -Dmaven.wagon.http.retryHandler.count=5 -Dmaven.wagon.http.retryHandler.requestSentEnabled=true -Dmaven.wagon.http.retryHandler.delay=2000"
    mvn clean package -pl services/ap-service -am -DskipTests spring-boot:repackage `
        --batch-mode `
        --show-version `
        --errors `
        --fail-at-end `
        --no-transfer-progress
}

# Strategy 5: Build with dependency:purge-local-repository and then build
$Strategy5 = {
    Write-ColorOutput Yellow "Purging local repository for problematic dependencies..."
    mvn dependency:purge-local-repository -DmanualInclude="org.assertj:assertj-core,org.junit.jupiter:junit-jupiter-params,org.mockito:mockito-core,net.bytebuddy:byte-buddy-agent"
    mvn clean package -pl services/ap-service -am -DskipTests spring-boot:repackage `
        --batch-mode `
        --show-version `
        --errors `
        --fail-at-end `
        --no-transfer-progress
}

Write-ColorOutput Cyan "Starting build attempts..."
Write-Output ""

# Try each strategy in order
if (Run-MavenBuild "strategy1" "Standard build with retry" $Strategy1) {
    exit 0
}

if (Run-MavenBuild "strategy2" "Build with offline mode disabled and update snapshots" $Strategy2) {
    exit 0
}

if (Run-MavenBuild "strategy3" "Offline dependency resolution followed by online build" $Strategy3) {
    exit 0
}

if (Run-MavenBuild "strategy4" "Build with increased timeout and retry settings" $Strategy4) {
    exit 0
}

if (Run-MavenBuild "strategy5" "Build with dependency purge and retry" $Strategy5) {
    exit 0
}

Write-Output ""
Write-ColorOutput Red "❌ All build strategies failed!"
Write-Output ""
Write-ColorOutput Yellow "Manual troubleshooting steps:"
Write-Output "1. Check your internet connection"
Write-Output "2. Try running: mvn dependency:purge-local-repository"
Write-Output "3. Clear Maven local repository: Remove-Item -Recurse -Force ~/.m2/repository"
Write-Output "4. Try using a VPN if you're behind a corporate firewall"
Write-Output "5. Check if your organization blocks certain Maven repositories"
Write-Output ""
Write-ColorOutput Yellow "Alternative solutions:"
Write-Output "- Use Docker with a different base image"
Write-Output "- Configure Maven to use different repositories"
Write-Output "- Use Maven offline mode if you have all dependencies cached"
Write-Output ""
exit 1
