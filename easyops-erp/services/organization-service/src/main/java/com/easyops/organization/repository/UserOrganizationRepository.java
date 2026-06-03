package com.easyops.organization.repository;

import com.easyops.organization.entity.UserOrganization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * User Organization Repository
 */
@Repository
public interface UserOrganizationRepository extends JpaRepository<UserOrganization, UUID> {

    List<UserOrganization> findByUserId(UUID userId);

    List<UserOrganization> findByOrganizationId(UUID organizationId);

    Optional<UserOrganization> findByUserIdAndOrganizationId(UUID userId, UUID organizationId);

    List<UserOrganization> findByUserIdAndStatus(UUID userId, String status);

    @Query("SELECT uo FROM UserOrganization uo WHERE uo.userId = :userId AND uo.isPrimary = true")
    Optional<UserOrganization> findPrimaryOrganization(UUID userId);

    @Modifying
    @Query("UPDATE UserOrganization uo SET uo.isPrimary = false WHERE uo.userId = :userId")
    void clearPrimaryOrganization(UUID userId);

    boolean existsByUserIdAndOrganizationId(UUID userId, UUID organizationId);
}

