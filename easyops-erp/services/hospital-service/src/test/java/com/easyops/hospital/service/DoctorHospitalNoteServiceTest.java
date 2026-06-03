package com.easyops.hospital.service;

import com.easyops.hospital.dto.request.DoctorHospitalNoteRequest;
import com.easyops.hospital.dto.request.DoctorHospitalNoteUpdateRequest;
import com.easyops.hospital.entity.Doctor;
import com.easyops.hospital.entity.DoctorHospitalNote;
import com.easyops.hospital.repository.DoctorHospitalNoteRepository;
import com.easyops.hospital.repository.DoctorRepository;
import com.easyops.rbac.client.RbacPermissionClient;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DoctorHospitalNoteServiceTest {

    @Mock
    private DoctorHospitalNoteRepository noteRepository;

    @Mock
    private DoctorRepository doctorRepository;

    private DoctorHospitalNoteService service;

    private final UUID userId = UUID.randomUUID();
    private final UUID orgId = UUID.randomUUID();
    private final UUID doctorId = UUID.randomUUID();
    private final UUID noteId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        RbacPermissionService rbac = new RbacPermissionService(new PermissiveRbacClient());
        service = new DoctorHospitalNoteService(noteRepository, doctorRepository, rbac);
    }

    @Test
    void createNote_rejectsInactiveDoctor() {
        Doctor doctor = Doctor.builder().doctorId(doctorId).doctorName("Dr. Test").isActive(false).build();
        when(doctorRepository.findById(doctorId)).thenReturn(Optional.of(doctor));

        assertThatThrownBy(() -> service.createNote(
                DoctorHospitalNoteRequest.builder().doctorId(doctorId).message("Late").build(),
                userId,
                orgId))
            .isInstanceOf(ResponseStatusException.class)
            .matches(ex -> ((ResponseStatusException) ex).getStatusCode() == HttpStatus.BAD_REQUEST);
    }

    @Test
    void createNote_linkedDoctorCannotPostAsAnotherDoctor() {
        UUID otherDoctorId = UUID.randomUUID();
        Doctor target = Doctor.builder().doctorId(otherDoctorId).doctorName("Dr. Other").isActive(true).build();
        Doctor linked = Doctor.builder().doctorId(doctorId).doctorName("Dr. Self").isActive(true).build();
        when(doctorRepository.findById(otherDoctorId)).thenReturn(Optional.of(target));
        when(doctorRepository.findByLinkedUserId(userId)).thenReturn(Optional.of(linked));

        assertThatThrownBy(() -> service.createNote(
                DoctorHospitalNoteRequest.builder().doctorId(otherDoctorId).message("Late").build(),
                userId,
                orgId))
            .isInstanceOf(ResponseStatusException.class)
            .matches(ex -> ((ResponseStatusException) ex).getStatusCode() == HttpStatus.FORBIDDEN);
    }

    @Test
    void updateNote_namedDoctorCanModifyEvenIfNotCreator() {
        UUID attendantId = UUID.randomUUID();
        DoctorHospitalNote note = DoctorHospitalNote.builder()
            .noteId(noteId)
            .doctorId(doctorId)
            .doctorName("Dr. Self")
            .message("Original")
            .createdBy(attendantId)
            .build();
        Doctor linked = Doctor.builder().doctorId(doctorId).build();

        when(noteRepository.findById(noteId)).thenReturn(Optional.of(note));
        when(doctorRepository.findByLinkedUserId(userId)).thenReturn(Optional.of(linked));
        when(noteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThatCode(() -> service.updateNote(
                noteId,
                DoctorHospitalNoteUpdateRequest.builder().message("Updated").build(),
                userId,
                orgId))
            .doesNotThrowAnyException();

        verify(noteRepository).save(note);
    }

    @Test
    void deleteNote_unrelatedUserForbidden() {
        DoctorHospitalNote note = DoctorHospitalNote.builder()
            .noteId(noteId)
            .doctorId(doctorId)
            .message("Original")
            .createdBy(UUID.randomUUID())
            .build();
        when(noteRepository.findById(noteId)).thenReturn(Optional.of(note));
        when(doctorRepository.findByLinkedUserId(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteNote(noteId, userId, orgId))
            .isInstanceOf(ResponseStatusException.class)
            .matches(ex -> ((ResponseStatusException) ex).getStatusCode() == HttpStatus.FORBIDDEN);
    }

    /** No-op RBAC client (no Mockito on RbacPermissionService — JDK 25 compatible). */
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
            return false;
        }
    }
}
