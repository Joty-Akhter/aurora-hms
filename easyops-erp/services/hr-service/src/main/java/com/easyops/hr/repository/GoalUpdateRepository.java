package com.easyops.hr.repository;

import com.easyops.hr.entity.GoalUpdate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GoalUpdateRepository extends JpaRepository<GoalUpdate, UUID> {
    
    List<GoalUpdate> findByGoalIdOrderByUpdateDateDesc(UUID goalId);
}

