package com.easyops.hr.dto;

import com.easyops.hr.entity.LoanCategoryType;
import com.easyops.hr.entity.LoanInterestMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class LoanCategoryUpdateRequest {

    @NotBlank
    @Size(max = 255)
    private String name;

    @Size(max = 1000)
    private String description;

    @NotNull
    private LoanCategoryType categoryType;

    @NotNull
    private Boolean isActive;

    @NotNull
    private Integer sortOrder;

    private BigDecimal maxPrincipalAmount;

    private Integer maxTenureMonths;

    private LoanInterestMethod interestMethod;

    private BigDecimal flatAnnualRatePercent;
}
