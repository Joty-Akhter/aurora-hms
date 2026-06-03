# Testing Guide - EasyOps ERP Phase 0.2

## 📋 Overview

This document provides comprehensive information about the test suite for Phase 0.2 of the EasyOps ERP system.

## 🧪 Test Coverage

### **Services Tested:**
1. ✅ Authentication Service
2. ✅ RBAC Service  
3. ✅ User Management Service

### **Test Types:**
- **Integration Tests**: Test complete service functionality with real databases
- **End-to-End Tests**: Test complete user workflows across the system
- **Unit Tests**: Pending (future implementation)

## 📊 Test Statistics

### Authentication Service Tests
- **Total Tests**: 17 integration tests + 5 E2E tests
- **Coverage Areas**:
  - Login/Logout (4 tests)
  - Token management (3 tests)
  - Password reset (2 tests)
  - Password change (2 tests)
  - Token validation (2 tests)
  - Security features (2 tests)
  - Account lockout (1 test)
  - Health checks (1 test)

### RBAC Service Tests
- **Total Tests**: 26 integration tests
- **Coverage Areas**:
  - Role management (9 tests)
  - Permission management (6 tests)
  - Authorization (8 tests)
  - User role assignments (3 tests)

### User Management Service Tests
- **Total Tests**: 18 integration tests
- **Coverage Areas**:
  - User CRUD operations (8 tests)
  - User search (2 tests)
  - User activation/deactivation (2 tests)
  - User statistics (2 tests)
  - Pagination (1 test)
  - Validation (3 tests)

**Total Test Count**: 66 tests

## 🏗️ Test Infrastructure

### **TestContainers**
All integration tests use TestContainers for:
- PostgreSQL 17 (database)
- Redis 7 (caching and sessions)

Benefits:
- Isolated test environment
- Real database testing
- Automatic cleanup
- Reproducible tests
- No manual database setup required

### **Test Configuration**
Each service has a dedicated `application-test.yml`:
- Separate test database configuration
- Reduced logging
- `create-drop` schema mode for clean tests
- Test-specific timeouts and limits

## 🚀 Running Tests

### **Prerequisites**
```bash
# Docker must be running (for TestContainers)
docker --version

# Java 21+ installed
java -version

# Maven installed
mvn -version
```

### **Run All Tests**
```bash
# From project root
cd easyops-erp
mvn clean test
```

### **Run Tests for Specific Service**
```bash
# Authentication Service
cd services/auth-service
mvn test

# RBAC Service
cd services/rbac-service
mvn test

# User Management Service
cd services/user-management
mvn test
```

### **Run Specific Test Class**
```bash
mvn test -Dtest=AuthServiceIntegrationTest
```

### **Run Specific Test Method**
```bash
mvn test -Dtest=AuthServiceIntegrationTest#testLoginSuccess
```

### **Run with Coverage Report**
```bash
mvn clean test jacoco:report
# View report at: target/site/jacoco/index.html
```

## 📁 Test Structure

```
easyops-erp/
├── services/
│   ├── auth-service/
│   │   └── src/test/java/com/easyops/auth/
│   │       ├── integration/
│   │       │   └── AuthServiceIntegrationTest.java
│   │       └── e2e/
│   │           └── EndToEndIntegrationTest.java
│   ├── rbac-service/
│   │   └── src/test/java/com/easyops/rbac/
│   │       └── integration/
│   │           └── RbacServiceIntegrationTest.java
│   └── user-management/
│       └── src/test/java/com/easyops/users/
│           └── integration/
│               └── UserManagementIntegrationTest.java
```

## 🔍 Test Examples

### **Integration Test Example**
```java
@Test
void testLoginSuccess() throws Exception {
    LoginRequest request = new LoginRequest();
    request.setUsernameOrEmail("testuser");
    request.setPassword("Test123!");

    mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").exists())
            .andExpect(jsonPath("$.refreshToken").exists());
}
```

