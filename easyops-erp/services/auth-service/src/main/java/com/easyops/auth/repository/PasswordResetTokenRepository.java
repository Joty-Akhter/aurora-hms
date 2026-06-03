package com.easyops.auth.repository;

import com.easyops.auth.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Password Reset Token Repository
 * 
 * Repository interface for PasswordResetToken entity operations.
 * 
 * @author EasyOps Team
 * @version 1.0.0
 */
@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByTokenAndIsUsedFalse(String token);
    
    Optional<PasswordResetToken> findByUserIdAndIsUsedFalseOrderByCreatedAtDesc(UUID userId);
    
    @Modifying
    @Query("UPDATE PasswordResetToken t SET t.isUsed = true WHERE t.userId = :userId AND t.isUsed = false")
    void invalidateAllUserTokens(@Param("userId") UUID userId);
    
    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.expiresAt < :currentTime OR t.isUsed = true")
    void deleteExpiredAndUsedTokens(@Param("currentTime") LocalDateTime currentTime);
}

