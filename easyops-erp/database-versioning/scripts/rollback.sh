#!/bin/bash

# EasyOps ERP Database Rollback Script
# This script rolls back database changes using Liquibase

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
CHANGESET_ID=""
COUNT=""

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
        --changeset)
            CHANGESET_ID="$2"
            shift 2
            ;;
        --count)
            COUNT="$2"
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
            echo "  --changeset ID    Rollback to specific changeset ID"
            echo "  --count N         Rollback N changesets"
            echo "  --help           Show this help message"
            echo ""
            echo "Examples:"
            echo "  $0 --count 1                    # Rollback 1 changeset"
            echo "  $0 --changeset 001-initial-schema  # Rollback to specific changeset"
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            echo "Use --help for usage information"
            exit 1
            ;;
    esac
done

echo -e "${GREEN}🔄 Starting EasyOps ERP Database Rollback...${NC}"

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

# Confirm rollback operation
echo -e "${YELLOW}⚠️  WARNING: This will rollback database changes!${NC}"
if [[ -n "$CHANGESET_ID" ]]; then
    echo -e "${YELLOW}Rollback target: Changeset $CHANGESET_ID${NC}"
elif [[ -n "$COUNT" ]]; then
    echo -e "${YELLOW}Rollback count: $COUNT changeset(s)${NC}"
else
    echo -e "${RED}❌ You must specify either --changeset or --count${NC}"
    exit 1
fi

read -p "Are you sure you want to continue? (y/N): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo -e "${YELLOW}Rollback cancelled.${NC}"
    exit 0
fi

# Perform rollback
echo -e "${YELLOW}🔄 Performing database rollback...${NC}"
if [[ -n "$CHANGESET_ID" ]]; then
    if liquibase rollback "$CHANGESET_ID" --contexts="$CONTEXT"; then
        echo -e "${GREEN}✅ Database rollback to $CHANGESET_ID completed successfully!${NC}"
    else
        echo -e "${RED}❌ Database rollback failed!${NC}"
        exit 1
    fi
elif [[ -n "$COUNT" ]]; then
    if liquibase rollback-count "$COUNT" --contexts="$CONTEXT"; then
        echo -e "${GREEN}✅ Database rollback of $COUNT changeset(s) completed successfully!${NC}"
    else
        echo -e "${RED}❌ Database rollback failed!${NC}"
        exit 1
    fi
fi

# Validate the rollback
echo -e "${YELLOW}🔍 Validating database schema after rollback...${NC}"
if liquibase validate --contexts="$CONTEXT"; then
    echo -e "${GREEN}✅ Database schema validation passed after rollback!${NC}"
else
    echo -e "${RED}❌ Database schema validation failed after rollback!${NC}"
    exit 1
fi

echo -e "${GREEN}🎉 Database rollback process completed successfully!${NC}"
