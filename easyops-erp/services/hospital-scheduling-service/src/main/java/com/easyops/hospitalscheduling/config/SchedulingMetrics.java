package com.easyops.hospitalscheduling.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class SchedulingMetrics {

    private static final String PREFIX = "scheduling_";

    private final Counter appointmentsCreatedTotal;
    private final Counter reservationsConflictsTotal;
    private final Counter availabilityRequestsTotal;

    public SchedulingMetrics(MeterRegistry registry) {
        this.appointmentsCreatedTotal = registry.counter(PREFIX + "appointments_created_total");
        this.reservationsConflictsTotal = registry.counter(PREFIX + "reservations_conflicts_total");
        this.availabilityRequestsTotal = registry.counter(PREFIX + "availability_requests_total");
    }

    public void incrementAppointmentsCreated() {
        appointmentsCreatedTotal.increment();
    }

    public void incrementReservationsConflicts() {
        reservationsConflictsTotal.increment();
    }

    public void incrementAvailabilityRequests() {
        availabilityRequestsTotal.increment();
    }
}
