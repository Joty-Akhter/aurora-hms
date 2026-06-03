package com.easyops.hr.service;

import com.easyops.hr.entity.EpfComplianceRecord;
import com.easyops.hr.entity.EpfContribution;
import com.easyops.hr.entity.EpfAuditEvent;
import com.easyops.hr.entity.EpfFiling;
import com.easyops.hr.repository.EpfAuditEventRepository;
import com.easyops.hr.repository.EpfComplianceRecordRepository;
import com.easyops.hr.repository.EpfContributionRepository;
import com.easyops.hr.repository.EpfFilingRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProvidentFundFilingService {

    private final EpfContributionRepository epfContributionRepository;
    private final EpfFilingRepository epfFilingRepository;
    private final EpfComplianceRecordRepository epfComplianceRecordRepository;
    private final EpfAuditEventRepository epfAuditEventRepository;
    private final ObjectMapper objectMapper;

    public EpfFiling generateEcr(UUID organizationId, Integer month, Integer year, String actor) {
        List<EpfContribution> contributions = fetchContributions(organizationId, month, year);
        Totals totals = aggregate(contributions);
        String artifact = buildEcrCsv(contributions);
        EpfFiling filing = upsertFiling(organizationId, month, year, "ECR", "CSV", artifact, totals, actor);
        appendAuditEvent(
                filing.getOrganizationId(),
                null,
                null,
                "filing",
                filing.getFilingId(),
                "filing_generated_ecr",
                actor,
                Map.of("month", month, "year", year, "liabilityAmount", nvl(filing.getTotalLiabilityAmount()))
        );
        return filing;
    }

    public EpfFiling generateChallan(UUID organizationId, Integer month, Integer year, String actor) {
        List<EpfContribution> contributions = fetchContributions(organizationId, month, year);
        Totals totals = aggregate(contributions);
        String artifact = buildChallanSummary(organizationId, month, year, totals, contributions.size());
        EpfFiling filing = upsertFiling(organizationId, month, year, "CHALLAN", "TEXT", artifact, totals, actor);
        appendAuditEvent(
                filing.getOrganizationId(),
                null,
                null,
                "filing",
                filing.getFilingId(),
                "filing_generated_challan",
                actor,
                Map.of("month", month, "year", year, "liabilityAmount", nvl(filing.getTotalLiabilityAmount()))
        );
        return filing;
    }

    public EpfFiling submitFiling(UUID filingId, String reference, LocalDate submissionDate, String actor) {
        EpfFiling filing = getFilingById(filingId);
        if (!"generated".equalsIgnoreCase(filing.getFilingStatus())) {
            throw new RuntimeException("Only generated filings can be submitted. Current status: " + filing.getFilingStatus());
        }
        if (reference == null || reference.isBlank()) {
            throw new RuntimeException("submissionReference is required to submit filing");
        }
        filing.setFilingStatus("submitted");
        filing.setSubmissionReference(reference);
        filing.setSubmissionDate(submissionDate != null ? submissionDate : LocalDate.now());
        filing.setUpdatedBy(actor);
        EpfFiling saved = epfFilingRepository.save(filing);

        EpfComplianceRecord record = getComplianceRecord(saved);
        record.setStatus("submitted");
        record.setSubmissionDate(saved.getSubmissionDate());
        record.setFileReference(reference);
        record.setUpdatedBy(actor);
        epfComplianceRecordRepository.save(record);
        appendAuditEvent(
                saved.getOrganizationId(),
                null,
                null,
                "filing",
                saved.getFilingId(),
                "filing_submitted",
                actor,
                Map.of("submissionReference", reference, "submissionDate", saved.getSubmissionDate() != null ? saved.getSubmissionDate().toString() : "")
        );
        return saved;
    }

    public EpfFiling verifyFiling(UUID filingId, String actor) {
        EpfFiling filing = getFilingById(filingId);
        if (!"submitted".equalsIgnoreCase(filing.getFilingStatus())) {
            throw new RuntimeException("Only submitted filings can be verified. Current status: " + filing.getFilingStatus());
        }
        filing.setFilingStatus("verified");
        filing.setVerifiedDate(LocalDate.now());
        filing.setUpdatedBy(actor);
        EpfFiling saved = epfFilingRepository.save(filing);

        EpfComplianceRecord record = getComplianceRecord(saved);
        record.setStatus("verified");
        record.setUpdatedBy(actor);
        epfComplianceRecordRepository.save(record);
        appendAuditEvent(
                saved.getOrganizationId(),
                null,
                null,
                "filing",
                saved.getFilingId(),
                "filing_verified",
                actor,
                Map.of("verifiedDate", saved.getVerifiedDate() != null ? saved.getVerifiedDate().toString() : "")
        );
        return saved;
    }

    public List<EpfFiling> getFilingsByOrganization(UUID organizationId, Integer month, Integer year) {
        if (month != null && year != null) {
            return epfFilingRepository.findByOrganizationIdAndFilingMonthAndFilingYear(organizationId, month, year);
        }
        return epfFilingRepository.findByOrganizationIdOrderByFilingYearDescFilingMonthDescCreatedAtDesc(organizationId);
    }

    public EpfFiling getFilingById(UUID filingId) {
        return epfFilingRepository.findById(filingId)
                .orElseThrow(() -> new RuntimeException("EPF filing not found: " + filingId));
    }

    private EpfFiling upsertFiling(UUID organizationId, Integer month, Integer year, String filingType, String format,
                                   String artifactContent, Totals totals, String actor) {
        EpfFiling filing = epfFilingRepository
                .findByOrganizationIdAndFilingMonthAndFilingYearAndFilingType(organizationId, month, year, filingType)
                .orElseGet(() -> EpfFiling.builder()
                        .organizationId(organizationId)
                        .filingMonth(month)
                        .filingYear(year)
                        .filingType(filingType)
                        .build());

        EpfComplianceRecord compliance = ensureComplianceRecord(organizationId, month, year, filingType, totals.totalLiability());

        filing.setFilingStatus("generated");
        filing.setArtifactFormat(format);
        filing.setArtifactContent(artifactContent);
        filing.setArtifactChecksum(sha256(artifactContent));
        filing.setComplianceRecordId(compliance.getComplianceRecordId());
        filing.setEmployeeContributionTotal(totals.employeeTotal());
        filing.setEmployerContributionTotal(totals.employerTotal());
        filing.setEmployerPensionTotal(totals.employerPensionTotal());
        filing.setEmployerEdliTotal(totals.employerEdliTotal());
        filing.setEmployerAdminChargeTotal(totals.employerAdminTotal());
        filing.setTotalLiabilityAmount(totals.totalLiability());
        filing.setCreatedBy(filing.getCreatedBy() == null ? actor : filing.getCreatedBy());
        filing.setUpdatedBy(actor);

        return epfFilingRepository.save(filing);
    }

    private EpfComplianceRecord ensureComplianceRecord(UUID organizationId, Integer month, Integer year,
                                                       String filingType, BigDecimal amount) {
        LocalDate periodStart = LocalDate.of(year, month, 1);
        LocalDate periodEnd = periodStart.withDayOfMonth(periodStart.lengthOfMonth());
        LocalDate dueDate = periodEnd.plusDays(15);
        String complianceType = "ECR".equals(filingType) ? "monthly_return" : "challan";

        return epfComplianceRecordRepository.findByOrganizationIdAndComplianceMonthAndComplianceYear(organizationId, month, year).stream()
                .filter(r -> complianceType.equalsIgnoreCase(r.getComplianceType()))
                .findFirst()
                .map(existing -> {
                    existing.setAmount(amount);
                    existing.setDueDate(dueDate);
                    if (existing.getStatus() == null || existing.getStatus().isBlank()) {
                        existing.setStatus("pending");
                    }
                    return epfComplianceRecordRepository.save(existing);
                })
                .orElseGet(() -> epfComplianceRecordRepository.save(EpfComplianceRecord.builder()
                        .organizationId(organizationId)
                        .complianceType(complianceType)
                        .compliancePeriodStart(periodStart)
                        .compliancePeriodEnd(periodEnd)
                        .dueDate(dueDate)
                        .status("pending")
                        .amount(amount)
                        .build()));
    }

    private EpfComplianceRecord getComplianceRecord(EpfFiling filing) {
        if (filing.getComplianceRecordId() == null) {
            throw new RuntimeException("No compliance record linked to filing: " + filing.getFilingId());
        }
        return epfComplianceRecordRepository.findById(filing.getComplianceRecordId())
                .orElseThrow(() -> new RuntimeException("Compliance record not found: " + filing.getComplianceRecordId()));
    }

    private List<EpfContribution> fetchContributions(UUID organizationId, Integer month, Integer year) {
        List<EpfContribution> contributions = epfContributionRepository.findByOrganizationAndPeriod(organizationId, month, year);
        if (contributions.isEmpty()) {
            throw new RuntimeException("No EPF contributions found for " + month + "/" + year);
        }
        return contributions;
    }

    private Totals aggregate(List<EpfContribution> contributions) {
        BigDecimal employeeTotal = contributions.stream()
                .map(c -> nvl(c.getEmployeeContributionAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal employerTotal = contributions.stream()
                .map(c -> nvl(c.getEmployerContributionAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal pensionTotal = contributions.stream()
                .map(c -> nvl(c.getEmployerPensionAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal edliTotal = contributions.stream()
                .map(c -> nvl(c.getEmployerEdliAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal adminTotal = contributions.stream()
                .map(c -> nvl(c.getEmployerAdminChargeAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalLiability = employeeTotal.add(employerTotal).add(edliTotal).add(adminTotal);
        return new Totals(employeeTotal, employerTotal, pensionTotal, edliTotal, adminTotal, totalLiability);
    }

    private String buildEcrCsv(List<EpfContribution> contributions) {
        StringBuilder sb = new StringBuilder();
        sb.append("employee_id,epf_account_id,month,year,pf_wage,employee_pf,employer_pf,eps,edli,admin_charge,total_contribution\n");
        for (EpfContribution c : contributions) {
            sb.append(c.getEmployeeId()).append(",")
                    .append(c.getEpfAccountId()).append(",")
                    .append(c.getContributionMonth()).append(",")
                    .append(c.getContributionYear()).append(",")
                    .append(nvl(c.getPfWageBase())).append(",")
                    .append(nvl(c.getEmployeeContributionAmount())).append(",")
                    .append(nvl(c.getEmployerEpfAmount())).append(",")
                    .append(nvl(c.getEmployerPensionAmount())).append(",")
                    .append(nvl(c.getEmployerEdliAmount())).append(",")
                    .append(nvl(c.getEmployerAdminChargeAmount())).append(",")
                    .append(nvl(c.getTotalContribution()))
                    .append("\n");
        }
        return sb.toString();
    }

    private String buildChallanSummary(UUID organizationId, Integer month, Integer year, Totals totals, int employeeCount) {
        return "EPF CHALLAN\n" +
                "organization_id=" + organizationId + "\n" +
                "period=" + month + "/" + year + "\n" +
                "employee_count=" + employeeCount + "\n" +
                "employee_pf_total=" + totals.employeeTotal() + "\n" +
                "employer_pf_total=" + totals.employerTotal() + "\n" +
                "eps_total=" + totals.employerPensionTotal() + "\n" +
                "edli_total=" + totals.employerEdliTotal() + "\n" +
                "admin_charge_total=" + totals.employerAdminTotal() + "\n" +
                "total_liability=" + totals.totalLiability() + "\n";
    }

    private static BigDecimal nvl(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private void appendAuditEvent(UUID organizationId, UUID epfAccountId, UUID employeeId, String entityType, UUID entityId,
                                  String eventType, String actorUserId, Map<String, Object> eventData) {
        String data;
        try {
            data = objectMapper.writeValueAsString(eventData);
        } catch (JsonProcessingException e) {
            data = "{\"serializationError\":\"" + e.getMessage() + "\"}";
        }
        EpfAuditEvent event = EpfAuditEvent.builder()
                .organizationId(organizationId)
                .epfAccountId(epfAccountId)
                .employeeId(employeeId)
                .entityType(entityType)
                .entityId(entityId)
                .eventType(eventType)
                .actorUserId(actorUserId)
                .eventData(data)
                .build();
        epfAuditEventRepository.save(event);
    }

    private String sha256(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }

    private record Totals(
            BigDecimal employeeTotal,
            BigDecimal employerTotal,
            BigDecimal employerPensionTotal,
            BigDecimal employerEdliTotal,
            BigDecimal employerAdminTotal,
            BigDecimal totalLiability
    ) {}
}
