package com.easyops.hospitalcorporatediscount.api;

import com.easyops.hospitalcorporatediscount.api.dto.EvaluateCoverageRequest;
import com.easyops.hospitalcorporatediscount.api.dto.EvaluateCoverageResponse;
import com.easyops.hospitalcorporatediscount.domain.coverage.CoverageEvaluationService;
import com.easyops.hospitalcorporatediscount.security.HospitalCorporateDiscountRbacService;
import com.easyops.hospitalcorporatediscount.security.RbacRequestHeaders;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/hospital-corporate-discount")
@RequiredArgsConstructor
public class CoverageEvaluationController {

    private final CoverageEvaluationService coverageEvaluationService;
    private final HospitalCorporateDiscountRbacService hospitalCorporateDiscountRbac;

    @PostMapping("/coverage/evaluate")
    public EvaluateCoverageResponse evaluate(
            @Valid @RequestBody EvaluateCoverageRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalCorporateDiscountRbac.requireHospitalView(actor, organizationId);
        return coverageEvaluationService.evaluate(request);
    }
}
