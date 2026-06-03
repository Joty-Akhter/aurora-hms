# Running Liquibase Migrations in Docker

This guide explains how to run Liquibase database migrations using Docker for the EasyOps ERP system.

---

## 🚀 Quick Start

### Option 1: Using Main Docker Compose (Recommended)

The main `docker-compose.yml` includes a Liquibase service that runs automatically:

```bash
# Navigate to project root
cd easyops-erp

# Start database and run migrations
docker-compose up -d postgres liquibase

# Check migration status
docker logs easyops-liquibase
```

The Liquibase container will:
1. Wait for PostgreSQL to be healthy
2. Run all pending migrations
3. Exit successfully

---

### Option 2: Using Database Versioning Docker Compose

For isolated testing of migrations:

```bash
# Navigate to database-versioning directory
cd easyops-erp/database-versioning

# Start PostgreSQL and run migrations
docker-compose -f docker-compose.yml up -d postgres

# Wait for database to be ready, then run Liquibase manually
docker run --rm \
  --network database-versioning_liquibase-network \
  -v "$(pwd)/changelog:/liquibase/changelog" \
  -v "$(pwd)/liquibase.properties:/liquibase/liquibase.properties" \
  liquibase/liquibase:4.24 \
  --defaultsFile=/liquibase/liquibase.properties \
  update
```

---

## 📋 Detailed Methods

### Method 1: Standalone Docker Run

Run Liquibase as a one-time container:

```bash
cd easyops-erp/database-versioning

docker run --rm \
  --network easyops_easyops-network \
  -v "$(pwd)/changelog:/liquibase/changelog" \
  -v "$(pwd)/liquibase.properties:/liquibase/liquibase.properties" \
  liquibase/liquibase:4.24 \
  --defaultsFile=/liquibase/liquibase.properties \
  --url="jdbc:postgresql://postgres:5432/easyops" \
  --username=easyops \
  --password=easyops123 \
  update
```

**Parameters:**
- `--rm`: Remove container after execution
- `--network`: Connect to Docker network (use `easyops_easyops-network` if using main docker-compose)
- `-v`: Mount changelog directory and properties file
- `liquibase/liquibase:4.24`: Official Liquibase Docker image
- `update`: Run pending migrations

---

### Method 2: Using Docker Compose Service

Add to your `docker-compose.yml`:

```yaml
services:
  liquibase:
    image: liquibase/liquibase:4.24
    container_name: easyops-liquibase
    volumes:
      - ./database-versioning/changelog:/liquibase/changelog
      - ./database-versioning/liquibase.properties:/liquibase/liquibase.properties
    environment:
      LIQUIBASE_COMMAND_URL: jdbc:postgresql://postgres:5432/easyops
      LIQUIBASE_COMMAND_USERNAME: easyops
      LIQUIBASE_COMMAND_PASSWORD: easyops123
      LIQUIBASE_COMMAND_CHANGELOG_FILE: changelog/master-changelog.xml
    command: ["sh", "-c", "sleep 10 && liquibase --defaultsFile=/liquibase/liquibase.properties update"]
    depends_on:
      postgres:
        condition: service_healthy
    networks:
      - easyops-network
    restart: "no"  # Run once and exit
```

Then run:
```bash
docker-compose up liquibase
```

---

### Method 3: Interactive Shell (For Testing)

Get a shell inside the Liquibase container:

```bash
docker run --rm -it \
  --network easyops_easyops-network \
  -v "$(pwd)/database-versioning/changelog:/liquibase/changelog" \
  -v "$(pwd)/database-versioning/liquibase.properties:/liquibase/liquibase.properties" \
  liquibase/liquibase:4.24 \
  /bin/bash
```

Inside the container:
```bash
# Run migrations
liquibase --defaultsFile=/liquibase/liquibase.properties update

# Check status
liquibase --defaultsFile=/liquibase/liquibase.properties status

# Validate
liquibase --defaultsFile=/liquibase/liquibase.properties validate

# Rollback (if needed)
liquibase --defaultsFile=/liquibase/liquibase.properties rollback-count 1
```

---

## 🔧 Common Commands

### Update (Run Migrations)
```bash
docker run --rm \
  --network easyops_easyops-network \
  -v "$(pwd)/database-versioning/changelog:/liquibase/changelog" \
  -v "$(pwd)/database-versioning/liquibase.properties:/liquibase/liquibase.properties" \
  liquibase/liquibase:4.24 \
  --defaultsFile=/liquibase/liquibase.properties \
  update
```

### Check Status
```bash
docker run --rm \
  --network easyops_easyops-network \
  -v "$(pwd)/database-versioning/changelog:/liquibase/changelog" \
  -v "$(pwd)/database-versioning/liquibase.properties:/liquibase/liquibase.properties" \
  liquibase/liquibase:4.24 \
  --defaultsFile=/liquibase/liquibase.properties \
  status
```

### Validate Changesets
```bash
docker run --rm \
  --network easyops_easyops-network \
  -v "$(pwd)/database-versioning/changelog:/liquibase/changelog" \
  -v "$(pwd)/database-versioning/liquibase.properties:/liquibase/liquibase.properties" \
  liquibase/liquibase:4.24 \
  --defaultsFile=/liquibase/liquibase.properties \
  validate
```

