package com.easyops.hospitalcorporatediscount.domain.corporate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface CorporateClientRepository extends JpaRepository<CorporateClient, UUID>, JpaSpecificationExecutor<CorporateClient> {

    Optional<CorporateClient> findByCode(String code);

    Page<CorporateClient> findByStatus(String status, Pageable pageable);

    Page<CorporateClient> findByType(String type, Pageable pageable);

    boolean existsByCode(String code);
}
