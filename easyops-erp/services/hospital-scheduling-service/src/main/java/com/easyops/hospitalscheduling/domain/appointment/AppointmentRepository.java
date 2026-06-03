package com.easyops.hospitalscheduling.domain.appointment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID>, JpaSpecificationExecutor<Appointment> {

    List<Appointment> findByPatientIdAndAppointmentDateBetween(UUID patientId, LocalDate start, LocalDate end);

    List<Appointment> findByResourceIdAndAppointmentDate(UUID resourceId, LocalDate appointmentDate);

    Optional<Appointment> findByReservationId(UUID reservationId);

    Optional<Appointment> findByIdempotencyKey(String idempotencyKey);

    long countByAppointmentDateBetweenAndStatus(LocalDate start, LocalDate end, String status);

    long countByResourceIdAndAppointmentDateBetweenAndStatus(UUID resourceId, LocalDate start, LocalDate end, String status);

    long countByAppointmentDateBetween(LocalDate start, LocalDate end);

    long countByResourceIdAndAppointmentDateBetween(UUID resourceId, LocalDate start, LocalDate end);

    boolean existsByPatientIdAndResourceIdAndAppointmentDateAndSlotStartAndSlotEndAndStatusNotIn(
            UUID patientId,
            UUID resourceId,
            LocalDate appointmentDate,
            OffsetDateTime slotStart,
            OffsetDateTime slotEnd,
            Collection<String> excludedStatuses);

    List<Appointment> findByPatientIdAndResourceIdAndAppointmentDateAndStatusNotIn(
            UUID patientId,
            UUID resourceId,
            LocalDate appointmentDate,
            Collection<String> excludedStatuses);

    List<Appointment> findByResourceIdAndAppointmentDateAndStatusNotIn(
            UUID resourceId,
            LocalDate appointmentDate,
            Collection<String> excludedStatuses);

    List<Appointment> findByResourceIdAndAppointmentDateAndSlotStartAndSlotEndAndStatusNotIn(
            UUID resourceId,
            LocalDate appointmentDate,
            OffsetDateTime slotStart,
            OffsetDateTime slotEnd,
            Collection<String> excludedStatuses);

    List<Appointment> findByResourceIdAndAppointmentDateAndSlotStartAndStatusNotIn(
            UUID resourceId,
            LocalDate appointmentDate,
            OffsetDateTime slotStart,
            Collection<String> excludedStatuses);
}
