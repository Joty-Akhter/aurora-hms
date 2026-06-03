package com.easyops.hospital.integration.usermanagement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.UUID;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserManagementUserResponse {
    private UUID id;
    private String username;
}
