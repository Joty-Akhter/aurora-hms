package com.easyops.hospital.dto.request;

import com.easyops.hospital.entity.ClinicalNote;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoteSignRequest {
    
    @NotNull(message = "Signature method is required")
    private ClinicalNote.SignatureMethod signatureMethod;
    
    private String notes;
}
