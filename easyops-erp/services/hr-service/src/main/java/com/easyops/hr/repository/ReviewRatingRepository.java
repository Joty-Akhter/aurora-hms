package com.easyops.hr.repository;

import com.easyops.hr.entity.ReviewRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReviewRatingRepository extends JpaRepository<ReviewRating, UUID> {
    
    List<ReviewRating> findByReviewId(UUID reviewId);
    
    List<ReviewRating> findByReviewIdAndCompetencyId(UUID reviewId, UUID competencyId);
}

