package com.easyops.auth.repository;

import com.easyops.auth.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * User Session Repository
 * 
 * Repository interface for UserSession entity operations.
 * 
 * @author EasyOps Team
 * @version 1.0.0
 */
@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {

    Optional<UserSession> findByToken(String token);
    
    Optional<UserSession> findByRefreshToken(String refreshToken);
    
    List<UserSession> findByUserIdAndIsActiveTrue(UUID userId);
    
    long countByUserIdAndIsActiveTrue(UUID userId);
    
    @Modifying
    @Query("UPDATE UserSession s SET s.isActive = false WHERE s.userId = :userId AND s.id != :currentSessionId")
    void deactivateOtherSessions(@Param("userId") UUID userId, @Param("currentSessionId") UUID currentSessionId);
    
    @Modifying
    @Query("UPDATE UserSession s SET s.isActive = false WHERE s.userId = :userId")
    void deactivateAllUserSessions(@Param("userId") UUID userId);
    
    @Modifying
    @Query("DELETE FROM UserSession s WHERE s.expiresAt < :currentTime")
    void deleteExpiredSessions(@Param("currentTime") LocalDateTime currentTime);
    
    @Modifying
    @Query("UPDATE UserSession s SET s.lastActivityAt = :activityTime WHERE s.token = :token")
    void updateLastActivity(@Param("token") String token, @Param("activityTime") LocalDateTime activityTime);
}

