package com.easyops.hospital.repository;

import com.easyops.hospital.entity.FormularyAlternative;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FormularyAlternativeRepository extends JpaRepository<FormularyAlternative, UUID> {
    List<FormularyAlternative> findByFormularyCheckFormularyCheckId(UUID formularyCheckId);
    List<FormularyAlternative> findByFormularyCheckFormularyCheckIdOrderByRankAsc(UUID formularyCheckId);
}
