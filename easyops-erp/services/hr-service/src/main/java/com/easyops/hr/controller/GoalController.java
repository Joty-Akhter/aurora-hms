package com.easyops.hr.controller;

import com.easyops.hr.entity.Goal;
import com.easyops.hr.entity.GoalUpdate;
import com.easyops.hr.security.HrRbacService;
import com.easyops.hr.security.RbacRequestHeaders;
import com.easyops.hr.service.GoalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/hr/goals")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class GoalController {
    
    private final GoalService goalService;
    private final HrRbacService hrRbac;
    
    @GetMapping
    public ResponseEntity<List<Goal>> getAllGoals(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam(required = false) UUID employeeId,
            @RequestParam(required = false) String status) {
        
        log.info("GET /goals - organizationId: {}, employeeId: {}, status: {}", 
                organizationId, employeeId, status);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrView(actor, organizationId);
        
        List<Goal> goals;
        
        if (employeeId != null) {
            goals = goalService.getEmployeeGoals(employeeId, organizationId);
        } else if (status != null) {
            goals = goalService.getGoalsByStatus(organizationId, status);
        } else {
            goals = goalService.getAllGoals(organizationId);
        }
        
        return ResponseEntity.ok(goals);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Goal> getGoalById(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id) {
        log.info("GET /goals/{}", id);
        Goal goal = goalService.getGoalById(id);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrView(actor, goal.getOrganizationId());
        return ResponseEntity.ok(goal);
    }
    
    @GetMapping("/cycle/{cycleId}")
    public ResponseEntity<List<Goal>> getCycleGoals(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID cycleId) {
        log.info("GET /goals/cycle/{}", cycleId);
        List<Goal> goals = goalService.getCycleGoals(cycleId);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        UUID org = goals.isEmpty() ? null : goals.get(0).getOrganizationId();
        hrRbac.requireHrView(actor, org);
        return ResponseEntity.ok(goals);
    }
    
    @PostMapping
    public ResponseEntity<Goal> createGoal(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestBody Goal goal) {
        log.info("POST /goals - Creating goal: {}", goal.getGoalTitle());
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrManage(actor, goal.getOrganizationId());
        Goal created = goalService.createGoal(goal);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Goal> updateGoal(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id,
            @RequestBody Goal goal) {
        log.info("PUT /goals/{}", id);
        Goal existing = goalService.getGoalById(id);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrManage(actor, existing.getOrganizationId());
        Goal updated = goalService.updateGoal(id, goal);
        return ResponseEntity.ok(updated);
    }
    
    @PostMapping("/{id}/update-progress")
    public ResponseEntity<Goal> updateGoalProgress(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id,
            @RequestBody GoalUpdate update) {
        log.info("POST /goals/{}/update-progress", id);
        Goal existing = goalService.getGoalById(id);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrManage(actor, existing.getOrganizationId());
        Goal updated = goalService.updateGoalProgress(id, update);
        return ResponseEntity.ok(updated);
    }
    
    @PostMapping("/{id}/complete")
    public ResponseEntity<Goal> completeGoal(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id) {
        log.info("POST /goals/{}/complete", id);
        Goal existing = goalService.getGoalById(id);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrManage(actor, existing.getOrganizationId());
        Goal completed = goalService.completeGoal(id);
        return ResponseEntity.ok(completed);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGoal(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id) {
        log.info("DELETE /goals/{}", id);
        Goal existing = goalService.getGoalById(id);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrManage(actor, existing.getOrganizationId());
        goalService.deleteGoal(id);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/{id}/updates")
    public ResponseEntity<List<GoalUpdate>> getGoalUpdates(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id) {
        log.info("GET /goals/{}/updates", id);
        Goal goal = goalService.getGoalById(id);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrView(actor, goal.getOrganizationId());
        List<GoalUpdate> updates = goalService.getGoalUpdates(id);
        return ResponseEntity.ok(updates);
    }
}
