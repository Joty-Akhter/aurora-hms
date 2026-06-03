package com.easyops.crm.service;

import com.easyops.crm.entity.LeadSource;
import com.easyops.crm.repository.LeadSourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class LeadSourceService {

    private final LeadSourceRepository leadSourceRepository;

    public List<LeadSource> getAllLeadSources(UUID organizationId) {
        return leadSourceRepository.findByOrganizationId(organizationId);
    }

    public Optional<LeadSource> findLeadSourceById(UUID id) {
        return leadSourceRepository.findById(id);
    }

    public LeadSource getLeadSourceById(UUID id) {
        return findLeadSourceById(id)
                .orElseThrow(() -> new RuntimeException("Lead source not found"));
    }

    public LeadSource createLeadSource(LeadSource leadSource) {
        return leadSourceRepository.save(leadSource);
    }

    public LeadSource updateLeadSource(UUID id, LeadSource leadSourceDetails) {
        LeadSource source = getLeadSourceById(id);
        source.setSourceName(leadSourceDetails.getSourceName());
        source.setSourceCode(leadSourceDetails.getSourceCode());
        source.setSourceType(leadSourceDetails.getSourceType());
        source.setIsActive(leadSourceDetails.getIsActive());
        source.setDescription(leadSourceDetails.getDescription());
        source.setUpdatedBy(leadSourceDetails.getUpdatedBy());
        return leadSourceRepository.save(source);
    }

    public void deleteLeadSource(UUID id) {
        leadSourceRepository.deleteById(id);
    }

    public UUID getOrganizationIdForLeadSource(UUID leadSourceId) {
        return getLeadSourceById(leadSourceId).getOrganizationId();
    }
}
