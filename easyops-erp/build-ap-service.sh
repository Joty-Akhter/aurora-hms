#!/bin/bash

# Build script for AP Service with improved Maven dependency resolution
# This script provides multiple fallback strategies for Maven dependency issues

set -e

echo "=== EasyOps AP Service Build Script ==="
echo "This script will attempt to build the AP service with multiple fallback strategies"
echo ""

# Function to run Maven build with specific strategy
run_maven_build() {
    local strategy=$1
    local description=$2
    
    echo "=== Strategy: $description ==="
    echo "Running: $strategy"
    echo ""
    
    if eval "$strategy"; then
        echo "✅ Build successful with strategy: $description"
        return 0
    else
        echo "❌ Build failed with strategy: $description"
        return 1
    fi
}

# Strategy 1: Standard build with retry
strategy1() {
    mvn clean package -pl services/ap-service -am -DskipTests spring-boot:repackage \
        --batch-mode \
        --show-version \
        --errors \
        --fail-at-end \
        --no-transfer-progress
}

# Strategy 2: Build with offline mode disabled and update snapshots
strategy2() {
    mvn clean package -pl services/ap-service -am -DskipTests spring-boot:repackage \
        --batch-mode \
        --show-version \
        --errors \
        --fail-at-end \
        --no-transfer-progress \
        --update-snapshots
}

# Strategy 3: Build with dependency resolution in offline mode first, then online
strategy3() {
    echo "Attempting offline dependency resolution..."
    mvn dependency:resolve -pl services/ap-service --offline || true
    echo "Now attempting online build..."
    mvn clean package -pl services/ap-service -am -DskipTests spring-boot:repackage \
        --batch-mode \
        --show-version \
        --errors \
        --fail-at-end \
        --no-transfer-progress
}

# Strategy 4: Build with increased timeout and retry settings
strategy4() {
    export MAVEN_OPTS="-Xmx2048m -XX:MaxPermSize=512m -Dmaven.wagon.http.retryHandler.count=5 -Dmaven.wagon.http.retryHandler.requestSentEnabled=true -Dmaven.wagon.http.retryHandler.delay=2000"
    mvn clean package -pl services/ap-service -am -DskipTests spring-boot:repackage \
        --batch-mode \
        --show-version \
        --errors \
        --fail-at-end \
        --no-transfer-progress
}

# Strategy 5: Build with dependency:purge-local-repository and then build
strategy5() {
    echo "Purging local repository for problematic dependencies..."
    mvn dependency:purge-local-repository -DmanualInclude="org.assertj:assertj-core,org.junit.jupiter:junit-jupiter-params,org.mockito:mockito-core,net.bytebuddy:byte-buddy-agent" || true
    mvn clean package -pl services/ap-service -am -DskipTests spring-boot:repackage \
        --batch-mode \
        --show-version \
        --errors \
        --fail-at-end \
        --no-transfer-progress
}

echo "Starting build attempts..."
echo ""

# Try each strategy in order
if run_maven_build "strategy1" "Standard build with retry"; then
    exit 0
fi

if run_maven_build "strategy2" "Build with offline mode disabled and update snapshots"; then
    exit 0
fi

if run_maven_build "strategy3" "Offline dependency resolution followed by online build"; then
    exit 0
fi

if run_maven_build "strategy4" "Build with increased timeout and retry settings"; then
    exit 0
fi

if run_maven_build "strategy5" "Build with dependency purge and retry"; then
    exit 0
fi

echo ""
echo "❌ All build strategies failed!"
echo ""
echo "Manual troubleshooting steps:"
echo "1. Check your internet connection"
echo "2. Try running: mvn dependency:purge-local-repository"
echo "3. Clear Maven local repository: rm -rf ~/.m2/repository"
echo "4. Try using a VPN if you're behind a corporate firewall"
echo "5. Check if your organization blocks certain Maven repositories"
echo ""
echo "Alternative solutions:"
echo "- Use Docker with a different base image"
echo "- Configure Maven to use different repositories"
echo "- Use Maven offline mode if you have all dependencies cached"
echo ""
exit 1
