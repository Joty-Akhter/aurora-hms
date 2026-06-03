package com.easyops.hospitalpharmacy.repository;

import com.easyops.hospitalpharmacy.entity.PharmacyCreditAccount;
import com.easyops.hospitalpharmacy.entity.PharmacyPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PharmacyPaymentRepository extends JpaRepository<PharmacyPayment, UUID> {

    List<PharmacyPayment> findByCreditAccountOrderByPaymentDateDesc(PharmacyCreditAccount account);
}
