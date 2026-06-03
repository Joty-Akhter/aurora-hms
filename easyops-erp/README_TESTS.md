# Running Integration Tests - Quick Reference

## 🚀 Quick Start

### Run All Tests
```bash
cd easyops-erp
mvn clean test
```

### Run Tests for Single Service
```bash
# Authentication Service
cd services/auth-service && mvn test

# RBAC Service  
cd services/rbac-service && mvn test

# User Management Service
cd services/user-management && mvn test
```

## ✅ What Gets Tested

### **66 Integration Tests Total**

#### Authentication Service (22 tests)
- ✅ Login/Logout (4 tests)
- ✅ Token Management (3 tests)
- ✅ Password Reset (2 tests)
- ✅ Password Change (2 tests)
- ✅ Token Validation (2 tests)
- ✅ Security Features (4 tests)
- ✅ End-to-End Flows (5 tests)

#### RBAC Service (26 tests)
- ✅ Role CRUD (9 tests)
- ✅ Permission CRUD (6 tests)
- ✅ Authorization (8 tests)
- ✅ User Assignments (3 tests)

#### User Management (18 tests)
- ✅ User CRUD (8 tests)
- ✅ Search & Stats (4 tests)
- ✅ Activation (2 tests)
- ✅ Validation (4 tests)

## 🐳 Prerequisites

**Docker must be running** (for TestContainers):
```bash
docker --version
# Docker version 20.10.0+
```

## 📊 Expected Output

```
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.easyops.auth.integration.AuthServiceIntegrationTest
[INFO] Tests run: 17, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.easyops.auth.e2e.EndToEndIntegrationTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 66, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] BUILD SUCCESS
```

## 🔧 Troubleshooting

### Docker Not Running
```bash
# Start Docker
sudo systemctl start docker

# Or on Mac/Windows
# Start Docker Desktop
```

### Port Conflicts
TestContainers uses random ports automatically.
If issues persist:
```bash
docker ps -a
docker rm -f $(docker ps -a -q)
```

### Tests Failing
1. Check Docker is running
2. Ensure you have internet access (for pulling images)
3. Check logs: `target/surefire-reports/`

## 📚 More Information

See **TESTING_GUIDE.md** for complete documentation.

## 🎯 Test Coverage

- **Auth Service**: ~85%
- **RBAC Service**: ~80%  
- **User Management**: ~90%

**Overall**: ~85% code coverage

