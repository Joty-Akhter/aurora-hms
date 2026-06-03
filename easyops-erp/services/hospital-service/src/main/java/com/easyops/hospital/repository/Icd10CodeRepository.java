package com.easyops.hospital.repository;

import com.easyops.hospital.entity.Icd10Code;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface Icd10CodeRepository extends JpaRepository<Icd10Code, String> {
    
    @Query("SELECT c FROM Icd10Code c WHERE c.isValid = true AND " +
           "(LOWER(c.code) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(c.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "ORDER BY c.code")
    List<Icd10Code> searchCodes(@Param("searchTerm") String searchTerm);
    
    @Query(value = "SELECT * FROM ehr.icd10_codes WHERE is_valid = true AND " +
           "(LOWER(code) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "to_tsvector('english', description) @@ plainto_tsquery('english', :searchTerm)) " +
           "ORDER BY code LIMIT :limit", nativeQuery = true)
    List<Icd10Code> searchCodesFullText(@Param("searchTerm") String searchTerm, @Param("limit") int limit);

    @Query("SELECT c FROM Icd10Code c WHERE (:includeInactive = true OR c.isValid = true) ORDER BY c.code")
    Page<Icd10Code> findCodes(@Param("includeInactive") boolean includeInactive, Pageable pageable);

    @Query("SELECT c FROM Icd10Code c WHERE (:includeInactive = true OR c.isValid = true) AND (" +
           "LOWER(c.code) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(c.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "ORDER BY CASE WHEN LOWER(c.code) LIKE LOWER(CONCAT(:searchTerm, '%')) THEN 0 ELSE 1 END, c.code")
    Page<Icd10Code> findCodesBySearch(@Param("searchTerm") String searchTerm, @Param("includeInactive") boolean includeInactive, Pageable pageable);

    @Query("SELECT c FROM Icd10Code c WHERE c.isValid = true ORDER BY c.code")
    Page<Icd10Code> findActiveCodes(Pageable pageable);

    @Query("SELECT c FROM Icd10Code c WHERE c.isValid = true AND (" +
           "LOWER(c.code) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(c.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "ORDER BY CASE WHEN LOWER(c.code) LIKE LOWER(CONCAT(:searchTerm, '%')) THEN 0 ELSE 1 END, c.code")
    Page<Icd10Code> findActiveCodesBySearch(@Param("searchTerm") String searchTerm, Pageable pageable);
}
