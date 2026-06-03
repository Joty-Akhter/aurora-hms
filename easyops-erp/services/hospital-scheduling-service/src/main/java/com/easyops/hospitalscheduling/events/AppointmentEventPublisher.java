package com.easyops.hospitalscheduling.events;

import com.easyops.hospitalscheduling.domain.appointment.Appointment;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
public class AppointmentEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;
    private final CommunicationAppointmentEventPublisher communicationAppointmentEventPublisher;

    public AppointmentEventPublisher(
            ApplicationEventPublisher applicationEventPublisher,
            CommunicationAppointmentEventPublisher communicationAppointmentEventPublisher
    ) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.communicationAppointmentEventPublisher = communicationAppointmentEventPublisher;
    }

    public void publishCreated(Appointment appointment) {
        applicationEventPublisher.publishEvent(toEvent(AppointmentEvent.Type.CREATED, appointment));
        communicationAppointmentEventPublisher.publish(AppointmentEvent.Type.CREATED, appointment);
    }

    public void publishRescheduled(Appointment appointment) {
        applicationEventPublisher.publishEvent(toEvent(AppointmentEvent.Type.RESCHEDULED, appointment));
        communicationAppointmentEventPublisher.publish(AppointmentEvent.Type.RESCHEDULED, appointment);
    }

    public void publishCancelled(Appointment appointment) {
        applicationEventPublisher.publishEvent(toEvent(AppointmentEvent.Type.CANCELLED, appointment));
    }

    public void publishCheckedIn(Appointment appointment) {
        applicationEventPublisher.publishEvent(toEvent(AppointmentEvent.Type.CHECKED_IN, appointment));
    }

    public void publishNoShow(Appointment appointment) {
        applicationEventPublisher.publishEvent(toEvent(AppointmentEvent.Type.NO_SHOW, appointment));
    }

    public void publishCompleted(Appointment appointment) {
        applicationEventPublisher.publishEvent(toEvent(AppointmentEvent.Type.COMPLETED, appointment));
    }

    private static AppointmentEvent toEvent(AppointmentEvent.Type type, Appointment a) {
        return new AppointmentEvent(
                type,
                a.getId(),
                a.getReservationId(),
                a.getPatientId(),
                a.getResourceId(),
                a.getStatus(),
                a.getSlotStart(),
                a.getSlotEnd(),
                a.getAppointmentDate(),
                OffsetDateTime.now()
        );
    }
}
