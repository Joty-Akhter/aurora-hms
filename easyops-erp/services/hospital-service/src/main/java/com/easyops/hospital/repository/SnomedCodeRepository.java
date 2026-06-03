package com.easyops.hospital.repository;

import com.easyops.hospital.entity.SnomedCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SnomedCodeRepository extends JpaRepository<SnomedCode, String> {
    
    @Query("SELECT c FROM SnomedCode c WHERE c.isValid = true AND " +
           "(LOWER(c.code) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(c.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(c.fullySpecifiedName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "ORDER BY c.code")
    List<SnomedCode> searchCodes(@Param("searchTerm") String searchTerm);
    
    @Query(value = "SELECT * FROM ehr.snomed_codes WHERE is_valid = true AND " +
           "(LOWER(code) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "to_tsvector('english', description) @@ plainto_tsquery('english', :searchTerm) OR " +
           "to_tsvector('english', fully_specified_name) @@ plainto_tsquery('english', :searchTerm)) " +
           "ORDER BY code LIMIT :limit", nativeQuery = true)
    List<SnomedCode> searchCodesFullText(@Param("searchTerm") String searchTerm, @Param("limit") int limit);
}
