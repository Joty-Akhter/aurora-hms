package com.easyops.hospital.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorHospitalNoteResponse {

    private UUID noteId;
    private UUID doctorId;
    private String doctorName;
    private String message;
    private UUID createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private UUID updatedBy;

    /** Whether the requesting user may edit or delete this note. */
    private Boolean canModify;
}
