package com.easyops.hospital.service;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validates U.S. DEA Controlled Substance Registration numbers (FR-P2 / FR-P1.10 / §3.1.7).
 *
 * <h3>DEA number format</h3>
 * A DEA number is exactly 9 characters: 2 upper-case letters followed by 7 decimal digits.
 * <pre>
 *   [registrant-type][last-name-initial][d1][d2][d3][d4][d5][d6][check]
 *    e.g.  A B 1 2 3 4 5 6 3
 * </pre>
 *
 * <h3>Check-digit algorithm</h3>
 * <ol>
 *   <li>sum1 = d1 + d3 + d5</li>
 *   <li>sum2 = (d2 + d4 + d6) × 2</li>
 *   <li>total = sum1 + sum2</li>
 *   <li>check == last digit of total  (i.e. total % 10)</li>
 * </ol>
 * Example: {@code AB1234563} → sum1=1+3+5=9, sum2=(2+4+6)×2=24, total=33, check=3 ✓
 *
 * <h3>Registrant-type characters (first letter)</h3>
 * The first letter encodes the registrant category.  The DEA currently issues the
 * following types; any other first letter is rejected:
 * A, B, C, D, E — practitioners (hospitals, physicians, etc.)
 * F — practitioners in training
 * G — non-practitioners (importers / exporters)
 * H — (retired prefix, still encountered in legacy records)
 * M — mid-level practitioners (nurse practitioners, physician assistants)
 * P — researchers / practitioners
 * R — researchers / non-practitioners
 * S — narcotic treatment programs
 * T — compound pharmacies / automated dispensing
 * U — automated dispensing systems
 * X — DATA-waivered practitioners (buprenorphine / Suboxone)
 */
@Component
public class DeaNumberValidator {

    /**
     * Pattern: exactly 2 alphabetic characters followed by exactly 7 digits.
     * Matching is done on the normalised (upper-cased, whitespace-stripped) input.
     */
    private static final Pattern DEA_PATTERN = Pattern.compile("^[A-Z]{2}\\d{7}$");

    /**
     * DEA registrant-type codes currently in use.
     * Any first character not in this set is an invalid registrant type.
     */
    private static final Set<Character> VALID_REGISTRANT_TYPES = Set.of(
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'M', 'P', 'R', 'S', 'T', 'U', 'X'
    );

    /**
     * Returns {@code true} when the DEA number passes all three checks:
     * format, registrant-type, and check-digit.
     *
     * <p>Input is normalised (trimmed, upper-cased) before validation so that
     * "ab1234563" and " AB1234563 " are both accepted as valid.
     *
     * @param deaNumber raw DEA number as supplied by the user
     */
    public boolean isValid(String deaNumber) {
        return validationMessage(deaNumber) == null;
    }

    /**
     * Returns {@code true} when the DEA number passes all four checks:
     * format, registrant-type, check-digit, and second-letter-matches-last-name (FR-P1.10).
     *
     * @param deaNumber          raw DEA number as supplied by the user
     * @param prescriberLastName prescriber's last name; if blank the second-letter check is skipped
     */
    public boolean isValid(String deaNumber, String prescriberLastName) {
        return validationMessage(deaNumber, prescriberLastName) == null;
    }

    /**
     * Returns a human-readable explanation of exactly why the DEA number is invalid,
     * or {@code null} if it is valid.
     *
     * <p>The returned message is suitable for inclusion in a validation error list
     * shown to a clinician, so it names the specific rule that failed.
     *
     * @param deaNumber raw DEA number as supplied by the user
     * @return error message, or {@code null} when valid
     */
    public String validationMessage(String deaNumber) {
        return validationMessage(deaNumber, null);
    }

