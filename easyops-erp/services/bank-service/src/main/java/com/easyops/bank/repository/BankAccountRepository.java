package com.easyops.bank.repository;

import com.easyops.bank.entity.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BankAccountRepository extends JpaRepository<BankAccount, UUID> {
    List<BankAccount> findByOrganizationId(UUID organizationId);
    List<BankAccount> findByOrganizationIdAndIsActive(UUID organizationId, Boolean isActive);
    Optional<BankAccount> findByOrganizationIdAndAccountNumber(UUID organizationId, String accountNumber);
    boolean existsByOrganizationIdAndAccountNumber(UUID organizationId, String accountNumber);
}

