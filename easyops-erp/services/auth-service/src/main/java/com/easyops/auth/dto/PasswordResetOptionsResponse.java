package com.easyops.auth.dto;

import java.util.Collections;
import java.util.List;

/**
 * Available OTP channels for the resolved account (no enumeration across unknown identifiers).
 */
public class PasswordResetOptionsResponse {

    private boolean eligible;
    private List<String> channels = Collections.emptyList();
    private String maskedEmail;
    private String maskedPhone;

    public PasswordResetOptionsResponse() {
    }

    public PasswordResetOptionsResponse(boolean eligible, List<String> channels,
                                       String maskedEmail, String maskedPhone) {
        this.eligible = eligible;
        this.channels = channels != null ? channels : Collections.emptyList();
        this.maskedEmail = maskedEmail;
        this.maskedPhone = maskedPhone;
    }

    public static PasswordResetOptionsResponse notEligible() {
        return new PasswordResetOptionsResponse(false, Collections.emptyList(), null, null);
    }

    public boolean isEligible() {
        return eligible;
    }

    public void setEligible(boolean eligible) {
        this.eligible = eligible;
    }

    public List<String> getChannels() {
        return channels;
    }

    public void setChannels(List<String> channels) {
        this.channels = channels != null ? channels : Collections.emptyList();
    }

    public String getMaskedEmail() {
        return maskedEmail;
    }

    public void setMaskedEmail(String maskedEmail) {
        this.maskedEmail = maskedEmail;
    }

    public String getMaskedPhone() {
        return maskedPhone;
    }

    public void setMaskedPhone(String maskedPhone) {
        this.maskedPhone = maskedPhone;
    }
}
