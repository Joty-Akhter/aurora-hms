package com.easyops.hospital.repository;

import com.easyops.hospital.entity.InHouseDispenseFillEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InHouseDispenseFillEventRepository extends JpaRepository<InHouseDispenseFillEvent, UUID> {

    Optional<InHouseDispenseFillEvent> findByIdempotencyKey(String idempotencyKey);
}
