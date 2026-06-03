package com.easyops.hospital.exception;

import com.easyops.hospital.dto.response.DuplicatePatientResponse;
import lombok.Getter;

@Getter
public class DuplicatePatientConflictException extends RuntimeException {

    private final DuplicatePatientResponse duplicatePatientResponse;

    public DuplicatePatientConflictException(DuplicatePatientResponse duplicatePatientResponse) {
        super("Potential duplicate patients found");
        this.duplicatePatientResponse = duplicatePatientResponse;
    }
}
