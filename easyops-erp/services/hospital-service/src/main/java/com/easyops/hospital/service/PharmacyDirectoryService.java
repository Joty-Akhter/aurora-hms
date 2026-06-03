package com.easyops.hospital.service;

import com.easyops.hospital.dto.request.PharmacyDirectoryRequest;
import com.easyops.hospital.dto.response.PharmacyDirectoryResponse;
import com.easyops.hospital.entity.PharmacyDirectory;
import com.easyops.hospital.repository.PharmacyDirectoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * FR-P3.5: Business logic for the pharmacy directory master data.
 * <ul>
 *   <li>search — name / city / NPI fuzzy lookup for the prescription picker</li>
 *   <li>CRUD — admin create / update / deactivate</li>
 *   <li>Staleness alerting — entries not verified within {@link #STALE_DAYS} days</li>
 *   <li>Verify — update lastVerifiedAt for a confirmed entry</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PharmacyDirectoryService {

    /** Entries not verified within this many days are flagged as stale. */
    public static final int STALE_DAYS = 90;

    private final PharmacyDirectoryRepository pharmacyDirectoryRepository;

    // ========== Search ==========

    /**
     * Search the pharmacy directory for the prescription picker.
     *
     * @param q              free-text query matched against name, city, NPI (may be null)
     * @param state          filter by state abbreviation (may be null)
     * @param eprescribingOnly when true only returns pharmacies capable of receiving e-Rx
     */
    public List<PharmacyDirectoryResponse> search(String q, String state, boolean eprescribingOnly) {
        String normalised = (q != null && !q.isBlank()) ? q.trim() : null;
        String normalisedState = (state != null && !state.isBlank()) ? state.trim() : null;
        return pharmacyDirectoryRepository.search(normalised, normalisedState, eprescribingOnly)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ========== Lookup ==========

    public PharmacyDirectoryResponse getById(UUID id) {
        return mapToResponse(findOrThrow(id));
    }

    public PharmacyDirectoryResponse getByNpi(String npi) {
        return pharmacyDirectoryRepository.findByNpi(npi)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No pharmacy found with NPI: " + npi));
    }

    public List<PharmacyDirectoryResponse> getAll() {
        return pharmacyDirectoryRepository.findByIsActiveTrueOrderByNameAsc()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ========== Admin CRUD ==========

    @Transactional
    public PharmacyDirectoryResponse create(PharmacyDirectoryRequest request, UUID userId) {
        if (request.getNpi() != null && !request.getNpi().isBlank()) {
            pharmacyDirectoryRepository.findByNpi(request.getNpi()).ifPresent(existing -> {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "A pharmacy with NPI " + request.getNpi() + " already exists (id=" + existing.getId() + ")");
            });
        }

        PharmacyDirectory entity = PharmacyDirectory.builder()
                .name(request.getName())
                .npi(blankToNull(request.getNpi()))
                .ncpdpId(blankToNull(request.getNcpdpId()))
                .addressLine1(request.getAddressLine1())
                .addressLine2(request.getAddressLine2())
                .city(request.getCity())
                .state(request.getState())
                .zip(request.getZip())
                .country(request.getCountry() != null ? request.getCountry() : "US")
                .phone(request.getPhone())
                .fax(request.getFax())
                .email(request.getEmail())
                .isEprescribingCapable(Boolean.TRUE.equals(request.getIsEprescribingCapable()))
                .eprescribingNetwork(request.getEprescribingNetwork())
                .dataSource(request.getDataSource() != null ? request.getDataSource() : PharmacyDirectory.DataSource.MANUAL)
                .verificationNotes(request.getVerificationNotes())
                .notes(request.getNotes())
                .createdBy(userId)
                .build();

        PharmacyDirectory saved = pharmacyDirectoryRepository.save(entity);
        log.info("Created pharmacy directory entry '{}' (id={})", saved.getName(), saved.getId());
        return mapToResponse(saved);
    }

    @Transactional
    public PharmacyDirectoryResponse update(UUID id, PharmacyDirectoryRequest request, UUID userId) {
        PharmacyDirectory entity = findOrThrow(id);

        // NPI uniqueness guard — allow keeping the same NPI on itself
        if (request.getNpi() != null && !request.getNpi().isBlank()
                && !request.getNpi().equals(entity.getNpi())) {
            pharmacyDirectoryRepository.findByNpi(request.getNpi()).ifPresent(conflict -> {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "NPI " + request.getNpi() + " is already used by pharmacy id=" + conflict.getId());
            });
        }

        entity.setName(request.getName());
        if (request.getNpi() != null) entity.setNpi(blankToNull(request.getNpi()));
        if (request.getNcpdpId() != null) entity.setNcpdpId(blankToNull(request.getNcpdpId()));
        if (request.getAddressLine1() != null) entity.setAddressLine1(request.getAddressLine1());
        if (request.getAddressLine2() != null) entity.setAddressLine2(request.getAddressLine2());
        if (request.getCity() != null) entity.setCity(request.getCity());
        if (request.getState() != null) entity.setState(request.getState());
        if (request.getZip() != null) entity.setZip(request.getZip());
        if (request.getCountry() != null) entity.setCountry(request.getCountry());
        if (request.getPhone() != null) entity.setPhone(request.getPhone());
        if (request.getFax() != null) entity.setFax(request.getFax());
        if (request.getEmail() != null) entity.setEmail(request.getEmail());
        if (request.getIsEprescribingCapable() != null) entity.setIsEprescribingCapable(request.getIsEprescribingCapable());
        if (request.getEprescribingNetwork() != null) entity.setEprescribingNetwork(request.getEprescribingNetwork());
        if (request.getDataSource() != null) entity.setDataSource(request.getDataSource());
        if (request.getVerificationNotes() != null) entity.setVerificationNotes(request.getVerificationNotes());
        if (request.getNotes() != null) entity.setNotes(request.getNotes());
        entity.setUpdatedBy(userId);

        PharmacyDirectory saved = pharmacyDirectoryRepository.save(entity);
        log.info("Updated pharmacy directory entry '{}' (id={})", saved.getName(), saved.getId());
        return mapToResponse(saved);
    }

    @Transactional
    public void deactivate(UUID id, UUID userId) {
        PharmacyDirectory entity = findOrThrow(id);
        entity.setIsActive(false);
        entity.setUpdatedBy(userId);
        pharmacyDirectoryRepository.save(entity);
        log.info("Deactivated pharmacy directory entry id={}", id);
    }

    // ========== Staleness ==========

    /**
     * Return all active entries whose {@code lastVerifiedAt} is null or older than {@link #STALE_DAYS} days.
     * Used by admin dashboards and alerting jobs.
     */
    public List<PharmacyDirectoryResponse> getStaleEntries() {
        OffsetDateTime threshold = OffsetDateTime.now().minusDays(STALE_DAYS);
        return pharmacyDirectoryRepository.findStale(threshold)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Mark a pharmacy as verified now, optionally recording a note.
     */
    @Transactional
    public PharmacyDirectoryResponse markVerified(UUID id, String verificationNotes, UUID userId) {
        PharmacyDirectory entity = findOrThrow(id);
        entity.setLastVerifiedAt(OffsetDateTime.now());
        if (verificationNotes != null) entity.setVerificationNotes(verificationNotes);
        entity.setUpdatedBy(userId);
        PharmacyDirectory saved = pharmacyDirectoryRepository.save(entity);
        log.info("Marked pharmacy id={} as verified at {}", id, saved.getLastVerifiedAt());
        return mapToResponse(saved);
    }

    // ========== Mapping ==========

    private PharmacyDirectoryResponse mapToResponse(PharmacyDirectory p) {
        boolean stale = p.getLastVerifiedAt() == null
                || p.getLastVerifiedAt().isBefore(OffsetDateTime.now().minusDays(STALE_DAYS));
        return PharmacyDirectoryResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .npi(p.getNpi())
                .ncpdpId(p.getNcpdpId())
                .addressLine1(p.getAddressLine1())
                .addressLine2(p.getAddressLine2())
                .city(p.getCity())
                .state(p.getState())
                .zip(p.getZip())
                .country(p.getCountry())
                .phone(p.getPhone())
                .fax(p.getFax())
                .email(p.getEmail())
                .isEprescribingCapable(p.getIsEprescribingCapable())
                .eprescribingNetwork(p.getEprescribingNetwork())
                .dataSource(p.getDataSource())
                .lastVerifiedAt(p.getLastVerifiedAt())
                .verificationNotes(p.getVerificationNotes())
                .isActive(p.getIsActive())
                .isStale(stale)
                .notes(p.getNotes())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .createdBy(p.getCreatedBy())
                .updatedBy(p.getUpdatedBy())
                .build();
    }

    // ========== Helpers ==========

    private PharmacyDirectory findOrThrow(UUID id) {
        return pharmacyDirectoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Pharmacy not found: " + id));
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
