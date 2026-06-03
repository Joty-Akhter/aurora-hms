package com.easyops.hospitalpharmacy.repository;

import com.easyops.hospitalpharmacy.entity.PharmacyCreditAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PharmacyCreditAccountRepository extends JpaRepository<PharmacyCreditAccount, UUID> {

    Optional<PharmacyCreditAccount> findByPatientId(UUID patientId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM PharmacyCreditAccount a WHERE a.id = :id")
    Optional<PharmacyCreditAccount> findWithLockById(@Param("id") UUID id);

    List<PharmacyCreditAccount> findByActiveTrue();
}
