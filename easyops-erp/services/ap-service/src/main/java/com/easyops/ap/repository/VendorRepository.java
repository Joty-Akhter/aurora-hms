package com.easyops.ap.repository;

import com.easyops.ap.entity.Vendor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VendorRepository extends JpaRepository<Vendor, UUID> {
    List<Vendor> findByOrganizationId(UUID organizationId);
    List<Vendor> findByOrganizationIdAndIsActive(UUID organizationId, Boolean isActive);
    Optional<Vendor> findByOrganizationIdAndVendorCode(UUID organizationId, String vendorCode);
    boolean existsByOrganizationIdAndVendorCode(UUID organizationId, String vendorCode);
}

