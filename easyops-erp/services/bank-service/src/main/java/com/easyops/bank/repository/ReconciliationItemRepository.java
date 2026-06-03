package com.easyops.bank.repository;

import com.easyops.bank.entity.ReconciliationItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReconciliationItemRepository extends JpaRepository<ReconciliationItem, UUID> {
    List<ReconciliationItem> findByReconciliationId(UUID reconciliationId);
    void deleteByReconciliationId(UUID reconciliationId);
}

