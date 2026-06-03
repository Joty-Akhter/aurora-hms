package com.easyops.rbac.repository;

import com.easyops.rbac.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * User Role Repository
 * 
 * Repository interface for UserRole entity operations.
 * 
 * @author EasyOps Team
 * @version 1.0.0
 */
@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, UUID> {

    List<UserRole> findByUserId(UUID userId);
    
    List<UserRole> findByUserIdAndOrganizationId(UUID userId, UUID organizationId);
    
    List<UserRole> findByRoleId(UUID roleId);
    
    boolean existsByUserIdAndRoleId(UUID userId, UUID roleId);
    
    @Modifying
    @Query("DELETE FROM UserRole ur WHERE ur.userId = :userId")
    void deleteByUserId(@Param("userId") UUID userId);
    
    @Modifying
    @Query("DELETE FROM UserRole ur WHERE ur.userId = :userId AND ur.role.id = :roleId")
    void deleteByUserIdAndRoleId(@Param("userId") UUID userId, @Param("roleId") UUID roleId);
    
    @Modifying
    @Query("DELETE FROM UserRole ur WHERE ur.userId = :userId AND ur.organizationId = :organizationId")
    void deleteByUserIdAndOrganizationId(@Param("userId") UUID userId, @Param("organizationId") UUID organizationId);
    
    @Modifying
    @Query("DELETE FROM UserRole ur WHERE ur.expiresAt < :currentTime")
    void deleteExpiredRoles(@Param("currentTime") LocalDateTime currentTime);
    
    @Query("SELECT ur FROM UserRole ur " +
           "WHERE ur.userId = :userId " +
           "AND (ur.expiresAt IS NULL OR ur.expiresAt > :currentTime)")
    List<UserRole> findActiveRolesByUserId(@Param("userId") UUID userId, 
                                          @Param("currentTime") LocalDateTime currentTime);
    
    @Query("SELECT ur FROM UserRole ur " +
           "WHERE ur.userId = :userId " +
           "AND (ur.organizationId = :organizationId OR ur.organizationId IS NULL) " +
           "AND (ur.expiresAt IS NULL OR ur.expiresAt > :currentTime)")
    List<UserRole> findActiveRolesByUserIdAndOrganization(@Param("userId") UUID userId,
                                                          @Param("organizationId") UUID organizationId,
                                                          @Param("currentTime") LocalDateTime currentTime);
}