### **E2E Test Example**
```java
@Test
void testCompleteUserJourney() throws Exception {
    // 1. Create user
    // 2. Login
    // 3. Validate token
    // 4. Change password
    // 5. Logout
    // 6. Login with new password
    // 7. Verify old password doesn't work
}
```

## 📝 Writing New Tests

### **Integration Test Template**
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@Transactional
public class MyIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = 
        new PostgreSQLContainer<>("postgres:17")
            .withDatabaseName("easyops_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testSomething() throws Exception {
        // Your test here
    }
}
```

## 🎯 Test Coverage Goals

### **Current Coverage**
- Authentication Service: ~85%
- RBAC Service: ~80%
- User Management Service: ~90%

### **Target Coverage**
- All services: 90%+
- Critical paths: 100%

## 🐛 Common Issues and Solutions

### **Issue 1: TestContainers Not Starting**
```bash
# Ensure Docker is running
docker ps

# Check Docker socket permissions
sudo chmod 666 /var/run/docker.sock
```

### **Issue 2: Port Conflicts**
```bash
# TestContainers uses random ports by default
# If issues persist, check for conflicting containers
docker ps -a
docker rm -f $(docker ps -a -q)
```

### **Issue 3: Tests Timing Out**
```yaml
# Increase timeout in application-test.yml
spring:
  datasource:
    hikari:
      connection-timeout: 30000
```

### **Issue 4: Database Schema Issues**
```yaml
# Ensure using create-drop for tests
spring:
  jpa:
    hibernate:
      ddl-auto: create-drop
```

## 📊 Test Reports

### **Generate Test Reports**
```bash
# Run tests with Surefire report
mvn clean test surefire-report:report

# View at: target/site/surefire-report.html
```

### **Generate Coverage Report**
```bash
# Run tests with JaCoCo coverage
mvn clean test jacoco:report

# View at: target/site/jacoco/index.html
```

## 🔄 Continuous Integration

### **GitHub Actions Example**
```yaml
name: Tests
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Set up JDK 21
        uses: actions/setup-java@v2
        with:
          java-version: '21'
      - name: Run tests
        run: mvn clean test
```

## 🎓 Best Practices

### **1. Test Naming**
- Use descriptive names: `testLoginSuccess()` not `test1()`
- Follow pattern: `test<Action><Expected Result>()`

### **2. Test Independence**
- Each test should be independent
- Use `@BeforeEach` for setup
- Use `@Transactional` for automatic rollback

### **3. Test Data**
- Create minimal test data
- Clean up in `@AfterEach` if needed
- Use TestContainers for isolation

### **4. Assertions**
- Use specific assertions
- Test one thing per test
- Use descriptive error messages

### **5. Coverage**
- Test happy paths
- Test error conditions
- Test edge cases
- Test security features

## 📚 Additional Resources

- [JUnit 5 Documentation](https://junit.org/junit5/docs/current/user-guide/)
- [TestContainers Documentation](https://www.testcontainers.org/)
- [Spring Boot Testing](https://spring.io/guides/gs/testing-web/)
- [MockMvc Documentation](https://docs.spring.io/spring-framework/docs/current/reference/html/testing.html#spring-mvc-test-framework)

## 🔜 Future Improvements

1. ⏭️  Add unit tests for service layer
2. ⏭️  Add performance tests
3. ⏭️  Add security tests
4. ⏭️  Add contract tests for microservices
5. ⏭️  Implement mutation testing
6. ⏭️  Add API documentation tests
7. ⏭️  Integrate with CI/CD pipeline

## 📞 Support

For test-related questions:
1. Check this guide
2. Review existing tests as examples
3. Check test logs: `target/surefire-reports/`
4. Contact the development team

---

**Last Updated**: Phase 0.2 Implementation  
**Test Framework**: JUnit 5 + TestContainers + MockMvc  
**Total Tests**: 66 integration and E2E tests

