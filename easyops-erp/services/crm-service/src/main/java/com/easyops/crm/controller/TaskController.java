package com.easyops.crm.controller;

import com.easyops.crm.entity.Task;
import com.easyops.crm.security.CrmRbacService;
import com.easyops.crm.security.RbacRequestHeaders;
import com.easyops.crm.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/tasks")
@CrossOrigin(origins = "*")
public class TaskController {

    @Autowired
    private TaskService taskService;

    @Autowired
    private CrmRbacService crmRbac;

    @GetMapping
    public ResponseEntity<List<Task>> getAllTasks(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam(required = false) UUID assignedTo,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority) {

        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        crmRbac.requireCrmView(actor, organizationId);

        List<Task> tasks;

        if (assignedTo != null) {
            tasks = taskService.getTasksByAssignedUser(organizationId, assignedTo);
        } else if (status != null) {
            tasks = taskService.getTasksByStatus(organizationId, status);
        } else if (priority != null) {
            tasks = taskService.getTasksByPriority(organizationId, priority);
        } else {
            tasks = taskService.getAllTasks(organizationId);
        }

        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Task> getTaskById(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        return taskService.getTaskById(id)
                .map(t -> {
                    crmRbac.requireCrmView(actor, t.getOrganizationId());
                    return ResponseEntity.ok(t);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Task> createTask(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestBody Task task) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        crmRbac.requireCrmManage(actor, task.getOrganizationId());
        Task created = taskService.createTask(task);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Task> updateTask(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id,
            @RequestBody Task task) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        crmRbac.requireCrmManage(actor, taskService.getOrganizationIdForTask(id));
        try {
            Task updated = taskService.updateTask(id, task);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        crmRbac.requireCrmManage(actor, taskService.getOrganizationIdForTask(id));
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/due-today")
    public ResponseEntity<List<Task>> getTasksDueToday(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam UUID userId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        crmRbac.requireCrmView(actor, organizationId);
        List<Task> tasks = taskService.getTasksDueToday(organizationId, userId);
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/overdue")
    public ResponseEntity<List<Task>> getOverdueTasks(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam UUID userId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        crmRbac.requireCrmView(actor, organizationId);
        List<Task> tasks = taskService.getOverdueTasks(organizationId, userId);
        return ResponseEntity.ok(tasks);
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<Task> completeTask(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id,
            @RequestBody Map<String, Object> request) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        crmRbac.requireCrmManage(actor, taskService.getOrganizationIdForTask(id));
        try {
            UUID completedBy = UUID.fromString(request.get("completedBy").toString());
            String notes = request.get("notes") != null ? request.get("notes").toString() : null;
            Task updated = taskService.completeTask(id, completedBy, notes);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getTaskStats(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam UUID userId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        crmRbac.requireCrmView(actor, organizationId);
        Map<String, Object> stats = taskService.getTaskStats(organizationId, userId);
        return ResponseEntity.ok(stats);
    }
}
