package com.easyops.hospital.service;

import com.easyops.hospital.entity.EpLookupItem;
import com.easyops.hospital.repository.EpAdviceUserUsageRepository;
import com.easyops.hospital.repository.EpLookupItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EpAdviceCatalogService {

    static final String CATEGORY_ADVICE = "ADVICE";
    private static final int MAX_LINES_PER_REQUEST = 64;
    private static final int MAX_VALUE_LENGTH = 1000;
    private static final int DEFAULT_SUGGESTION_POOL = 400;
    /** Bound substring-filter input so pathological query strings cannot inflate CPU/memory. */
    private static final int MAX_QUERY_CHARS = 256;

    private final EpLookupItemRepository lookupItemRepository;
    private final EpAdviceUserUsageRepository usageRepository;

    public static String normalizeForMatch(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    public static String canonicalDisplay(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().replaceAll("\\s+", " ");
    }

    @Transactional(readOnly = true)
    public List<String> getSuggestions(UUID userId, String query, int limit) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User id is required");
        }
        int cap = Math.min(Math.max(limit, 1), 200);
        String qraw = query == null ? "" : query;
        if (qraw.length() > MAX_QUERY_CHARS) {
            qraw = qraw.substring(0, MAX_QUERY_CHARS);
        }
        String qnorm = normalizeForMatch(qraw);

        List<String> rankedPersonal = new ArrayList<>();
        usageRepository.findRankedForUser(userId).stream()
                .limit(DEFAULT_SUGGESTION_POOL)
                .map(u -> u.getAdviceLookupItem() != null ? u.getAdviceLookupItem().getValue() : null)
                .filter(v -> v != null && !v.isBlank())
                .filter(v -> qnorm.isEmpty() || normalizeForMatch(v).contains(qnorm))
                .forEach(rankedPersonal::add);

        Set<String> seen = new LinkedHashSet<>(rankedPersonal);

        List<String> catalogRest = lookupItemRepository.findByCategoryAndActiveTrueOrderByDisplayOrderAsc(CATEGORY_ADVICE)
                .stream()
                .map(EpLookupItem::getValue)
                .filter(v -> v != null && !v.isBlank())
                .filter(v -> qnorm.isEmpty() || normalizeForMatch(v).contains(qnorm))
                .filter(seen::add)
                .collect(Collectors.toList());

        List<String> merged = new ArrayList<>(rankedPersonal.size() + catalogRest.size());
        merged.addAll(rankedPersonal);
        merged.addAll(catalogRest);
        // One display row per normalized text (DB unique key is case-sensitive; legacy duplicates possible).
        List<String> dedupedNorm = new ArrayList<>();
        Set<String> normSeen = new LinkedHashSet<>();
        for (String v : merged) {
            if (v == null || v.isBlank()) {
                continue;
            }
            String n = normalizeForMatch(v);
            if (n.isEmpty() || !normSeen.add(n)) {
                continue;
            }
            dedupedNorm.add(v);
            if (dedupedNorm.size() >= cap) {
                break;
            }
        }
        return List.copyOf(dedupedNorm);
    }

    @Transactional
    public void ensureLines(List<String> lines) {
        for (String line : dedupeLines(lines)) {
            findOrCreateAdviceItem(line);
        }
    }

    @Transactional
    public void dismissSuggestions(UUID userId, List<String> lines) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User id is required");
        }
        for (String line : dedupeLines(lines)) {
            String norm = normalizeForMatch(line);
            if (norm.isEmpty()) {
                continue;
            }
            lookupItemRepository.findAdviceByNormalizedPreferActive(norm).ifPresent(item -> {
                if (item.getId() != null) {
                    usageRepository.deleteByUserIdAndLookupId(userId, item.getId());
                }
            });
        }
    }

    @Transactional
    public void recordUsage(UUID userId, List<String> lines) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User id is required");
        }
        LocalDateTime now = LocalDateTime.now();
        for (String line : dedupeLines(lines)) {
            EpLookupItem item = findOrCreateAdviceItem(line);
            bumpUsage(userId, item, now);
        }
    }

    private void bumpUsage(UUID userId, EpLookupItem item, LocalDateTime now) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User id is required");
        }
        if (item == null || item.getId() == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Advice catalog row is missing an identifier; cannot record usage.");
        }
        usageRepository.upsertIncrementUsage(userId, item.getId(), now);
    }

    private List<String> dedupeLines(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return List.of();
        }
        Set<String> seenNorm = new LinkedHashSet<>();
        List<String> out = new ArrayList<>();
        for (String raw : lines) {
            String canon = canonicalDisplay(raw);
            if (canon.isEmpty()) {
                continue;
            }
            if (canon.length() > MAX_VALUE_LENGTH) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Advice line exceeds " + MAX_VALUE_LENGTH + " characters");
            }
            String norm = normalizeForMatch(canon);
            if (seenNorm.add(norm)) {
                out.add(canon);
            }
        }
        if (out.size() > MAX_LINES_PER_REQUEST) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At most " + MAX_LINES_PER_REQUEST + " distinct advice lines per request");
        }
        return out;
    }

    private EpLookupItem findOrCreateAdviceItem(String canonicalLine) {
        String norm = normalizeForMatch(canonicalLine);
        if (norm.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Advice text is empty");
        }
        Optional<EpLookupItem> existing = lookupItemRepository.findAdviceByNormalizedPreferActive(norm);
        if (existing.isPresent()) {
            EpLookupItem row = existing.get();
            if (!Boolean.TRUE.equals(row.getActive())) {
                row.setActive(true);
                row.setUpdatedAt(LocalDateTime.now());
                return lookupItemRepository.save(row);
            }
            return row;
        }

        int nextOrder = Optional.ofNullable(lookupItemRepository.maxDisplayOrderForCategory(CATEGORY_ADVICE)).orElse(0) + 1;
        LocalDateTime now = LocalDateTime.now();
        EpLookupItem created = EpLookupItem.builder()
                .category(CATEGORY_ADVICE)
                .value(canonicalLine)
                .displayOrder(nextOrder)
                .active(true)
                .createdAt(now)
                .updatedAt(now)
                .build();
        try {
            return lookupItemRepository.save(created);
        } catch (DataIntegrityViolationException ex) {
            // Concurrent insert with same normalized text but different casing / race on display_order
            EpLookupItem row = lookupItemRepository.findAdviceByNormalizedPreferActive(norm)
                    .orElseThrow(() -> ex);
            if (!Boolean.TRUE.equals(row.getActive())) {
                row.setActive(true);
                row.setUpdatedAt(LocalDateTime.now());
                return lookupItemRepository.save(row);
            }
            return row;
        }
    }
}
