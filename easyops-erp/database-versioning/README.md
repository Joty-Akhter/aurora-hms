# EasyOps ERP Database Versioning

This project manages database schema changes and migrations for the EasyOps ERP system using Liquibase.

## Project Structure

```
database-versioning/
├── README.md                           # This file
├── liquibase.properties                # Liquibase configuration
├── docker-compose.yml                  # Database services for testing
├── changelog/                          # Database change scripts
│   ├── master-changelog.xml            # Main changelog file
│   ├── phase-0/                        # Phase 0: Core System
│   │   ├── 001-initial-schema.sql
│   │   ├── 002-auth-schema.sql
│   │   ├── 003-rbac-schema.sql
│   │   ├── 004-notifications.sql
│   │   └── 005-integration.sql
│   ├── phase-1/                        # Phase 1: Accounting
│   │   ├── 101-accounting-schema.sql
│   │   ├── 102-ar-ap-bank.sql
│   │   └── 103-fixes-and-updates.sql
│   ├── phase-2/                        # Phase 2: Sales
│   │   ├── 201-sales-schema.sql
│   │   └── 202-sales-updates.sql
│   └── data/                           # Reference and test data
│       ├── 001-default-roles.sql
│       ├── 002-default-permissions.sql
│       ├── 003-system-config.sql
│       ├── 004-test-data.sql
│       ├── 005-sales-test-data.sql
│       └── 006-accounting-test-data.sql
├── scripts/                            # Utility scripts
│   ├── migrate.sh                      # Run migrations
│   ├── rollback.sh                     # Rollback changes
│   ├── validate.sh                     # Validate schema
│   └── generate-changelog.sh           # Generate changelog from existing DB
└── docs/                               # Documentation
    ├── migration-guide.md
    ├── rollback-procedures.md
    └── test-data-guide.md
```

## Quick Start

### Prerequisites
- Docker and Docker Compose
- Java 11+ (for Liquibase CLI) - Optional (Docker version available)
- PostgreSQL client tools

### Option 1: Test Database Versioning Standalone

1. **Start test environment**:
   ```bash
   # Linux/Mac
   ./scripts/test-database-versioning.sh
   
   # Windows
   scripts\test-database-versioning.bat
   ```

2. **Access test database**:
   - Adminer: http://localhost:8081
   - pgAdmin: http://localhost:5050 (admin@easyops.com / admin123)

### Option 2: Full Application Stack with Migrations

1. **Start entire development environment**:
   ```bash
   # Linux/Mac
   cd ..
   ./scripts/start-dev-with-migrations.sh
   
   # Windows
   cd ..
   scripts\start-dev-with-migrations.bat
   ```

2. **Access services**:
   - Frontend: http://localhost:3000
   - API Gateway: http://localhost:8081
   - Database Admin: http://localhost:8080

### Option 3: Manual Migration Process

1. **Start the database**:
   ```bash
   docker-compose up -d postgres
   ```

2. **Run migrations**:
   ```bash
   ./scripts/migrate.sh
   ```

3. **Validate schema**:
   ```bash
   ./scripts/validate.sh
   ```

### Option 4: Run Liquibase without Docker

Use this when Postgres is already running (e.g. Docker Postgres, local install, or remote) and you want to run migrations from your machine without the Liquibase Docker container.

#### 1. Standalone executable JAR (recommended for deployments)

This module also builds a single, self-contained executable JAR that bundles
Liquibase, the PostgreSQL JDBC driver, and the entire `changelog/` tree. You
can ship the JAR to any environment (CI/CD, jump host, Kubernetes job, etc.)
that has a JRE 21 and run migrations against any database by passing
connection details at runtime.

**Build:**

This module ships with its own Maven Wrapper, so you can build it standalone
without installing Maven globally.

```bash
cd easyops-erp/database-versioning

# Linux / macOS
./mvnw clean package -DskipTests

# Windows (PowerShell or cmd)
.\mvnw.cmd clean package -DskipTests

# Or, if you already have Maven on PATH
mvn clean package -DskipTests
```

