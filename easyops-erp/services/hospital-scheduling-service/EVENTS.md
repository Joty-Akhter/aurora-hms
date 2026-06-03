# Appointment domain events

The scheduling service publishes in-process domain events after appointment lifecycle changes. Use `ApplicationEventPublisher` (Spring); for outbound Kafka/Rabbit a listener can forward these events.

## Event names

| Event name              | When published      |
|-------------------------|---------------------|
| `appointment.created`   | After booking       |
| `appointment.rescheduled` | After reschedule  |
| `appointment.cancelled` | After cancel        |
| `appointment.checked_in`| After check-in      |
| `appointment.no_show`   | After no-show       |

**Note:** No event is published when create is satisfied by idempotency (existing appointment returned).

## Payload schema (AppointmentEvent)

| Field            | Type              | Description                    |
|------------------|-------------------|--------------------------------|
| `type`           | enum              | CREATED, RESCHEDULED, CANCELLED, CHECKED_IN, NO_SHOW |
| `appointmentId`  | UUID              | Scheduling appointment id      |
| `reservationId` | UUID              | Linked reservation id          |
| `patientId`      | UUID              | Patient                        |
| `resourceId`     | UUID              | Resource (e.g. doctor)         |
| `status`         | string            | CONFIRMED, CANCELLED, CHECKED_IN, NO_SHOW |
| `slotStart`      | OffsetDateTime ISO| Slot start                     |
| `slotEnd`        | OffsetDateTime ISO| Slot end                       |
| `appointmentDate`| LocalDate ISO     | Appointment date               |
| `timestamp`      | OffsetDateTime ISO| Event time                     |

## Consuming events

In the same application, use `@EventListener`:

```java
@EventListener
public void onAppointmentEvent(AppointmentEvent event) {
    if (event.getType() == AppointmentEvent.Type.NO_SHOW) {
        // e.g. emit to Billing for no-show fee
    }
}
```

For outbound messaging (Kafka/Rabbit), add a listener that maps `AppointmentEvent` to your message schema and publishes to the broker.

---

# Planned admission domain events

The scheduling service publishes in-process domain events when a planned admission is reserved or converted. Consumers: Portal BFF, analytics.

## Event names

| Event name                     | When published        |
|--------------------------------|------------------------|
| `planned_admission.reserved`   | After status → RESERVED (bed reserved, reservation created) |
| `planned_admission.converted`  | After status → CONVERTED (linked reservation completed)     |

## Payload schema (PlannedAdmissionEvent)

| Field                | Type              | Description                          |
|----------------------|-------------------|--------------------------------------|
| `type`               | enum              | RESERVED, CONVERTED                  |
| `plannedAdmissionId` | UUID              | Planned admission id                 |
| `patientId`          | UUID              | Patient                              |
| `status`             | string            | RESERVED or CONVERTED                |
| `preferredDate`      | LocalDate ISO     | Preferred admission date             |
| `reservationId`      | UUID nullable     | Linked reservation (set for RESERVED)|
| `bedGroupResourceId` | UUID nullable     | Bed group resource (set for RESERVED)|
| `expiresAt`          | OffsetDateTime ISO nullable | Reservation expiry (RESERVED only) |
| `timestamp`          | OffsetDateTime ISO| Event time                           |

## Consuming events

```java
@EventListener
public void onPlannedAdmissionEvent(PlannedAdmissionEvent event) {
    if (event.getType() == PlannedAdmissionEvent.Type.RESERVED) {
        // e.g. notify bed management, Portal BFF
    }
    if (event.getType() == PlannedAdmissionEvent.Type.CONVERTED) {
        // e.g. analytics, IPD sync
    }
}
```

For outbound messaging, add a listener that maps `PlannedAdmissionEvent` to your message schema and publishes to the broker.
