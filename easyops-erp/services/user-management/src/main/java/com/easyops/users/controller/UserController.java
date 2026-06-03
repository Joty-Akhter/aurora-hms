package com.easyops.users.controller;

import com.easyops.users.dto.UserCreateRequest;
import com.easyops.users.dto.UserResponse;
import com.easyops.users.dto.UserUpdateRequest;
import com.easyops.users.security.RbacRequestHeaders;
import com.easyops.users.security.UserManagementRbacService;
import com.easyops.users.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * User Controller
 *
 * REST controller for user management operations.
 */
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    private final UserService userService;
    private final UserManagementRbacService userRbac;

    public UserController(UserService userService, UserManagementRbacService userRbac) {
        this.userService = userService;
        this.userRbac = userRbac;
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(
            @Valid @RequestBody UserCreateRequest request,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userId);
        userRbac.requireUserWrite(actor, organizationId);
        UserResponse user = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userId);
        userRbac.requireUserRead(actor, organizationId);
        UserResponse user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    @GetMapping
    public ResponseEntity<Page<UserResponse>> getAllUsers(
            Pageable pageable,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userId);
        userRbac.requireUserRead(actor, organizationId);
        Page<UserResponse> users = userService.getAllUsers(pageable);
        return ResponseEntity.ok(users);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody UserUpdateRequest request,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userId);
        userRbac.requireUserWrite(actor, organizationId);
        UserResponse user = userService.updateUser(id, request);
        return ResponseEntity.ok(user);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userId);
        userRbac.requireUserWrite(actor, organizationId);
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<UserResponse> activateUser(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userId);
        userRbac.requireUserWrite(actor, organizationId);
        UserResponse user = userService.activateUser(id);
        return ResponseEntity.ok(user);
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<UserResponse> deactivateUser(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userId);
        userRbac.requireUserWrite(actor, organizationId);
        UserResponse user = userService.deactivateUser(id);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/search")
    public ResponseEntity<Page<UserResponse>> searchUsers(
            @RequestParam(name = "searchTerm", required = true) String searchTerm,
            Pageable pageable,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userId);
        userRbac.requireUserRead(actor, organizationId);
        Page<UserResponse> users = userService.searchUsers(searchTerm, pageable);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/stats")
    public ResponseEntity<Object> getUserStats(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userId);
        userRbac.requireUserRead(actor, organizationId);
        Object stats = userService.getUserStats();
        return ResponseEntity.ok(stats);
    }
}
