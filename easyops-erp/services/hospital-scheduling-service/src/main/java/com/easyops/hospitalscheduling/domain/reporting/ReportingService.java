package com.easyops.hospitalscheduling.domain.reporting;

import com.easyops.hospitalscheduling.api.dto.*;
import com.easyops.hospitalscheduling.domain.appointment.AppointmentRepository;
import com.easyops.hospitalscheduling.domain.reservation.ReservationRepository;
import com.easyops.hospitalscheduling.domain.resource.AvailabilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReportingService {

    private final ReservationRepository reservationRepository;
    private final AppointmentRepository appointmentRepository;
    private final AvailabilityService availabilityService;

    /**
     * Utilization: count reserved (non-cancelled) slots by resource and date.
     * slotAvailable is derived per resource/date from working hours and slot template (capacity).
     */
    public UtilizationReportResponse getUtilizationReport(UUID resourceId, LocalDate fromDate, LocalDate toDate, String groupBy) {
        if (fromDate == null || toDate == null || fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("fromDate and toDate must be set and fromDate <= toDate");
        }
        OffsetDateTime from = OffsetDateTime.of(fromDate, LocalTime.MIN, ZoneOffset.UTC);
        OffsetDateTime to = OffsetDateTime.of(toDate.plusDays(1), LocalTime.MIN, ZoneOffset.UTC);

        List<Object[]> rows = reservationRepository.countSlotUsedByResourceAndDate(from, to, resourceId);
        List<UtilizationReportResponse.UtilizationDataPoint> dataPoints = new ArrayList<>();
        for (Object[] row : rows) {
            UtilizationReportResponse.UtilizationDataPoint point = new UtilizationReportResponse.UtilizationDataPoint();
            UUID pointResourceId = row[0] != null ? UUID.fromString(row[0].toString()) : null;
            LocalDate pointDate = toLocalDate(row[1]);
            point.setResourceId(pointResourceId);
            point.setDate(pointDate);
            point.setSlotUsed(((Number) row[2]).longValue());
            long available = pointResourceId != null && pointDate != null
                    ? availabilityService.countAvailableSlotUnits(pointResourceId, pointDate)
                    : 0L;
            point.setSlotAvailable(available);
            dataPoints.add(point);
        }

        UtilizationReportResponse response = new UtilizationReportResponse();
        response.setResourceId(resourceId);
        response.setFromDate(fromDate);
        response.setToDate(toDate);
        response.setGroupBy(groupBy != null && !groupBy.isBlank() ? groupBy.trim() : "DAY");
        response.setDataPoints(dataPoints);
        return response;
    }

    public NoShowReportResponse getNoShowReport(UUID resourceId, LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null || toDate == null || fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("fromDate and toDate must be set and fromDate <= toDate");
        }
        long count = resourceId != null
                ? appointmentRepository.countByResourceIdAndAppointmentDateBetweenAndStatus(resourceId, fromDate, toDate, "NO_SHOW")
                : appointmentRepository.countByAppointmentDateBetweenAndStatus(fromDate, toDate, "NO_SHOW");
        long total = resourceId != null
                ? appointmentRepository.countByResourceIdAndAppointmentDateBetween(resourceId, fromDate, toDate)
                : appointmentRepository.countByAppointmentDateBetween(fromDate, toDate);

        NoShowReportResponse response = new NoShowReportResponse();
        response.setResourceId(resourceId);
        response.setFromDate(fromDate);
        response.setToDate(toDate);
        response.setCount(count);
        response.setTotalAppointmentsInRange(total);
        response.setNoShowRate(total > 0 ? (double) count / total : null);
        return response;
    }

    public CancellationReportResponse getCancellationReport(UUID resourceId, LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null || toDate == null || fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("fromDate and toDate must be set and fromDate <= toDate");
        }
        long count = resourceId != null
                ? appointmentRepository.countByResourceIdAndAppointmentDateBetweenAndStatus(resourceId, fromDate, toDate, "CANCELLED")
                : appointmentRepository.countByAppointmentDateBetweenAndStatus(fromDate, toDate, "CANCELLED");
        long total = resourceId != null
                ? appointmentRepository.countByResourceIdAndAppointmentDateBetween(resourceId, fromDate, toDate)
                : appointmentRepository.countByAppointmentDateBetween(fromDate, toDate);

        CancellationReportResponse response = new CancellationReportResponse();
        response.setResourceId(resourceId);
        response.setFromDate(fromDate);
        response.setToDate(toDate);
        response.setCount(count);
        response.setTotalAppointmentsInRange(total);
        response.setCancellationRate(total > 0 ? (double) count / total : null);
        return response;
    }

    private static LocalDate toLocalDate(Object value) {
        if (value == null) return null;
        if (value instanceof LocalDate d) return d;
        if (value instanceof java.sql.Date d) return d.toLocalDate();
        if (value instanceof java.util.Date d) return new java.sql.Date(d.getTime()).toLocalDate();
        return null;
    }
}
