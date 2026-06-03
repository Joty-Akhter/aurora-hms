package com.easyops.hospital.service;

import com.easyops.hospital.repository.EpLookupItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EpLookupService {

    private final EpLookupItemRepository repository;

    /**
     * Returns all active lookup items grouped by category, ordered by display_order within each category.
     * Keys are category names (e.g. "DOSAGE_FORM", "REFERRAL").
     */
    @Transactional(readOnly = true)
    public Map<String, List<String>> getAllGrouped() {
        return repository.findByActiveTrueOrderByCategoryAscDisplayOrderAsc()
                .stream()
                .collect(Collectors.groupingBy(
                        item -> item.getCategory(),
                        LinkedHashMap::new,
                        Collectors.mapping(item -> item.getValue(), Collectors.toList())
                ));
    }

    /**
     * Returns active lookup values for a single category, ordered by display_order.
     */
    @Transactional(readOnly = true)
    public List<String> getByCategory(String category) {
        return repository
                .findByCategoryAndActiveTrueOrderByDisplayOrderAsc(category.toUpperCase())
                .stream()
                .map(item -> item.getValue())
                .collect(Collectors.toList());
    }
}