    /**
     * Validates the DEA number against all four rules (FR-P1.10):
     * <ol>
     *   <li>Format: 2 letters + 7 digits</li>
     *   <li>Registrant-type code (first letter)</li>
     *   <li>Check-digit algorithm</li>
     *   <li>Second letter must match the first letter of the prescriber's last name
     *       (case-insensitive). Skipped when {@code prescriberLastName} is null or blank.</li>
     * </ol>
     *
     * @param deaNumber          raw DEA number as supplied by the user
     * @param prescriberLastName prescriber's last name; if null/blank rule 4 is skipped
     * @return error message, or {@code null} when valid
     */
    public String validationMessage(String deaNumber, String prescriberLastName) {
        if (deaNumber == null || deaNumber.isBlank()) {
            return "DEA number is required";
        }

        String normalized = deaNumber.trim().toUpperCase();

        if (!DEA_PATTERN.matcher(normalized).matches()) {
            return "DEA number must be 2 letters followed by 7 digits (e.g. AB1234563); got: '"
                    + deaNumber.trim() + "'";
        }

        char registrantType = normalized.charAt(0);
        if (!VALID_REGISTRANT_TYPES.contains(registrantType)) {
            return "DEA number has an invalid registrant-type code '" + registrantType
                    + "'. Valid codes: A B C D E F G H M P R S T U X";
        }

        if (!checkDigitValid(normalized)) {
            int[] digits = extractDigits(normalized);
            int expected = computeCheckDigit(digits);
            return "DEA number has an invalid check digit (expected " + expected
                    + " at position 9, got " + digits[6] + "); "
                    + "please verify the number with the prescriber's DEA certificate";
        }

        // FR-P1.10: second letter must match the DEA-registered initial derived from the
        // prescriber's last name (case-insensitive).  Handles common patterns per DEA guidance:
        // hyphenated surnames (first segment), Irish/Italian apostrophe forms (O'Brien, D'Angelo),
        // and plain single-token last names.  Multi-word surnames without a hyphen (e.g. "Van Halen"
        // stored as last token "Halen") may not match — the prescriber record should use the legal
        // surname as on the DEA certificate.
        if (prescriberLastName != null && !prescriberLastName.isBlank()) {
            char deaSecond = normalized.charAt(1);
            char expected = expectedSecondLetterFromLastName(prescriberLastName);
            if (expected != 0 && deaSecond != expected) {
                return "DEA number second letter ('" + deaSecond + "') does not match the expected "
                        + "initial from the prescriber's last name ('" + expected + "' from '"
                        + prescriberLastName.trim() + "'); "
                        + "please verify the DEA number against the prescriber's DEA certificate";
            }
        }

        return null;
    }

    /**
     * Returns the upper-case letter the DEA uses as the second character for an individual
     * registrant's surname, or {@code 0} when {@code lastName} is null/blank and the check
     * should be skipped by the caller.
     */
    static char expectedSecondLetterFromLastName(String lastName) {
        if (lastName == null || lastName.isBlank()) {
            return 0;
        }
        String t = lastName.trim();
        if (t.isEmpty()) {
            return 0;
        }
        // Hyphenated: DEA uses the first letter of the first surname (e.g. Smith-Jones → S).
        int hy = t.indexOf('-');
        if (hy > 0) {
            t = t.substring(0, hy).trim();
        }
        if (t.isEmpty()) {
            return 0;
        }
        // Apostrophe surnames: O'Brien, D'Angelo — initial is the letter before the apostrophe.
        int ap = t.indexOf('\'');
        if (ap > 0) {
            return Character.toUpperCase(t.charAt(0));
        }
        return Character.toUpperCase(t.charAt(0));
    }

    // ---------------------------------------------------------------------------
    // Internals
    // ---------------------------------------------------------------------------

    private boolean checkDigitValid(String normalized) {
        int[] digits = extractDigits(normalized);
        return computeCheckDigit(digits) == digits[6];
    }

    /**
     * Extracts the 7 numeric digit characters (index 2..8) as an int array.
     * Assumes the string is already normalised and matches {@link #DEA_PATTERN}.
     */
    private static int[] extractDigits(String normalized) {
        int[] d = new int[7];
        for (int i = 0; i < 7; i++) {
            d[i] = normalized.charAt(i + 2) - '0';
        }
        return d;
    }

    /**
     * Computes the expected check digit (last digit of total) per the DEA algorithm.
     * d[0]..d[5] are the first six numeric digits; d[6] is the check digit to verify.
     */
    private static int computeCheckDigit(int[] d) {
        int sum1 = d[0] + d[2] + d[4];           // odd positions (1, 3, 5)
        int sum2 = (d[1] + d[3] + d[5]) * 2;     // even positions (2, 4, 6) × 2
        return (sum1 + sum2) % 10;
    }
}
