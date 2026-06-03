#!/bin/bash

# EasyOps ERP Database Validation Script
# This script validates the database schema using Liquibase

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Default values
DB_HOST="localhost"
DB_PORT="5433"
DB_NAME="easyops"
DB_USER="easyops"
DB_PASSWORD="easyops123"
CONTEXT="default"

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --host)
            DB_HOST="$2"
            shift 2
            ;;
        --port)
            DB_PORT="$2"
            shift 2
            ;;
        --database)
            DB_NAME="$2"
            shift 2
            ;;
        --user)
            DB_USER="$2"
            shift 2
            ;;
        --password)
            DB_PASSWORD="$2"
            shift 2
            ;;
        --context)
            CONTEXT="$2"
            shift 2
            ;;
        --help)
            echo "Usage: $0 [OPTIONS]"
            echo "Options:"
            echo "  --host HOST       Database host (default: localhost)"
            echo "  --port PORT       Database port (default: 5433)"
            echo "  --database NAME   Database name (default: easyops)"
            echo "  --user USER       Database user (default: easyops)"
            echo "  --password PASS   Database password (default: easyops123)"
            echo "  --context CONTEXT Liquibase context (default: default)"
            echo "  --help           Show this help message"
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            echo "Use --help for usage information"
            exit 1
            ;;
    esac
done

echo -e "${GREEN}🔍 Starting EasyOps ERP Database Validation...${NC}"

# Check if Liquibase is installed
if ! command -v liquibase &> /dev/null; then
    echo -e "${RED}❌ Liquibase is not installed. Please install Liquibase first.${NC}"
    echo "Visit: https://www.liquibase.org/download"
    exit 1
fi

# Update liquibase.properties with provided parameters
echo -e "${YELLOW}📝 Updating Liquibase configuration...${NC}"
cat > liquibase.properties << EOF
# Liquibase Configuration for EasyOps ERP
driver=org.postgresql.Driver
url=jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}
username=${DB_USER}
password=${DB_PASSWORD}
changeLogFile=changelog/master-changelog.xml
liquibaseSchemaName=liquibase
contexts=${CONTEXT}
outputDefaultSchema=true
outputDefaultCatalog=true
rollbackOnError=true
validateOnMigrate=true
logLevel=INFO
databaseChangeLogTableName=databasechangelog
databaseChangeLogLockTableName=databasechangeloglock
includeSystemClasspath=false
promptForNonLocalDatabase=false
runOnChange=true
runAlways=false
EOF

# Validate the database schema
echo -e "${YELLOW}🔍 Validating database schema...${NC}"
if liquibase validate --contexts="$CONTEXT"; then
    echo -e "${GREEN}✅ Database schema validation passed!${NC}"
else
    echo -e "${RED}❌ Database schema validation failed!${NC}"
    exit 1
fi

# Check for pending changes
echo -e "${YELLOW}🔍 Checking for pending changes...${NC}"
if liquibase status --contexts="$CONTEXT" | grep -q "No pending changes"; then
    echo -e "${GREEN}✅ No pending changes found. Database is up to date!${NC}"
else
    echo -e "${YELLOW}⚠️  Pending changes found. Run migrate.sh to apply them.${NC}"
    liquibase status --contexts="$CONTEXT"
fi

echo -e "${GREEN}🎉 Database validation completed successfully!${NC}"
