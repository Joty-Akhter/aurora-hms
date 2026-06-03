package com.easyops.hospital.controller;

import com.easyops.hospital.dto.request.DoctorHospitalNoteRequest;
import com.easyops.hospital.dto.request.DoctorHospitalNoteUpdateRequest;
import com.easyops.hospital.dto.response.DoctorHospitalNoteResponse;
import com.easyops.hospital.service.DoctorHospitalNoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/doctor-hospital-notes")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Doctor Hospital Notes", description = "Hospital-wide messages from doctors to staff (not patient-specific)")
public class DoctorHospitalNoteController {

    private final DoctorHospitalNoteService doctorHospitalNoteService;

    @GetMapping
    @Operation(summary = "List doctor hospital notes", description = "All broadcast notes from doctors, newest first")
    public ResponseEntity<List<DoctorHospitalNoteResponse>> listNotes(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        return ResponseEntity.ok(doctorHospitalNoteService.listNotes(userId, organizationId));
    }

    @PostMapping
    @Operation(summary = "Create doctor hospital note", description = "Post a message on behalf of a doctor")
    public ResponseEntity<DoctorHospitalNoteResponse> createNote(
            @Valid @RequestBody DoctorHospitalNoteRequest request,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        log.info("Creating doctor hospital note for doctor {}", request.getDoctorId());
        DoctorHospitalNoteResponse response = doctorHospitalNoteService.createNote(request, userId, organizationId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{noteId}")
    @Operation(summary = "Update doctor hospital note", description = "Update the message text (creator or named doctor only)")
    public ResponseEntity<DoctorHospitalNoteResponse> updateNote(
            @PathVariable UUID noteId,
            @Valid @RequestBody DoctorHospitalNoteUpdateRequest request,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        log.info("Updating doctor hospital note {}", noteId);
        return ResponseEntity.ok(doctorHospitalNoteService.updateNote(noteId, request, userId, organizationId));
    }

    @DeleteMapping("/{noteId}")
    @Operation(summary = "Delete doctor hospital note", description = "Delete a note (creator or named doctor only)")
    public ResponseEntity<Void> deleteNote(
            @PathVariable UUID noteId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        log.info("Deleting doctor hospital note {}", noteId);
        doctorHospitalNoteService.deleteNote(noteId, userId, organizationId);
        return ResponseEntity.noContent().build();
    }
}
