package com.easyops.hr.controller;

import com.easyops.hr.entity.Competency;
import com.easyops.hr.entity.PerformanceCycle;
import com.easyops.hr.entity.PerformanceReview;
import com.easyops.hr.entity.ReviewRating;
import com.easyops.hr.security.HrRbacService;
import com.easyops.hr.security.RbacRequestHeaders;
import com.easyops.hr.service.PerformanceReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/hr/performance")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class PerformanceReviewController {
    
    private final PerformanceReviewService performanceReviewService;
    private final HrRbacService hrRbac;
    
    // Cycle Management
    @GetMapping("/cycles")
    public ResponseEntity<List<PerformanceCycle>> getAllCycles(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId) {
        log.info("GET /performance/cycles - organizationId: {}", organizationId);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrView(actor, organizationId);
        List<PerformanceCycle> cycles = performanceReviewService.getAllCycles(organizationId);
        return ResponseEntity.ok(cycles);
    }
    
    @PostMapping("/cycles")
    public ResponseEntity<PerformanceCycle> createCycle(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestBody PerformanceCycle cycle) {
        log.info("POST /performance/cycles - Creating cycle: {}", cycle.getCycleName());
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrManage(actor, cycle.getOrganizationId());
        PerformanceCycle created = performanceReviewService.createCycle(cycle);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    @PutMapping("/cycles/{id}")
    public ResponseEntity<PerformanceCycle> updateCycle(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id,
            @RequestBody PerformanceCycle cycle) {
        log.info("PUT /performance/cycles/{}", id);
        PerformanceCycle existing = performanceReviewService.getCycleById(id);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrManage(actor, existing.getOrganizationId());
        PerformanceCycle updated = performanceReviewService.updateCycle(id, cycle);
        return ResponseEntity.ok(updated);
    }
    
    // Review Management
    @GetMapping("/reviews")
    public ResponseEntity<List<PerformanceReview>> getAllReviews(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam(required = false) UUID employeeId) {
        
        log.info("GET /performance/reviews - organizationId: {}, employeeId: {}", 
                organizationId, employeeId);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrView(actor, organizationId);
        
        List<PerformanceReview> reviews;
        
        if (employeeId != null) {
            reviews = performanceReviewService.getEmployeeReviews(employeeId, organizationId);
        } else {
            reviews = performanceReviewService.getAllReviews(organizationId);
        }
        
        return ResponseEntity.ok(reviews);
    }
    
    @GetMapping("/reviews/{id}")
    public ResponseEntity<PerformanceReview> getReviewById(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id) {
        log.info("GET /performance/reviews/{}", id);
        PerformanceReview review = performanceReviewService.getReviewById(id);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrView(actor, review.getOrganizationId());
        return ResponseEntity.ok(review);
    }
    
    @PostMapping("/reviews")
    public ResponseEntity<PerformanceReview> createReview(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestBody PerformanceReview review) {
        log.info("POST /performance/reviews - Creating review for employee: {}", review.getEmployeeId());
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrManage(actor, review.getOrganizationId());
        PerformanceReview created = performanceReviewService.createReview(review);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    @PutMapping("/reviews/{id}")
    public ResponseEntity<PerformanceReview> updateReview(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id,
            @RequestBody PerformanceReview review) {
        log.info("PUT /performance/reviews/{}", id);
        PerformanceReview existing = performanceReviewService.getReviewById(id);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrManage(actor, existing.getOrganizationId());
        PerformanceReview updated = performanceReviewService.updateReview(id, review);
        return ResponseEntity.ok(updated);
    }
    
    @PostMapping("/reviews/{id}/submit")
    public ResponseEntity<PerformanceReview> submitReview(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id) {
        log.info("POST /performance/reviews/{}/submit", id);
        PerformanceReview existing = performanceReviewService.getReviewById(id);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrManage(actor, existing.getOrganizationId());
        PerformanceReview submitted = performanceReviewService.submitReview(id);
        return ResponseEntity.ok(submitted);
    }
    
    @PostMapping("/reviews/{id}/approve")
    public ResponseEntity<PerformanceReview> approveReview(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id,
            @RequestBody Map<String, String> request) {
        
        UUID approvedBy = UUID.fromString(request.get("approvedBy"));
        log.info("POST /performance/reviews/{}/approve - approvedBy: {}", id, approvedBy);
        PerformanceReview existing = performanceReviewService.getReviewById(id);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrManage(actor, existing.getOrganizationId());
        PerformanceReview approved = performanceReviewService.approveReview(id, approvedBy);
        return ResponseEntity.ok(approved);
    }
    
    // Competency Management
    @GetMapping("/competencies")
    public ResponseEntity<List<Competency>> getAllCompetencies(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId) {
        log.info("GET /performance/competencies - organizationId: {}", organizationId);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrView(actor, organizationId);
        List<Competency> competencies = performanceReviewService.getAllCompetencies(organizationId);
        return ResponseEntity.ok(competencies);
    }
    
    @PostMapping("/competencies")
    public ResponseEntity<Competency> createCompetency(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestBody Competency competency) {
        log.info("POST /performance/competencies - Creating competency: {}", competency.getCompetencyName());
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrManage(actor, competency.getOrganizationId());
        Competency created = performanceReviewService.createCompetency(competency);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    // Rating Management
    @PostMapping("/reviews/{reviewId}/ratings")
    public ResponseEntity<ReviewRating> addRating(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID reviewId,
            @RequestBody ReviewRating rating) {
        
        rating.setReviewId(reviewId);
        log.info("POST /performance/reviews/{}/ratings", reviewId);
        PerformanceReview review = performanceReviewService.getReviewById(reviewId);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrManage(actor, review.getOrganizationId());
        ReviewRating created = performanceReviewService.addRating(rating);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    @GetMapping("/reviews/{reviewId}/ratings")
    public ResponseEntity<List<ReviewRating>> getReviewRatings(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID reviewId) {
        log.info("GET /performance/reviews/{}/ratings", reviewId);
        PerformanceReview review = performanceReviewService.getReviewById(reviewId);
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hrRbac.requireHrView(actor, review.getOrganizationId());
        List<ReviewRating> ratings = performanceReviewService.getReviewRatings(reviewId);
        return ResponseEntity.ok(ratings);
    }
}
