package com.easyops.hospitalpharmacy.repository;

import com.easyops.hospitalpharmacy.entity.Drug;
import com.easyops.hospitalpharmacy.entity.FormularyRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FormularyRuleRepository extends JpaRepository<FormularyRule, UUID> {

    List<FormularyRule> findByDrug(Drug drug);
}

