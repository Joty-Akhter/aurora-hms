package com.easyops.rbac.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.UUID;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RbacPermissionDto {

    private UUID id;
    private String name;
    private String code;
    private String resource;
    private String action;
    private String description;
    private Boolean isActive;
}
