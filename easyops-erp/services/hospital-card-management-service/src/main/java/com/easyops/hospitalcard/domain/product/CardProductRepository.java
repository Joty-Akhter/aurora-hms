package com.easyops.hospitalcard.domain.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CardProductRepository extends JpaRepository<CardProduct, UUID>, JpaSpecificationExecutor<CardProduct> {

    Optional<CardProduct> findByCode(String code);

    Page<CardProduct> findByStatus(String status, Pageable pageable);

    List<CardProduct> findByDefaultLimitProfileId(UUID defaultLimitProfileId);
}
