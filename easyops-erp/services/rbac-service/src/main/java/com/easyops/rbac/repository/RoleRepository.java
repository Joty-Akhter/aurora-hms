package com.easyops.rbac.repository;

import com.easyops.rbac.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Role Repository
 * 
 * Repository interface for Role entity operations.
 * 
 * @author EasyOps Team
 * @version 1.0.0
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {

    Optional<Role> findByCode(String code);
    
    boolean existsByCode(String code);
    
    List<Role> findByIsActiveTrue();
    
    List<Role> findByIsSystemRoleTrue();
    

    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO rbac.role_permissions (role_id, permission_id)
        SELECT r.id, p.id
        FROM rbac.roles r
        JOIN rbac.permissions p ON 1=1
        WHERE r.code = :roleCode
          AND NOT EXISTS (
            SELECT 1 FROM rbac.role_permissions rp
            WHERE rp.role_id = r.id AND rp.permission_id = p.id
        )
        """, nativeQuery = true)
    int assignAllPermissionsToRole(@Param("roleCode") String roleCode);

    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO rbac.role_permissions (role_id, permission_id)
        SELECT r.id, :permissionId
        FROM rbac.roles r
        WHERE r.code = :roleCode
        ON CONFLICT (role_id, permission_id) DO NOTHING
        """, nativeQuery = true)
    void assignPermissionToRole(@Param("roleCode") String roleCode, @Param("permissionId") UUID permissionId);

    @Query("SELECT r FROM Role r WHERE " +
           "LOWER(r.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(r.code) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Role> searchRoles(@Param("searchTerm") String searchTerm);
}

