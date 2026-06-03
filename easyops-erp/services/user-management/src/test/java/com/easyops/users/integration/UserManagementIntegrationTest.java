package com.easyops.users.integration;

import com.easyops.users.dto.UserCreateRequest;
import com.easyops.users.dto.UserUpdateRequest;
import com.easyops.users.entity.User;
import com.easyops.users.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for User Management Service
 * 
 * Tests user CRUD operations, search, and statistics.
 * 
 * @author EasyOps Team
 * @version 1.0.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@Import(IntegrationTestRbacClientConfig.class)
@Transactional
public class UserManagementIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17")
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

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        
        testUser = new User();
        testUser.setUsername("existing");
        testUser.setEmail("existing@test.com");
        testUser.setPasswordHash("hashedpassword");
        testUser.setFirstName("Existing");
        testUser.setLastName("User");
        testUser.setIsActive(true);
        testUser.setIsVerified(false);
        userRepository.save(testUser);
    }

    @Test
    void testCreateUser() throws Exception {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("newuser");
        request.setEmail("newuser@test.com");
        request.setPassword("NewUser123!");
        request.setFirstName("New");
        request.setLastName("User");
        request.setPhone("+1234567890");

        mockMvc.perform(post("/api/users")
                .header("X-User-Id", testUser.getId().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("newuser"))
                .andExpect(jsonPath("$.email").value("newuser@test.com"))
                .andExpect(jsonPath("$.firstName").value("New"))
                .andExpect(jsonPath("$.lastName").value("User"))
                .andExpect(jsonPath("$.phone").value("+1234567890"))
                .andExpect(jsonPath("$.isActive").value(true))
                .andExpect(jsonPath("$.isVerified").value(false));

        // Verify user was created in database
        assertTrue(userRepository.existsByUsername("newuser"));
        assertTrue(userRepository.existsByEmail("newuser@test.com"));
    }

    @Test
    void testCreateUserWithoutEmail() throws Exception {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("noemailuser");
        request.setPassword("NoEmail123!");
        request.setFirstName("No");
        request.setLastName("Email");

        mockMvc.perform(post("/api/users")
                .header("X-User-Id", testUser.getId().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("noemailuser"));

        User created = userRepository.findByUsername("noemailuser").orElseThrow();
        assertNull(created.getEmail());
    }

    @Test
    void testCreateUserWithDuplicateUsername() throws Exception {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("existing"); // Already exists
        request.setEmail("unique@test.com");
        request.setPassword("Password123!");

        mockMvc.perform(post("/api/users")
                .header("X-User-Id", testUser.getId().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateUserWithDuplicateEmail() throws Exception {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("uniqueuser");
        request.setEmail("existing@test.com"); // Already exists
        request.setPassword("Password123!");

        mockMvc.perform(post("/api/users")
                .header("X-User-Id", testUser.getId().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateUserWithInvalidEmail() throws Exception {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("testuser");
        request.setEmail("invalid-email");
        request.setPassword("Password123!");

        mockMvc.perform(post("/api/users")
                .header("X-User-Id", testUser.getId().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateUserWithShortPassword() throws Exception {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("testuser");
        request.setEmail("test@test.com");
        request.setPassword("short");

        mockMvc.perform(post("/api/users")
                .header("X-User-Id", testUser.getId().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetUserById() throws Exception {
        mockMvc.perform(get("/api/users/" + testUser.getId())
                .header("X-User-Id", testUser.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testUser.getId().toString()))
                .andExpect(jsonPath("$.username").value("existing"))
                .andExpect(jsonPath("$.email").value("existing@test.com"));
    }

    @Test
    void testGetUserByIdNotFound() throws Exception {
        mockMvc.perform(get("/api/users/" + java.util.UUID.randomUUID())
                .header("X-User-Id", testUser.getId().toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetAllUsers() throws Exception {
        mockMvc.perform(get("/api/users")
                .header("X-User-Id", testUser.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void testUpdateUser() throws Exception {
        UserUpdateRequest request = new UserUpdateRequest();
        request.setFirstName("Updated");
        request.setLastName("Name");
        request.setEmail("updated@test.com");
        request.setPhone("+9876543210");

        mockMvc.perform(put("/api/users/" + testUser.getId())
                .header("X-User-Id", testUser.getId().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Updated"))
                .andExpect(jsonPath("$.lastName").value("Name"))
                .andExpect(jsonPath("$.email").value("updated@test.com"))
                .andExpect(jsonPath("$.phone").value("+9876543210"));

        // Verify changes in database
        User updated = userRepository.findById(testUser.getId()).orElseThrow();
        assertEquals("Updated", updated.getFirstName());
        assertEquals("Name", updated.getLastName());
        assertEquals("updated@test.com", updated.getEmail());
    }

    @Test
    void testUpdateUserWithDuplicateEmail() throws Exception {
        // Create another user
        User anotherUser = new User();
        anotherUser.setUsername("another");
        anotherUser.setEmail("another@test.com");
        anotherUser.setPasswordHash("hash");
        anotherUser.setIsActive(true);
        userRepository.save(anotherUser);

        // Try to update testUser with anotherUser's email
        UserUpdateRequest request = new UserUpdateRequest();
        request.setEmail("another@test.com");

        mockMvc.perform(put("/api/users/" + testUser.getId())
                .header("X-User-Id", testUser.getId().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testDeleteUser() throws Exception {
        mockMvc.perform(delete("/api/users/" + testUser.getId())
                .header("X-User-Id", testUser.getId().toString()))
                .andExpect(status().isNoContent());

        // Verify user was deleted
        assertFalse(userRepository.existsById(testUser.getId()));
    }

    @Test
    void testDeleteNonExistentUser() throws Exception {
        mockMvc.perform(delete("/api/users/" + java.util.UUID.randomUUID())
                .header("X-User-Id", testUser.getId().toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testActivateUser() throws Exception {
        testUser.setIsActive(false);
        userRepository.save(testUser);

        mockMvc.perform(patch("/api/users/" + testUser.getId() + "/activate")
                .header("X-User-Id", testUser.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(true));

        // Verify in database
        User activated = userRepository.findById(testUser.getId()).orElseThrow();
        assertTrue(activated.getIsActive());
    }

    @Test
    void testDeactivateUser() throws Exception {
        mockMvc.perform(patch("/api/users/" + testUser.getId() + "/deactivate")
                .header("X-User-Id", testUser.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(false));

        // Verify in database
        User deactivated = userRepository.findById(testUser.getId()).orElseThrow();
        assertFalse(deactivated.getIsActive());
    }

    @Test
    void testSearchUsers() throws Exception {
        mockMvc.perform(get("/api/users/search")
                .header("X-User-Id", testUser.getId().toString())
                .param("searchTerm", "existing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void testSearchUsersByEmail() throws Exception {
        mockMvc.perform(get("/api/users/search")
                .header("X-User-Id", testUser.getId().toString())
                .param("searchTerm", "existing@test.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].email").value("existing@test.com"));
    }

    @Test
    void testGetUserStats() throws Exception {
        mockMvc.perform(get("/api/users/stats")
                .header("X-User-Id", testUser.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(1))
                .andExpect(jsonPath("$.activeUsers").value(1))
                .andExpect(jsonPath("$.inactiveUsers").value(0));
    }

    @Test
    void testGetUserStatsWithMixedActiveStatus() throws Exception {
        // Create inactive user
        User inactiveUser = new User();
        inactiveUser.setUsername("inactive");
        inactiveUser.setEmail("inactive@test.com");
        inactiveUser.setPasswordHash("hash");
        inactiveUser.setIsActive(false);
        userRepository.save(inactiveUser);

        mockMvc.perform(get("/api/users/stats")
                .header("X-User-Id", testUser.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(2))
                .andExpect(jsonPath("$.activeUsers").value(1))
                .andExpect(jsonPath("$.inactiveUsers").value(1));
    }

    @Test
    void testGetAllUsersWithPagination() throws Exception {
        // Create multiple users
        for (int i = 0; i < 15; i++) {
            User user = new User();
            user.setUsername("user" + i);
            user.setEmail("user" + i + "@test.com");
            user.setPasswordHash("hash");
            user.setIsActive(true);
            userRepository.save(user);
        }

        mockMvc.perform(get("/api/users")
                .header("X-User-Id", testUser.getId().toString())
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(10))
                .andExpect(jsonPath("$.totalElements").value(16)) // 15 + 1 existing
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void testCreateUserWithMinimalInfo() throws Exception {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("minimal");
        request.setEmail("minimal@test.com");
        request.setPassword("Minimal123!");

        mockMvc.perform(post("/api/users")
                .header("X-User-Id", testUser.getId().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("minimal"))
                .andExpect(jsonPath("$.email").value("minimal@test.com"))
                .andExpect(jsonPath("$.firstName").isEmpty())
                .andExpect(jsonPath("$.lastName").isEmpty());
    }

    @Test
    void testPasswordIsHashedOnCreation() throws Exception {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("hashtest");
        request.setEmail("hashtest@test.com");
        request.setPassword("PlainPassword123!");

        MvcResult result = mockMvc.perform(post("/api/users")
                .header("X-User-Id", testUser.getId().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        // Verify password is hashed in database
        User created = userRepository.findByUsername("hashtest").orElseThrow();
        assertNotEquals("PlainPassword123!", created.getPasswordHash());
        assertTrue(created.getPasswordHash().startsWith("$2a$")); // BCrypt hash prefix
    }
}

