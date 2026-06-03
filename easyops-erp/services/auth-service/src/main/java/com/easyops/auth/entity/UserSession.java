package com.easyops.auth.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * User Session Entity
 * 
 * Represents an active user session with JWT token information.
 * 
 * @author EasyOps Team
 * @version 1.0.0
 */
@Entity
@Table(name = "user_sessions", schema = "auth")
public class UserSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** JWTs with large claims often exceed 4k; keep in sync with auth.user_sessions DDL (see 002b migration). */
    @Column(nullable = false, unique = true, length = 8192)
    private String token;

    @Column(name = "refresh_token", unique = true, length = 8192)
    private String refreshToken;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 1024)
    private String userAgent;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "last_activity_at")
    private LocalDateTime lastActivityAt;

    public UserSession() {
        this.isActive = Boolean.TRUE;
    }

    public UserSession(UUID userId, String token, String refreshToken, String ipAddress,
                       String userAgent, Boolean isActive, LocalDateTime expiresAt,
                       LocalDateTime lastActivityAt) {
        this.userId = userId;
        this.token = token;
        this.refreshToken = refreshToken;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.isActive = isActive != null ? isActive : Boolean.TRUE;
        this.expiresAt = expiresAt;
        this.lastActivityAt = lastActivityAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LocalDateTime getLastActivityAt() {
        return lastActivityAt;
    }

    public void setLastActivityAt(LocalDateTime lastActivityAt) {
        this.lastActivityAt = lastActivityAt;
    }
}

