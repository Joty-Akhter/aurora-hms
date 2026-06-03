package com.easyops.hospitalpharmacy.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProductGroupRequest {

    @NotBlank
    @Size(max = 200)
    private String name;

    private String description;

    private Boolean active;
}
