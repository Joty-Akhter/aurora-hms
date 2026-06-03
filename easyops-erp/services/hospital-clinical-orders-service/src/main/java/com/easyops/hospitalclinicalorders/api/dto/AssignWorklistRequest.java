package com.easyops.hospitalclinicalorders.api.dto;

import java.util.UUID;

public class AssignWorklistRequest {
    private UUID assignedToUserId;
    private String assignedToRole;

    public UUID getAssignedToUserId() { return assignedToUserId; }
    public void setAssignedToUserId(UUID assignedToUserId) { this.assignedToUserId = assignedToUserId; }
    public String getAssignedToRole() { return assignedToRole; }
    public void setAssignedToRole(String assignedToRole) { this.assignedToRole = assignedToRole; }
}
