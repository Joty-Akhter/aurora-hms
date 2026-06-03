package com.easyops.hospitalscheduling.api.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class QueueResponse {

    private UUID resourceId;
    private LocalDate date;
    private List<AppointmentResponse> appointments;

    public UUID getResourceId() { return resourceId; }
    public void setResourceId(UUID resourceId) { this.resourceId = resourceId; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public List<AppointmentResponse> getAppointments() { return appointments; }
    public void setAppointments(List<AppointmentResponse> appointments) { this.appointments = appointments; }
}
