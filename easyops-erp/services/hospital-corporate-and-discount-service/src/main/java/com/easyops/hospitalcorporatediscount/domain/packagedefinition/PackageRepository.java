package com.easyops.hospitalcorporatediscount.domain.packagedefinition;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface PackageRepository extends JpaRepository<PackageDefinition, UUID>, JpaSpecificationExecutor<PackageDefinition> {

    Optional<PackageDefinition> findByCode(String code);

    boolean existsByCode(String code);
}
