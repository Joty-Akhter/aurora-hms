package com.easyops.ar.repository;

import com.easyops.ar.entity.ReminderHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReminderHistoryRepository extends JpaRepository<ReminderHistory, UUID> {
    
    List<ReminderHistory> findByOrganizationId(UUID organizationId);
    
    List<ReminderHistory> findByInvoiceId(UUID invoiceId);
    
    @Query("SELECT rh FROM ReminderHistory rh WHERE rh.invoice.id = :invoiceId AND rh.reminderLevel = :level")
    Optional<ReminderHistory> findByInvoiceIdAndLevel(@Param("invoiceId") UUID invoiceId, @Param("level") Integer level);
    
    @Query("SELECT rh FROM ReminderHistory rh WHERE rh.sentDate >= :startDate ORDER BY rh.sentDate DESC")
    List<ReminderHistory> findRecentReminders(@Param("startDate") LocalDate startDate);
}

