package com.easyops.organization.dto;

import lombok.Data;

@Data
public class OrganizationAppDataRequest {
    private String type;
    private String code;
    private String name;
    private String description;
    private Integer displayOrder;
    private Boolean isActive;
}

