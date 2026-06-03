package com.easyops.hr.dto;

import com.easyops.hr.entity.LoanCategoryType;
import com.easyops.hr.entity.LoanInterestMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanCategoryDto {

    private UUID categoryId;
    private UUID organizationId;
    private String code;
    private String name;
    private String description;
    private LoanCategoryType categoryType;
    private Boolean isActive;
    private Integer sortOrder;
    private BigDecimal maxPrincipalAmount;
    private Integer maxTenureMonths;
    private LoanInterestMethod interestMethod;
    private BigDecimal flatAnnualRatePercent;
    /** LC-04: human-readable note when interest method is not NONE. */
    private String scheduleInterestNote;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
