package com.easyops.hr.service;

import com.easyops.hr.entity.Goal;
import com.easyops.hr.entity.GoalUpdate;
import com.easyops.hr.repository.GoalRepository;
import com.easyops.hr.repository.GoalUpdateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class GoalService {
    
    private final GoalRepository goalRepository;
    private final GoalUpdateRepository goalUpdateRepository;
    
    public List<Goal> getAllGoals(UUID organizationId) {
        return goalRepository.findByOrganizationId(organizationId);
    }
    
    public Goal getGoalById(UUID goalId) {
        return goalRepository.findById(goalId)
                .orElseThrow(() -> new RuntimeException("Goal not found"));
    }
    
    public List<Goal> getEmployeeGoals(UUID employeeId, UUID organizationId) {
        return goalRepository.findByEmployeeIdAndOrganizationId(employeeId, organizationId);
    }
    
    public List<Goal> getGoalsByStatus(UUID organizationId, String status) {
        return goalRepository.findByOrganizationIdAndStatus(organizationId, status);
    }
    
    public List<Goal> getCycleGoals(UUID cycleId) {
        return goalRepository.findByCycleId(cycleId);
    }
    
    public Goal createGoal(Goal goal) {
        log.info("Creating goal: {} for employee: {}", goal.getGoalTitle(), goal.getEmployeeId());
        goal.setStatus("in_progress");
        goal.setProgressPercentage(0);
        return goalRepository.save(goal);
    }
    
    public Goal updateGoal(UUID goalId, Goal goal) {
        Goal existing = getGoalById(goalId);
        
        if (goal.getGoalTitle() != null) existing.setGoalTitle(goal.getGoalTitle());
        if (goal.getGoalDescription() != null) existing.setGoalDescription(goal.getGoalDescription());
        if (goal.getGoalCategory() != null) existing.setGoalCategory(goal.getGoalCategory());
        if (goal.getTargetValue() != null) existing.setTargetValue(goal.getTargetValue());
        if (goal.getCurrentValue() != null) existing.setCurrentValue(goal.getCurrentValue());
        if (goal.getPriority() != null) existing.setPriority(goal.getPriority());
        if (goal.getWeight() != null) existing.setWeight(goal.getWeight());
        if (goal.getTargetDate() != null) existing.setTargetDate(goal.getTargetDate());
        if (goal.getStatus() != null) existing.setStatus(goal.getStatus());
        if (goal.getProgressPercentage() != null) existing.setProgressPercentage(goal.getProgressPercentage());
        if (goal.getNotes() != null) existing.setNotes(goal.getNotes());
        
        return goalRepository.save(existing);
    }
    
    public Goal updateGoalProgress(UUID goalId, GoalUpdate update) {
        Goal goal = getGoalById(goalId);
        
        if (update.getCurrentValue() != null) {
            goal.setCurrentValue(update.getCurrentValue());
        }
        if (update.getProgressPercentage() != null) {
            goal.setProgressPercentage(update.getProgressPercentage());
        }
        
        // Save the update record
        update.setGoalId(goalId);
        update.setUpdateDate(LocalDate.now());
        goalUpdateRepository.save(update);
        
        return goalRepository.save(goal);
    }
    
    public Goal completeGoal(UUID goalId) {
        Goal goal = getGoalById(goalId);
        goal.setStatus("completed");
        goal.setProgressPercentage(100);
        goal.setCompletionDate(LocalDate.now());
        return goalRepository.save(goal);
    }
    
    public void deleteGoal(UUID goalId) {
        goalRepository.deleteById(goalId);
    }
    
    public List<GoalUpdate> getGoalUpdates(UUID goalId) {
        return goalUpdateRepository.findByGoalIdOrderByUpdateDateDesc(goalId);
    }
}

