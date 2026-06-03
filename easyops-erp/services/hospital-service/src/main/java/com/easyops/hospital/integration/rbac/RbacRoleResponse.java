package com.easyops.hospital.integration.rbac;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.UUID;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RbacRoleResponse {
    private UUID id;
    private String code;
}
