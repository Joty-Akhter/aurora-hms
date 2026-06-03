package com.easyops.hospitalcorporatediscount.api;

import com.easyops.hospitalcorporatediscount.api.dto.EvaluateDiscountsRequest;
import com.easyops.hospitalcorporatediscount.api.dto.EvaluateDiscountsResponse;
import com.easyops.hospitalcorporatediscount.domain.discount.DiscountEvaluationService;
import com.easyops.hospitalcorporatediscount.security.HospitalCorporateDiscountRbacService;
import com.easyops.hospitalcorporatediscount.security.RbacRequestHeaders;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/hospital-corporate-discount")
@RequiredArgsConstructor
public class DiscountEvaluationController {

    private final DiscountEvaluationService discountEvaluationService;
    private final HospitalCorporateDiscountRbacService hospitalCorporateDiscountRbac;

    @PostMapping("/discounts/evaluate")
    public EvaluateDiscountsResponse evaluate(
            @Valid @RequestBody EvaluateDiscountsRequest request,
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader(value = "X-Organization-Id", required = false) UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        hospitalCorporateDiscountRbac.requireHospitalView(actor, organizationId);
        return discountEvaluationService.evaluate(request);
    }
}
