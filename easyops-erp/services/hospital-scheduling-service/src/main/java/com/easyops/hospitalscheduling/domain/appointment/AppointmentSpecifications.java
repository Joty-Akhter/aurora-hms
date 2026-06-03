package com.easyops.hospitalscheduling.domain.appointment;

import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.UUID;

public final class AppointmentSpecifications {

    private AppointmentSpecifications() {}

    public static Specification<Appointment> hasPatientId(UUID patientId) {
        if (patientId == null) return (root, q, cb) -> cb.conjunction();
        return (root, q, cb) -> cb.equal(root.get("patientId"), patientId);
    }

    public static Specification<Appointment> hasResourceId(UUID resourceId) {
        if (resourceId == null) return (root, q, cb) -> cb.conjunction();
        return (root, q, cb) -> cb.equal(root.get("resourceId"), resourceId);
    }

    public static Specification<Appointment> hasClinicOrLocationId(UUID clinicOrLocationId) {
        if (clinicOrLocationId == null) return (root, q, cb) -> cb.conjunction();
        return (root, q, cb) -> cb.equal(root.get("clinicOrLocationId"), clinicOrLocationId);
    }

    public static Specification<Appointment> appointmentDateFrom(LocalDate from) {
        if (from == null) return (root, q, cb) -> cb.conjunction();
        return (root, q, cb) -> cb.greaterThanOrEqualTo(root.get("appointmentDate"), from);
    }

    public static Specification<Appointment> appointmentDateTo(LocalDate to) {
        if (to == null) return (root, q, cb) -> cb.conjunction();
        return (root, q, cb) -> cb.lessThanOrEqualTo(root.get("appointmentDate"), to);
    }

    public static Specification<Appointment> hasStatus(String status) {
        if (status == null || status.isBlank()) return (root, q, cb) -> cb.conjunction();
        return (root, q, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Appointment> hasAppointmentType(String appointmentType) {
        if (appointmentType == null || appointmentType.isBlank()) return (root, q, cb) -> cb.conjunction();
        String normalized = appointmentType.trim().toUpperCase();
        return (root, q, cb) -> cb.equal(cb.upper(root.get("appointmentType")), normalized);
    }

    public static Specification<Appointment> hasSlotTemplateId(UUID slotTemplateId) {
        return (root, query, cb) -> slotTemplateId == null ? cb.conjunction() : cb.equal(root.get("slotTemplateId"), slotTemplateId);
    }

    public static Specification<Appointment> hasSessionShift(String sessionShift) {
        return (root, query, cb) -> sessionShift == null || sessionShift.isBlank() ? cb.conjunction() : cb.equal(root.get("sessionShift"), sessionShift.toUpperCase());
    }

    public static Specification<Appointment> hasBookingChannel(String bookingChannel) {
        return (root, query, cb) -> bookingChannel == null || bookingChannel.isBlank() ? cb.conjunction() : cb.equal(root.get("bookingChannel"), bookingChannel.toUpperCase());
    }
}
