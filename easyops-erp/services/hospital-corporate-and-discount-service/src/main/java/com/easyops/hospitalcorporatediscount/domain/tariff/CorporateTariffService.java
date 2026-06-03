package com.easyops.hospitalcorporatediscount.domain.tariff;

import com.easyops.hospitalcorporatediscount.api.dto.CorporateTariffResponse;
import com.easyops.hospitalcorporatediscount.api.dto.CreateCorporateTariffRequest;
import com.easyops.hospitalcorporatediscount.domain.contract.CorporateContractRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CorporateTariffService {

    private final CorporateTariffRepository repository;
    private final CorporateContractRepository corporateContractRepository;

    @Transactional
    public CorporateTariffResponse create(UUID contractId, CreateCorporateTariffRequest request) {
        if (!corporateContractRepository.existsById(contractId)) {
            throw new NoSuchElementException("Contract not found: " + contractId);
        }
        CorporateTariff entity = new CorporateTariff();
        entity.setCorporateContractId(contractId);
        entity.setScopeType(request.getScopeType().trim());
        entity.setScopeValue(request.getScopeValue().trim());
        entity.setTariffType(request.getTariffType().trim());
        entity.setTariffAmount(request.getTariffAmount());
        entity.setTariffPercent(request.getTariffPercent());
        repository.save(entity);
        return toResponse(entity);
    }

    public List<CorporateTariffResponse> listByContractId(UUID contractId) {
        if (!corporateContractRepository.existsById(contractId)) {
            throw new NoSuchElementException("Contract not found: " + contractId);
        }
        return repository.findByCorporateContractIdOrderByScopeTypeAscScopeValueAsc(contractId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new NoSuchElementException("Tariff not found: " + id);
        }
        repository.deleteById(id);
    }

    private CorporateTariffResponse toResponse(CorporateTariff e) {
        CorporateTariffResponse r = new CorporateTariffResponse();
        r.setId(e.getId());
        r.setCorporateContractId(e.getCorporateContractId());
        r.setScopeType(e.getScopeType());
        r.setScopeValue(e.getScopeValue());
        r.setTariffType(e.getTariffType());
        r.setTariffAmount(e.getTariffAmount());
        r.setTariffPercent(e.getTariffPercent());
        r.setCreatedAt(e.getCreatedAt());
        return r;
    }
}
