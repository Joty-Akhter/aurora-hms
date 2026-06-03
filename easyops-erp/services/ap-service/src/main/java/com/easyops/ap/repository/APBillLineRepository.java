package com.easyops.ap.repository;

import com.easyops.ap.entity.APBillLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface APBillLineRepository extends JpaRepository<APBillLine, UUID> {
    List<APBillLine> findByBillId(UUID billId);
    void deleteByBillId(UUID billId);
}

