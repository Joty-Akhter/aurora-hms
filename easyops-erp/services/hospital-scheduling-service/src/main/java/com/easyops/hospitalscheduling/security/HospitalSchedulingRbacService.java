package com.easyops.hospitalscheduling.security;

import com.easyops.rbac.client.RbacPermissionClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Coarse {@code HOSPITAL_VIEW} / {@code HOSPITAL_MANAGE} ({@code hospital} / {@code view|manage}),
 * plus {@code hospital.appointment} view/book for call-center / narrow roles (see Liquibase 090),
 * and {@code hospital.appointment} {@code update_status} for doctor attendants (Liquibase 101),
 * {@code hospital.doctor_schedule} {@code manage} for slots/off-days sync (Liquibase 102),
 * {@code hospital.appointment} {@code reschedule} for call-center + doctor attendants (Liquibase 121),
 * {@code hospital.appointment} {@code cancel} for call-center + doctor attendants (Liquibase 124).
 */
@Service
public class HospitalSchedulingRbacService {

    private static final String RESOURCE_APPOINTMENT = "hospital.appointment";
    private static final String RESOURCE_DOCTOR_SCHEDULE = "hospital.doctor_schedule";

    private static final String[][] HOSPITAL_VIEW = {
            {"hospital", "view"},
            {"hospital", "manage"},
    };

    private static final String[][] HOSPITAL_MANAGE = {
            {"hospital", "manage"},
    };

    /** Read appointments, queues, resources list, availability — without full hospital menu access. */
    private static final String[][] APPOINTMENT_STAFF_READ = {
            {"hospital", "view"},
            {"hospital", "manage"},
            {RESOURCE_APPOINTMENT, "view"},
    };

    /** Create appointment only. */
    private static final String[][] APPOINTMENT_BOOK = {
            {"hospital", "manage"},
            {RESOURCE_APPOINTMENT, "book"},
    };

    /** Reschedule — call-center (book) and doctor-attendants (reschedule) can move appointments. */
    private static final String[][] APPOINTMENT_RESCHEDULE = {
            {"hospital", "manage"},
            {RESOURCE_APPOINTMENT, "book"},
            {RESOURCE_APPOINTMENT, "reschedule"},
    };

    /** Cancel — call-center and doctor-attendants can cancel appointments. */
    private static final String[][] APPOINTMENT_CANCEL = {
            {"hospital", "manage"},
            {RESOURCE_APPOINTMENT, "book"},
            {RESOURCE_APPOINTMENT, "cancel"},
    };

    /** Check-in / no-show / complete — narrow role without {@code hospital.manage}. */
    private static final String[][] APPOINTMENT_STATUS_UPDATE = {
            {"hospital", "manage"},
            {RESOURCE_APPOINTMENT, "update_status"},
    };

    /** Working hours, blackouts, roster blocks, scheduling-resource patch — doctor desk / admins. */
    private static final String[][] DOCTOR_SCHEDULE_MANAGE = {
            {"hospital", "manage"},
            {RESOURCE_DOCTOR_SCHEDULE, "manage"},
    };

    /** Create scheduling resource when booking or maintaining doctor slots (first sync). */
    private static final String[][] SCHEDULING_RESOURCE_CREATE = {
            {"hospital", "manage"},
            {RESOURCE_APPOINTMENT, "book"},
            {RESOURCE_DOCTOR_SCHEDULE, "manage"},
    };

    private final RbacPermissionClient rbac;
    private final boolean bypassRbac;

    public HospitalSchedulingRbacService(
            RbacPermissionClient rbac,
            @Value("${hospital.scheduling.security.bypass-rbac:false}") boolean bypassRbac
    ) {
        this.rbac = rbac;
        this.bypassRbac = bypassRbac;
    }

    public void requireHospitalView(UUID actorUserId, UUID organizationId) {
        if (bypassRbac) {
            return;
        }
        rbac.requireAnyResourceAction(actorUserId, organizationId, HOSPITAL_VIEW, "hospital_scheduling_view");
    }

    public void requireHospitalManage(UUID actorUserId, UUID organizationId) {
        if (bypassRbac) {
            return;
        }
        rbac.requireAnyResourceAction(actorUserId, organizationId, HOSPITAL_MANAGE, "hospital_scheduling_manage");
    }

    public void requireAppointmentStaffRead(UUID actorUserId, UUID organizationId) {
        if (bypassRbac) {
            return;
        }
        rbac.requireAnyResourceAction(actorUserId, organizationId, APPOINTMENT_STAFF_READ, "hospital_scheduling_appt_staff_read");
    }

    public void requireAppointmentBook(UUID actorUserId, UUID organizationId) {
        if (bypassRbac) {
            return;
        }
        rbac.requireAnyResourceAction(actorUserId, organizationId, APPOINTMENT_BOOK, "hospital_scheduling_appt_book");
    }

    public void requireAppointmentReschedule(UUID actorUserId, UUID organizationId) {
        if (bypassRbac) {
            return;
        }
        rbac.requireAnyResourceAction(
                actorUserId, organizationId, APPOINTMENT_RESCHEDULE, "hospital_scheduling_appt_reschedule");
    }

    public void requireAppointmentCancel(UUID actorUserId, UUID organizationId) {
        if (bypassRbac) {
            return;
        }
        rbac.requireAnyResourceAction(
                actorUserId, organizationId, APPOINTMENT_CANCEL, "hospital_scheduling_appt_cancel");
    }

    public void requireAppointmentStatusUpdate(UUID actorUserId, UUID organizationId) {
        if (bypassRbac) {
            return;
        }
        rbac.requireAnyResourceAction(
                actorUserId, organizationId, APPOINTMENT_STATUS_UPDATE, "hospital_scheduling_appt_status_update");
    }

    public void requireDoctorScheduleManage(UUID actorUserId, UUID organizationId) {
        if (bypassRbac) {
            return;
        }
        rbac.requireAnyResourceAction(
                actorUserId, organizationId, DOCTOR_SCHEDULE_MANAGE, "hospital_scheduling_doctor_schedule_manage");
    }

    public void requireSchedulingResourceCreate(UUID actorUserId, UUID organizationId) {
        if (bypassRbac) {
            return;
        }
        rbac.requireAnyResourceAction(
                actorUserId, organizationId, SCHEDULING_RESOURCE_CREATE, "hospital_scheduling_resource_create");
    }
}
