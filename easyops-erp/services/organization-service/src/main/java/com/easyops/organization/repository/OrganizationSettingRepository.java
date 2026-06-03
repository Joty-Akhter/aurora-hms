package com.easyops.organization.repository;

import com.easyops.organization.entity.OrganizationSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Organization Setting Repository
 */
@Repository
public interface OrganizationSettingRepository extends JpaRepository<OrganizationSetting, UUID> {

    List<OrganizationSetting> findByOrganizationId(UUID organizationId);

    Optional<OrganizationSetting> findByOrganizationIdAndSettingKey(UUID organizationId, String settingKey);

    void deleteByOrganizationIdAndSettingKey(UUID organizationId, String settingKey);

    boolean existsByOrganizationIdAndSettingKey(UUID organizationId, String settingKey);
}

