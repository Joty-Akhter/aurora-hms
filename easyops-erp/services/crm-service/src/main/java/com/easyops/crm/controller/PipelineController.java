package com.easyops.crm.controller;

import com.easyops.crm.entity.OpportunityStage;
import com.easyops.crm.security.CrmRbacService;
import com.easyops.crm.security.RbacRequestHeaders;
import com.easyops.crm.service.PipelineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/pipeline")
@CrossOrigin(origins = "*")
public class PipelineController {

    @Autowired
    private PipelineService pipelineService;

    @Autowired
    private CrmRbacService crmRbac;

    @GetMapping("/stages")
    public ResponseEntity<List<OpportunityStage>> getAllStages(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam(required = false, defaultValue = "false") Boolean activeOnly) {

        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        crmRbac.requireCrmView(actor, organizationId);

        List<OpportunityStage> stages;
        if (activeOnly) {
            stages = pipelineService.getActiveStages(organizationId);
        } else {
            stages = pipelineService.getAllStages(organizationId);
        }
        return ResponseEntity.ok(stages);
    }

    @GetMapping("/stages/{id}")
    public ResponseEntity<OpportunityStage> getStageById(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        return pipelineService.getStageById(id)
                .map(stage -> {
                    crmRbac.requireCrmView(actor, stage.getOrganizationId());
                    return ResponseEntity.ok(stage);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/stages/code/{code}")
    public ResponseEntity<OpportunityStage> getStageByCode(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @PathVariable String code) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        crmRbac.requireCrmView(actor, organizationId);
        return pipelineService.getStageByCode(organizationId, code)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/stages")
    public ResponseEntity<OpportunityStage> createStage(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestBody OpportunityStage stage) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        crmRbac.requireCrmManage(actor, stage.getOrganizationId());
        OpportunityStage created = pipelineService.createStage(stage);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/stages/{id}")
    public ResponseEntity<OpportunityStage> updateStage(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id,
            @RequestBody OpportunityStage stage) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        crmRbac.requireCrmManage(actor, pipelineService.getOrganizationIdForStage(id));
        try {
            OpportunityStage updated = pipelineService.updateStage(id, stage);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/stages/{id}")
    public ResponseEntity<Void> deleteStage(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        crmRbac.requireCrmManage(actor, pipelineService.getOrganizationIdForStage(id));
        pipelineService.deleteStage(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getPipelineStats(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        crmRbac.requireCrmView(actor, organizationId);
        Map<String, Object> stats = pipelineService.getPipelineStats(organizationId);
        return ResponseEntity.ok(stats);
    }
}
