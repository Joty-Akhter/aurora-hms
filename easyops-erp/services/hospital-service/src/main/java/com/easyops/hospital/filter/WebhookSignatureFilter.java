package com.easyops.hospital.filter;

import com.easyops.hospital.entity.WebhookIntegration;
import com.easyops.hospital.repository.WebhookIntegrationRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.UUID;

/**
 * FR-P3.11a — Validates the HMAC-SHA256 signature on inbound pharmacy fill-status webhooks.
 *
 * <h3>How it works</h3>
 * <ol>
 *   <li>Applies only to {@code POST /api/prescriptions/transmissions/fill-status}.</li>
 *   <li>The caller must include {@code X-Webhook-Signature: sha256=<hex>} computed as
 *       {@code HMAC-SHA256(sharedSecret, rawRequestBody)}.</li>
 *   <li>Optionally the caller includes {@code X-Webhook-Integration-Id: <UUID>} to select a
 *       per-integration secret from {@code ehr.webhook_integrations}. When the header is absent
 *       the filter falls back to the {@code "default"} integration row.</li>
 *   <li>Comparison is constant-time ({@link MessageDigest#isEqual}) to prevent timing attacks.</li>
 *   <li>On failure the filter returns HTTP 401 with a generic JSON body containing no PHI.</li>
 * </ol>
 *
 * <h3>Disabling for local dev</h3>
 * Set {@code webhook.fill-status.auth.enabled=false} to bypass the check (never use in production).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@RequiredArgsConstructor
@Slf4j
public class WebhookSignatureFilter extends OncePerRequestFilter {

    private static final String FILL_STATUS_PATH_SUFFIX = "/transmissions/fill-status";
    private static final String SIGNATURE_HEADER       = "X-Webhook-Signature";
    private static final String INTEGRATION_ID_HEADER  = "X-Webhook-Integration-Id";
    private static final String SHA256_PREFIX          = "sha256=";
    private static final String HMAC_ALGORITHM         = "HmacSHA256";
    private static final String DEFAULT_INTEGRATION    = "default";

    private final WebhookIntegrationRepository integrationRepository;

    @Value("${webhook.fill-status.auth.enabled:true}")
    private boolean webhookAuthEnabled;

    // -------------------------------------------------------------------------
    // Filter scope
    // -------------------------------------------------------------------------

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equalsIgnoreCase(request.getMethod())
                || !request.getRequestURI().endsWith(FILL_STATUS_PATH_SUFFIX);
    }

    // -------------------------------------------------------------------------
    // Core logic
    // -------------------------------------------------------------------------

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        if (!webhookAuthEnabled) {
            log.warn("Webhook HMAC auth is DISABLED — fill-status endpoint is unprotected. "
                   + "Set webhook.fill-status.auth.enabled=true in production.");
            chain.doFilter(request, response);
            return;
        }

        // 1. Cache the body so the controller can still read it after we consume the stream.
        CachedBodyHttpServletRequestWrapper cachedRequest =
                new CachedBodyHttpServletRequestWrapper(request);

        // 2. Resolve the shared secret for this integration.
        String integrationIdHeader = request.getHeader(INTEGRATION_ID_HEADER);
        String secret = resolveSecret(integrationIdHeader, request.getRemoteAddr());

        if (secret == null || secret.isBlank()
                || "REPLACE_WITH_RANDOM_SECRET_BEFORE_GO_LIVE".equals(secret)) {
            log.warn("Fill-status webhook: no usable secret for integration '{}' from {}. "
                   + "Rejecting with 401.", integrationIdHeader, request.getRemoteAddr());
            sendUnauthorized(response, "No webhook integration secret configured");
            return;
        }

        // 3. Validate the X-Webhook-Signature header.
        String signatureHeader = request.getHeader(SIGNATURE_HEADER);
        if (signatureHeader == null || !signatureHeader.startsWith(SHA256_PREFIX)) {
            log.warn("Fill-status webhook: missing or malformed {} header from integration '{}' at {}.",
                    SIGNATURE_HEADER, integrationIdHeader, request.getRemoteAddr());
            sendUnauthorized(response, "Missing or malformed " + SIGNATURE_HEADER + " header");
            return;
        }

        String providedHex  = signatureHeader.substring(SHA256_PREFIX.length());
        byte[] body         = cachedRequest.getCachedBody();
        String computedHex  = computeHmacSha256Hex(secret, body);

        byte[] providedBytes  = hexToBytesSafe(providedHex);
        byte[] computedBytes  = hexToBytesSafe(computedHex);

        if (providedBytes == null || !MessageDigest.isEqual(providedBytes, computedBytes)) {
            log.warn("Fill-status webhook: HMAC signature mismatch from integration '{}' at {}. "
                   + "Expected sha256={} got sha256={}",
                    integrationIdHeader, request.getRemoteAddr(), computedHex, providedHex);
            sendUnauthorized(response, "Invalid webhook signature");
            return;
        }

        log.debug("Fill-status webhook signature validated for integration '{}' from {}.",
                integrationIdHeader, request.getRemoteAddr());
        chain.doFilter(cachedRequest, response);
    }

    // -------------------------------------------------------------------------
    // Secret resolution
    // -------------------------------------------------------------------------

    /**
     * Looks up the shared secret for the integration identified by {@code integrationIdHeader}.
     * If the header is absent or blank, falls back to the {@code "default"} integration row.
     * Returns {@code null} when no active integration is found.
     */
    private String resolveSecret(String integrationIdHeader, String callerIp) {
        Optional<WebhookIntegration> integration;

        if (integrationIdHeader != null && !integrationIdHeader.isBlank()) {
            UUID id;
            try {
                id = UUID.fromString(integrationIdHeader);
            } catch (IllegalArgumentException e) {
                log.warn("Fill-status webhook: invalid UUID in {} header: '{}' from {}",
                        INTEGRATION_ID_HEADER, integrationIdHeader, callerIp);
                return null;
            }
            integration = integrationRepository.findByIntegrationIdAndIsActiveTrue(id);
            if (integration.isEmpty()) {
                log.warn("Fill-status webhook: no active integration found for id={} from {}",
                        id, callerIp);
                return null;
            }
        } else {
            integration = integrationRepository.findByIntegrationNameAndIsActiveTrue(DEFAULT_INTEGRATION);
            if (integration.isEmpty()) {
                log.warn("Fill-status webhook: no active '{}' integration row found.", DEFAULT_INTEGRATION);
                return null;
            }
        }

        return integration.get().getSecret();
    }

    // -------------------------------------------------------------------------
    // HMAC helpers
    // -------------------------------------------------------------------------

    private static String computeHmacSha256Hex(String secret, byte[] data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return bytesToHex(mac.doFinal(data));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("HMAC-SHA256 computation failed", e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * Converts a hex string to bytes. Returns {@code null} on malformed input so the
     * caller can reject the request rather than throw an exception mid-filter.
     */
    private static byte[] hexToBytesSafe(String hex) {
        if (hex == null || hex.length() % 2 != 0) return null;
        try {
            byte[] data = new byte[hex.length() / 2];
            for (int i = 0; i < hex.length(); i += 2) {
                int hi = Character.digit(hex.charAt(i), 16);
                int lo = Character.digit(hex.charAt(i + 1), 16);
                if (hi < 0 || lo < 0) return null;
                data[i / 2] = (byte) ((hi << 4) + lo);
            }
            return data;
        } catch (Exception e) {
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // Response helpers
    // -------------------------------------------------------------------------

    private static void sendUnauthorized(HttpServletResponse response, String message)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        // Deliberately generic — no PHI, no stack trace.
        response.getWriter().write(
                "{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"" + message + "\"}");
    }
}
