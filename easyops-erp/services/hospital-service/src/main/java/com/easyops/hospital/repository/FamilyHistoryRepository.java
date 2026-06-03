package com.easyops.hospital.repository;

import com.easyops.hospital.entity.FamilyHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FamilyHistoryRepository extends JpaRepository<FamilyHistory, UUID> {
    
    List<FamilyHistory> findByPatientPatientId(UUID patientId);
    
    List<FamilyHistory> findByPatientPatientIdAndFamilyMemberRelationship(
        UUID patientId, FamilyHistory.FamilyMemberRelationship relationship);
    
    void deleteByPatientPatientId(UUID patientId);
}
