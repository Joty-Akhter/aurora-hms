# Maven Dependency Resolution Troubleshooting Guide

## Problem Description
You're encountering Maven dependency resolution issues during Docker build with errors like:
```
Could not transfer artifact org.assertj:assertj-core:jar:3.25.3 from/to central
Premature end of Content-Length delimited message body
```

## Root Causes
1. **Network connectivity issues** - Intermittent connection to Maven Central
2. **Corporate firewall** - Blocking access to Maven repositories
3. **DNS resolution problems** - Unable to resolve repository URLs
4. **Timeout issues** - Downloads timing out before completion
5. **Corrupted local repository** - Cached dependencies are corrupted

## Solutions Implemented

### 1. Enhanced Maven Configuration
- **Maven Settings**: Created `maven-settings.xml` with multiple repository mirrors
- **Retry Configuration**: Added retry handlers with delays
- **Memory Settings**: Increased heap size and permgen space
- **Repository Configuration**: Added Spring repositories as fallback

### 2. Improved Dockerfile
- **Maven Wrapper**: Added Maven wrapper for consistent builds
- **Retry Logic**: Multiple build attempts with different strategies
- **Better Error Handling**: Comprehensive error messages and fallbacks
- **Network Configuration**: Enhanced Maven network settings

### 3. Build Scripts
- **PowerShell Script**: `build-ap-service.ps1` with multiple strategies
- **Bash Script**: `build-ap-service.sh` for Unix systems
- **Docker Compose Override**: `docker-compose.override.yml` with volume mounts

## How to Use the Solutions

### Option 1: Use the Enhanced Dockerfile (Recommended)
```bash
docker compose up -d --build ap-service
```

### Option 2: Use the Build Scripts
```powershell
# PowerShell (Windows)
.\build-ap-service.ps1

# Bash (Linux/Mac)
./build-ap-service.sh
```

### Option 3: Manual Maven Build
```bash
# Clear local repository
mvn dependency:purge-local-repository

# Build with retry settings
mvn clean package -pl services/ap-service -am -DskipTests spring-boot:repackage \
    --batch-mode \
    --show-version \
    --errors \
    --fail-at-end \
    --no-transfer-progress
```

## Advanced Troubleshooting

### If All Solutions Fail

1. **Check Network Connectivity**
   ```bash
   ping repo.maven.apache.org
   nslookup repo.maven.apache.org
   ```

2. **Clear Maven Local Repository**
   ```bash
   # Windows
   Remove-Item -Recurse -Force $env:USERPROFILE\.m2\repository
   
   # Linux/Mac
   rm -rf ~/.m2/repository
   ```

3. **Use Different Maven Repository**
   - Edit `maven-settings.xml`
   - Change repository URLs to alternatives like:
     - `https://repo1.maven.org/maven2`
     - `https://maven.aliyun.com/repository/central`

4. **Corporate Network Solutions**
   - Configure Maven proxy settings
   - Use internal Maven repository if available
   - Contact IT for firewall exceptions

5. **Docker Network Issues**
   ```bash
   # Rebuild Docker network
   docker network prune
   docker compose down
   docker compose up -d --build
   ```

## Configuration Files Created

### maven-settings.xml
- Multiple repository mirrors
- Retry configuration
- Timeout settings
- Checksum validation

### Dockerfile.dev (Enhanced)
- Maven wrapper integration
- Multiple build strategies
- Better error handling
- Network configuration

### Build Scripts
- PowerShell script for Windows
- Bash script for Unix systems
- Multiple fallback strategies
- Comprehensive error reporting

## Monitoring and Debugging

### Enable Verbose Maven Output
```bash
mvn clean package -X -pl services/ap-service -am -DskipTests spring-boot:repackage
```

### Check Maven Dependencies
```bash
mvn dependency:tree -pl services/ap-service
mvn dependency:resolve -pl services/ap-service
```

### Docker Build Debug
```bash
docker build --no-cache -f services/ap-service/Dockerfile.dev .
```

## Prevention Strategies

1. **Use Maven Wrapper**: Ensures consistent Maven version
2. **Repository Mirrors**: Multiple fallback repositories
3. **Dependency Caching**: Docker volume mounts for Maven cache
4. **Network Resilience**: Retry mechanisms and timeouts
5. **Regular Updates**: Keep dependencies up to date

## Contact and Support

If you continue to experience issues:
1. Check the build logs for specific error messages
2. Verify network connectivity to Maven repositories
3. Consider using a different base image or Maven version
4. Contact your system administrator for network-related issues

## Additional Resources

- [Maven Troubleshooting Guide](https://maven.apache.org/guides/mini/guide-troubleshooting.html)
- [Docker Build Best Practices](https://docs.docker.com/develop/dev-best-practices/)
- [Spring Boot Maven Plugin](https://docs.spring.io/spring-boot/docs/current/maven-plugin/reference/htmlsingle/)
