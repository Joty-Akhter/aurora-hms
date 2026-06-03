package com.easyops.hr.service;

import com.easyops.hr.entity.Benefit;
import com.easyops.hr.entity.EmployeeBenefit;
import com.easyops.hr.repository.BenefitRepository;
import com.easyops.hr.repository.EmployeeBenefitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BenefitService {
    
    private final BenefitRepository benefitRepository;
    private final EmployeeBenefitRepository employeeBenefitRepository;
    
    // Benefit Methods
    public List<Benefit> getAllBenefits(UUID organizationId) {
        return benefitRepository.findByOrganizationId(organizationId);
    }
    
    public Benefit getBenefitById(UUID benefitId) {
        return benefitRepository.findById(benefitId)
                .orElseThrow(() -> new RuntimeException("Benefit not found"));
    }
    
    public Benefit createBenefit(Benefit benefit) {
        log.info("Creating benefit: {}", benefit.getBenefitName());
        return benefitRepository.save(benefit);
    }
    
    public Benefit updateBenefit(UUID benefitId, Benefit benefit) {
        Benefit existing = getBenefitById(benefitId);
        
        if (benefit.getBenefitName() != null) existing.setBenefitName(benefit.getBenefitName());
        if (benefit.getDescription() != null) existing.setDescription(benefit.getDescription());
        if (benefit.getProviderName() != null) existing.setProviderName(benefit.getProviderName());
        if (benefit.getEmployerContribution() != null) existing.setEmployerContribution(benefit.getEmployerContribution());
        if (benefit.getEmployeeContribution() != null) existing.setEmployeeContribution(benefit.getEmployeeContribution());
        if (benefit.getIsActive() != null) existing.setIsActive(benefit.getIsActive());
        
        return benefitRepository.save(existing);
    }
    
    // Employee Benefit Methods
    public List<EmployeeBenefit> getEmployeeBenefits(UUID employeeId, UUID organizationId) {
        return employeeBenefitRepository.findByEmployeeIdAndOrganizationId(employeeId, organizationId);
    }

    public EmployeeBenefit getEmployeeBenefitById(UUID employeeBenefitId) {
        return employeeBenefitRepository.findById(employeeBenefitId)
                .orElseThrow(() -> new RuntimeException("Employee benefit enrollment not found"));
    }
    
    public EmployeeBenefit enrollBenefit(EmployeeBenefit enrollment) {
        log.info("Enrolling employee {} in benefit {}", enrollment.getEmployeeId(), enrollment.getBenefitId());
        enrollment.setStatus("active");
        return employeeBenefitRepository.save(enrollment);
    }
    
    public EmployeeBenefit updateBenefitEnrollment(UUID enrollmentId, EmployeeBenefit enrollment) {
        EmployeeBenefit existing = employeeBenefitRepository.findById(enrollmentId)
                .orElseThrow(() -> new RuntimeException("Employee benefit enrollment not found"));
        
        if (enrollment.getStatus() != null) existing.setStatus(enrollment.getStatus());
        if (enrollment.getEndDate() != null) existing.setEndDate(enrollment.getEndDate());
        if (enrollment.getBeneficiaryName() != null) existing.setBeneficiaryName(enrollment.getBeneficiaryName());
        if (enrollment.getNotes() != null) existing.setNotes(enrollment.getNotes());
        
        return employeeBenefitRepository.save(existing);
    }
}

