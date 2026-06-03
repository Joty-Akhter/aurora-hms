package com.easyops.hospitalpharmacy.repository;

import com.easyops.hospitalpharmacy.entity.Manufacturer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ManufacturerRepository extends JpaRepository<Manufacturer, UUID> {

    List<Manufacturer> findByNameContainingIgnoreCase(String name);

    List<Manufacturer> findByActiveTrue();
}

