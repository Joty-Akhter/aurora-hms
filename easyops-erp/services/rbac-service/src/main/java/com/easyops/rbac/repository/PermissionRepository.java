package com.easyops.rbac.repository;

import com.easyops.rbac.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Permission Repository
 * 
 * Repository interface for Permission entity operations.
 * 
 * @author EasyOps Team
 * @version 1.0.0
 */
@Repository
public interface PermissionRepository extends JpaRepository<Permission, UUID> {

    Optional<Permission> findByCode(String code);
    
    boolean existsByCode(String code);
    
    List<Permission> findByIsActiveTrue();
    
    List<Permission> findByResource(String resource);
    
    List<Permission> findByResourceAndAction(String resource, String action);
    
    @Query("SELECT p FROM Permission p WHERE " +
           "LOWER(p.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(p.code) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(p.resource) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Permission> searchPermissions(@Param("searchTerm") String searchTerm);
}

