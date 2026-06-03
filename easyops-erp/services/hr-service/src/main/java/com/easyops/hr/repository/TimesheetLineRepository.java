package com.easyops.hr.repository;

import com.easyops.hr.entity.TimesheetLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TimesheetLineRepository extends JpaRepository<TimesheetLine, UUID> {
    
    List<TimesheetLine> findByTimesheetId(UUID timesheetId);
    
    void deleteByTimesheetId(UUID timesheetId);
}