Produces `target/easyops-database-versioning.jar` — a single self-contained jar
containing Liquibase, the PostgreSQL JDBC driver, and the entire `changelog/`
tree.

**Run against any environment:**

```bash
# Via CLI flags
java -jar target/easyops-database-versioning.jar update \
     --url=jdbc:postgresql://HOST:PORT/DATABASE \
     --username=USER \
     --password=PASS

# Via environment variables (recommended for CI/CD - no secrets in shell history)
export DB_URL=jdbc:postgresql://HOST:PORT/DATABASE
export DB_USERNAME=USER
export DB_PASSWORD=PASS
java -jar target/easyops-database-versioning.jar update

# Via -D system properties
java -Ddb.url=jdbc:postgresql://HOST:PORT/DATABASE \
     -Ddb.username=USER \
     -Ddb.password=PASS \
     -jar target/easyops-database-versioning.jar update
```

Resolution order for every setting: **CLI flag > `-D` system property > environment variable > default**.

| Setting          | CLI flag             | System property         | Env var               | Default                                   |
|------------------|----------------------|-------------------------|-----------------------|-------------------------------------------|
| JDBC URL         | `--url`              | `-Ddb.url`              | `DB_URL`              | `jdbc:postgresql://localhost:5432/easyops`|
| Username         | `--username`         | `-Ddb.username`         | `DB_USERNAME`         | `easyops`                                 |
| Password         | `--password`         | `-Ddb.password`         | `DB_PASSWORD`         | `easyops123`                              |
| Contexts         | `--contexts`         | `-Ddb.contexts`         | `DB_CONTEXTS`         | (all)                                     |
| Labels           | `--labels`           | `-Ddb.labels`           | `DB_LABELS`           | (all)                                     |
| Changelog path   | `--changelog`        | `-Ddb.changelog`        | `DB_CHANGELOG`        | `changelog/master-changelog.xml`          |
| Default schema   | `--default-schema`   | `-Ddb.defaultSchema`    | `DB_DEFAULT_SCHEMA`   | (DB user's default)                       |
| Liquibase schema | `--liquibase-schema` | `-Ddb.liquibaseSchema`  | `DB_LIQUIBASE_SCHEMA` | (DB user's default)                       |

**Available commands:**

| Command                       | Description                                         |
|-------------------------------|-----------------------------------------------------|
| `update` (default)            | Apply all pending changesets                        |
| `update-sql`                  | Print the SQL `update` would run, without executing |
| `status` / `history`          | Show pending changesets                             |
| `validate`                    | Validate the bundled changelog                      |
| `tag --tag NAME`              | Tag the current DB state                            |
| `rollback --tag NAME`         | Roll back to the named tag                          |
| `rollback-count --count N`    | Roll back the last `N` changesets                   |
| `drop-all`                    | Drop all DB objects (DANGEROUS)                     |
| `clear-checksums`             | Reset stored changelog checksums                    |

Full help:

```bash
java -jar target/easyops-database-versioning.jar --help
```

**Examples:**

```bash
# Apply migrations limited to certain Liquibase contexts (skip test data)
java -jar target/easyops-database-versioning.jar update \
     --url=jdbc:postgresql://prod-db:5432/easyops \
     --username=easyops --password=$DB_PASS \
     --contexts=default,initial

# Generate a SQL preview of pending changes (no changes applied)
java -jar target/easyops-database-versioning.jar update-sql \
     --url=jdbc:postgresql://staging-db:5432/easyops \
     --username=easyops --password=$DB_PASS > pending.sql

# Roll back the last 2 changesets on staging
java -jar target/easyops-database-versioning.jar rollback-count --count=2 \
     --url=jdbc:postgresql://staging-db:5432/easyops \
     --username=easyops --password=$DB_PASS
```

The JAR exits with status `0` on success and `1` on failure. Re-run with
`-Ddb.debug=true` to see the full stack trace when a migration fails.

#### 2. Maven Liquibase plugin (no install)

The `pom.xml` also keeps the Liquibase Maven plugin wired up for ad-hoc local
runs—no Liquibase CLI install needed.

**From the repo root:**

```bash
cd easyops-erp/database-versioning
mvn liquibase:update
```

**Requirements:** Java 21+ and Maven. Default connection is `localhost:5432`, database `easyops`, user `easyops`, password `easyops123` (so Postgres must be reachable on localhost, e.g. Docker with port 5432 published).

**Use another database:**

```bash
mvn liquibase:update -Dliquibase.url=jdbc:postgresql://HOST:PORT/DATABASE -Dliquibase.username=USER -Dliquibase.password=PASS
```

Example for a remote DB:

```bash
../mvnw liquibase:update   -Dliquibase.url=jdbc:postgresql://HOST:PORT/DATABASE   -Dliquibase.username=DB_USER   -Dliquibase.password=DB_PASSWORD   -Dliquibase.contexts=initial,default
```

#### 3. Liquibase CLI (optional)

If you prefer the CLI instead of Maven:

1. **Install Liquibase:** e.g. [liquibase.com](https://www.liquibase.com/download) or `choco install liquibase` on Windows.
2. **From `database-versioning/`** run:
   ```bash
   liquibase --defaults-file=liquibase.properties update
   ```
3. **For a DB on localhost**, override the URL (since `liquibase.properties` uses `postgres:5432`):
   ```bash
   liquibase --defaults-file=liquibase.properties --url=jdbc:postgresql://localhost:5432/easyops update
   ```

   You need the PostgreSQL JDBC driver on the classpath or use Liquibase’s `--classpath` if required by your Liquibase version.

### Development Workflow

1. **Create a new changeset**:
   - Add your changes to a new SQL file in the appropriate phase directory
   - Update the master changelog to include your changeset
   - Test the migration on a development database

2. **Test migrations**:
   ```bash
   # Run migrations
   ./scripts/migrate.sh
   
   # Validate the result
   ./scripts/validate.sh
   ```

3. **Rollback if needed**:
   ```bash
   ./scripts/rollback.sh <changeset-id>
   ```

## Optional module RBAC seeds

Platform RBAC rows (`rbac.permissions`, roles) are applied from this repo’s `changelog/data/` (e.g. `002-default-permissions.sql`, `011-rbac-module-permissions.sql`) when you run **database-versioning** Liquibase.

**Optional product modules** (e.g. **hospital**, **pharma**) also insert additional `rbac.*` rows from **each service’s own** Liquibase changelog (e.g. `services/hospital-service/.../002-hospital-permissions.sql`), often tagged with **`context:data`**. If a deployment does not run that service’s migrations, those permission rows are never inserted — the shared `rbac` schema is not polluted by unused modules.

Authoritative policy and PR checklist: [`../rbac/README.md`](../rbac/README.md).

## Configuration

The `liquibase.properties` file contains database connection settings and other configuration options. For different environments, you can override these settings using environment variables or separate properties files.

## Best Practices

1. **Naming Convention**: Use descriptive names for changesets with format: `YYYY-MM-DD-description`
2. **Atomic Changes**: Each changeset should be atomic and reversible
3. **Testing**: Always test migrations on a copy of production data
4. **Rollback Plans**: Ensure every changeset has a proper rollback strategy
5. **Documentation**: Document complex changes and business logic

## Environment-Specific Configuration

- **Development**: Uses local PostgreSQL instance
- **Testing**: Uses test database with sample data
- **Production**: Uses production database with proper credentials

## Test Data

The system includes comprehensive test data for development and testing:

- **Customers**: 20 test customers (10 accounting + 10 sales)
- **Products**: 15 test products (goods and services)
- **Vendors**: 10 test vendors for accounts payable
- **Sales Orders**: 5 test sales orders with line items
- **Quotations**: 5 test quotations with line items
- **AR Invoices**: 8 test invoices with line items
- **AP Bills**: 8 test bills with line items
- **Bank Transactions**: 15 test bank transactions
- **Chart of Accounts**: Complete account structure

All test data is tagged with `context:test-data` and can be excluded from production deployments.

See the [Test Data Guide](docs/test-data-guide.md) for detailed information.

## Troubleshooting

See the [Migration Guide](docs/migration-guide.md) for common issues and solutions.
