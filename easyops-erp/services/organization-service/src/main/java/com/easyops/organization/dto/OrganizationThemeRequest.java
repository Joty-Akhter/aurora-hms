package com.easyops.organization.dto;

import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Theme / branding update request — used by PATCH /api/organizations/{id}/theme.
 * All fields are optional; only non-null values are applied.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationThemeRequest {

    @Pattern(regexp = "^(light|dark)$", message = "themeMode must be 'light' or 'dark'")
    private String themeMode;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "themePrimaryColor must be a 6-digit hex color (e.g. #2563eb)")
    private String themePrimaryColor;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "themeSecondaryColor must be a 6-digit hex color")
    private String themeSecondaryColor;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "themeAccentColor must be a 6-digit hex color")
    private String themeAccentColor;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "themeSidebarColor must be a 6-digit hex color")
    private String themeSidebarColor;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "themeSidebarTextColor must be a 6-digit hex color")
    private String themeSidebarTextColor;
}
