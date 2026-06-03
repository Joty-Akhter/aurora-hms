package com.easyops.hospitalclinicalorders.domain.order;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrderAuditLogRepository extends JpaRepository<OrderAuditLog, UUID> {
}

