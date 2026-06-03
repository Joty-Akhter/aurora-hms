#!/bin/bash

# EasyOps ERP Generate Changelog Script
# This script generates a Liquibase changelog from an existing database

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
OUTPUT_FILE="generated-changelog.sql"
INCLUDE_SCHEMA=""

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
        --output)
            OUTPUT_FILE="$2"
            shift 2
            ;;
        --include-schema)
            INCLUDE_SCHEMA="$2"
            shift 2
            ;;
        --help)
            echo "Usage: $0 [OPTIONS]"
            echo "Options:"
            echo "  --host HOST           Database host (default: localhost)"
            echo "  --port PORT           Database port (default: 5433)"
            echo "  --database NAME       Database name (default: easyops)"
            echo "  --user USER           Database user (default: easyops)"
            echo "  --password PASS       Database password (default: easyops123)"
            echo "  --output FILE         Output file name (default: generated-changelog.sql)"
            echo "  --include-schema SCHEMA Specific schema to include (optional)"
            echo "  --help               Show this help message"
            echo ""
            echo "Examples:"
            echo "  $0 --output initial-schema.sql"
            echo "  $0 --include-schema accounting"
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            echo "Use --help for usage information"
            exit 1
            ;;
    esac
done

echo -e "${GREEN}📝 Starting EasyOps ERP Changelog Generation...${NC}"

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
contexts=default
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

# Generate changelog
echo -e "${YELLOW}📝 Generating changelog from existing database...${NC}"

GENERATE_CMD="liquibase generateChangeLog --changeLogFile=$OUTPUT_FILE"
if [[ -n "$INCLUDE_SCHEMA" ]]; then
    GENERATE_CMD="$GENERATE_CMD --includeSchema=$INCLUDE_SCHEMA"
fi

if $GENERATE_CMD; then
    echo -e "${GREEN}✅ Changelog generated successfully: $OUTPUT_FILE${NC}"
else
    echo -e "${RED}❌ Changelog generation failed!${NC}"
    exit 1
fi

# Display file information
if [[ -f "$OUTPUT_FILE" ]]; then
    FILE_SIZE=$(wc -l < "$OUTPUT_FILE")
    echo -e "${GREEN}📊 Generated file contains $FILE_SIZE lines${NC}"
    echo -e "${YELLOW}📁 Output file: $OUTPUT_FILE${NC}"
    
    # Show first few lines as preview
    echo -e "${YELLOW}📖 Preview of generated changelog:${NC}"
    echo "----------------------------------------"
    head -20 "$OUTPUT_FILE"
    echo "----------------------------------------"
fi

echo -e "${GREEN}🎉 Changelog generation completed successfully!${NC}"
echo -e "${YELLOW}💡 Tip: Review the generated changelog before using it in production${NC}"
