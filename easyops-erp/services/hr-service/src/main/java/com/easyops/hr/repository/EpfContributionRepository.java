package com.easyops.hr.repository;

import com.easyops.hr.entity.EpfContribution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EpfContributionRepository extends JpaRepository<EpfContribution, UUID> {
    
    List<EpfContribution> findByEpfAccountId(UUID epfAccountId);
    
    List<EpfContribution> findByEmployeeId(UUID employeeId);
    
    List<EpfContribution> findByOrganizationId(UUID organizationId);
    
    List<EpfContribution> findByContributionMonthAndContributionYear(Integer month, Integer year);
    
    Optional<EpfContribution> findByEpfAccountIdAndContributionMonthAndContributionYear(
            UUID epfAccountId, Integer month, Integer year);
    
    List<EpfContribution> findByPayrollRunId(UUID payrollRunId);
    
    @Query("SELECT c FROM EpfContribution c WHERE c.organizationId = :organizationId " +
           "AND c.contributionMonth = :month AND c.contributionYear = :year")
    List<EpfContribution> findByOrganizationAndPeriod(
            @Param("organizationId") UUID organizationId,
            @Param("month") Integer month,
            @Param("year") Integer year);
    
    @Query("SELECT c FROM EpfContribution c WHERE c.epfAccountId = :epfAccountId " +
           "AND c.contributionPeriodStart >= :startDate AND c.contributionPeriodEnd <= :endDate")
    List<EpfContribution> findByEpfAccountIdAndContributionDateBetween(
            @Param("epfAccountId") UUID epfAccountId,
            @Param("startDate") java.time.LocalDate startDate,
            @Param("endDate") java.time.LocalDate endDate);
    
    @Query("SELECT c FROM EpfContribution c WHERE c.organizationId = :organizationId " +
           "AND c.contributionPeriodStart >= :startDate AND c.contributionPeriodEnd <= :endDate")
    List<EpfContribution> findByOrganizationIdAndContributionDateBetween(
            @Param("organizationId") UUID organizationId,
            @Param("startDate") java.time.LocalDate startDate,
            @Param("endDate") java.time.LocalDate endDate);
    
    @Query("SELECT c FROM EpfContribution c WHERE c.organizationId = :organizationId " +
           "AND c.contributionYear = :year")
    List<EpfContribution> findByOrganizationIdAndContributionYear(
            @Param("organizationId") UUID organizationId,
            @Param("year") Integer year);
}

