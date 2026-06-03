package com.easyops.hospitalcorporatediscount.domain.packagedefinition;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PackageItemRepository extends JpaRepository<PackageItem, UUID> {

    List<PackageItem> findByPackageIdOrderByItemTypeAscItemCodeAsc(UUID packageId);

    boolean existsByPackageIdAndId(UUID packageId, UUID itemId);
}
