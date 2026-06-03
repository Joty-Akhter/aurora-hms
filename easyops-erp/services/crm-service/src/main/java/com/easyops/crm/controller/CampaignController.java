package com.easyops.crm.controller;

import com.easyops.crm.entity.Campaign;
import com.easyops.crm.entity.CampaignMember;
import com.easyops.crm.security.CrmRbacService;
import com.easyops.crm.security.RbacRequestHeaders;
import com.easyops.crm.service.CampaignService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/campaigns")
@CrossOrigin(origins = "*")
public class CampaignController {

    @Autowired
    private CampaignService campaignService;

    @Autowired
    private CrmRbacService crmRbac;

    @GetMapping
    public ResponseEntity<List<Campaign>> getAllCampaigns(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String search) {

        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        crmRbac.requireCrmView(actor, organizationId);

        List<Campaign> campaigns;

        if (search != null && !search.isEmpty()) {
            campaigns = campaignService.searchCampaigns(organizationId, search);
        } else if (status != null) {
            campaigns = campaignService.getCampaignsByStatus(organizationId, status);
        } else if (type != null) {
            campaigns = campaignService.getCampaignsByType(organizationId, type);
        } else {
            campaigns = campaignService.getAllCampaigns(organizationId);
        }

        return ResponseEntity.ok(campaigns);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Campaign> getCampaignById(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        return campaignService.getCampaignById(id)
                .map(c -> {
                    crmRbac.requireCrmView(actor, c.getOrganizationId());
                    return ResponseEntity.ok(c);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/number/{number}")
    public ResponseEntity<Campaign> getCampaignByNumber(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId,
            @PathVariable String number) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        crmRbac.requireCrmView(actor, organizationId);
        return campaignService.getCampaignByNumber(organizationId, number)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Campaign> createCampaign(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestBody Campaign campaign) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        crmRbac.requireCrmManage(actor, campaign.getOrganizationId());
        Campaign created = campaignService.createCampaign(campaign);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Campaign> updateCampaign(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id,
            @RequestBody Campaign campaign) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        crmRbac.requireCrmManage(actor, campaignService.getOrganizationIdForCampaign(id));
        try {
            Campaign updated = campaignService.updateCampaign(id, campaign);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCampaign(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        crmRbac.requireCrmManage(actor, campaignService.getOrganizationIdForCampaign(id));
        campaignService.deleteCampaign(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/active")
    public ResponseEntity<List<Campaign>> getActiveCampaigns(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestParam UUID organizationId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        crmRbac.requireCrmView(actor, organizationId);
        List<Campaign> campaigns = campaignService.getActiveCampaigns(organizationId);
        return ResponseEntity.ok(campaigns);
    }

    @GetMapping("/{id}/stats")
    public ResponseEntity<Map<String, Object>> getCampaignStats(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        crmRbac.requireCrmView(actor, campaignService.getOrganizationIdForCampaign(id));
        Map<String, Object> stats = campaignService.getCampaignStats(id);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<List<CampaignMember>> getCampaignMembers(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        crmRbac.requireCrmView(actor, campaignService.getOrganizationIdForCampaign(id));
        List<CampaignMember> members = campaignService.getCampaignMembers(id);
        return ResponseEntity.ok(members);
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<CampaignMember> addCampaignMember(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID id,
            @RequestBody CampaignMember member) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        crmRbac.requireCrmManage(actor, campaignService.getOrganizationIdForCampaign(id));
        member.setCampaignId(id);
        CampaignMember created = campaignService.addCampaignMember(member);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/members/{memberId}")
    public ResponseEntity<CampaignMember> updateCampaignMember(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID memberId,
            @RequestBody CampaignMember member) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        crmRbac.requireCrmManage(actor, campaignService.getOrganizationIdForCampaignMember(memberId));
        try {
            CampaignMember updated = campaignService.updateCampaignMember(memberId, member);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/members/{memberId}")
    public ResponseEntity<Void> deleteCampaignMember(
            @RequestHeader("X-User-Id") String userIdHeader,
            @PathVariable UUID memberId) {
        UUID actor = RbacRequestHeaders.requireUserId(userIdHeader);
        crmRbac.requireCrmManage(actor, campaignService.getOrganizationIdForCampaignMember(memberId));
        campaignService.deleteCampaignMember(memberId);
        return ResponseEntity.noContent().build();
    }
}
