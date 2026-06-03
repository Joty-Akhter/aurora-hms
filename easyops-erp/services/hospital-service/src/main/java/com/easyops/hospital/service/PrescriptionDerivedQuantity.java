package com.easyops.hospital.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Derives a total unit count (tablets/doses to dispense) from {@code frequency} and
 * {@code durationDays}, matching the Easy Prescription UI rules: shorthand like {@code 1+0+1}
 * sums to doses per day, multiplied by duration and rounded up; PRN/stat defaults to 1.
 */
public final class PrescriptionDerivedQuantity {

    private static final Pattern NUMERIC_TOKEN = Pattern.compile("^\\d*\\.?\\d+$");
    private static final Map<String, Double> NAMED_DOSES_PER_DAY = Map.ofEntries(
            Map.entry("once daily", 1d),
            Map.entry("twice daily", 2d),
            Map.entry("three times daily", 3d),
            Map.entry("four times daily", 4d),
            Map.entry("every 8 hours", 3d),
            Map.entry("every 12 hours", 2d),
            Map.entry("every 6 hours", 4d),
            Map.entry("once a week", 1d / 7d),
            Map.entry("twice a week", 2d / 7d),
            Map.entry("once a month", 1d / 30d),
            Map.entry("as needed (prn)", 1d),
            Map.entry("stat (once only)", 1d)
    );

    private PrescriptionDerivedQuantity() {
    }

    /**
     * Total units for the course: {@code ceil(dosesPerDay × durationDays)}, or {@code 1} when
     * PRN/stat or when frequency/duration cannot be combined.
     */
    public static BigDecimal deriveUnits(String frequency, Integer durationDays) {
        if (isPrnOrStat(frequency)) {
            return BigDecimal.ONE;
        }
        double dpm = parseFrequencyDosesPerDay(frequency);
        int days = durationDays != null ? durationDays : 0;
        if (dpm > 0 && days > 0) {
            return BigDecimal.valueOf(Math.ceil(dpm * days)).setScale(0, RoundingMode.UNNECESSARY);
        }
        return BigDecimal.ONE;
    }

    static boolean isPrnOrStat(String frequency) {
        if (frequency == null || frequency.isBlank()) {
            return false;
        }
        String f = frequency.toLowerCase(Locale.ROOT);
        return f.contains("prn") || f.contains("as needed") || f.contains("stat");
    }

    static double parseFrequencyDosesPerDay(String frequency) {
        if (frequency == null || frequency.isBlank()) {
            return 0d;
        }
        String normalised = normaliseFraction(frequency).replaceAll("\\s+", "");
        String[] parts = normalised.split("[+\\-×x]");
        if (parts.length >= 2) {
            boolean allNumeric = true;
            for (String p : parts) {
                String t = p.trim();
                if (t.isEmpty() || !NUMERIC_TOKEN.matcher(t).matches()) {
                    allNumeric = false;
                    break;
                }
            }
            if (allNumeric) {
                double sum = 0d;
                int limit = Math.min(4, parts.length);
                for (int i = 0; i < limit; i++) {
                    sum += Double.parseDouble(parts[i].trim());
                }
                return sum;
            }
        }
        Double named = NAMED_DOSES_PER_DAY.get(frequency.toLowerCase(Locale.ROOT).trim());
        return named != null ? named : 0d;
    }

    private static String normaliseFraction(String s) {
        return s.trim()
                .replace("½", "0.5")
                .replace("⅓", "0.333")
                .replace("¼", "0.25")
                .replace("¾", "0.75")
                .replace("⅔", "0.667");
    }
}
