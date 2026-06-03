package com.easyops.crm.service;

import com.easyops.crm.entity.Task;
import com.easyops.crm.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class TaskService {
    
    @Autowired
    private TaskRepository taskRepository;
    
    public List<Task> getAllTasks(UUID organizationId) {
        return taskRepository.findByOrganizationIdOrderByDueDateAsc(organizationId);
    }
    
    public Optional<Task> getTaskById(UUID taskId) {
        return taskRepository.findById(taskId);
    }

    public UUID getOrganizationIdForTask(UUID taskId) {
        return getTaskById(taskId)
                .map(Task::getOrganizationId)
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + taskId));
    }
    
    public Task createTask(Task task) {
        return taskRepository.save(task);
    }
    
    public Task updateTask(UUID taskId, Task taskDetails) {
        return taskRepository.findById(taskId)
                .map(task -> {
                    task.setSubject(taskDetails.getSubject());
                    task.setDescription(taskDetails.getDescription());
                    task.setTaskType(taskDetails.getTaskType());
                    task.setStatus(taskDetails.getStatus());
                    task.setPriority(taskDetails.getPriority());
                    task.setDueDate(taskDetails.getDueDate());
                    task.setReminderDate(taskDetails.getReminderDate());
                    task.setCompletedDate(taskDetails.getCompletedDate());
                    task.setAssignedTo(taskDetails.getAssignedTo());
                    task.setLeadId(taskDetails.getLeadId());
                    task.setContactId(taskDetails.getContactId());
                    task.setAccountId(taskDetails.getAccountId());
                    task.setOpportunityId(taskDetails.getOpportunityId());
                    task.setCampaignId(taskDetails.getCampaignId());
                    task.setCompletedBy(taskDetails.getCompletedBy());
                    task.setCompletionNotes(taskDetails.getCompletionNotes());
                    task.setIsRecurring(taskDetails.getIsRecurring());
                    task.setRecurrencePattern(taskDetails.getRecurrencePattern());
                    task.setRecurrenceEndDate(taskDetails.getRecurrenceEndDate());
                    task.setUpdatedBy(taskDetails.getUpdatedBy());
                    return taskRepository.save(task);
                })
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + taskId));
    }
    
    public void deleteTask(UUID taskId) {
        taskRepository.deleteById(taskId);
    }
    
    public List<Task> getTasksByAssignedUser(UUID organizationId, UUID userId) {
        return taskRepository.findByOrganizationIdAndAssignedTo(organizationId, userId);
    }
    
    public List<Task> getTasksByStatus(UUID organizationId, String status) {
        return taskRepository.findByOrganizationIdAndStatus(organizationId, status);
    }
    
    public List<Task> getTasksByPriority(UUID organizationId, String priority) {
        return taskRepository.findByOrganizationIdAndPriority(organizationId, priority);
    }
    
    public List<Task> getTasksDueToday(UUID organizationId, UUID userId) {
        return taskRepository.findDueTasksForUserOnDate(organizationId, userId, LocalDate.now());
    }
    
    public List<Task> getOverdueTasks(UUID organizationId, UUID userId) {
        return taskRepository.findOverdueTasksForUser(organizationId, userId, LocalDate.now());
    }
    
    public Task completeTask(UUID taskId, UUID completedBy, String completionNotes) {
        return taskRepository.findById(taskId)
                .map(task -> {
                    task.setStatus("COMPLETED");
                    task.setCompletedDate(LocalDate.now());
                    task.setCompletedBy(completedBy);
                    task.setCompletionNotes(completionNotes);
                    return taskRepository.save(task);
                })
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + taskId));
    }
    
    public Map<String, Object> getTaskStats(UUID organizationId, UUID userId) {
        Map<String, Object> stats = new HashMap<>();
        List<Task> allTasks = taskRepository.findByOrganizationIdAndAssignedTo(organizationId, userId);
        
        long total = allTasks.size();
        long dueToday = taskRepository.findDueTasksForUserOnDate(organizationId, userId, LocalDate.now()).size();
        long overdue = taskRepository.findOverdueTasksForUser(organizationId, userId, LocalDate.now()).size();
        long completed = allTasks.stream().filter(t -> "COMPLETED".equals(t.getStatus())).count();
        
        stats.put("total", total);
        stats.put("dueToday", dueToday);
        stats.put("overdue", overdue);
        stats.put("completed", completed);
        stats.put("pending", total - completed);
        
        return stats;
    }
}

