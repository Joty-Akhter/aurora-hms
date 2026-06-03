package com.easyops.hr.service;

import com.easyops.hr.dto.LoanCategoryCreateRequest;
import com.easyops.hr.dto.LoanOrganizationSettingsPatchRequest;
import com.easyops.hr.entity.LoanCategory;
import com.easyops.hr.entity.LoanCategoryType;
import com.easyops.hr.entity.LoanOrganizationSettings;
import com.easyops.hr.repository.LoanCategoryRepository;
import com.easyops.hr.repository.LoanOrganizationSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanConfigurationServiceTest {

    @Mock
    private LoanOrganizationSettingsRepository settingsRepository;

    @Mock
    private LoanCategoryRepository categoryRepository;

    @InjectMocks
    private LoanConfigurationService loanConfigurationService;

    private UUID organizationId;

    @BeforeEach
    void setUp() {
        organizationId = UUID.randomUUID();
    }

    @Test
    void getSettings_retriesWhenConcurrentInsertWins() {
        when(categoryRepository.countByOrganizationId(organizationId)).thenReturn(4L);
        LoanOrganizationSettings existing = existingSettings();
        when(settingsRepository.findById(organizationId))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(existing));
        when(settingsRepository.save(any(LoanOrganizationSettings.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        var dto = loanConfigurationService.getSettings(organizationId);

        assertEquals(organizationId, dto.getOrganizationId());
        verify(settingsRepository, times(2)).findById(organizationId);
    }

    @Test
    void getSettings_createsDefaultsWhenMissing() {
        when(categoryRepository.countByOrganizationId(organizationId)).thenReturn(0L);
        when(categoryRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(settingsRepository.findById(organizationId)).thenReturn(Optional.empty());
        when(settingsRepository.save(any(LoanOrganizationSettings.class))).thenAnswer(inv -> inv.getArgument(0));

        var dto = loanConfigurationService.getSettings(organizationId);

        assertEquals(organizationId, dto.getOrganizationId());
        assertEquals(6, dto.getMinTenureMonths());
        assertEquals(0, new BigDecimal("150000.00").compareTo(dto.getMaxPrincipalAmount()));
        assertEquals("BDT", dto.getCurrency());
        assertTrue(dto.getEnforceSingleActiveLoan());
        assertFalse(dto.getAllowSalaryAdvanceWithActiveTermLoan());

        verify(settingsRepository).save(any(LoanOrganizationSettings.class));
        verify(categoryRepository).saveAll(anyList());
    }

    @Test
    void patchSettings_rejectsInvalidMaxPrincipal() {
        when(categoryRepository.countByOrganizationId(organizationId)).thenReturn(4L);
        when(settingsRepository.findById(organizationId)).thenReturn(Optional.of(existingSettings()));

        LoanOrganizationSettingsPatchRequest patch = new LoanOrganizationSettingsPatchRequest();
        patch.setMaxPrincipalAmount(BigDecimal.ZERO);

        assertThrows(ResponseStatusException.class,
                () -> loanConfigurationService.patchSettings(organizationId, patch));
    }

    @Test
    void createCategory_rejectsDuplicateCode() {
        when(categoryRepository.countByOrganizationId(organizationId)).thenReturn(1L);
        when(categoryRepository.existsByOrganizationIdAndCodeIgnoreCase(organizationId, "X")).thenReturn(true);

        LoanCategoryCreateRequest req = new LoanCategoryCreateRequest();
        req.setCode("x");
        req.setName("Dup");
        req.setCategoryType(LoanCategoryType.TERM_LOAN);

        assertThrows(ResponseStatusException.class,
                () -> loanConfigurationService.createCategory(organizationId, req));
    }

    @Test
    void createCategory_savesNormalizedCode() {
        when(categoryRepository.countByOrganizationId(organizationId)).thenReturn(1L);
        when(categoryRepository.existsByOrganizationIdAndCodeIgnoreCase(organizationId, "NEWCAT")).thenReturn(false);
        when(categoryRepository.save(any(LoanCategory.class))).thenAnswer(inv -> {
            LoanCategory c = inv.getArgument(0);
            c.setCategoryId(UUID.randomUUID());
            return c;
        });

        LoanCategoryCreateRequest req = new LoanCategoryCreateRequest();
        req.setCode(" newcat ");
        req.setName("New");
        req.setCategoryType(LoanCategoryType.TERM_LOAN);

        var dto = loanConfigurationService.createCategory(organizationId, req);
        assertEquals("NEWCAT", dto.getCode());

        ArgumentCaptor<LoanCategory> captor = ArgumentCaptor.forClass(LoanCategory.class);
        verify(categoryRepository).save(captor.capture());
        assertEquals("NEWCAT", captor.getValue().getCode());
    }

    private LoanOrganizationSettings existingSettings() {
        LoanOrganizationSettings s = new LoanOrganizationSettings();
        s.setOrganizationId(organizationId);
        s.setMinTenureMonths(6);
        s.setMaxPrincipalAmount(new BigDecimal("150000.00"));
        s.setCurrency("BDT");
        s.setEnforceSingleActiveLoan(true);
        s.setAllowSalaryAdvanceWithActiveTermLoan(false);
        return s;
    }
}
