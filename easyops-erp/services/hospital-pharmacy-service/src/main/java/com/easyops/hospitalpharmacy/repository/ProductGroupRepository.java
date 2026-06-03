package com.easyops.hospitalpharmacy.repository;

import com.easyops.hospitalpharmacy.entity.ProductGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProductGroupRepository extends JpaRepository<ProductGroup, UUID> {

    List<ProductGroup> findByActiveTrue();

    List<ProductGroup> findByNameContainingIgnoreCase(String name);
}
