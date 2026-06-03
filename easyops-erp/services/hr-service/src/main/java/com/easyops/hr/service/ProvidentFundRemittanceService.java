package com.easyops.hr.service;

import com.easyops.hr.entity.EpfComplianceRecord;
import com.easyops.hr.entity.EpfContribution;
import com.easyops.hr.entity.EpfFiling;
import com.easyops.hr.entity.EpfRemittance;
import com.easyops.hr.entity.EpfAuditEvent;
import com.easyops.hr.repository.EpfAuditEventRepository;
import com.easyops.hr.repository.EpfComplianceRecordRepository;
import com.easyops.hr.repository.EpfContributionRepository;
import com.easyops.hr.repository.EpfFilingRepository;
import com.easyops.hr.repository.EpfRemittanceRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProvidentFundRemittanceService {

    private final EpfRemittanceRepository epfRemittanceRepository;
    private final EpfContributionRepository epfContributionRepository;
    private final EpfFilingRepository epfFilingRepository;
    private final EpfComplianceRecordRepository epfComplianceRecordRepository;
    private final EpfAuditEventRepository epfAuditEventRepository;
    private final ObjectMapper objectMapper;

    public EpfRemittance registerAccountingPosting(UUID organizationId, Integer month, Integer year,
                                                   BigDecimal liabilityAmount, String accountingReference, String actor) {
        EpfRemittance remittance = epfRemittanceRepository
                .findFirstByOrganizationIdAndRemittanceMonthAndRemittanceYear(organizationId, month, year)
                .orElseGet(() -> EpfRemittance.builder()
                        .organizationId(organizationId)
                        .remittanceMonth(month)
                        .remittanceYear(year)
                        .build());

        remittance.setLiabilityAmount(liabilityAmount != null ? liabilityAmount : BigDecimal.ZERO);
        remittance.setAccountingReference(accountingReference);
        remittance.setAccountingPostedDate(LocalDate.now());
        remittance.setStatus("posted_to_accounting");
        remittance.setCreatedBy(remittance.getCreatedBy() == null ? actor : remittance.getCreatedBy());
        remittance.setUpdatedBy(actor);

        attachPeriodReferences(remittance);
        EpfRemittance saved = epfRemittanceRepository.save(remittance);
        appendAuditEvent(
                saved.getOrganizationId(),
                null,
                null,
                "remittance",
                saved.getRemittanceId(),
                "remittance_posted_to_accounting",
                actor,
                Map.of("month", month, "year", year, "liabilityAmount", saved.getLiabilityAmount() != null ? saved.getLiabilityAmount() : BigDecimal.ZERO)
        );
        return saved;
    }

    public EpfRemittance markRemittancePaid(UUID remittanceId, BigDecimal amountPaid, LocalDate paymentDate,
                                            String paymentReference, String paymentChannel, String notes, String actor) {
        EpfRemittance remittance = getById(remittanceId);
        if (!"posted_to_accounting".equalsIgnoreCase(remittance.getStatus())
                && !"failed".equalsIgnoreCase(remittance.getStatus())) {
            throw new RuntimeException("Remittance can be marked paid only from posted_to_accounting/failed. Current status: " + remittance.getStatus());
        }
        if (amountPaid == null || amountPaid.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("amountPaid must be greater than zero");
        }
        if (paymentReference == null || paymentReference.isBlank()) {
            throw new RuntimeException("paymentReference is required when marking remittance as paid");
        }
        remittance.setAmountPaid(amountPaid);
        remittance.setPaymentDate(paymentDate != null ? paymentDate : LocalDate.now());
        remittance.setPaymentReference(paymentReference);
        remittance.setPaymentChannel(paymentChannel);
        remittance.setNotes(notes);
        remittance.setStatus("paid");
        remittance.setUpdatedBy(actor);

        EpfRemittance saved = epfRemittanceRepository.save(remittance);
        if (saved.getComplianceRecordId() != null) {
            epfComplianceRecordRepository.findById(saved.getComplianceRecordId()).ifPresent(record -> {
                record.setStatus("verified");
                record.setSubmissionDate(saved.getPaymentDate());
                record.setFileReference(saved.getPaymentReference());
                record.setUpdatedBy(actor);
                epfComplianceRecordRepository.save(record);
            });
        }
        appendAuditEvent(
                saved.getOrganizationId(),
                null,
                null,
                "remittance",
                saved.getRemittanceId(),
                "remittance_marked_paid",
                actor,
                Map.of(
                        "amountPaid", saved.getAmountPaid() != null ? saved.getAmountPaid() : BigDecimal.ZERO,
                        "paymentReference", saved.getPaymentReference() != null ? saved.getPaymentReference() : "",
                        "paymentDate", saved.getPaymentDate() != null ? saved.getPaymentDate().toString() : ""
                )
        );
        return saved;
    }

    public EpfRemittance markRemittanceFailed(UUID remittanceId, String reason, String actor) {
        EpfRemittance remittance = getById(remittanceId);
        if (!"posted_to_accounting".equalsIgnoreCase(remittance.getStatus())) {
            throw new RuntimeException("Only posted_to_accounting remittance can be marked failed. Current status: " + remittance.getStatus());
        }
        remittance.setStatus("failed");
        remittance.setNotes(reason);
        remittance.setUpdatedBy(actor);
        EpfRemittance saved = epfRemittanceRepository.save(remittance);
        appendAuditEvent(
                saved.getOrganizationId(),
                null,
                null,
                "remittance",
                saved.getRemittanceId(),
                "remittance_marked_failed",
                actor,
                Map.of("reason", reason != null ? reason : "")
        );
        return saved;
    }

    public EpfRemittance getById(UUID remittanceId) {
        return epfRemittanceRepository.findById(remittanceId)
                .orElseThrow(() -> new RuntimeException("EPF remittance not found: " + remittanceId));
    }

    public List<EpfRemittance> list(UUID organizationId, Integer month, Integer year) {
        if (month != null && year != null) {
            return epfRemittanceRepository.findByOrganizationIdAndRemittanceMonthAndRemittanceYear(organizationId, month, year);
        }
        return epfRemittanceRepository.findByOrganizationIdOrderByRemittanceYearDescRemittanceMonthDescCreatedAtDesc(organizationId);
    }

    public BigDecimal computeLiability(UUID organizationId, Integer month, Integer year) {
        List<EpfContribution> contributions = epfContributionRepository.findByOrganizationAndPeriod(organizationId, month, year);
        return contributions.stream()
                .map(EpfContribution::getTotalContribution)
                .map(v -> v != null ? v : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void attachPeriodReferences(EpfRemittance remittance) {
        epfFilingRepository.findByOrganizationIdAndFilingMonthAndFilingYearAndFilingType(
                        remittance.getOrganizationId(), remittance.getRemittanceMonth(), remittance.getRemittanceYear(), "CHALLAN")
                .map(EpfFiling::getFilingId)
                .ifPresent(remittance::setFilingId);

        epfComplianceRecordRepository.findByOrganizationIdAndComplianceMonthAndComplianceYear(
                        remittance.getOrganizationId(), remittance.getRemittanceMonth(), remittance.getRemittanceYear()).stream()
                .filter(r -> "challan".equalsIgnoreCase(r.getComplianceType()))
                .map(EpfComplianceRecord::getComplianceRecordId)
                .findFirst()
                .ifPresent(remittance::setComplianceRecordId);
    }

    public Map<String, Object> getPeriodRemittanceSummary(UUID organizationId, Integer month, Integer year) {
        EpfRemittance remittance = epfRemittanceRepository
                .findFirstByOrganizationIdAndRemittanceMonthAndRemittanceYear(organizationId, month, year)
                .orElse(null);
        BigDecimal liability = computeLiability(organizationId, month, year);
        Map<String, Object> summary = new java.util.HashMap<>();
        summary.put("organizationId", organizationId);
        summary.put("month", month);
        summary.put("year", year);
        summary.put("liabilityAmount", liability);
        summary.put("remittanceRecorded", remittance != null);
        summary.put("status", remittance != null ? remittance.getStatus() : "not_started");
        summary.put("amountPaid", remittance != null && remittance.getAmountPaid() != null ? remittance.getAmountPaid() : BigDecimal.ZERO);
        summary.put("paymentDate", remittance != null ? remittance.getPaymentDate() : null);
        summary.put("paymentReference", remittance != null ? remittance.getPaymentReference() : null);
        summary.put("remittanceId", remittance != null ? remittance.getRemittanceId() : null);
        return summary;
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
}
