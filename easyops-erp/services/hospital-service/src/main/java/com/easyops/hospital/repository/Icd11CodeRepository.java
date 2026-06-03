package com.easyops.hospital.repository;

import com.easyops.hospital.entity.Icd11Code;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface Icd11CodeRepository extends JpaRepository<Icd11Code, String> {
    
    @Query("SELECT c FROM Icd11Code c WHERE c.isValid = true AND " +
           "(LOWER(c.code) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(c.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "ORDER BY c.code")
    List<Icd11Code> searchCodes(@Param("searchTerm") String searchTerm);
    
    @Query(value = "SELECT * FROM ehr.icd11_codes WHERE is_valid = true AND " +
           "(LOWER(code) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "to_tsvector('english', description) @@ plainto_tsquery('english', :searchTerm)) " +
           "ORDER BY code LIMIT :limit", nativeQuery = true)
    List<Icd11Code> searchCodesFullText(@Param("searchTerm") String searchTerm, @Param("limit") int limit);

    @Query("SELECT c FROM Icd11Code c WHERE (:includeInactive = true OR c.isValid = true) ORDER BY c.code")
    Page<Icd11Code> findCodes(@Param("includeInactive") boolean includeInactive, Pageable pageable);

    @Query("SELECT c FROM Icd11Code c WHERE (:includeInactive = true OR c.isValid = true) AND (" +
           "LOWER(c.code) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(c.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "ORDER BY CASE WHEN LOWER(c.code) LIKE LOWER(CONCAT(:searchTerm, '%')) THEN 0 ELSE 1 END, c.code")
    Page<Icd11Code> findCodesBySearch(@Param("searchTerm") String searchTerm, @Param("includeInactive") boolean includeInactive, Pageable pageable);

    @Query("SELECT c FROM Icd11Code c WHERE c.isValid = true ORDER BY c.code")
    Page<Icd11Code> findActiveCodes(Pageable pageable);

    @Query("SELECT c FROM Icd11Code c WHERE c.isValid = true AND (" +
           "LOWER(c.code) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(c.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "ORDER BY CASE WHEN LOWER(c.code) LIKE LOWER(CONCAT(:searchTerm, '%')) THEN 0 ELSE 1 END, c.code")
    Page<Icd11Code> findActiveCodesBySearch(@Param("searchTerm") String searchTerm, Pageable pageable);
}
