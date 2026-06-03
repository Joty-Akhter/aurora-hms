package com.easyops.hospital.service;

import com.easyops.hospital.dto.request.DoctorHospitalNoteRequest;
import com.easyops.hospital.dto.request.DoctorHospitalNoteUpdateRequest;
import com.easyops.hospital.dto.response.DoctorHospitalNoteResponse;
import com.easyops.hospital.entity.Doctor;
import com.easyops.hospital.entity.DoctorHospitalNote;
import com.easyops.hospital.repository.DoctorHospitalNoteRepository;
import com.easyops.hospital.repository.DoctorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DoctorHospitalNoteService {

    private final DoctorHospitalNoteRepository noteRepository;
    private final DoctorRepository doctorRepository;
    private final RbacPermissionService rbacPermissionService;

    private record NoteListContext(
            UUID userId,
            Optional<Doctor> linkedDoctor,
            boolean hasManagePermission,
            boolean hospitalAdmin) {}

    public List<DoctorHospitalNoteResponse> listNotes(UUID userId, UUID organizationId) {
        rbacPermissionService.requireAuthenticatedUser(userId);
        rbacPermissionService.requireDoctorNotesView(userId, organizationId);

        NoteListContext ctx = buildListContext(userId, organizationId);
        return noteRepository.findAllByOrderByCreatedAtDesc().stream()
            .map(note -> mapToResponse(note, ctx))
            .collect(Collectors.toList());
    }

    @Transactional
    public DoctorHospitalNoteResponse createNote(
            DoctorHospitalNoteRequest request, UUID userId, UUID organizationId) {
        rbacPermissionService.requireAuthenticatedUser(userId);
        rbacPermissionService.requireDoctorNotesManage(userId, organizationId);

        Doctor doctor = doctorRepository.findById(request.getDoctorId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Doctor not found: " + request.getDoctorId()));

        if (!Boolean.TRUE.equals(doctor.getIsActive())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Cannot post notes for an inactive doctor");
        }

        doctorRepository.findByLinkedUserId(userId).ifPresent(linked -> {
            if (!linked.getDoctorId().equals(doctor.getDoctorId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You can only post notes as your own doctor profile");
            }
        });

        String message = request.getMessage().trim();
        if (message.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message is required");
        }

        DoctorHospitalNote note = DoctorHospitalNote.builder()
            .doctorId(doctor.getDoctorId())
            .doctorName(doctor.getDoctorName())
            .message(message)
            .createdBy(userId)
            .build();

        DoctorHospitalNote saved = noteRepository.save(note);
        log.info("Created doctor hospital note {} for doctor {}", saved.getNoteId(), doctor.getDoctorId());
        return mapToResponse(saved, buildListContext(userId, organizationId));
    }

    @Transactional
    public void deleteNote(UUID noteId, UUID userId, UUID organizationId) {
        rbacPermissionService.requireAuthenticatedUser(userId);
        rbacPermissionService.requireDoctorNotesManage(userId, organizationId);

        DoctorHospitalNote note = noteRepository.findById(noteId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Doctor note not found: " + noteId));
        requireCanModify(note, buildListContext(userId, organizationId));

        noteRepository.delete(note);
        log.info("Deleted doctor hospital note {}", noteId);
    }

    @Transactional
    public DoctorHospitalNoteResponse updateNote(
            UUID noteId, DoctorHospitalNoteUpdateRequest request, UUID userId, UUID organizationId) {
        rbacPermissionService.requireAuthenticatedUser(userId);
        rbacPermissionService.requireDoctorNotesManage(userId, organizationId);

        DoctorHospitalNote note = noteRepository.findById(noteId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Doctor note not found: " + noteId));
        requireCanModify(note, buildListContext(userId, organizationId));

        String message = request.getMessage().trim();
        if (message.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message is required");
        }

        note.setMessage(message);
        note.setUpdatedBy(userId);

        DoctorHospitalNote updated = noteRepository.save(note);
        log.info("Updated doctor hospital note {}", noteId);
        return mapToResponse(updated, buildListContext(userId, organizationId));
    }

    private NoteListContext buildListContext(UUID userId, UUID organizationId) {
        return new NoteListContext(
            userId,
            doctorRepository.findByLinkedUserId(userId),
            rbacPermissionService.hasDoctorNotesManage(userId, organizationId),
            rbacPermissionService.hasHospitalManage(userId, organizationId));
    }

    private void requireCanModify(DoctorHospitalNote note, NoteListContext ctx) {
        if (!canUserModify(note, ctx)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Only the creator, the named doctor, or a hospital administrator can modify this note");
        }
    }

    private boolean canUserModify(DoctorHospitalNote note, NoteListContext ctx) {
        if (note.getCreatedBy() != null && note.getCreatedBy().equals(ctx.userId())) {
            return true;
        }
        if (ctx.linkedDoctor().isPresent()
                && ctx.linkedDoctor().get().getDoctorId().equals(note.getDoctorId())) {
            return true;
        }
        return ctx.hospitalAdmin();
    }

    private DoctorHospitalNoteResponse mapToResponse(DoctorHospitalNote note, NoteListContext ctx) {
        boolean canModify = ctx.hasManagePermission() && canUserModify(note, ctx);

        return DoctorHospitalNoteResponse.builder()
            .noteId(note.getNoteId())
            .doctorId(note.getDoctorId())
            .doctorName(note.getDoctorName())
            .message(note.getMessage())
            .createdBy(note.getCreatedBy())
            .createdAt(note.getCreatedAt())
            .updatedAt(note.getUpdatedAt())
            .updatedBy(note.getUpdatedBy())
            .canModify(canModify)
            .build();
    }
}
