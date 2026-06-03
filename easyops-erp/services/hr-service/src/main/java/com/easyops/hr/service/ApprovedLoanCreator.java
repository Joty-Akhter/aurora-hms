package com.easyops.hr.service;

import com.easyops.hr.entity.LoanApplication;

/**
 * Phase 3: creates loan account when an application is approved. Interface for testability.
 */
public interface ApprovedLoanCreator {

    void createLoanFromApprovedApplication(LoanApplication application);
}
