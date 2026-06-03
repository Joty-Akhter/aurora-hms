package com.easyops.hr.service;

import com.easyops.hr.dto.LoanAccountingCoaMappingDto;
import com.easyops.hr.dto.LoanAccountingCoaMappingUpsertRequest;
import com.easyops.hr.entity.LoanAccountingCoaMapping;
import com.easyops.hr.entity.LoanAuditLog;
import com.easyops.hr.repository.LoanAccountingCoaMappingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/** PI-05 optional COA mapping for loan accounting export. */
@Service
@RequiredArgsConstructor
public class LoanAccountingCoaService {

    private final LoanAccountingCoaMappingRepository repository;
    private final LoanAuditService loanAuditService;

    @Transactional(readOnly = true)
    public List<LoanAccountingCoaMappingDto> list(UUID organizationId) {
        return repository.findByOrganizationIdOrderByMappingKeyAsc(organizationId).stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Replace all mappings for the org with the given list (typical save from UI).
     */
    @Transactional
    public List<LoanAccountingCoaMappingDto> replaceAll(
            UUID organizationId, List<LoanAccountingCoaMappingUpsertRequest> rows, UUID actorUserId) {
        if (rows == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "body is required");
        }
        String before = describePersistedMappings(organizationId);
        repository.deleteByOrganizationId(organizationId);
        for (LoanAccountingCoaMappingUpsertRequest r : rows) {
            if (r.getMappingKey() == null || r.getMappingKey().isBlank()) {
                continue;
            }
            LoanAccountingCoaMapping m = new LoanAccountingCoaMapping();
            m.setOrganizationId(organizationId);
            m.setMappingKey(r.getMappingKey().trim().toUpperCase());
            m.setDebitAccountCode(r.getDebitAccountCode().trim());
            m.setCreditAccountCode(r.getCreditAccountCode().trim());
            m.setNotes(trimToNull(r.getNotes()));
            repository.save(m);
        }
        List<LoanAccountingCoaMappingDto> after = list(organizationId);
        String afterText = describeDtos(after);
        loanAuditService.log(
                organizationId,
                LoanAuditLog.ENTITY_LOAN_ACCOUNTING_COA,
                organizationId,
                LoanAuditLog.ACTION_COA_MAPPINGS_REPLACED,
                before,
                afterText,
                actorUserId);
        return after;
    }

    private String describePersistedMappings(UUID organizationId) {
        return describeEntities(repository.findByOrganizationIdOrderByMappingKeyAsc(organizationId));
    }

    private static String describeEntities(List<LoanAccountingCoaMapping> list) {
        if (list == null || list.isEmpty()) {
            return "(none)";
        }
        return list.stream()
                .map(m -> m.getMappingKey() + "=" + m.getDebitAccountCode() + "/" + m.getCreditAccountCode())
                .collect(Collectors.joining("; "));
    }

    private static String describeDtos(List<LoanAccountingCoaMappingDto> list) {
        if (list == null || list.isEmpty()) {
            return "(none)";
        }
        return list.stream()
                .map(m -> m.getMappingKey() + "=" + m.getDebitAccountCode() + "/" + m.getCreditAccountCode())
                .collect(Collectors.joining("; "));
    }

    private LoanAccountingCoaMappingDto toDto(LoanAccountingCoaMapping e) {
        return LoanAccountingCoaMappingDto.builder()
                .mappingId(e.getMappingId())
                .organizationId(e.getOrganizationId())
                .mappingKey(e.getMappingKey())
                .debitAccountCode(e.getDebitAccountCode())
                .creditAccountCode(e.getCreditAccountCode())
                .notes(e.getNotes())
                .build();
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
