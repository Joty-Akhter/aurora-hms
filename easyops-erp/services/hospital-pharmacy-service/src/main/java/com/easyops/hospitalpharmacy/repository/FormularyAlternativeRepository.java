package com.easyops.hospitalpharmacy.repository;

import com.easyops.hospitalpharmacy.entity.FormularyAlternative;
import com.easyops.hospitalpharmacy.entity.FormularyRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FormularyAlternativeRepository extends JpaRepository<FormularyAlternative, UUID> {

    List<FormularyAlternative> findByFormularyRuleOrderByPriority(FormularyRule rule);

    void deleteByFormularyRule(FormularyRule rule);
}
