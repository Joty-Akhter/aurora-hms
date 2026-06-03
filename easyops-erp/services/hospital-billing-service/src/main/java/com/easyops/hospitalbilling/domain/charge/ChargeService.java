package com.easyops.hospitalbilling.domain.charge;

import com.easyops.hospitalbilling.api.dto.ChargeResponse;
import com.easyops.hospitalbilling.api.dto.CreateChargeRequest;
import com.easyops.hospitalbilling.api.dto.PagedResponse;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChargeService {

    private final ChargeLineRepository chargeLineRepository;
    private final MeterRegistry meterRegistry;

    @Transactional
    public List<ChargeResponse> createCharges(List<CreateChargeRequest> requests) {
        List<ChargeLine> result = requests.stream()
                .map(this::createOrGetExisting)
                .collect(Collectors.toList());
        meterRegistry.counter("billing_charges_created_total").increment(result.size());
        return result.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ChargeResponse getCharge(UUID id) {
        ChargeLine entity = chargeLineRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Charge not found: " + id));
        return toDto(entity);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ChargeResponse> listCharges(
            UUID patientId,
            UUID visitId,
            List<String> statuses,
            String sourceService,
            OffsetDateTime createdFrom,
            OffsetDateTime createdTo,
            int page,
            int size
    ) {
        PageRequest pageRequest = PageRequest.of(page, size);
        Specification<ChargeLine> spec = Specification
                .where(ChargeSpecifications.hasPatientId(patientId))
                .and(ChargeSpecifications.hasVisitId(visitId))
                .and(ChargeSpecifications.hasStatuses(statuses))
                .and(ChargeSpecifications.hasSourceService(sourceService))
                .and(ChargeSpecifications.createdAtBetween(createdFrom, createdTo));

        Page<ChargeLine> result = chargeLineRepository.findAll(spec, pageRequest);
        PagedResponse<ChargeResponse> response = new PagedResponse<>();
        response.setContent(result.getContent().stream().map(this::toDto).collect(Collectors.toList()));
        response.setTotalElements(result.getTotalElements());
        response.setTotalPages(result.getTotalPages());
        response.setPage(result.getNumber());
        response.setSize(result.getSize());
        return response;
    }

    private ChargeLine createOrGetExisting(CreateChargeRequest request) {
        if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank()) {
            return chargeLineRepository.findByIdempotencyKey(request.getIdempotencyKey())
                    .orElseGet(() -> chargeLineRepository.save(toEntity(request)));
        }
        return chargeLineRepository.save(toEntity(request));
    }

    private ChargeLine toEntity(CreateChargeRequest request) {
        ChargeLine e = new ChargeLine();
        e.setId(UUID.randomUUID());
        e.setSourceService(request.getSourceService());
        e.setSourceReferenceId(request.getSourceReferenceId());
        e.setPatientId(request.getPatientId());
        e.setVisitId(request.getVisitId());
        e.setCorporateContractId(request.getCorporateContractId());
        e.setItemCode(request.getItemCode());
        e.setItemDescription(request.getItemDescription());

        BigDecimal quantity = defaultValue(request.getQuantity());
        BigDecimal unitPrice = defaultValue(request.getUnitPrice());
        BigDecimal discount = defaultValue(request.getDiscountAmount());
        BigDecimal tax = defaultValue(request.getTaxAmount());

        BigDecimal gross = quantity.multiply(unitPrice);
        BigDecimal net = gross.subtract(discount).add(tax);

        e.setQuantity(quantity);
        e.setUnitPrice(unitPrice);
        e.setGrossAmount(gross);
        e.setDiscountAmount(discount);
        e.setDiscountSource(request.getDiscountSource());
        e.setTaxAmount(tax);
        e.setNetAmount(net);
        e.setStatus("PENDING");
        e.setIdempotencyKey(request.getIdempotencyKey());
        OffsetDateTime now = OffsetDateTime.now();
        e.setCreatedAt(now);
        e.setUpdatedAt(now);
        return e;
    }

    private ChargeResponse toDto(ChargeLine e) {
        ChargeResponse dto = new ChargeResponse();
        dto.setId(e.getId());
        dto.setSourceService(e.getSourceService());
        dto.setSourceReferenceId(e.getSourceReferenceId());
        dto.setPatientId(e.getPatientId());
        dto.setVisitId(e.getVisitId());
        dto.setCorporateContractId(e.getCorporateContractId());
        dto.setItemCode(e.getItemCode());
        dto.setItemDescription(e.getItemDescription());
        dto.setQuantity(e.getQuantity());
        dto.setUnitPrice(e.getUnitPrice());
        dto.setGrossAmount(e.getGrossAmount());
        dto.setDiscountAmount(e.getDiscountAmount());
        dto.setDiscountSource(e.getDiscountSource());
        dto.setTaxAmount(e.getTaxAmount());
        dto.setNetAmount(e.getNetAmount());
        dto.setStatus(e.getStatus());
        dto.setInvoiceId(e.getInvoiceId());
        return dto;
    }

    private BigDecimal defaultValue(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}

