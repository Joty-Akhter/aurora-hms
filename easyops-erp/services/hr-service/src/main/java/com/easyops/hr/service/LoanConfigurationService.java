package com.easyops.hr.service;

import com.easyops.hr.dto.*;
import com.easyops.hr.entity.LoanCategory;
import com.easyops.hr.util.LoanJsonUtil;
import com.easyops.hr.entity.LoanCategoryType;
import com.easyops.hr.entity.LoanHolidayShiftMode;
import com.easyops.hr.entity.LoanInterestMethod;
import com.easyops.hr.entity.LoanOrganizationSettings;
import com.easyops.hr.repository.LoanCategoryRepository;
import com.easyops.hr.repository.LoanOrganizationSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LoanConfigurationService implements LoanOrgSettingsProvider {

    private final LoanOrganizationSettingsRepository settingsRepository;
    private final LoanCategoryRepository categoryRepository;

    @Override
    @Transactional
    public LoanOrganizationSettingsDto getSettings(UUID organizationId) {
        ensureDefaultLoanCategories(organizationId);
        return toSettingsDto(findOrCreateSettings(organizationId));
    }

    @Transactional
    public LoanOrganizationSettingsDto patchSettings(UUID organizationId, LoanOrganizationSettingsPatchRequest patch) {
        ensureDefaultLoanCategories(organizationId);
        LoanOrganizationSettings entity = findOrCreateSettings(organizationId);

        if (patch.getMinTenureMonths() != null) {
            if (patch.getMinTenureMonths() < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "minTenureMonths must be >= 0");
            }
            entity.setMinTenureMonths(patch.getMinTenureMonths());
        }
        if (patch.getMaxPrincipalAmount() != null) {
            if (patch.getMaxPrincipalAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "maxPrincipalAmount must be > 0");
            }
            entity.setMaxPrincipalAmount(patch.getMaxPrincipalAmount());
        }
        if (patch.getCurrency() != null) {
            String c = patch.getCurrency().trim();
            if (c.length() != 3) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "currency must be a 3-letter code");
            }
            entity.setCurrency(c.toUpperCase());
        }
        if (patch.getEnforceSingleActiveLoan() != null) {
            entity.setEnforceSingleActiveLoan(patch.getEnforceSingleActiveLoan());
        }
        if (patch.getAllowSalaryAdvanceWithActiveTermLoan() != null) {
            entity.setAllowSalaryAdvanceWithActiveTermLoan(patch.getAllowSalaryAdvanceWithActiveTermLoan());
        }
        if (patch.getDisqualifyingEmploymentStatuses() != null) {
            entity.setDisqualifyingEmploymentStatusesJson(
                    LoanJsonUtil.toJsonStringList(patch.getDisqualifyingEmploymentStatuses()));
        }
        if (patch.getSettlementAllocationPriority() != null) {
            entity.setSettlementAllocationPriorityJson(
                    LoanJsonUtil.toJsonStringList(patch.getSettlementAllocationPriority()));
        }
        if (patch.getEnforceSettlementAllocationOrder() != null) {
            entity.setEnforceSettlementAllocationOrder(patch.getEnforceSettlementAllocationOrder());
        }
        if (patch.getSkipFinanceApproval() != null) {
            entity.setSkipFinanceApproval(patch.getSkipFinanceApproval());
        }
        if (patch.getSalaryAdvanceSkipFinanceApproval() != null) {
            entity.setSalaryAdvanceSkipFinanceApproval(patch.getSalaryAdvanceSkipFinanceApproval());
        }
        if (patch.getShiftInstallmentDueDatesForHolidays() != null) {
            entity.setShiftInstallmentDueDatesForHolidays(patch.getShiftInstallmentDueDatesForHolidays());
        }
        if (patch.getLoanHolidayShiftMode() != null) {
            entity.setLoanHolidayShiftMode(patch.getLoanHolidayShiftMode());
        }

        return toSettingsDto(settingsRepository.save(entity));
    }

    @Transactional
    public List<LoanCategoryDto> listCategories(UUID organizationId, boolean includeInactive) {
        ensureDefaultLoanCategories(organizationId);
        List<LoanCategory> list = categoryRepository.findByOrganizationIdOrderBySortOrderAscCodeAsc(organizationId);
        return list.stream()
                .filter(c -> includeInactive || Boolean.TRUE.equals(c.getIsActive()))
                .map(this::toCategoryDto)
                .toList();
    }

    @Transactional
    public LoanCategoryDto getCategory(UUID organizationId, UUID categoryId) {
        ensureDefaultLoanCategories(organizationId);
        return categoryRepository.findByCategoryIdAndOrganizationId(categoryId, organizationId)
                .map(this::toCategoryDto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Loan category not found"));
    }

    @Transactional
    public LoanCategoryDto createCategory(UUID organizationId, LoanCategoryCreateRequest request) {
        ensureDefaultLoanCategories(organizationId);
        String code = normalizeCode(request.getCode());
        if (categoryRepository.existsByOrganizationIdAndCodeIgnoreCase(organizationId, code)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Loan category code already exists for this organization");
        }
        validateCategoryCaps(request.getMaxPrincipalAmount(), request.getMaxTenureMonths());

        LoanCategory c = new LoanCategory();
        c.setOrganizationId(organizationId);
        c.setCode(code);
        c.setName(request.getName().trim());
        c.setDescription(trimToNull(request.getDescription()));
        c.setCategoryType(request.getCategoryType());
        c.setIsActive(true);
        c.setSortOrder(Objects.requireNonNullElse(request.getSortOrder(), 0));
        c.setMaxPrincipalAmount(request.getMaxPrincipalAmount());
        c.setMaxTenureMonths(request.getMaxTenureMonths());
        c.setInterestMethod(Objects.requireNonNullElse(request.getInterestMethod(), LoanInterestMethod.NONE));
        c.setFlatAnnualRatePercent(request.getFlatAnnualRatePercent());
        validateInterestMethod(c.getInterestMethod(), c.getFlatAnnualRatePercent());

        return toCategoryDto(categoryRepository.save(c));
    }

    @Transactional
    public LoanCategoryDto updateCategory(UUID organizationId, UUID categoryId, LoanCategoryUpdateRequest request) {
        ensureDefaultLoanCategories(organizationId);
        LoanCategory c = categoryRepository.findByCategoryIdAndOrganizationId(categoryId, organizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Loan category not found"));
        validateCategoryCaps(request.getMaxPrincipalAmount(), request.getMaxTenureMonths());

        c.setName(request.getName().trim());
        c.setDescription(trimToNull(request.getDescription()));
        c.setCategoryType(request.getCategoryType());
        c.setIsActive(request.getIsActive());
        c.setSortOrder(request.getSortOrder());
        c.setMaxPrincipalAmount(request.getMaxPrincipalAmount());
        c.setMaxTenureMonths(request.getMaxTenureMonths());
        if (request.getInterestMethod() != null) {
            c.setInterestMethod(request.getInterestMethod());
        }
        if (request.getFlatAnnualRatePercent() != null) {
            c.setFlatAnnualRatePercent(request.getFlatAnnualRatePercent());
        }
        validateInterestMethod(c.getInterestMethod(), c.getFlatAnnualRatePercent());

        return toCategoryDto(categoryRepository.save(c));
    }

    @Transactional
    public void deactivateCategory(UUID organizationId, UUID categoryId) {
        ensureDefaultLoanCategories(organizationId);
        LoanCategory c = categoryRepository.findByCategoryIdAndOrganizationId(categoryId, organizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Loan category not found"));
        c.setIsActive(false);
        categoryRepository.save(c);
    }

    /**
     * Seeds default loan categories (LC-02) when the organization has none.
     */
    private void ensureDefaultLoanCategories(UUID organizationId) {
        if (categoryRepository.countByOrganizationId(organizationId) > 0) {
            return;
        }
        List<LoanCategory> seed = List.of(
                categorySeed(organizationId, "EMERGENCY", "Emergency Loan", "Emergency Loan", LoanCategoryType.TERM_LOAN, 1),
                categorySeed(organizationId, "STAFF", "Staff Loan", "Staff Loan", LoanCategoryType.TERM_LOAN, 2),
                categorySeed(organizationId, "MOTORCYCLE", "Motorcycle Loan", "Motorcycle Loan", LoanCategoryType.TERM_LOAN, 3),
                categorySeed(organizationId, "SALARY_ADVANCE", "Salary Advance", "Salary Advance", LoanCategoryType.SALARY_ADVANCE, 4)
        );
        try {
            categoryRepository.saveAll(seed);
        } catch (DataIntegrityViolationException ex) {
            if (categoryRepository.countByOrganizationId(organizationId) == 0) {
                throw ex;
            }
        }
    }

    private LoanOrganizationSettings findOrCreateSettings(UUID organizationId) {
        Optional<LoanOrganizationSettings> existing = settingsRepository.findById(organizationId);
        if (existing.isPresent()) {
            return existing.get();
        }
        try {
            return createDefaultSettingsRow(organizationId);
        } catch (DataIntegrityViolationException ex) {
            return settingsRepository.findById(organizationId)
                    .orElseThrow(() -> ex);
        }
    }

    private LoanCategory categorySeed(UUID organizationId, String code, String name, String description,
                                      LoanCategoryType type, int sortOrder) {
        LoanCategory c = new LoanCategory();
        c.setOrganizationId(organizationId);
        c.setCode(code);
        c.setName(name);
        c.setDescription(description);
        c.setCategoryType(type);
        c.setIsActive(true);
        c.setSortOrder(sortOrder);
        c.setInterestMethod(LoanInterestMethod.NONE);
        return c;
    }

    private void validateInterestMethod(LoanInterestMethod method, BigDecimal flatRate) {
        if (method == null) {
            return;
        }
        if (method == LoanInterestMethod.FLAT) {
            if (flatRate == null || flatRate.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "flatAnnualRatePercent must be > 0 when interestMethod is FLAT");
            }
        }
    }

    private static String scheduleInterestNote(LoanInterestMethod method) {
        if (method == null || method == LoanInterestMethod.NONE) {
            return "Equal principal; zero interest (v1).";
        }
        return "Interest method " + method
                + " is stored on the category; schedule generation still uses equal principal with zero interest until the interest engine is enabled.";
    }

    private LoanOrganizationSettings createDefaultSettingsRow(UUID organizationId) {
        LoanOrganizationSettings s = new LoanOrganizationSettings();
        s.setOrganizationId(organizationId);
        s.setMinTenureMonths(6);
        s.setMaxPrincipalAmount(new BigDecimal("150000.00"));
        s.setCurrency("BDT");
        s.setEnforceSingleActiveLoan(true);
        s.setAllowSalaryAdvanceWithActiveTermLoan(false);
        s.setDisqualifyingEmploymentStatusesJson("[\"LONG_TERM_SUSPENSION\",\"SUSPENDED\"]");
        s.setSettlementAllocationPriorityJson("[\"PF_SETTLEMENT\",\"FINAL_SALARY\",\"OTHER_DUES\"]");
        s.setEnforceSettlementAllocationOrder(true);
        s.setSkipFinanceApproval(false);
        s.setSalaryAdvanceSkipFinanceApproval(true);
        return settingsRepository.save(s);
    }

    private void validateCategoryCaps(BigDecimal maxPrincipal, Integer maxTenureMonths) {
        if (maxPrincipal != null && maxPrincipal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category maxPrincipalAmount must be > 0 when set");
        }
        if (maxTenureMonths != null && maxTenureMonths <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category maxTenureMonths must be > 0 when set");
        }
    }

    private static String normalizeCode(String code) {
        if (code == null || code.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "code is required");
        }
        return code.trim().toUpperCase();
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private LoanOrganizationSettingsDto toSettingsDto(LoanOrganizationSettings e) {
        return LoanOrganizationSettingsDto.builder()
                .organizationId(e.getOrganizationId())
                .minTenureMonths(e.getMinTenureMonths())
                .maxPrincipalAmount(e.getMaxPrincipalAmount())
                .currency(e.getCurrency())
                .enforceSingleActiveLoan(e.getEnforceSingleActiveLoan())
                .allowSalaryAdvanceWithActiveTermLoan(e.getAllowSalaryAdvanceWithActiveTermLoan())
                .disqualifyingEmploymentStatuses(LoanJsonUtil.parseStringList(e.getDisqualifyingEmploymentStatusesJson()))
                .settlementAllocationPriority(LoanJsonUtil.parseStringList(e.getSettlementAllocationPriorityJson()))
                .enforceSettlementAllocationOrder(e.getEnforceSettlementAllocationOrder())
                .skipFinanceApproval(e.getSkipFinanceApproval())
                .salaryAdvanceSkipFinanceApproval(e.getSalaryAdvanceSkipFinanceApproval())
                .shiftInstallmentDueDatesForHolidays(e.getShiftInstallmentDueDatesForHolidays())
                .loanHolidayShiftMode(e.getLoanHolidayShiftMode() != null ? e.getLoanHolidayShiftMode() : LoanHolidayShiftMode.NEXT_BUSINESS_DAY)
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }

    private LoanCategoryDto toCategoryDto(LoanCategory c) {
        LoanInterestMethod im = c.getInterestMethod() != null ? c.getInterestMethod() : LoanInterestMethod.NONE;
        return LoanCategoryDto.builder()
                .categoryId(c.getCategoryId())
                .organizationId(c.getOrganizationId())
                .code(c.getCode())
                .name(c.getName())
                .description(c.getDescription())
                .categoryType(c.getCategoryType())
                .isActive(c.getIsActive())
                .sortOrder(c.getSortOrder())
                .maxPrincipalAmount(c.getMaxPrincipalAmount())
                .maxTenureMonths(c.getMaxTenureMonths())
                .interestMethod(im)
                .flatAnnualRatePercent(c.getFlatAnnualRatePercent())
                .scheduleInterestNote(scheduleInterestNote(im))
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}
