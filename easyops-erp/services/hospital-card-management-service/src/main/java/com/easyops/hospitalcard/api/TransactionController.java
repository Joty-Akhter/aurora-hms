package com.easyops.hospitalcard.api;

import com.easyops.hospitalcard.api.dto.CardTransactionResponse;
import com.easyops.hospitalcard.api.dto.RefundRequest;
import com.easyops.hospitalcard.domain.account.CardAccountService;
import com.easyops.hospitalcard.security.HospitalCardRbacService;
import com.easyops.hospitalcard.security.RbacRequestHeaders;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/hospital-card-management/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final CardAccountService cardAccountService;
    private final HospitalCardRbacService hospitalCardRbac;

    @PostMapping("/{id}/refund")
    public ResponseEntity<CardTransactionResponse> refund(
            @PathVariable("id") UUID transactionId,
            @Valid @RequestBody RefundRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalCardRbac.requireHospitalManage(actor, organizationId);
        CardTransactionResponse result = cardAccountService.refundTransaction(transactionId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
}
