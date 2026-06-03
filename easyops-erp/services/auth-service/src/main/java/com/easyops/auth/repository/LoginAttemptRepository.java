package com.easyops.auth.repository;

import com.easyops.auth.entity.LoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Login Attempt Repository
 * 
 * Repository interface for LoginAttempt entity operations.
 * 
 * @author EasyOps Team
 * @version 1.0.0
 */
@Repository
public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, UUID> {

    List<LoginAttempt> findByUsernameAndAttemptedAtAfterOrderByAttemptedAtDesc(
            String username, LocalDateTime since);
    
    long countByUsernameAndSuccessfulFalseAndAttemptedAtAfter(
            String username, LocalDateTime since);
    
    @Modifying
    @Query("DELETE FROM LoginAttempt a WHERE a.attemptedAt < :cutoffDate")
    void deleteOldAttempts(@Param("cutoffDate") LocalDateTime cutoffDate);
}

