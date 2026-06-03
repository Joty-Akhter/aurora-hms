package com.easyops.hospitalclinicalorders.domain.orderset;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface OrderSetRepository extends JpaRepository<OrderSet, UUID>, JpaSpecificationExecutor<OrderSet> {

    List<OrderSet> findByPatientIdOrderByCreatedAtDesc(UUID patientId, Pageable pageable);
    List<OrderSet> findByVisitIdOrderByCreatedAtDesc(UUID visitId, Pageable pageable);

    Page<OrderSet> findByPatientIdAndCreatedAtBetween(
            UUID patientId, OffsetDateTime from, OffsetDateTime to, Pageable pageable);
    Page<OrderSet> findByVisitIdAndCreatedAtBetween(
            UUID visitId, OffsetDateTime from, OffsetDateTime to, Pageable pageable);
    Page<OrderSet> findByPatientId(UUID patientId, Pageable pageable);
    Page<OrderSet> findByVisitId(UUID visitId, Pageable pageable);
    Page<OrderSet> findByCreatedAtBetween(OffsetDateTime from, OffsetDateTime to, Pageable pageable);
    Page<OrderSet> findAll(Pageable pageable);
}
