package com.easyops.auth.e2e;

import com.easyops.auth.dto.*;
import com.easyops.auth.entity.User;
import com.easyops.auth.repository.PasswordResetTokenRepository;
import com.easyops.auth.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
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

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-End Integration Tests
 * 
 * Tests complete user workflows from registration to authentication.
 * 
 * @author EasyOps Team
 * @version 1.0.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@Transactional
public class EndToEndIntegrationTest {

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
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void testCompleteUserJourney() throws Exception {
        String username = "journeyuser";
        String email = "journey@test.com";
        String password = "Journey123!";

        // 1. Create user (simulated - normally done via User Management Service)
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setFirstName("Journey");
        user.setLastName("User");
        user.setIsActive(true);
        user.setIsVerified(true);
        userRepository.save(user);

        // 2. Login
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsernameOrEmail(username);
        loginRequest.setPassword(password);

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andReturn();

        LoginResponse loginResponse = objectMapper.readValue(
                loginResult.getResponse().getContentAsString(), LoginResponse.class);

        String accessToken = loginResponse.getAccessToken();
        String refreshToken = loginResponse.getRefreshToken();

        assertNotNull(accessToken);
        assertNotNull(refreshToken);

        // 3. Validate token
        mockMvc.perform(get("/api/auth/validate")
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));

        // 4. Change password
        ChangePasswordRequest changePasswordRequest = new ChangePasswordRequest();
        changePasswordRequest.setNewPassword("NewJourney123!");

        mockMvc.perform(post("/api/auth/password/change/" + user.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(changePasswordRequest)))
                .andExpect(status().isOk());

        // 5. Logout
        mockMvc.perform(post("/api/auth/logout")
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        // 6. Login with new password
        LoginRequest newLoginRequest = new LoginRequest();
        newLoginRequest.setUsernameOrEmail(username);
        newLoginRequest.setPassword("NewJourney123!");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newLoginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists());