### Rollback Last Change
```bash
docker run --rm \
  --network easyops_easyops-network \
  -v "$(pwd)/database-versioning/changelog:/liquibase/changelog" \
  -v "$(pwd)/database-versioning/liquibase.properties:/liquibase/liquibase.properties" \
  liquibase/liquibase:4.24 \
  --defaultsFile=/liquibase/liquibase.properties \
  rollback-count 1
```

### Rollback to Tag
```bash
docker run --rm \
  --network easyops_easyops-network \
  -v "$(pwd)/database-versioning/changelog:/liquibase/changelog" \
  -v "$(pwd)/database-versioning/liquibase.properties:/liquibase/liquibase.properties" \
  liquibase/liquibase:4.24 \
  --defaultsFile=/liquibase/liquibase.properties \
  rollback v1.0.0
```

### Generate SQL (Dry Run)
```bash
docker run --rm \
  --network easyops_easyops-network \
  -v "$(pwd)/database-versioning/changelog:/liquibase/changelog" \
  -v "$(pwd)/database-versioning/liquibase.properties:/liquibase/liquibase.properties" \
  liquibase/liquibase:4.24 \
  --defaultsFile=/liquibase/liquibase.properties \
  update-sql > migration.sql
```

---

## 🌍 Environment-Specific Migrations

### Using Contexts

Run migrations for specific contexts (dev, test, prod):

```bash
docker run --rm \
  --network easyops_easyops-network \
  -v "$(pwd)/database-versioning/changelog:/liquibase/changelog" \
  -v "$(pwd)/database-versioning/liquibase.properties:/liquibase/liquibase.properties" \
  liquibase/liquibase:4.24 \
  --defaultsFile=/liquibase/liquibase.properties \
  --contexts=dev \
  update
```

### Different Database Connections

Override connection parameters:

```bash
docker run --rm \
  --network easyops_easyops-network \
  -v "$(pwd)/database-versioning/changelog:/liquibase/changelog" \
  liquibase/liquibase:4.24 \
  --url="jdbc:postgresql://postgres:5432/easyops_test" \
  --username=easyops \
  --password=easyops123 \
  --changeLogFile=changelog/master-changelog.xml \
  update
```

---

## 🔍 Troubleshooting

### Issue: Container can't connect to database

**Solution:** Ensure containers are on the same network:
```bash
# Check network
docker network ls

# Inspect network
docker network inspect easyops_easyops-network

# Connect container to network
docker network connect easyops_easyops-network <container-name>
```

### Issue: Volume mount not working

**Solution:** Use absolute paths:
```bash
# Get absolute path
pwd  # Linux/Mac
cd  # Windows PowerShell

# Use absolute path in volume mount
-v "/absolute/path/to/database-versioning/changelog:/liquibase/changelog"
```

### Issue: Permission denied

**Solution:** Check file permissions:
```bash
# Linux/Mac
chmod +x database-versioning/scripts/*.sh
ls -la database-versioning/changelog

# Windows
# Ensure files are not read-only
```

### Issue: Changesets not found

**Solution:** Verify changelog path:
```bash
# Check if master-changelog.xml exists
ls -la database-versioning/changelog/master-changelog.xml

# Verify volume mount
docker run --rm \
  -v "$(pwd)/database-versioning/changelog:/liquibase/changelog" \
  liquibase/liquibase:4.24 \
  ls -la /liquibase/changelog
```

---

## 📝 Example: Running New RBAC Migrations

To apply the new RBAC implementation (including business roles):

```bash
# 1. Ensure database is running
cd easyops-erp
docker-compose up -d postgres

# 2. Wait for database to be ready
sleep 10

# 3. Run migrations
cd database-versioning
docker run --rm \
  --network easyops_easyops-network \
  -v "$(pwd)/changelog:/liquibase/changelog" \
  -v "$(pwd)/liquibase.properties:/liquibase/liquibase.properties" \
  liquibase/liquibase:4.24 \
  --defaultsFile=/liquibase/liquibase.properties \
  update

# 4. Verify migrations
docker run --rm \
  --network easyops_easyops-network \
  -v "$(pwd)/changelog:/liquibase/changelog" \
  -v "$(pwd)/liquibase.properties:/liquibase/liquibase.properties" \
  liquibase/liquibase:4.24 \
  --defaultsFile=/liquibase/liquibase.properties \
  status
```

---

## 🎯 Best Practices

1. **Always backup before migrations:**
   ```bash
   docker exec easyops-postgres pg_dump -U easyops easyops > backup_$(date +%Y%m%d).sql
   ```

2. **Test migrations in test environment first:**
   ```bash
   docker-compose -f database-versioning/docker-compose.test.yml up -d
   ```

3. **Use dry-run (update-sql) to preview changes:**
   ```bash
   docker run --rm ... liquibase update-sql > preview.sql
   ```

4. **Validate before running:**
   ```bash
   docker run --rm ... liquibase validate
   ```

5. **Check status after migration:**
   ```bash
   docker run --rm ... liquibase status
   ```

---

## 📚 Additional Resources

- [Liquibase Docker Hub](https://hub.docker.com/r/liquibase/liquibase)
- [Liquibase Documentation](https://docs.liquibase.com/)
- [Database Versioning README](../easyops-erp/database-versioning/README.md)
- [Migration Guide](../easyops-erp/database-versioning/docs/migration-guide.md)

---

**Last Updated:** 2024  
**Liquibase Version:** 4.24
