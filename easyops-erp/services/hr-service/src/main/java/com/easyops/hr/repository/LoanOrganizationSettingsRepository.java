package com.easyops.hr.repository;

import com.easyops.hr.entity.LoanOrganizationSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LoanOrganizationSettingsRepository extends JpaRepository<LoanOrganizationSettings, UUID> {
}
