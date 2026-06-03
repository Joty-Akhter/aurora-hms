package com.easyops.hr.controller;

import com.easyops.hr.entity.DevelopmentPlan;
import com.easyops.hr.entity.Feedback360;
import com.easyops.hr.entity.OneOnOneMeeting;
import com.easyops.hr.entity.TrainingCertification;
import com.easyops.hr.security.HrRbacService;
import com.easyops.hr.security.RbacRequestHeaders;
import com.easyops.hr.service.DevelopmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/hr/development")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class DevelopmentController {
    
    private final DevelopmentService developmentService;
    private final HrRbacService hrRbac;
    
    // Development Plans
    @GetMapping("/plans")
    public ResponseEntity<List<DevelopmentPlan>> getAllDevelopmentPlans(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam(required = false) UUID employeeId) {
        
        log.info("GET /development/plans - organizationId: {}, employeeId: {}", 
                organizationId, employeeId);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrView(actor, organizationId);
        
        List<DevelopmentPlan> plans;
        
        if (employeeId != null) {
            plans = developmentService.getEmployeeDevelopmentPlans(employeeId, organizationId);
        } else {
            plans = developmentService.getAllDevelopmentPlans(organizationId);
        }
        
        return ResponseEntity.ok(plans);
    }
    
    @GetMapping("/plans/{id}")
    public ResponseEntity<DevelopmentPlan> getDevelopmentPlanById(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id) {
        log.info("GET /development/plans/{}", id);
        DevelopmentPlan plan = developmentService.getDevelopmentPlanById(id);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrView(actor, plan.getOrganizationId());
        return ResponseEntity.ok(plan);
    }
    
    @PostMapping("/plans")
    public ResponseEntity<DevelopmentPlan> createDevelopmentPlan(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestBody DevelopmentPlan plan) {
        log.info("POST /development/plans - Creating plan: {}", plan.getPlanName());
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrManage(actor, plan.getOrganizationId());
        DevelopmentPlan created = developmentService.createDevelopmentPlan(plan);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    @PutMapping("/plans/{id}")
    public ResponseEntity<DevelopmentPlan> updateDevelopmentPlan(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id,
            @RequestBody DevelopmentPlan plan) {
        log.info("PUT /development/plans/{}", id);
        DevelopmentPlan existing = developmentService.getDevelopmentPlanById(id);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrManage(actor, existing.getOrganizationId());
        DevelopmentPlan updated = developmentService.updateDevelopmentPlan(id, plan);
        return ResponseEntity.ok(updated);
    }
    
    // Training & Certifications
    @GetMapping("/training")
    public ResponseEntity<List<TrainingCertification>> getAllTrainings(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam(required = false) UUID employeeId) {
        
        log.info("GET /development/training - organizationId: {}, employeeId: {}", 
                organizationId, employeeId);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrView(actor, organizationId);
        
        List<TrainingCertification> trainings;
        
        if (employeeId != null) {
            trainings = developmentService.getEmployeeTrainings(employeeId, organizationId);
        } else {
            trainings = developmentService.getAllTrainings(organizationId);
        }
        
        return ResponseEntity.ok(trainings);
    }
    
    @PostMapping("/training")
    public ResponseEntity<TrainingCertification> createTraining(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestBody TrainingCertification training) {
        log.info("POST /development/training - Creating training: {}", training.getTrainingName());
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrManage(actor, training.getOrganizationId());
        TrainingCertification created = developmentService.createTraining(training);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    @PutMapping("/training/{id}")
    public ResponseEntity<TrainingCertification> updateTraining(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id,
            @RequestBody TrainingCertification training) {
        log.info("PUT /development/training/{}", id);
        TrainingCertification existing = developmentService.getTrainingById(id);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrManage(actor, existing.getOrganizationId());
        TrainingCertification updated = developmentService.updateTraining(id, training);
        return ResponseEntity.ok(updated);
    }
    
    // One-on-One Meetings
    @GetMapping("/one-on-ones")
    public ResponseEntity<List<OneOnOneMeeting>> getAllOneOnOnes(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam(required = false) UUID employeeId) {
        
        log.info("GET /development/one-on-ones - organizationId: {}, employeeId: {}", 
                organizationId, employeeId);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrView(actor, organizationId);
        
        List<OneOnOneMeeting> meetings;
        
        if (employeeId != null) {
            meetings = developmentService.getEmployeeOneOnOnes(employeeId, organizationId);
        } else {
            meetings = developmentService.getAllOneOnOnes(organizationId);
        }
        
        return ResponseEntity.ok(meetings);
    }
    
    @PostMapping("/one-on-ones")
    public ResponseEntity<OneOnOneMeeting> createOneOnOne(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestBody OneOnOneMeeting meeting) {
        log.info("POST /development/one-on-ones - Scheduling meeting for employee: {}", 
                meeting.getEmployeeId());
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrManage(actor, meeting.getOrganizationId());
        OneOnOneMeeting created = developmentService.createOneOnOne(meeting);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    @PutMapping("/one-on-ones/{id}")
    public ResponseEntity<OneOnOneMeeting> updateOneOnOne(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id,
            @RequestBody OneOnOneMeeting meeting) {
        log.info("PUT /development/one-on-ones/{}", id);
        OneOnOneMeeting existing = developmentService.getOneOnOneById(id);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrManage(actor, existing.getOrganizationId());
        OneOnOneMeeting updated = developmentService.updateOneOnOne(id, meeting);
        return ResponseEntity.ok(updated);
    }
    
    // 360 Feedback
    @GetMapping("/feedback360")
    public ResponseEntity<List<Feedback360>> getEmployeeFeedback(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID employeeId,
            @RequestParam UUID organizationId) {
        
        log.info("GET /development/feedback360 - employeeId: {}", employeeId);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrView(actor, organizationId);
        List<Feedback360> feedback = developmentService.getEmployeeFeedback(employeeId, organizationId);
        return ResponseEntity.ok(feedback);
    }
    
    @PostMapping("/feedback360")
    public ResponseEntity<Feedback360> submitFeedback(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestBody Feedback360 feedback) {
        log.info("POST /development/feedback360 - Submitting feedback for employee: {}", 
                feedback.getEmployeeId());
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrManage(actor, feedback.getOrganizationId());
        Feedback360 submitted = developmentService.submitFeedback(feedback);
        return ResponseEntity.status(HttpStatus.CREATED).body(submitted);
    }
}
