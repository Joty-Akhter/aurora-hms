package com.easyops.hospitalscheduling.api.dto;

import java.util.List;

public class ConflictCheckResponse {

    private boolean hasConflict;
    private List<ReservationResponse> conflictingReservations;

    public ConflictCheckResponse() {}

    public ConflictCheckResponse(boolean hasConflict, List<ReservationResponse> conflictingReservations) {
        this.hasConflict = hasConflict;
        this.conflictingReservations = conflictingReservations;
    }

    public boolean isHasConflict() { return hasConflict; }
    public void setHasConflict(boolean hasConflict) { this.hasConflict = hasConflict; }
    public List<ReservationResponse> getConflictingReservations() { return conflictingReservations; }
    public void setConflictingReservations(List<ReservationResponse> conflictingReservations) { this.conflictingReservations = conflictingReservations; }
}
