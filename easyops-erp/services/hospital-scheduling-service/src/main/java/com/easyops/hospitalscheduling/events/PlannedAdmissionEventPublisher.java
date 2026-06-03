package com.easyops.hospitalscheduling.events;

import com.easyops.hospitalscheduling.domain.plannedadmission.PlannedAdmission;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
public class PlannedAdmissionEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public PlannedAdmissionEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public void publishReserved(PlannedAdmission plannedAdmission) {
        applicationEventPublisher.publishEvent(toEvent(PlannedAdmissionEvent.Type.RESERVED, plannedAdmission));
    }

    public void publishConverted(PlannedAdmission plannedAdmission) {
        applicationEventPublisher.publishEvent(toEvent(PlannedAdmissionEvent.Type.CONVERTED, plannedAdmission));
    }

    private static PlannedAdmissionEvent toEvent(PlannedAdmissionEvent.Type type, PlannedAdmission p) {
        return new PlannedAdmissionEvent(
                type,
                p.getId(),
                p.getPatientId(),
                p.getStatus(),
                p.getPreferredDate(),
                p.getReservationId(),
                p.getBedGroupResourceId(),
                p.getExpiresAt(),
                OffsetDateTime.now()
        );
    }
}
