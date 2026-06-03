package com.easyops.rbac.integration;

import com.easyops.rbac.dto.*;
import com.easyops.rbac.entity.Permission;
import com.easyops.rbac.entity.Role;
import com.easyops.rbac.repository.PermissionRepository;
import com.easyops.rbac.repository.RoleRepository;
import com.easyops.rbac.repository.UserRoleRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for RBAC Service
 * 
 * Tests role management, permission management, and authorization.
 * 
 * @author EasyOps Team
 * @version 1.0.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@Transactional
public class RbacServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17")
            .withDatabaseName("easyops_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", () -> redis.getMappedPort(6379).toString());
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    private Role testRole;
    private Permission testPermission;

    /** Required by RBAC API controllers; enforcement is off in {@code application-test.yml}. */
    private static final String ACTOR_USER_ID = "00000000-0000-4000-8000-000000000001";

    @BeforeEach
    void setUp() {
        // Create test permission
        testPermission = new Permission();
        testPermission.setName("Test Permission");
        testPermission.setCode("TEST_PERMISSION");
        testPermission.setResource("test");
        testPermission.setAction("read");
        testPermission.setDescription("Test permission");
        testPermission.setIsActive(true);
        permissionRepository.save(testPermission);

        // Create test role
        testRole = new Role();
        testRole.setName("Test Role");
        testRole.setCode("TEST_ROLE");
        testRole.setDescription("Test role");
        testRole.setIsSystemRole(false);
        testRole.setIsActive(true);
        testRole.setPermissions(new HashSet<>());
        testRole.getPermissions().add(testPermission);
        roleRepository.save(testRole);
    }

    // ========== Role Tests ==========

    @Test
    void testCreateRole() throws Exception {
        RoleRequest request = new RoleRequest();
        request.setName("New Role");
        request.setCode("NEW_ROLE");
        request.setDescription("A new role");
        request.setIsActive(true);

        mockMvc.perform(post("/api/rbac/roles")
                .header("X-User-Id", ACTOR_USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("New Role"))
                .andExpect(jsonPath("$.code").value("NEW_ROLE"))
                .andExpect(jsonPath("$.isSystemRole").value(false));
    }

    @Test
    void testCreateRoleWithDuplicateCode() throws Exception {
        RoleRequest request = new RoleRequest();
        request.setName("Duplicate Role");
        request.setCode("TEST_ROLE"); // Already exists
        request.setDescription("Duplicate");
        request.setIsActive(true);

        mockMvc.perform(post("/api/rbac/roles")
                .header("X-User-Id", ACTOR_USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetRoleById() throws Exception {
        mockMvc.perform(get("/api/rbac/roles/" + testRole.getId())
                .header("X-User-Id", ACTOR_USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testRole.getId().toString()))
                .andExpect(jsonPath("$.name").value("Test Role"))
                .andExpect(jsonPath("$.code").value("TEST_ROLE"))
                .andExpect(jsonPath("$.permissions").isArray());
    }

    @Test
    void testGetRoleByCode() throws Exception {
        mockMvc.perform(get("/api/rbac/roles/code/TEST_ROLE")
                .header("X-User-Id", ACTOR_USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("TEST_ROLE"))
                .andExpect(jsonPath("$.name").value("Test Role"));
    }

    @Test
    void testGetAllRoles() throws Exception {
        mockMvc.perform(get("/api/rbac/roles")
                .header("X-User-Id", ACTOR_USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").exists());
    }

    @Test
    void testGetActiveRoles() throws Exception {
        mockMvc.perform(get("/api/rbac/roles/active")
                .header("X-User-Id", ACTOR_USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void testUpdateRole() throws Exception {
        RoleRequest request = new RoleRequest();
        request.setName("Updated Role");
        request.setCode("TEST_ROLE");
        request.setDescription("Updated description");
        request.setIsActive(true);

        mockMvc.perform(put("/api/rbac/roles/" + testRole.getId())
                .header("X-User-Id", ACTOR_USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Role"))
                .andExpect(jsonPath("$.description").value("Updated description"));
    }

    @Test
    void testDeleteRole() throws Exception {
        // Create a role to delete
        Role roleToDelete = new Role();
        roleToDelete.setName("Delete Me");
        roleToDelete.setCode("DELETE_ME");
        roleToDelete.setIsSystemRole(false);
        roleToDelete.setIsActive(true);
        roleRepository.save(roleToDelete);

        mockMvc.perform(delete("/api/rbac/roles/" + roleToDelete.getId())
                .header("X-User-Id", ACTOR_USER_ID))
                .andExpect(status().isNoContent());

        // Verify it's deleted
        assertFalse(roleRepository.findById(roleToDelete.getId()).isPresent());
    }

    @Test
    void testCannotDeleteSystemRole() throws Exception {
        testRole.setIsSystemRole(true);
        roleRepository.save(testRole);

        mockMvc.perform(delete("/api/rbac/roles/" + testRole.getId())
                .header("X-User-Id", ACTOR_USER_ID))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testAddPermissionToRole() throws Exception {
        // Create new permission
        Permission newPermission = new Permission();
        newPermission.setName("Write Permission");
        newPermission.setCode("TEST_WRITE");
        newPermission.setResource("test");
        newPermission.setAction("write");
        newPermission.setIsActive(true);
        permissionRepository.save(newPermission);

        mockMvc.perform(post("/api/rbac/roles/" + testRole.getId() + 
                "/permissions/" + newPermission.getId())
                .header("X-User-Id", ACTOR_USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.permissions").isArray());
    }

    @Test
    void testRemovePermissionFromRole() throws Exception {
        mockMvc.perform(delete("/api/rbac/roles/" + testRole.getId() + 
                "/permissions/" + testPermission.getId())
                .header("X-User-Id", ACTOR_USER_ID))
                .andExpect(status().isOk());
    }

    @Test
    void testSearchRoles() throws Exception {
        mockMvc.perform(get("/api/rbac/roles/search")
                .header("X-User-Id", ACTOR_USER_ID)
                .param("query", "Test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // ========== Permission Tests ==========

    @Test
    void testCreatePermission() throws Exception {
        PermissionRequest request = new PermissionRequest();
        request.setName("Create Permission");
        request.setCode("CREATE_PERM");
        request.setResource("documents");
        request.setAction("create");
        request.setDescription("Create documents");
        request.setIsActive(true);

        mockMvc.perform(post("/api/rbac/permissions")
                .header("X-User-Id", ACTOR_USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Create Permission"))
                .andExpect(jsonPath("$.code").value("CREATE_PERM"))
                .andExpect(jsonPath("$.resource").value("documents"))
                .andExpect(jsonPath("$.action").value("create"));
    }

    @Test
    void testGetPermissionById() throws Exception {
        mockMvc.perform(get("/api/rbac/permissions/" + testPermission.getId())
                .header("X-User-Id", ACTOR_USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testPermission.getId().toString()))
                .andExpect(jsonPath("$.code").value("TEST_PERMISSION"));
    }

    @Test
    void testGetPermissionByCode() throws Exception {
        mockMvc.perform(get("/api/rbac/permissions/code/TEST_PERMISSION")
                .header("X-User-Id", ACTOR_USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("TEST_PERMISSION"));
    }

    @Test
    void testGetPermissionsByResource() throws Exception {
        mockMvc.perform(get("/api/rbac/permissions/resource/test")
                .header("X-User-Id", ACTOR_USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void testUpdatePermission() throws Exception {
        PermissionRequest request = new PermissionRequest();
        request.setName("Updated Permission");
        request.setCode("TEST_PERMISSION");
        request.setResource("test");
        request.setAction("write");
        request.setDescription("Updated");
        request.setIsActive(true);

        mockMvc.perform(put("/api/rbac/permissions/" + testPermission.getId())
                .header("X-User-Id", ACTOR_USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Permission"));
    }

    @Test
    void testDeletePermission() throws Exception {
        Permission permToDelete = new Permission();
        permToDelete.setName("Delete Me");
        permToDelete.setCode("DELETE_PERM");
        permToDelete.setResource("test");
        permToDelete.setAction("delete");
        permToDelete.setIsActive(true);
        permissionRepository.save(permToDelete);

        mockMvc.perform(delete("/api/rbac/permissions/" + permToDelete.getId())
                .header("X-User-Id", ACTOR_USER_ID))
                .andExpect(status().isNoContent());
    }

    // ========== Authorization Tests ==========

    @Test
    void testAssignRolesToUser() throws Exception {
        UUID userId = UUID.randomUUID();
        Set<UUID> roleIds = new HashSet<>();
        roleIds.add(testRole.getId());

        UserRoleRequest request = new UserRoleRequest();
        request.setUserId(userId);
        request.setRoleIds(roleIds);

        mockMvc.perform(post("/api/rbac/authorization/users/roles")
                .header("X-User-Id", ACTOR_USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void testGetUserRoles() throws Exception {
        UUID userId = UUID.randomUUID();
        Set<UUID> roleIds = new HashSet<>();
        roleIds.add(testRole.getId());

        UserRoleRequest request = new UserRoleRequest();
        request.setUserId(userId);
        request.setRoleIds(roleIds);

        // Assign role first
        mockMvc.perform(post("/api/rbac/authorization/users/roles")
                .header("X-User-Id", ACTOR_USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Get user roles
        mockMvc.perform(get("/api/rbac/authorization/users/" + userId + "/roles")
                .header("X-User-Id", ACTOR_USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void testGetUserPermissions() throws Exception {
        UUID userId = UUID.randomUUID();
        Set<UUID> roleIds = new HashSet<>();
        roleIds.add(testRole.getId());

        UserRoleRequest request = new UserRoleRequest();
        request.setUserId(userId);
        request.setRoleIds(roleIds);

        // Assign role
        mockMvc.perform(post("/api/rbac/authorization/users/roles")
                .header("X-User-Id", ACTOR_USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Get permissions
        mockMvc.perform(get("/api/rbac/authorization/users/" + userId + "/permissions")
                .header("X-User-Id", ACTOR_USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void testCheckPermission() throws Exception {
        UUID userId = UUID.randomUUID();
        Set<UUID> roleIds = new HashSet<>();
        roleIds.add(testRole.getId());

        UserRoleRequest roleRequest = new UserRoleRequest();
        roleRequest.setUserId(userId);
        roleRequest.setRoleIds(roleIds);

        // Assign role
        mockMvc.perform(post("/api/rbac/authorization/users/roles")
                .header("X-User-Id", ACTOR_USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(roleRequest)))
                .andExpect(status().isOk());

        // Check permission
        AuthorizationRequest authRequest = new AuthorizationRequest();
        authRequest.setUserId(userId);
        authRequest.setResource("test");
        authRequest.setAction("read");

        mockMvc.perform(post("/api/rbac/authorization/check")
                .header("X-User-Id", ACTOR_USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authorized").value(true));
    }

    @Test
    void testCheckPermissionUnauthorized() throws Exception {
        UUID userId = UUID.randomUUID();

        AuthorizationRequest authRequest = new AuthorizationRequest();
        authRequest.setUserId(userId);
        authRequest.setResource("test");
        authRequest.setAction("read");

        mockMvc.perform(post("/api/rbac/authorization/check")
                .header("X-User-Id", ACTOR_USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authorized").value(false));
    }

    @Test
    void testCheckRole() throws Exception {
        UUID userId = UUID.randomUUID();
        Set<UUID> roleIds = new HashSet<>();
        roleIds.add(testRole.getId());

        UserRoleRequest request = new UserRoleRequest();
        request.setUserId(userId);
        request.setRoleIds(roleIds);

        // Assign role
        mockMvc.perform(post("/api/rbac/authorization/users/roles")
                .header("X-User-Id", ACTOR_USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Check role
        mockMvc.perform(get("/api/rbac/authorization/users/" + userId + "/has-role/TEST_ROLE")
                .header("X-User-Id", ACTOR_USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasRole").value(true));
    }

    @Test
    void testRemoveRoleFromUser() throws Exception {
        UUID userId = UUID.randomUUID();
        Set<UUID> roleIds = new HashSet<>();
        roleIds.add(testRole.getId());

        UserRoleRequest request = new UserRoleRequest();
        request.setUserId(userId);
        request.setRoleIds(roleIds);

        // Assign role
        mockMvc.perform(post("/api/rbac/authorization/users/roles")
                .header("X-User-Id", ACTOR_USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Remove role
        mockMvc.perform(delete("/api/rbac/authorization/users/" + userId + 
                "/roles/" + testRole.getId())
                .header("X-User-Id", ACTOR_USER_ID))
                .andExpect(status().isNoContent());
    }

    @Test
    void testGetUsersByRole() throws Exception {
        UUID userId = UUID.randomUUID();
        Set<UUID> roleIds = new HashSet<>();
        roleIds.add(testRole.getId());

        UserRoleRequest request = new UserRoleRequest();
        request.setUserId(userId);
        request.setRoleIds(roleIds);

        // Assign role
        mockMvc.perform(post("/api/rbac/authorization/users/roles")
                .header("X-User-Id", ACTOR_USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Get users by role
        mockMvc.perform(get("/api/rbac/authorization/roles/" + testRole.getId() + "/users")
                .header("X-User-Id", ACTOR_USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}

