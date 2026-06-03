package com.easyops.hospital.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DuplicatePatientResponse {
    
    private Boolean hasDuplicates;
    private List<DuplicateMatch> matches;
    private String matchReason;
    /** When true, registration must not proceed (e.g. duplicate mobile number). */
    private Boolean phoneDuplicateBlocked;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DuplicateMatch {
        private UUID patientId;
        private String mrn;
        private String fullName;
        private String dateOfBirth;
        private String idNo;
        private String phone;
        private String email;
        private Double matchScore;
        private String matchReason;
    }
}
