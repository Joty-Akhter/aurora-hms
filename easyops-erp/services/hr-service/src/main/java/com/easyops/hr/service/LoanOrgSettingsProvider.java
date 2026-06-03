package com.easyops.hr.service;

import com.easyops.hr.dto.LoanOrganizationSettingsDto;

import java.util.UUID;

/**
 * Abstraction for loading org loan settings + default category seed (Phase 1 context).
 * Implemented by {@link LoanConfigurationService}; mockable in tests.
 */
public interface LoanOrgSettingsProvider {

    LoanOrganizationSettingsDto getSettings(UUID organizationId);
}
