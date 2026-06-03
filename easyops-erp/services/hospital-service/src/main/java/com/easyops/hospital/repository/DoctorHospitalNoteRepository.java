package com.easyops.hospital.repository;

import com.easyops.hospital.entity.DoctorHospitalNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DoctorHospitalNoteRepository extends JpaRepository<DoctorHospitalNote, UUID> {

    List<DoctorHospitalNote> findAllByOrderByCreatedAtDesc();
}
