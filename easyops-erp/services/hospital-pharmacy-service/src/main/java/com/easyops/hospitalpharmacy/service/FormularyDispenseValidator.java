package com.easyops.hospitalpharmacy.service;

import com.easyops.hospitalpharmacy.dto.request.DispenseLineRequest;
import com.easyops.hospitalpharmacy.entity.DispenseOrder;
import com.easyops.hospitalpharmacy.entity.Drug;
import com.easyops.hospitalpharmacy.entity.FormularyRule;
import com.easyops.hospitalpharmacy.repository.DrugRepository;
import com.easyops.hospitalpharmacy.repository.FormularyRuleRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Phase P3 WS-G — formulary restriction checks for dispense lines.
 */
@Service
@RequiredArgsConstructor
public class FormularyDispenseValidator {

    private final FormularyRuleRepository formularyRuleRepository;
    private final DrugRepository drugRepository;
    private final ObjectMapper objectMapper;

    public void validateDispenseLine(DispenseOrder order, Drug dispensedDrug, DispenseLineRequest req) {
        UUID subjectId = req.getSubstitutedDrugId() != null ? req.getSubstitutedDrugId() : req.getDrugId();
        Drug formularySubject = drugRepository.findById(subjectId)
                .orElseThrow(() -> new IllegalArgumentException("Drug not found for formulary check: " + subjectId));

        List<FormularyRule> rules = formularyRuleRepository.findByDrug(formularySubject);
        List<FormularyRule> restricted = rules.stream()
                .filter(FormularyRule::isRestricted)
                .filter(r -> ruleMatchesOrder(r, order))
                .toList();
        if (restricted.isEmpty()) {
            return;
        }

        boolean substitution = req.getSubstitutedDrugId() != null
                && !req.getSubstitutedDrugId().equals(req.getDrugId());
        if (substitution) {
            Set<UUID> allowed = new LinkedHashSet<>();
            for (FormularyRule r : restricted) {
                allowed.addAll(parsePreferredAlternativeIds(r));
            }
            if (!allowed.contains(dispensedDrug.getId())) {
                throw new IllegalArgumentException(
                        "Dispensed drug must be a configured formulary alternative when substituting for restricted drug "
                                + formularySubject.getId());
            }
            return;
        }

        String reason = req.getFormularyOverrideReason();
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException(
                    "Formulary restriction applies to drug " + formularySubject.getId()
                            + ". Provide formularyOverrideReason, or set substitutedDrugId to the restricted drug and drugId to a preferred alternative.");
        }
    }

    private static boolean ruleMatchesOrder(FormularyRule rule, DispenseOrder order) {
        if (rule.getDepartmentId() != null) {
            return rule.getDepartmentId().equals(order.getDepartmentId());
        }
        if (rule.getWardId() != null) {
            return false;
        }
        return true;
    }

    private List<UUID> parsePreferredAlternativeIds(FormularyRule rule) {
        String json = rule.getPreferredAlternativeDrugIdsJson();
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            List<String> raw = objectMapper.readValue(json, new TypeReference<List<String>>() {});
            List<UUID> out = new ArrayList<>();
            for (String s : raw) {
                if (s != null && !s.isBlank()) {
                    out.add(UUID.fromString(s.trim()));
                }
            }
            return out;
        } catch (Exception ex) {
            throw new IllegalStateException("Invalid preferred_alternative_drug_ids JSON for formulary rule " + rule.getId(), ex);
        }
    }
}
