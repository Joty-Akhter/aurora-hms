package com.easyops.hospitalscheduling.api.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Extends AppointmentResponse with embedded reservation.
 */
public class AppointmentDetailResponse extends AppointmentResponse {

    private ReservationResponse reservation;

    public ReservationResponse getReservation() { return reservation; }
    public void setReservation(ReservationResponse reservation) { this.reservation = reservation; }
}
