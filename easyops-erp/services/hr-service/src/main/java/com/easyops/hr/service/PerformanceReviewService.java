package com.easyops.hr.service;

import com.easyops.hr.entity.Competency;
import com.easyops.hr.entity.PerformanceCycle;
import com.easyops.hr.entity.PerformanceReview;
import com.easyops.hr.entity.ReviewRating;
import com.easyops.hr.repository.CompetencyRepository;
import com.easyops.hr.repository.PerformanceCycleRepository;
import com.easyops.hr.repository.PerformanceReviewRepository;
import com.easyops.hr.repository.ReviewRatingRepository;
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
public class PerformanceReviewService {
    
    private final PerformanceReviewRepository reviewRepository;
    private final PerformanceCycleRepository cycleRepository;
    private final CompetencyRepository competencyRepository;
    private final ReviewRatingRepository ratingRepository;
    
    // Cycle Management
    public List<PerformanceCycle> getAllCycles(UUID organizationId) {
        return cycleRepository.findByOrganizationId(organizationId);
    }

    public PerformanceCycle getCycleById(UUID cycleId) {
        return cycleRepository.findById(cycleId)
                .orElseThrow(() -> new RuntimeException("Performance cycle not found"));
    }
    
    public PerformanceCycle createCycle(PerformanceCycle cycle) {
        log.info("Creating performance cycle: {}", cycle.getCycleName());
        cycle.setStatus("planned");
        return cycleRepository.save(cycle);
    }
    
    public PerformanceCycle updateCycle(UUID cycleId, PerformanceCycle cycle) {
        PerformanceCycle existing = cycleRepository.findById(cycleId)
                .orElseThrow(() -> new RuntimeException("Performance cycle not found"));
        
        if (cycle.getCycleName() != null) existing.setCycleName(cycle.getCycleName());
        if (cycle.getStatus() != null) existing.setStatus(cycle.getStatus());
        if (cycle.getStartDate() != null) existing.setStartDate(cycle.getStartDate());
        if (cycle.getEndDate() != null) existing.setEndDate(cycle.getEndDate());
        if (cycle.getReviewStartDate() != null) existing.setReviewStartDate(cycle.getReviewStartDate());
        if (cycle.getReviewEndDate() != null) existing.setReviewEndDate(cycle.getReviewEndDate());
        if (cycle.getDescription() != null) existing.setDescription(cycle.getDescription());
        
        return cycleRepository.save(existing);
    }
    
    // Review Management
    public List<PerformanceReview> getAllReviews(UUID organizationId) {
        return reviewRepository.findByOrganizationId(organizationId);
    }
    
    public PerformanceReview getReviewById(UUID reviewId) {
        return reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Performance review not found"));
    }
    
    public List<PerformanceReview> getEmployeeReviews(UUID employeeId, UUID organizationId) {
        return reviewRepository.findByEmployeeIdAndOrganizationId(employeeId, organizationId);
    }
    
    public PerformanceReview createReview(PerformanceReview review) {
        log.info("Creating performance review for employee: {}", review.getEmployeeId());
        review.setStatus("draft");
        return reviewRepository.save(review);
    }
    
    public PerformanceReview updateReview(UUID reviewId, PerformanceReview review) {
        PerformanceReview existing = getReviewById(reviewId);
        
        if (review.getOverallRating() != null) existing.setOverallRating(review.getOverallRating());
        if (review.getStrengths() != null) existing.setStrengths(review.getStrengths());
        if (review.getAreasForImprovement() != null) existing.setAreasForImprovement(review.getAreasForImprovement());
        if (review.getAchievements() != null) existing.setAchievements(review.getAchievements());
        if (review.getComments() != null) existing.setComments(review.getComments());
        if (review.getRecommendedAction() != null) existing.setRecommendedAction(review.getRecommendedAction());
        if (review.getSelfReviewCompleted() != null) existing.setSelfReviewCompleted(review.getSelfReviewCompleted());
        if (review.getManagerReviewCompleted() != null) existing.setManagerReviewCompleted(review.getManagerReviewCompleted());
        if (review.getHrReviewCompleted() != null) existing.setHrReviewCompleted(review.getHrReviewCompleted());
        
        return reviewRepository.save(existing);
    }
    
    public PerformanceReview submitReview(UUID reviewId) {
        PerformanceReview review = getReviewById(reviewId);
        review.setStatus("submitted");
        review.setSubmittedAt(LocalDateTime.now());
        return reviewRepository.save(review);
    }
    
    public PerformanceReview approveReview(UUID reviewId, UUID approvedBy) {
        PerformanceReview review = getReviewById(reviewId);
        review.setStatus("approved");
        review.setApprovedBy(approvedBy);
        review.setApprovedAt(LocalDateTime.now());
        return reviewRepository.save(review);
    }
    
    // Competency Management
    public List<Competency> getAllCompetencies(UUID organizationId) {
        return competencyRepository.findByOrganizationId(organizationId);
    }
    
    public Competency createCompetency(Competency competency) {
        return competencyRepository.save(competency);
    }
    
    // Rating Management
    public ReviewRating addRating(ReviewRating rating) {
        return ratingRepository.save(rating);
    }
    
    public List<ReviewRating> getReviewRatings(UUID reviewId) {
        return ratingRepository.findByReviewId(reviewId);
    }
}

