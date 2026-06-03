# HR Service (`hr-service`)

Spring Boot microservice for **Human Resources** in EasyOps ERP: employees, payroll coordination, **employee loans**, provident fund, salary structures, and related APIs consumed by the SPA and gateway.

## Documentation

| Resource | Description |
|----------|-------------|
| **[docs/README.md](docs/README.md)** | Documentation index |
| **[docs/knowledge-base/README.md](docs/knowledge-base/README.md)** | **HR knowledge base** (topic index) |
| **[docs/knowledge-base/EASYOPS-HR-USER-MANUAL.md](docs/knowledge-base/EASYOPS-HR-USER-MANUAL.md)** | **Master user manual** |
| [HR-SALARY-USER-MANUAL.md](HR-SALARY-USER-MANUAL.md) | Salary & payroll (detailed) |
| [HR-EPF-USER-MANUAL.md](HR-EPF-USER-MANUAL.md) | Provident fund / EPF (detailed) |

## Build & test

From repository root `easyops-erp/`:

```bash
./mvnw -pl services/hr-service compile
./mvnw -pl services/hr-service test
```

See project root `CLAUDE.md` for ports and stack.
