package com.easyops.hospitalscheduling.domain.resource;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BookingRuleRepository extends JpaRepository<BookingRule, UUID> {

    List<BookingRule> findByScopeTypeAndScopeId(String scopeType, UUID scopeId);

    List<BookingRule> findByScopeTypeAndScopeIdIsNull(String scopeType);
}