        // 7. Verify old password doesn't work
        LoginRequest oldPasswordRequest = new LoginRequest();
        oldPasswordRequest.setUsernameOrEmail(username);
        oldPasswordRequest.setPassword(password);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(oldPasswordRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testPasswordResetFlow() throws Exception {
        String email = "resetuser@test.com";
        String password = "Original123!";

        // 1. Create user
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("resetuser");
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setFirstName("Reset");
        user.setLastName("User");
        user.setIsActive(true);
        user.setIsVerified(true);
        userRepository.save(user);

        // 2. Recovery options
        PasswordResetOptionsRequest optionsRequest = new PasswordResetOptionsRequest();
        optionsRequest.setUsernameOrEmail("resetuser");
        mockMvc.perform(post("/api/auth/password/reset/options")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(optionsRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligible").value(true))
                .andExpect(jsonPath("$.channels").isArray());

        // 3. Initiate OTP (notification client skipped when communication base-url empty in tests)
        PasswordResetRequest resetRequest = new PasswordResetRequest();
        resetRequest.setUsernameOrEmail("resetuser");
        resetRequest.setChannel("EMAIL");

        mockMvc.perform(post("/api/auth/password/reset")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(resetRequest)))
                .andExpect(status().isOk());

        String otp = passwordResetTokenRepository.findByUserIdAndIsUsedFalseOrderByCreatedAtDesc(user.getId())
                .orElseThrow()
                .getToken();

        // 4. Confirm with OTP + new password
        PasswordResetConfirmRequest confirmRequest = new PasswordResetConfirmRequest();
        confirmRequest.setToken(otp);
        confirmRequest.setNewPassword("FreshReset456!");

        mockMvc.perform(post("/api/auth/password/reset/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(confirmRequest)))
                .andExpect(status().isOk());

        // 5. Login with new password
        LoginRequest loginAfterReset = new LoginRequest();
        loginAfterReset.setUsernameOrEmail("resetuser");
        loginAfterReset.setPassword("FreshReset456!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginAfterReset)))
                .andExpect(status().isOk());
    }

    @Test
    void testTokenRefreshFlow() throws Exception {
        String username = "refreshuser";
        String password = "Refresh123!";

        // 1. Create user
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername(username);
        user.setEmail("refresh@test.com");
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setIsActive(true);
        user.setIsVerified(true);
        userRepository.save(user);

        // 2. Login
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsernameOrEmail(username);
        loginRequest.setPassword(password);

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        LoginResponse loginResponse = objectMapper.readValue(
                loginResult.getResponse().getContentAsString(), LoginResponse.class);

        // 3. Refresh token
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest();
        refreshRequest.setRefreshToken(loginResponse.getRefreshToken());

        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andReturn();

        LoginResponse refreshResponse = objectMapper.readValue(
                refreshResult.getResponse().getContentAsString(), LoginResponse.class);

        // 4. Verify new tokens are different from old ones
        assertNotEquals(loginResponse.getAccessToken(), refreshResponse.getAccessToken());
        assertNotEquals(loginResponse.getRefreshToken(), refreshResponse.getRefreshToken());

        // 5. Verify new token is valid
        mockMvc.perform(get("/api/auth/validate")
                .header("Authorization", "Bearer " + refreshResponse.getAccessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));
    }

    @Test
    void testMultipleDeviceLogin() throws Exception {
        String username = "multidevice";
        String password = "Multi123!";

        // 1. Create user
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername(username);
        user.setEmail("multi@test.com");
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setIsActive(true);
        user.setIsVerified(true);
        userRepository.save(user);

        // 2. Login from device 1
        LoginRequest device1Request = new LoginRequest();
        device1Request.setUsernameOrEmail(username);
        device1Request.setPassword(password);
        device1Request.setIpAddress("192.168.1.1");
        device1Request.setUserAgent("Device1");

        MvcResult device1Result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(device1Request)))
                .andExpect(status().isOk())
                .andReturn();

        LoginResponse device1Response = objectMapper.readValue(
                device1Result.getResponse().getContentAsString(), LoginResponse.class);

        // 3. Login from device 2
        LoginRequest device2Request = new LoginRequest();
        device2Request.setUsernameOrEmail(username);
        device2Request.setPassword(password);
        device2Request.setIpAddress("192.168.1.2");
        device2Request.setUserAgent("Device2");

        MvcResult device2Result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(device2Request)))
                .andExpect(status().isOk())
                .andReturn();

        LoginResponse device2Response = objectMapper.readValue(
                device2Result.getResponse().getContentAsString(), LoginResponse.class);

        // 4. Verify both tokens are valid (concurrent sessions allowed)
        mockMvc.perform(get("/api/auth/validate")
                .header("Authorization", "Bearer " + device1Response.getAccessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));

        mockMvc.perform(get("/api/auth/validate")
                .header("Authorization", "Bearer " + device2Response.getAccessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));
    }

    @Test
    void testSecurityFeatures() throws Exception {
        String username = "secureuser";
        String password = "Secure123!";

        // 1. Create user
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername(username);
        user.setEmail("secure@test.com");
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setIsActive(true);
        user.setIsVerified(true);
        userRepository.save(user);

        // 2. Test account lockout after failed attempts
        LoginRequest wrongPasswordRequest = new LoginRequest();
        wrongPasswordRequest.setUsernameOrEmail(username);
        wrongPasswordRequest.setPassword("WrongPassword");

        // Attempt 5 failed logins
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(wrongPasswordRequest)))
                    .andExpect(status().isBadRequest());
        }

        // 6th attempt should show account locked
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(wrongPasswordRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Account is locked. Please try again later."));

        // 3. Verify correct password also doesn't work when locked
        LoginRequest correctPasswordRequest = new LoginRequest();
        correctPasswordRequest.setUsernameOrEmail(username);
        correctPasswordRequest.setPassword(password);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(correctPasswordRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Account is locked. Please try again later."));
    }
}

