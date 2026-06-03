package com.easyops.hospitalpharmacy.repository;

import com.easyops.hospitalpharmacy.entity.SupplierReturnOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SupplierReturnOrderRepository extends JpaRepository<SupplierReturnOrder, UUID> {

    List<SupplierReturnOrder> findByManufacturerIdOrderByCreatedAtDesc(UUID manufacturerId);

    List<SupplierReturnOrder> findByFromLocationIdOrderByCreatedAtDesc(UUID fromLocationId);

    List<SupplierReturnOrder> findByStatusOrderByCreatedAtDesc(SupplierReturnOrder.SupplierReturnStatus status);
}
