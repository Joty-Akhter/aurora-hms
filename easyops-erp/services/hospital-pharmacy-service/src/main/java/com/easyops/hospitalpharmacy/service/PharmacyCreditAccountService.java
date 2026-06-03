package com.easyops.hospitalpharmacy.service;

import com.easyops.hospitalpharmacy.dto.request.PharmacyCreditAccountRequest;
import com.easyops.hospitalpharmacy.dto.request.PharmacyPaymentRequest;
import com.easyops.hospitalpharmacy.dto.response.PharmacyCreditAccountResponse;
import com.easyops.hospitalpharmacy.dto.response.PharmacyPaymentResponse;
import com.easyops.hospitalpharmacy.entity.PharmacyCreditAccount;
import com.easyops.hospitalpharmacy.entity.PharmacyPayment;
import com.easyops.hospitalpharmacy.repository.PharmacyCreditAccountRepository;
import com.easyops.hospitalpharmacy.repository.PharmacyPaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PharmacyCreditAccountService {

    private final PharmacyCreditAccountRepository accountRepository;
    private final PharmacyPaymentRepository paymentRepository;

    @Transactional
    public PharmacyCreditAccountResponse createAccount(PharmacyCreditAccountRequest request) {
        if (accountRepository.findByPatientId(request.getPatientId()).isPresent()) {
            throw new IllegalArgumentException("Credit account already exists for patient: " + request.getPatientId());
        }
        PharmacyCreditAccount account = PharmacyCreditAccount.builder()
                .patientId(request.getPatientId())
                .customerName(request.getCustomerName())
                .creditLimit(request.getCreditLimit())
                .notes(request.getNotes())
                .build();
        return toResponse(accountRepository.save(account));
    }

    @Transactional(readOnly = true)
    public PharmacyCreditAccountResponse getById(UUID id) {
        return toResponse(accountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Credit account not found: " + id)));
    }

    @Transactional(readOnly = true)
    public PharmacyCreditAccountResponse getByPatient(UUID patientId) {
        return toResponse(accountRepository.findByPatientId(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Credit account not found for patient: " + patientId)));
    }

    @Transactional(readOnly = true)
    public List<PharmacyCreditAccountResponse> listActive() {
        return accountRepository.findByActiveTrue().stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public PharmacyCreditAccountResponse chargeAccount(UUID accountId, java.math.BigDecimal amount) {
        PharmacyCreditAccount account = accountRepository.findWithLockById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Credit account not found: " + accountId));
        if (!account.isActive()) {
            throw new IllegalStateException("Credit account is inactive: " + accountId);
        }
        java.math.BigDecimal newBalance = account.getOutstandingBalance().add(amount);
        if (newBalance.compareTo(account.getCreditLimit()) > 0) {
            throw new IllegalArgumentException(
                    "Charge of " + amount + " would exceed credit limit " + account.getCreditLimit()
                            + " (current balance " + account.getOutstandingBalance() + ")");
        }
        account.setOutstandingBalance(newBalance);
        return toResponse(accountRepository.save(account));
    }

    @Transactional
    public PharmacyPaymentResponse recordPayment(PharmacyPaymentRequest request, UUID receivedBy) {
        PharmacyCreditAccount account = accountRepository.findWithLockById(request.getCreditAccountId())
                .orElseThrow(() -> new IllegalArgumentException("Credit account not found: " + request.getCreditAccountId()));

        if (request.getAmount().compareTo(account.getOutstandingBalance()) > 0) {
            throw new IllegalArgumentException(
                    "Payment amount " + request.getAmount() + " exceeds outstanding balance " + account.getOutstandingBalance());
        }

        account.setOutstandingBalance(account.getOutstandingBalance().subtract(request.getAmount()));
        accountRepository.save(account);

        PharmacyPayment payment = PharmacyPayment.builder()
                .creditAccount(account)
                .dispenseOrderId(request.getDispenseOrderId())
                .amount(request.getAmount())
                .paymentMode(request.getPaymentMode())
                .referenceNo(request.getReferenceNo())
                .receivedBy(receivedBy)
                .paymentDate(OffsetDateTime.now())
                .notes(request.getNotes())
                .build();
        return toPaymentResponse(paymentRepository.save(payment));
    }

    @Transactional(readOnly = true)
    public List<PharmacyPaymentResponse> listPayments(UUID accountId) {
        PharmacyCreditAccount account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Credit account not found: " + accountId));
        return paymentRepository.findByCreditAccountOrderByPaymentDateDesc(account)
                .stream().map(this::toPaymentResponse).collect(Collectors.toList());
    }

    private PharmacyCreditAccountResponse toResponse(PharmacyCreditAccount account) {
        return PharmacyCreditAccountResponse.builder()
                .id(account.getId())
                .patientId(account.getPatientId())
                .customerName(account.getCustomerName())
                .creditLimit(account.getCreditLimit())
                .outstandingBalance(account.getOutstandingBalance())
                .availableCredit(account.getCreditLimit().subtract(account.getOutstandingBalance()))
                .active(account.isActive())
                .notes(account.getNotes())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();
    }

    private PharmacyPaymentResponse toPaymentResponse(PharmacyPayment payment) {
        return PharmacyPaymentResponse.builder()
                .id(payment.getId())
                .creditAccountId(payment.getCreditAccount().getId())
                .dispenseOrderId(payment.getDispenseOrderId())
                .amount(payment.getAmount())
                .paymentMode(payment.getPaymentMode())
                .referenceNo(payment.getReferenceNo())
                .receivedBy(payment.getReceivedBy())
                .paymentDate(payment.getPaymentDate())
                .notes(payment.getNotes())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
