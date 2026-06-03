package com.easyops.hr.controller;

import com.easyops.hr.entity.EpfOrganizationPolicy;
import com.easyops.hr.repository.EpfOrganizationPolicyRepository;
import com.easyops.hr.security.HrRbacService;
import com.easyops.hr.security.RbacRequestHeaders;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * INT-09 / INT-12: Per-organization EPF rates, PF wage ceiling/floor, employment eligibility.
 */
@RestController
@RequestMapping("/api/hr/epf/organization-policy")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class EpfOrganizationPolicyController {

    private final EpfOrganizationPolicyRepository epfOrganizationPolicyRepository;
    private final HrRbacService hrRbac;

    @GetMapping
    public ResponseEntity<EpfOrganizationPolicy> getByOrganization(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId) {
        hrRbac.requireHrView(RbacRequestHeaders.requireUserId(userIdHeader), organizationId);
        return epfOrganizationPolicyRepository.findByOrganizationId(organizationId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping
    public ResponseEntity<EpfOrganizationPolicy> upsert(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestBody EpfOrganizationPolicy policy) {
        if (policy.getOrganizationId() == null) {
            return ResponseEntity.badRequest().build();
        }
        hrRbac.requirePfPolicyManage(RbacRequestHeaders.requireUserId(userIdHeader), policy.getOrganizationId());
        LocalDateTime now = LocalDateTime.now();
        Optional<EpfOrganizationPolicy> existing = epfOrganizationPolicyRepository.findByOrganizationId(policy.getOrganizationId());
        if (existing.isPresent()) {
            EpfOrganizationPolicy e = existing.get();
            e.setEmployeeContributionRate(policy.getEmployeeContributionRate());
            e.setEmployerContributionRate(policy.getEmployerContributionRate());
            e.setPfWageCeiling(policy.getPfWageCeiling());
            e.setPfWageFloor(policy.getPfWageFloor());
            e.setEligibleEmploymentTypes(policy.getEligibleEmploymentTypes());
            e.setIneligibleEmploymentTypes(policy.getIneligibleEmploymentTypes());
            e.setUpdatedAt(now);
            return ResponseEntity.ok(epfOrganizationPolicyRepository.save(e));
        }
        policy.setCreatedAt(now);
        policy.setUpdatedAt(now);
        return ResponseEntity.ok(epfOrganizationPolicyRepository.save(policy));
    }
}
