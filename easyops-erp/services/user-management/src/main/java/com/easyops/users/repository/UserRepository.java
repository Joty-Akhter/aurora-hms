package com.easyops.users.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.easyops.users.entity.User;

/**
 * User Repository
 * 
 * Repository interface for User entity operations.
 * 
 * @author EasyOps Team
 * @version 1.0.0
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Find user by username
     * 
     * @param username Username
     * @return Optional User
     */
    Optional<User> findByUsername(String username);

    /**
     * Find user by email
     * 
     * @param email Email address
     * @return Optional User
     */
    Optional<User> findByEmail(String email);

    /**
     * Find user by username or email
     * 
     * @param username Username
     * @param email Email address
     * @return Optional User
     */
    Optional<User> findByUsernameOrEmail(String username, String email);

    /**
     * Check if username exists
     * 
     * @param username Username
     * @return true if exists
     */
    boolean existsByUsername(String username);

    /**
     * Check if email exists
     * 
     * @param email Email address
     * @return true if exists
     */
    boolean existsByEmail(String email);

    /**
     * Find active users
     * 
     * @return List of active users
     */
    List<User> findByIsActiveTrue();

    /**
     * Find users by organization
     * 
     * @param organizationId Organization ID
     * @return List of users
     */
    @Query(value = "SELECT u.* FROM users.users u " +
           "JOIN users.user_organizations uo ON u.id = uo.user_id " +
           "WHERE uo.organization_id = :organizationId", nativeQuery = true)
    List<User> findByOrganizationId(@Param("organizationId") UUID organizationId);

    /**
     * Find users by role
     * 
     * @param roleId Role ID
     * @return List of users
     */
    @Query(value = "SELECT u.* FROM users.users u " +
           "JOIN rbac.user_roles ur ON u.id = ur.user_id " +
           "WHERE ur.role_id = :roleId", nativeQuery = true)
    List<User> findByRoleId(@Param("roleId") UUID roleId);

    /**
     * Search users by name or email
     * 
     * @param searchTerm Search term
     * @param pageable Pagination parameters
     * @return Page of users
     */
    @Query("SELECT u FROM User u WHERE " +
           "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    org.springframework.data.domain.Page<User> searchUsers(@Param("searchTerm") String searchTerm, org.springframework.data.domain.Pageable pageable);

    /**
     * Count active users
     * 
     * @return Count of active users
     */
    long countByIsActiveTrue();

    /**
     * Find users created in date range
     * 
     * @param startDate Start date
     * @param endDate End date
     * @return List of users
     */
    @Query("SELECT u FROM User u WHERE u.createdAt BETWEEN :startDate AND :endDate")
    List<User> findUsersCreatedBetween(@Param("startDate") java.time.LocalDateTime startDate, 
                                     @Param("endDate") java.time.LocalDateTime endDate);
}
