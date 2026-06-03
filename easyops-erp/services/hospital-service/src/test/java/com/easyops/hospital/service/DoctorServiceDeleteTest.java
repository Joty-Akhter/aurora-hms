package com.easyops.hospital.service;

import com.easyops.hospital.dto.request.DoctorRequest;
import com.easyops.hospital.entity.Doctor;
import com.easyops.hospital.entity.DoctorDepartment;
import com.easyops.hospital.integration.rbac.RbacAuthorizationClient;
import com.easyops.hospital.integration.scheduling.SchedulingServiceClient;
import com.easyops.hospital.integration.usermanagement.UserManagementClient;
import com.easyops.hospital.repository.DoctorDepartmentRepository;
import com.easyops.hospital.repository.DoctorRepository;
import com.easyops.rbac.client.RbacPermissionClient;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DoctorServiceDeleteTest {

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private DoctorDepartmentRepository doctorDepartmentRepository;

    private DoctorService doctorService;

    private final UUID doctorId = UUID.randomUUID();
    private final UUID linkedUserId = UUID.randomUUID();
    private final UUID actorUserId = UUID.randomUUID();
    private final UUID orgId = UUID.randomUUID();
    private final UUID departmentId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        RbacPermissionService rbac = new RbacPermissionService(new PermissiveRbacClient());
        doctorService = new DoctorService(
                doctorRepository,
                doctorDepartmentRepository,
                new NoOpSchedulingClient(),
                new ThrowingUserManagementClient(),
                new NoOpRbacAuthorizationClient(),
                rbac);
        ReflectionTestUtils.setField(doctorService, "defaultPortalUserPassword", "ChangeMeDoc1!");
        ReflectionTestUtils.setField(doctorService, "prescribingAuthorityRoleCode", "PRESCRIBING_AUTHORITY");
        ReflectionTestUtils.setField(doctorService, "baseUserRoleCode", "USER");
    }

    @Test
    void deleteDoctor_softDeletesEvenWhenPortalUserDeactivationFails() {
        Doctor doctor = activeDoctorWithLinkedUser();
        when(doctorRepository.findById(doctorId)).thenReturn(Optional.of(doctor));

        assertThatCode(() -> doctorService.deleteDoctor(doctorId, actorUserId.toString(), orgId))
                .doesNotThrowAnyException();

        ArgumentCaptor<Doctor> saved = ArgumentCaptor.forClass(Doctor.class);
        verify(doctorRepository).save(saved.capture());
        Doctor updated = saved.getValue();
        assertThat(updated.getIsActive()).isFalse();
        assertThat(updated.getAvailabilityStatus()).isEqualTo(Doctor.AvailabilityStatus.NOT_AVAILABLE);
        assertThat(updated.getAppointmentsFromWeb()).isFalse();
        assertThat(updated.getAppointmentsFromMobile()).isFalse();
    }

    @Test
    void updateDoctor_deactivatingSkipsDegreeAndBmdcValidation() {
        Doctor doctor = activeDoctorWithLinkedUser();
        doctor.setDegree(null);
        doctor.setBmdcRegistrationNumber(null);
        when(doctorRepository.findById(doctorId)).thenReturn(Optional.of(doctor));
        when(doctorRepository.save(any(Doctor.class))).thenAnswer(inv -> inv.getArgument(0));

        DoctorRequest request = DoctorRequest.builder()
                .doctorName(doctor.getDoctorName())
                .departmentId(departmentId)
                .doctorType(Doctor.DoctorType.CONSULTANT)
                .indoorOutdoorStatus(Doctor.IndoorOutdoorStatus.INDOOR)
                .registrationDate(doctor.getRegistrationDate())
                .isActive(false)
                .build();

        assertThatCode(() -> doctorService.updateDoctor(doctorId, request, actorUserId.toString(), orgId))
                .doesNotThrowAnyException();

        ArgumentCaptor<Doctor> saved = ArgumentCaptor.forClass(Doctor.class);
        verify(doctorRepository).save(saved.capture());
        assertThat(saved.getValue().getIsActive()).isFalse();
    }

    private Doctor activeDoctorWithLinkedUser() {
        DoctorDepartment department = DoctorDepartment.builder()
                .departmentId(departmentId)
                .departmentName("Urology")
                .build();
        return Doctor.builder()
                .doctorId(doctorId)
                .doctorCode("UROLDR01")
                .doctorName("Dr. Test Urology")
                .department(department)
                .doctorType(Doctor.DoctorType.CONSULTANT)
                .indoorOutdoorStatus(Doctor.IndoorOutdoorStatus.INDOOR)
                .registrationDate(java.time.LocalDate.of(2020, 1, 1))
                .isActive(true)
                .appointmentsFromWeb(true)
                .appointmentsFromMobile(false)
                .linkedUserId(linkedUserId)
                .build();
    }

    private static class NoOpSchedulingClient extends SchedulingServiceClient {
        NoOpSchedulingClient() {
            super(new RestTemplate(), new RestTemplate());
        }
    }

    private static class ThrowingUserManagementClient extends UserManagementClient {
        ThrowingUserManagementClient() {
            super(new RestTemplate(), new RestTemplate());
        }

        @Override
        public void deactivateUser(UUID userId, UUID actorUserId, UUID organizationId) {
            throw new RuntimeException("user-management unavailable");
        }
    }

    private static class NoOpRbacAuthorizationClient extends RbacAuthorizationClient {
        NoOpRbacAuthorizationClient() {
            super(new RestTemplate(), new RestTemplate());
        }
    }

  /** Grants all RBAC checks (JDK 25–compatible; avoids Mockito on concrete @Service types). */
    private static class PermissiveRbacClient extends RbacPermissionClient {
        PermissiveRbacClient() {
            super(new RestTemplate(), new SimpleMeterRegistry(), "http://localhost:8084", "hospital-test");
        }

        @Override
        public void requireAnyResourceAction(
                UUID userId,
                UUID organizationId,
                String[][] resourceActionPairs,
                String metricOperation) {
            // allow
        }

        @Override
        public void requireAuthenticatedUser(UUID userId) {
            // allow
        }

        @Override
        public boolean hasAnyResourceAction(
                UUID userId, UUID organizationId, String[][] resourceActionPairs) {
            return true;
        }
    }
}
