package com.easyops.hospital.util;

import com.easyops.hospital.dto.request.EmergencyContactRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.regex.Pattern;

/**
 * Server-side checks aligned with the EHR emergency-contact UI (digits-only phones, optional email).
 */
public final class EmergencyContactRequestValidation {

    private static final int MAX_PHONE_DIGITS = 15;
    private static final Pattern DIGITS_ONLY = Pattern.compile("^[0-9]{1," + MAX_PHONE_DIGITS + "}$");
    /** Same rules as frontend {@code isValidOptionalEmail}: domain must contain a dot; TLD length ≥ 2. */
    private static final Pattern EMAIL_BASE = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$");
    private static final Pattern TLD_ALNUM = Pattern.compile("^[a-zA-Z0-9]+$");

    private EmergencyContactRequestValidation() {
    }

    static boolean isValidOptionalEmail(String email) {
        if (email == null) {
            return true;
        }
        String s = email.trim();
        if (s.isEmpty()) {
            return true;
        }
        if (!EMAIL_BASE.matcher(s).matches()) {
            return false;
        }
        int at = s.indexOf('@');
        String domain = s.substring(at + 1);
        int lastDot = domain.lastIndexOf('.');
        if (lastDot <= 0 || lastDot >= domain.length() - 1) {
            return false;
        }
        String tld = domain.substring(lastDot + 1);
        return tld.length() >= 2 && TLD_ALNUM.matcher(tld).matches();
    }

    public static void assertValid(EmergencyContactRequest request) {
        if (request.getPrimaryPhone() == null || request.getPrimaryPhone().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Primary phone is required.");
        }
        String primary = request.getPrimaryPhone().trim();
        if (!DIGITS_ONLY.matcher(primary).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Primary phone must contain digits only (up to " + MAX_PHONE_DIGITS + " digits).");
        }
        if (request.getSecondaryPhone() != null && !request.getSecondaryPhone().isBlank()) {
            String sec = request.getSecondaryPhone().trim();
            if (!DIGITS_ONLY.matcher(sec).matches()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Secondary phone must contain digits only (up to " + MAX_PHONE_DIGITS + " digits), or be omitted.");
            }
        }
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            if (!isValidOptionalEmail(request.getEmail())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Please provide a valid email address.");
            }
        }
    }
}
