package com.easyops.hr.controller;

import com.easyops.hr.dto.LoanAccountingCoaMappingDto;
import com.easyops.hr.dto.LoanAccountingCoaMappingUpsertRequest;
import com.easyops.hr.security.LoanRbacService;
import com.easyops.hr.security.RbacRequestHeaders;
import com.easyops.hr.service.LoanAccountingCoaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** PI-05 optional: chart-of-account codes for loan journal export lines. */
@RestController
@RequestMapping("/api/hr/loans/accounting-coa-mappings")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class LoanAccountingCoaController {

    private final LoanAccountingCoaService loanAccountingCoaService;
    private final LoanRbacService loanRbac;

    @GetMapping
    public ResponseEntity<List<LoanAccountingCoaMappingDto>> list(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        loanRbac.requireLoansView(actor, organizationId);
        return ResponseEntity.ok(loanAccountingCoaService.list(organizationId));
    }

    @PutMapping
    public ResponseEntity<List<LoanAccountingCoaMappingDto>> replace(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @Valid @RequestBody List<LoanAccountingCoaMappingUpsertRequest> body) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        loanRbac.requireLoansManage(actor, organizationId);
        return ResponseEntity.ok(loanAccountingCoaService.replaceAll(organizationId, body, actor));
    }
}
