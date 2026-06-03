package com.easyops.hospitalcard.domain.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LimitProfileRepository extends JpaRepository<LimitProfile, UUID> {

    Optional<LimitProfile> findByName(String name);

    Page<LimitProfile> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
