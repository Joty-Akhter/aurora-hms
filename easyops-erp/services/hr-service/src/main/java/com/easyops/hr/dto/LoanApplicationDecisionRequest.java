package com.easyops.hr.dto;

import lombok.Data;

import java.time.LocalDate;

/** Optional comment when approving (AL-03 audit). */
@Data
public class LoanApplicationDecisionRequest {

    private String comment;
    /** AD-02: optional expiry for an approved limit exception (HR sets on HR approval step). */
    private LocalDate limitOverrideExpiresAt;
    /** LC-05: optional expiry for facility exception. */
    private LocalDate facilityOverrideExpiresAt;
}
