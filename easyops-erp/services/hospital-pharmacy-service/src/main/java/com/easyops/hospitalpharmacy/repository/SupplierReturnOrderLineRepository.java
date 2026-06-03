package com.easyops.hospitalpharmacy.repository;

import com.easyops.hospitalpharmacy.entity.SupplierReturnOrder;
import com.easyops.hospitalpharmacy.entity.SupplierReturnOrderLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SupplierReturnOrderLineRepository extends JpaRepository<SupplierReturnOrderLine, UUID> {

    List<SupplierReturnOrderLine> findByReturnOrderOrderByCreatedAtAsc(SupplierReturnOrder returnOrder);
}
