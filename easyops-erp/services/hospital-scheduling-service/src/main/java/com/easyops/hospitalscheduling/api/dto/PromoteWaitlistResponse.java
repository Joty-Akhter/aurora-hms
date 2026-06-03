package com.easyops.hospitalscheduling.api.dto;

public class PromoteWaitlistResponse {

    private AppointmentResponse appointment;
    private Integer candidatesContacted;

    public AppointmentResponse getAppointment() { return appointment; }
    public void setAppointment(AppointmentResponse appointment) { this.appointment = appointment; }
    public Integer getCandidatesContacted() { return candidatesContacted; }
    public void setCandidatesContacted(Integer candidatesContacted) { this.candidatesContacted = candidatesContacted; }
}
