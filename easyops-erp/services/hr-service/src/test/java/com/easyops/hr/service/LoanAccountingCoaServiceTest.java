package com.easyops.hr.service;

import com.easyops.hr.dto.LoanAccountingCoaMappingUpsertRequest;
import com.easyops.hr.entity.LoanAccountingCoaMapping;
import com.easyops.hr.entity.LoanAuditLog;
import com.easyops.hr.repository.LoanAccountingCoaMappingRepository;
import com.easyops.hr.repository.LoanAuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanAccountingCoaServiceTest {

    @Mock
    private LoanAccountingCoaMappingRepository repository;

    @Mock
    private LoanAuditLogRepository loanAuditLogRepository;

    private LoanAccountingCoaService service;

    private UUID orgId;
    private UUID actorId;

    @BeforeEach
    void setUp() {
        orgId = UUID.randomUUID();
        actorId = UUID.randomUUID();
        service = new LoanAccountingCoaService(repository, new LoanAuditService(loanAuditLogRepository));
    }

    @Test
    void replaceAll_replacesRowsAndWritesAudit() {
        LoanAccountingCoaMappingUpsertRequest req = new LoanAccountingCoaMappingUpsertRequest();
        req.setMappingKey("LOAN_DISBURSEMENT");
        req.setDebitAccountCode("1200");
        req.setCreditAccountCode("2100");

        LoanAccountingCoaMapping saved = new LoanAccountingCoaMapping();
        saved.setMappingKey("LOAN_DISBURSEMENT");
        saved.setDebitAccountCode("1200");
        saved.setCreditAccountCode("2100");

        when(repository.findByOrganizationIdOrderByMappingKeyAsc(orgId))
                .thenReturn(Collections.emptyList())
                .thenReturn(Collections.singletonList(saved));
        when(repository.save(any(LoanAccountingCoaMapping.class))).thenAnswer(inv -> inv.getArgument(0));

        service.replaceAll(orgId, List.of(req), actorId);

        verify(repository).deleteByOrganizationId(orgId);
        verify(repository).save(any(LoanAccountingCoaMapping.class));

        ArgumentCaptor<LoanAuditLog> logCap = ArgumentCaptor.forClass(LoanAuditLog.class);
        verify(loanAuditLogRepository).save(logCap.capture());
        LoanAuditLog row = logCap.getValue();
        assertEquals(LoanAuditLog.ENTITY_LOAN_ACCOUNTING_COA, row.getEntityType());
        assertEquals(orgId, row.getEntityId());
        assertEquals(LoanAuditLog.ACTION_COA_MAPPINGS_REPLACED, row.getAction());
        assertEquals("(none)", row.getOldValues());
        assertEquals("LOAN_DISBURSEMENT=1200/2100", row.getNewValues());
        assertEquals(actorId.toString(), row.getPerformedBy());
    }

    @Test
    void replaceAll_emptyBodyClearsAndAudits() {
        when(repository.findByOrganizationIdOrderByMappingKeyAsc(orgId))
                .thenReturn(Collections.emptyList())
                .thenReturn(Collections.emptyList());

        service.replaceAll(orgId, Collections.emptyList(), actorId);

        verify(repository).deleteByOrganizationId(orgId);
        verify(repository, never()).save(any());
        ArgumentCaptor<LoanAuditLog> logCap = ArgumentCaptor.forClass(LoanAuditLog.class);
        verify(loanAuditLogRepository).save(logCap.capture());
        assertEquals("(none)", logCap.getValue().getOldValues());
        assertEquals("(none)", logCap.getValue().getNewValues());
    }
}
