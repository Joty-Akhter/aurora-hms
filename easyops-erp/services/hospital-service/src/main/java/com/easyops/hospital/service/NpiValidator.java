package com.easyops.hospital.service;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Validates U.S. National Provider Identifiers (NPI) per FR-P3.x.
 *
 * <h3>NPI format</h3>
 * An NPI is exactly 10 decimal digits.  The final digit is a Luhn check digit
 * computed over the 9 payload digits plus a CMS-issued ISO Issuer ID prefix.
 *
 * <h3>Luhn algorithm (CMS variant)</h3>
 * <ol>
 *   <li>Prepend the CMS ISO Issuer ID {@code "80840"} to the 10-digit NPI to form a
 *       15-digit working string.</li>
 *   <li>Starting from the rightmost digit and moving left, double every second digit
 *       (i.e. even-indexed positions when counting from the right, 1-based).</li>
 *   <li>If doubling produces a value &gt; 9, subtract 9.</li>
 *   <li>Sum all resulting digits.  The sum must be divisible by 10.</li>
 * </ol>
 *
 * <p>Reference: CMS National Plan and Provider Enumeration System (NPPES),
 * <em>NPI: What You Need to Know</em>, MLN Matters article SE0927.
 *
 * <h3>Why this matters</h3>
 * The prescriber NPI is placed in the NCPDP SCRIPT {@code <NPI>} element and the
 * {@code <From>} header.  Surescripts validates the Luhn checksum at the network
 * level and rejects messages with an invalid NPI.  Format-only validation (10 digits)
 * lets transcription errors through; Luhn validation catches single-digit typos and
 * most transposition errors before the message leaves the system.
 */
@Component
public class NpiValidator {

    private static final Pattern NPI_PATTERN = Pattern.compile("^\\d{10}$");

    /**
     * The CMS ISO Issuer ID prepended before Luhn computation (per NPPES specification).
     * All Type-1 and Type-2 NPIs use this prefix.
     */
    private static final String CMS_ISSUER_PREFIX = "80840";

    // ---------------------------------------------------------------------------
    // Public API (mirrors DeaNumberValidator contract)
    // ---------------------------------------------------------------------------

    /**
     * Returns {@code true} when the NPI passes format and Luhn checksum validation.
     *
     * @param npi raw NPI as supplied (leading/trailing whitespace is stripped)
     */
    public boolean isValid(String npi) {
        return validationMessage(npi) == null;
    }

    /**
     * Returns a human-readable explanation of exactly why the NPI is invalid,
     * or {@code null} if it is valid.
     *
     * <p>The returned message is suitable for inclusion in a validation error list
     * shown to a clinician or logged as a pre-flight error in the NCPDP builder.
     *
     * @param npi raw NPI as supplied by the user or system
     * @return error message, or {@code null} when valid
     */
    public String validationMessage(String npi) {
        if (npi == null || npi.isBlank()) {
            return "NPI is required";
        }

        String normalized = npi.trim();

        if (!NPI_PATTERN.matcher(normalized).matches()) {
            return "NPI must be exactly 10 digits (e.g. 1234567893); got: '" + normalized + "'";
        }

        if (!luhnValid(CMS_ISSUER_PREFIX + normalized)) {
            return "NPI '" + normalized + "' has an invalid Luhn check digit — "
                    + "please verify the NPI at https://npiregistry.cms.hhs.gov/";
        }

        return null;
    }

    // ---------------------------------------------------------------------------
    // Internals
    // ---------------------------------------------------------------------------

    /**
     * Applies the standard Luhn algorithm to a string of decimal digits.
     *
     * <p>Iterates from right to left; every second digit (even position, 1-based from
     * the right) is doubled, and if the result exceeds 9, 9 is subtracted.
     * Returns {@code true} if the total sum is divisible by 10.
     *
     * <p>Package-private for unit testing.
     *
     * @param digits string of decimal digit characters (no spaces or separators)
     */
    static boolean luhnValid(String digits) {
        int sum = 0;
        boolean doubleIt = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int d = digits.charAt(i) - '0';
            if (doubleIt) {
                d *= 2;
                if (d > 9) d -= 9;
            }
            sum += d;
            doubleIt = !doubleIt;
        }
        return sum % 10 == 0;
    }
}
