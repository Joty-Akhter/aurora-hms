package com.easyops.hospital.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MedicalCodeUpsertRequest {

    @NotBlank(message = "Code is required")
    @Size(max = 20, message = "Code must be at most 20 characters")
    private String code;

    @NotBlank(message = "Description is required")
    private String description;

    @Size(max = 100, message = "Category must be at most 100 characters")
    private String category;

    @Size(max = 100, message = "Chapter must be at most 100 characters")
    private String chapter;

    private Boolean isValid;
}
