package com.easyops.hr.service;

import com.easyops.hr.entity.DevelopmentPlan;
import com.easyops.hr.entity.Feedback360;
import com.easyops.hr.entity.OneOnOneMeeting;
import com.easyops.hr.entity.TrainingCertification;
import com.easyops.hr.repository.DevelopmentPlanRepository;
import com.easyops.hr.repository.Feedback360Repository;
import com.easyops.hr.repository.OneOnOneMeetingRepository;
import com.easyops.hr.repository.TrainingCertificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DevelopmentService {
    
    private final DevelopmentPlanRepository developmentPlanRepository;
    private final TrainingCertificationRepository trainingRepository;
    private final OneOnOneMeetingRepository meetingRepository;
    private final Feedback360Repository feedback360Repository;
    
    // Development Plan Methods
    public List<DevelopmentPlan> getAllDevelopmentPlans(UUID organizationId) {
        return developmentPlanRepository.findByOrganizationId(organizationId);
    }
    
    public DevelopmentPlan getDevelopmentPlanById(UUID planId) {
        return developmentPlanRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Development plan not found"));
    }
    
    public List<DevelopmentPlan> getEmployeeDevelopmentPlans(UUID employeeId, UUID organizationId) {
        return developmentPlanRepository.findByEmployeeIdAndOrganizationId(employeeId, organizationId);
    }
    
    public DevelopmentPlan createDevelopmentPlan(DevelopmentPlan plan) {
        log.info("Creating development plan: {} for employee: {}", plan.getPlanName(), plan.getEmployeeId());
        plan.setStatus("active");
        return developmentPlanRepository.save(plan);
    }
    
    public DevelopmentPlan updateDevelopmentPlan(UUID planId, DevelopmentPlan plan) {
        DevelopmentPlan existing = getDevelopmentPlanById(planId);
        
        if (plan.getPlanName() != null) existing.setPlanName(plan.getPlanName());
        if (plan.getStatus() != null) existing.setStatus(plan.getStatus());
        if (plan.getTargetCompletionDate() != null) existing.setTargetCompletionDate(plan.getTargetCompletionDate());
        if (plan.getObjectives() != null) existing.setObjectives(plan.getObjectives());
        if (plan.getActionItems() != null) existing.setActionItems(plan.getActionItems());
        if (plan.getProgressNotes() != null) existing.setProgressNotes(plan.getProgressNotes());
        
        return developmentPlanRepository.save(existing);
    }
    
    // Training Methods
    public List<TrainingCertification> getAllTrainings(UUID organizationId) {
        return trainingRepository.findByOrganizationId(organizationId);
    }
    
    public List<TrainingCertification> getEmployeeTrainings(UUID employeeId, UUID organizationId) {
        return trainingRepository.findByEmployeeIdAndOrganizationId(employeeId, organizationId);
    }
    
    public TrainingCertification createTraining(TrainingCertification training) {
        log.info("Creating training record: {} for employee: {}", training.getTrainingName(), training.getEmployeeId());
        return trainingRepository.save(training);
    }

    public TrainingCertification getTrainingById(UUID trainingId) {
        return trainingRepository.findById(trainingId)
                .orElseThrow(() -> new RuntimeException("Training record not found"));
    }
    
    public TrainingCertification updateTraining(UUID trainingId, TrainingCertification training) {
        TrainingCertification existing = trainingRepository.findById(trainingId)
                .orElseThrow(() -> new RuntimeException("Training record not found"));
        
        if (training.getStatus() != null) existing.setStatus(training.getStatus());
        if (training.getCompletionDate() != null) existing.setCompletionDate(training.getCompletionDate());
        if (training.getScore() != null) existing.setScore(training.getScore());
        if (training.getCertificateUrl() != null) existing.setCertificateUrl(training.getCertificateUrl());
        if (training.getNotes() != null) existing.setNotes(training.getNotes());
        
        return trainingRepository.save(existing);
    }
    
    // One-on-One Methods
    public List<OneOnOneMeeting> getAllOneOnOnes(UUID organizationId) {
        return meetingRepository.findByOrganizationId(organizationId);
    }
    
    public List<OneOnOneMeeting> getEmployeeOneOnOnes(UUID employeeId, UUID organizationId) {
        return meetingRepository.findByEmployeeIdAndOrganizationId(employeeId, organizationId);
    }
    
    public OneOnOneMeeting createOneOnOne(OneOnOneMeeting meeting) {
        log.info("Scheduling one-on-one meeting for employee: {}", meeting.getEmployeeId());
        meeting.setStatus("scheduled");
        return meetingRepository.save(meeting);
    }

    public OneOnOneMeeting getOneOnOneById(UUID meetingId) {
        return meetingRepository.findById(meetingId)
                .orElseThrow(() -> new RuntimeException("One-on-one meeting not found"));
    }
    
    public OneOnOneMeeting updateOneOnOne(UUID meetingId, OneOnOneMeeting meeting) {
        OneOnOneMeeting existing = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new RuntimeException("One-on-one meeting not found"));
        
        if (meeting.getStatus() != null) existing.setStatus(meeting.getStatus());
        if (meeting.getDiscussionPoints() != null) existing.setDiscussionPoints(meeting.getDiscussionPoints());
        if (meeting.getActionItems() != null) existing.setActionItems(meeting.getActionItems());
        if (meeting.getEmployeeNotes() != null) existing.setEmployeeNotes(meeting.getEmployeeNotes());
        if (meeting.getManagerNotes() != null) existing.setManagerNotes(meeting.getManagerNotes());
        if (meeting.getNextMeetingDate() != null) existing.setNextMeetingDate(meeting.getNextMeetingDate());
        
        return meetingRepository.save(existing);
    }
    
    // 360 Feedback Methods
    public List<Feedback360> getEmployeeFeedback(UUID employeeId, UUID organizationId) {
        return feedback360Repository.findByEmployeeIdAndOrganizationId(employeeId, organizationId);
    }
    
    public Feedback360 submitFeedback(Feedback360 feedback) {
        log.info("Submitting 360 feedback for employee: {}", feedback.getEmployeeId());
        feedback.setStatus("submitted");
        feedback.setSubmittedAt(LocalDateTime.now());
        return feedback360Repository.save(feedback);
    }
}

